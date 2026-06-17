package com.smartbrew.smartbrew.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "告警记录展示对象")
@Data
public class AlarmRecordVO {

    @Schema(description = "告警记录 ID")
    private Long id;

    @Schema(description = "设备编号")
    private String deviceId;

    @Schema(description = "告警类型：HIGH_TEMP / LOW_TEMP / DEVICE_OFFLINE")
    private String alarmType;

    @Schema(description = "告警级别：WARN / ERROR")
    private String alarmLevel;

    @Schema(description = "告警标题")
    private String alarmTitle;

    @Schema(description = "告警详情")
    private String alarmMessage;

    @Schema(description = "触发告警的实际值")
    private BigDecimal alarmValue;

    @Schema(description = "告警阈值")
    private BigDecimal thresholdValue;

    @Schema(description = "是否已清除：0=未清除, 1=已清除")
    private Integer isCleared;

    @Schema(description = "清除时间")
    private LocalDateTime clearedTime;

    @Schema(description = "告警时间")
    private LocalDateTime createTime;
}
