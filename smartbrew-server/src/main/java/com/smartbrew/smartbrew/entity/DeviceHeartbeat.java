package com.smartbrew.smartbrew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_heartbeat")
public class DeviceHeartbeat {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;
    private Integer rssi;
    private Integer heapFree;
    private String firmwareVersion;
    private String ipAddress;
    private Integer uptimeSeconds;
    private LocalDateTime createTime;
}
