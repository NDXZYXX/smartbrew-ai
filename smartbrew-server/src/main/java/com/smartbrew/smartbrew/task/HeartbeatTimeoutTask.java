package com.smartbrew.smartbrew.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartbrew.smartbrew.entity.Device;
import com.smartbrew.smartbrew.mapper.DeviceMapper;
import com.smartbrew.smartbrew.service.AlarmService;
import com.smartbrew.smartbrew.service.DeviceEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 心跳超时检测定时任务
 * 每 60 秒扫描一次：last_heartbeat_time 超过 120 秒未更新的设备 → 标记为离线
 */
@Component
public class HeartbeatTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatTimeoutTask.class);

    private final DeviceMapper deviceMapper;
    private final DeviceEventService eventService;
    private final AlarmService alarmService;

    public HeartbeatTimeoutTask(DeviceMapper deviceMapper, DeviceEventService eventService,
                                 AlarmService alarmService) {
        this.deviceMapper = deviceMapper;
        this.eventService = eventService;
        this.alarmService = alarmService;
    }

    @Scheduled(fixedRate = 60000) // 每 60 秒执行
    public void checkOfflineDevices() {
        // 查找"在线"但心跳超时的设备
        LocalDateTime timeout = LocalDateTime.now().minusSeconds(120);

        List<Device> onlineDevices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getStatus, 1)
                        .and(w -> w
                                .lt(Device::getLastHeartbeatTime, timeout)
                                .or()
                                .isNull(Device::getLastHeartbeatTime)));

        for (Device device : onlineDevices) {
            log.info("设备离线: device={} lastHeartbeat={}", device.getDeviceId(), device.getLastHeartbeatTime());

            // 更新为离线
            device.setStatus(0);
            deviceMapper.updateById(device);

            // 记录离线事件
            eventService.record(device.getDeviceId(), "DEVICE_OFFLINE",
                    "WARN", "设备离线",
                    "心跳超时（>120s），设备自动标记为离线", null);

            // 产生离线告警
            alarmService.checkOffline(device.getDeviceId());
        }

        if (!onlineDevices.isEmpty()) {
            log.info("本轮标记离线设备数: {}", onlineDevices.size());
        }
    }
}
