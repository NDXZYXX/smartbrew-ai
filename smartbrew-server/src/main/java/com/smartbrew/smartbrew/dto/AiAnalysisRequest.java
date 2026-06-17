package com.smartbrew.smartbrew.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "AI 分析请求")
@Data
public class AiAnalysisRequest {

    @Schema(description = "设备编号", example = "A4:CF:12:AB:CD:EF")
    @NotBlank(message = "设备编号不能为空")
    private String deviceId;

    @Schema(description = "分析类型，默认 STATUS_ANALYSIS", example = "STATUS_ANALYSIS")
    private String analysisType = "STATUS_ANALYSIS";
}
