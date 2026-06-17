package com.smartbrew.smartbrew.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_config")
public class SystemConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String configKey;
    private String configValue;
    private String configType;
    private String configGroup;
    private String description;
    private Integer isEditable;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
