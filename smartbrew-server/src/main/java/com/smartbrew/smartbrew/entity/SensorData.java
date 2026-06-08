package com.smartbrew.smartbrew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sensor_data")
public class SensorData {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;
    private BigDecimal tankTemperature;
    private BigDecimal envTemperature;
    private BigDecimal envHumidity;
    private LocalDateTime createTime;
}
