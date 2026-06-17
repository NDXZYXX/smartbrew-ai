package com.smartbrew.smartbrew.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "传感器数据展示对象")
@Data
public class SensorDataVO {

    @Schema(description = "设备唯一标识", example = "A4:CF:12:AB:CD:EF")
    private String deviceId;

    @Schema(description = "桶内温度（℃）", example = "22.50")
    private BigDecimal tankTemperature;

    @Schema(description = "环境温度（℃）", example = "25.00")
    private BigDecimal envTemperature;

    @Schema(description = "环境湿度（%RH）", example = "65.00")
    private BigDecimal envHumidity;

    @Schema(description = "采集时间")
    private LocalDateTime createTime;
}
