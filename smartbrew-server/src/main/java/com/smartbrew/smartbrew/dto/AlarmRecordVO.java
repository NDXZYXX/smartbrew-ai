package com.smartbrew.smartbrew.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlarmRecordVO {
    private Long id;
    private String deviceId;
    private String alarmType;
    private String alarmLevel;
    private String alarmTitle;
    private String alarmMessage;
    private BigDecimal alarmValue;
    private BigDecimal thresholdValue;
    private Integer isCleared;
    private LocalDateTime clearedTime;
    private LocalDateTime createTime;
}
