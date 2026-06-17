package com.smartbrew.smartbrew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartbrew.smartbrew.entity.SensorData;
import com.smartbrew.smartbrew.entity.SystemConfig;
import com.smartbrew.smartbrew.mapper.SystemConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 自动温控规则引擎
 * 带滞后的双阈值控制：
 *   - 风扇：温度 &gt; fanOn 开启，&lt; fanOff 关闭（默认 28°C/26°C，2°C 死区）
 *   - 加热：温度 &lt; heaterOn 开启，&gt; heaterOff 关闭（默认 18°C/22°C，4°C 死区）
 */
@Service
public class TemperatureControlService {

    private static final Logger log = LoggerFactory.getLogger(TemperatureControlService.class);

    private static final String REDIS_KEY_PREFIX = "device:ctrl:";

    // 默认阈值
    private static final BigDecimal DEFAULT_ENABLED     = BigDecimal.ONE;
    private static final BigDecimal DEFAULT_HEATER_ON   = new BigDecimal("18.00");
    private static final BigDecimal DEFAULT_HEATER_OFF  = new BigDecimal("22.00");
    private static final BigDecimal DEFAULT_FAN_ON      = new BigDecimal("28.00");
    private static final BigDecimal DEFAULT_FAN_OFF     = new BigDecimal("26.00");

    private final SystemConfigMapper systemConfigMapper;
    private final ControlService controlService;
    private final DeviceEventService eventService;
    private final RedisTemplate<String, Object> redisTemplate;

    public TemperatureControlService(SystemConfigMapper systemConfigMapper,
                                     @Lazy ControlService controlService,
                                     DeviceEventService eventService,
                                     RedisTemplate<String, Object> redisTemplate) {
        this.systemConfigMapper = systemConfigMapper;
        this.controlService = controlService;
        this.eventService = eventService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 评估温度并自动执行控制
     */
    public void evaluate(String deviceId, SensorData data) {
        BigDecimal tankTemp = data.getTankTemperature();
        if (tankTemp == null) return;

        // 检查自动温控总开关
        if (!isEnabled()) {
            return;
        }

        BigDecimal fanOn    = getConfigAsDecimal("temp.control.fan-on",    DEFAULT_FAN_ON);
        BigDecimal fanOff   = getConfigAsDecimal("temp.control.fan-off",   DEFAULT_FAN_OFF);
        BigDecimal heaterOn = getConfigAsDecimal("temp.control.heater-on", DEFAULT_HEATER_ON);
        BigDecimal heaterOff= getConfigAsDecimal("temp.control.heater-off", DEFAULT_HEATER_OFF);

        log.debug("温控评估: device={} temp={} fan={}-{} heater={}-{}",
                deviceId, tankTemp, fanOff, fanOn, heaterOn, heaterOff);

        // --- 风扇控制（滞后：> fanOn 开，< fanOff 关） ---
        if (tankTemp.compareTo(fanOn) > 0) {
            sendIfChanged(deviceId, "FAN", "ON", tankTemp, fanOn, fanOff, "FAN", "HEATER");
        } else if (tankTemp.compareTo(fanOff) < 0) {
            sendIfChanged(deviceId, "FAN", "OFF", tankTemp, fanOn, fanOff, "FAN", "HEATER");
        }

        // --- 加热控制（滞后：< heaterOn 开，> heaterOff 关） ---
        if (tankTemp.compareTo(heaterOn) < 0) {
            sendIfChanged(deviceId, "HEATER", "ON", tankTemp, heaterOff, heaterOn, "HEATER", "FAN");
        } else if (tankTemp.compareTo(heaterOff) > 0) {
            sendIfChanged(deviceId, "HEATER", "OFF", tankTemp, heaterOff, heaterOn, "HEATER", "FAN");
        }
    }

    /**
     * 检查状态是否变化，避免重复下发相同指令
     * @param deviceId   设备ID
     * @param target     控制目标
     * @param command    指令
     * @param temp       当前温度
     * @param offThresh  关闭阈值
     * @param onThresh   开启阈值
     * @param primary    主控目标（如 FAN）
     * @param secondary  互斥目标（如 HEATER）
     */
    private void sendIfChanged(String deviceId, String target, String command,
                               BigDecimal temp, BigDecimal offThresh, BigDecimal onThresh,
                               String primary, String secondary) {
        String redisKey = REDIS_KEY_PREFIX + deviceId;
        String prevState = (String) redisTemplate.opsForHash().get(redisKey, target);

        if (command.equals(prevState)) {
            log.debug("温控指令重复，跳过: device={} target={} command={}", deviceId, target, command);
            return;
        }

        // 如果是开启指令，确保互斥设备关闭
        if ("ON".equals(command)) {
            String oppState = (String) redisTemplate.opsForHash().get(redisKey, secondary);
            if ("ON".equals(oppState)) {
                log.debug("温控互斥，先关闭{}再开启{}: device={}", secondary, target, deviceId);
                redisTemplate.opsForHash().put(redisKey, secondary, "OFF");
            }
        }

        // 更新 Redis 状态
        redisTemplate.opsForHash().put(redisKey, target, command);

        // 构造触发原因
        String reason = String.format("桶内温度 %.2f℃ %s %s阈值 %.2f℃（滞后: %s %.2f℃ / %s %.2f℃）",
                temp,
                "ON".equals(command) ? "超过" : "低于",
                target,
                "ON".equals(command) ? onThresh : offThresh,
                "ON".equals(command) ? "关" : "开",
                offThresh,
                "ON".equals(command) ? "开" : "关",
                onThresh);

        controlService.sendAutoControl(deviceId, target, command, reason);

        // 记录设备事件
        String action = "ON".equals(command) ? "开启" : "关闭";
        String name = "FAN".equals(target) ? "风扇" : "加热";
        eventService.record(deviceId, "AUTO_CTRL", "INFO",
                "自动" + action + name, reason, null);
    }

    private boolean isEnabled() {
        BigDecimal enabled = getConfigAsDecimal("temp.control.enabled", DEFAULT_ENABLED);
        return BigDecimal.ONE.compareTo(enabled) == 0;
    }

    private BigDecimal getConfigAsDecimal(String key, BigDecimal defaultValue) {
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigKey, key));
        if (config != null && config.getConfigValue() != null) {
            try {
                return new BigDecimal(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("配置 {} 值格式错误: {}，使用默认值 {}", key, config.getConfigValue(), defaultValue);
            }
        }
        return defaultValue;
    }
}
