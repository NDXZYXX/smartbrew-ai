package com.smartbrew.smartbrew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_event")
public class DeviceEvent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;
    private String eventType;       // DEVICE_ONLINE / DEVICE_OFFLINE / ...
    private String eventLevel;      // INFO / WARN / ERROR
    private String eventTitle;
    private String eventDetail;
    private Long referenceId;
    private LocalDateTime createTime;
}
