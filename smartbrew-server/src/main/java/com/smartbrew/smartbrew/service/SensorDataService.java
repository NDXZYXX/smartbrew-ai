package com.smartbrew.smartbrew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartbrew.smartbrew.dto.SensorDataVO;
import com.smartbrew.smartbrew.entity.SensorData;
import com.smartbrew.smartbrew.mapper.SensorDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class SensorDataService {

    private static final Logger log = LoggerFactory.getLogger(SensorDataService.class);

    private final SensorDataMapper sensorDataMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public SensorDataService(SensorDataMapper sensorDataMapper,
                             RedisTemplate<String, Object> redisTemplate) {
        this.sensorDataMapper = sensorDataMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取设备最新传感器数据（优先 Redis，无数据时 fallback 到 DB）
     */
    public SensorDataVO getLatest(String deviceId) {
        String key = "device:latest:" + deviceId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries != null && !entries.isEmpty()) {
            SensorDataVO vo = new SensorDataVO();
            vo.setDeviceId(deviceId);
            if (entries.containsKey("tank_temp")) {
                vo.setTankTemperature(new BigDecimal(entries.get("tank_temp").toString()));
            }
            if (entries.containsKey("env_temp")) {
                vo.setEnvTemperature(new BigDecimal(entries.get("env_temp").toString()));
            }
            if (entries.containsKey("env_humidity")) {
                vo.setEnvHumidity(new BigDecimal(entries.get("env_humidity").toString()));
            }
            if (entries.containsKey("create_time")) {
                vo.setCreateTime(LocalDateTime.parse(entries.get("create_time").toString(),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            return vo;
        }

        // Redis 无数据，fallback 到 DB
        log.debug("Redis 中无设备 {} 的最新数据，fallback 到 DB", deviceId);
        SensorData latest = sensorDataMapper.selectOne(
                new LambdaQueryWrapper<SensorData>()
                        .eq(SensorData::getDeviceId, deviceId)
                        .orderByDesc(SensorData::getCreateTime)
                        .last("LIMIT 1"));
        if (latest != null) {
            return toVO(latest);
        }
        return null;
    }

    /**
     * 查询设备历史传感器数据（分页）
     */
    public Page<SensorDataVO> getHistory(String deviceId, LocalDateTime start, LocalDateTime end,
                                          int pageNum, int pageSize) {
        LambdaQueryWrapper<SensorData> wrapper = new LambdaQueryWrapper<SensorData>()
                .eq(SensorData::getDeviceId, deviceId)
                .between(SensorData::getCreateTime, start, end)
                .orderByDesc(SensorData::getCreateTime);

        Page<SensorData> page = sensorDataMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        Page<SensorDataVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(this::toVO)
                .toList());
        return voPage;
    }

    private SensorDataVO toVO(SensorData data) {
        SensorDataVO vo = new SensorDataVO();
        vo.setDeviceId(data.getDeviceId());
        vo.setTankTemperature(data.getTankTemperature());
        vo.setEnvTemperature(data.getEnvTemperature());
        vo.setEnvHumidity(data.getEnvHumidity());
        vo.setCreateTime(data.getCreateTime());
        return vo;
    }
}
