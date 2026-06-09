package com.smartbrew.smartbrew.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SensorDataVO {
    private String deviceId;
    private BigDecimal tankTemperature;
    private BigDecimal envTemperature;
    private BigDecimal envHumidity;
    private LocalDateTime createTime;
}
