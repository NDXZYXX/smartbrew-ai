package com.smartbrew.smartbrew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device")
public class Device {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;
    private String deviceName;
    private String deviceSecret;
    private Integer status;         // 0-离线, 1-在线
    private String ipAddress;
    private String firmwareVersion;
    private LocalDateTime lastHeartbeatTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
