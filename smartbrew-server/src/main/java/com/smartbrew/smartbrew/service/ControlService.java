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

        long timestamp = System.currentTimeMillis();
        String mqttMsgId = null;

        try {
            mqttMsgId = mqttService.publishControl(deviceId, target, command, timestamp);
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
}
