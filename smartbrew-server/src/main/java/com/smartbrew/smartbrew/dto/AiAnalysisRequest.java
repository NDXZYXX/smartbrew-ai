package com.smartbrew.smartbrew.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiAnalysisRequest {
    @NotBlank(message = "设备编号不能为空")
    private String deviceId;

    /** 分析类型，默认 STATUS_ANALYSIS */
    private String analysisType = "STATUS_ANALYSIS";
}
