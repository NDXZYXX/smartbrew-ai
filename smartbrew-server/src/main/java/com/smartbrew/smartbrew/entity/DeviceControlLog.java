package com.smartbrew.smartbrew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_control_log")
public class DeviceControlLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;
    private String controlTarget;   // FAN / HEATER
    private String command;         // ON / OFF
    private String triggerSource;   // AUTO / MANUAL
    private Long operatorId;
    private String triggerReason;
    private String mqttMsgId;
    private Integer executeStatus;  // 0-已下发, 1-成功, 2-超时, 3-失败
    private LocalDateTime executeTime;
    private LocalDateTime createTime;
}
