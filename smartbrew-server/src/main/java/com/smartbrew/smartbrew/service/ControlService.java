package com.smartbrew.smartbrew.service;

import com.smartbrew.smartbrew.entity.Device;
import com.smartbrew.smartbrew.entity.DeviceControlLog;
import com.smartbrew.smartbrew.mapper.DeviceControlLogMapper;
import com.smartbrew.smartbrew.mapper.DeviceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ControlService {

    private static final Logger log = LoggerFactory.getLogger(ControlService.class);

    private final MqttSubscriberService mqttService;
    private final DeviceControlLogMapper controlLogMapper;
    private final DeviceMapper deviceMapper;

    public ControlService(MqttSubscriberService mqttService,
                          DeviceControlLogMapper controlLogMapper,
                          DeviceMapper deviceMapper) {
        this.mqttService = mqttService;
        this.controlLogMapper = controlLogMapper;
        this.deviceMapper = deviceMapper;
    }

    /**
     * 向设备下发控制指令（手动）
     */
    public void sendControl(String deviceId, String target, String command) {
        // 校验设备是否存在
        Device device = deviceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceId, deviceId));
        if (device == null) {
            throw new IllegalArgumentException("设备不存在: " + deviceId);
        }

        // 校验设备是否在线
        if (device.getStatus() != 1) {
            throw new IllegalStateException("设备不在线，无法下发指令: " + deviceId);
        }

        String mqttMsgId = null;

        try {
            mqttMsgId = mqttService.publishControl(deviceId, target, command);
        } catch (Exception e) {
            log.error("MQTT控制指令下发失败: device={} target={} command={}", deviceId, target, command, e);
        }

        // 写入控制日志
        DeviceControlLog logRecord = new DeviceControlLog();
        logRecord.setDeviceId(deviceId);
        logRecord.setControlTarget(target);
        logRecord.setCommand(command);
        logRecord.setTriggerSource("MANUAL");
        logRecord.setTriggerReason(String.format("用户手动%s%s", "ON".equals(command) ? "开启" : "关闭",
                "FAN".equals(target) ? "风扇" : "加热"));
        logRecord.setMqttMsgId(mqttMsgId);
        logRecord.setExecuteStatus(mqttMsgId != null ? 0 : 3); // 0-已下发, 3-失败

        controlLogMapper.insert(logRecord);
        log.info("控制指令已记录: device={} target={} command={} msgId={}", deviceId, target, command, mqttMsgId);
    }

    /**
     * 自动温控下发的控制指令
     * 与 sendControl() 的区别：
     *   - 不抛出异常（优雅降级，仅记录日志）
     *   - triggerSource 设为 "AUTO"
     *   - triggerReason 包含实际温度值和阈值的详细信息
     *   - 跳过在线状态检查（离线时仍记录日志，executeStatus=3）
     */
    public void sendAutoControl(String deviceId, String target, String command, String reason) {
        String mqttMsgId = null;
        Integer executeStatus;

        try {
            mqttMsgId = mqttService.publishControl(deviceId, target, command);
            executeStatus = 0;
        } catch (Exception e) {
            log.warn("自动温控MQTT下发失败: device={} target={} command={}", deviceId, target, command, e);
            executeStatus = 3;
        }

        DeviceControlLog logRecord = new DeviceControlLog();
        logRecord.setDeviceId(deviceId);
        logRecord.setControlTarget(target);
        logRecord.setCommand(command);
        logRecord.setTriggerSource("AUTO");
        logRecord.setTriggerReason(reason);
        logRecord.setMqttMsgId(mqttMsgId);
        logRecord.setExecuteStatus(executeStatus);

        controlLogMapper.insert(logRecord);
        log.info("自动温控已记录: device={} target={} command={} status={} reason={}",
                deviceId, target, command, executeStatus, reason);
    }
}
