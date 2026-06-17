package com.smartbrew.smartbrew.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "AI 分析记录展示对象")
@Data
public class AiAnalysisRecordVO {

    @Schema(description = "分析记录 ID")
    private Long id;

    @Schema(description = "设备编号")
    private String deviceId;

    @Schema(description = "发酵批次 ID")
    private Long batchId;

    @Schema(description = "分析类型：STATUS_ANALYSIS / CYCLE_PREDICT")
    private String analysisType;

    @Schema(description = "输入数据快照（JSON）")
    private String inputSnapshot;

    @Schema(description = "发送给 AI 的提示词")
    private String prompt;

    @Schema(description = "AI 返回的完整分析结果")
    private String analysisResult;

    @Schema(description = "发酵状态评估")
    private String statusAssessment;

    @Schema(description = "风险提示")
    private String riskWarning;

    @Schema(description = "调整建议")
    private String suggestion;

    @Schema(description = "预计发酵完成时间")
    private LocalDateTime predictedEndTime;

    @Schema(description = "使用的 AI 模型", example = "deepseek-chat")
    private String aiModel;

    @Schema(description = "AI 响应耗时（毫秒）")
    private Integer responseTimeMs;

    @Schema(description = "分析时间")
    private LocalDateTime createTime;
}
