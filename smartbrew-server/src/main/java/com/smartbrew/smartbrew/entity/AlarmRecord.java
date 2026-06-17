package com.smartbrew.smartbrew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("alarm_record")
public class AlarmRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;
    private Long sensorDataId;
    private String alarmType;       // HIGH_TEMP / LOW_TEMP / DEVICE_OFFLINE
    private String alarmLevel;      // WARN / ERROR
    private String alarmTitle;
    private String alarmMessage;
    private BigDecimal alarmValue;
    private BigDecimal thresholdValue;
    private Integer isCleared;      // 0-未消除, 1-已消除
    private LocalDateTime clearedTime;
    private LocalDateTime createTime;
}
