package com.smartbrew.smartbrew.service;

import com.smartbrew.smartbrew.entity.DeviceEvent;
import com.smartbrew.smartbrew.mapper.DeviceEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeviceEventService {

    private static final Logger log = LoggerFactory.getLogger(DeviceEventService.class);

    private final DeviceEventMapper eventMapper;

    public DeviceEventService(DeviceEventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    /**
     * 记录设备事件
     */
    public void record(String deviceId, String eventType, String eventLevel,
                       String eventTitle, String eventDetail, Long referenceId) {
        DeviceEvent event = new DeviceEvent();
        event.setDeviceId(deviceId);
        event.setEventType(eventType);
        event.setEventLevel(eventLevel);
        event.setEventTitle(eventTitle);
        event.setEventDetail(eventDetail);
        event.setReferenceId(referenceId);
        eventMapper.insert(event);
        log.info("设备事件: [{}] {} — {}", deviceId, eventType, eventTitle);
    }
}
