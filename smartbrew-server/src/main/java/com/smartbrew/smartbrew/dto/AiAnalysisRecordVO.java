package com.smartbrew.smartbrew.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiAnalysisRecordVO {
    private Long id;
    private String deviceId;
    private Long batchId;
    private String analysisType;
    private String inputSnapshot;
    private String prompt;
    private String analysisResult;
    private String statusAssessment;
    private String riskWarning;
    private String suggestion;
    private LocalDateTime predictedEndTime;
    private String aiModel;
    private Integer responseTimeMs;
    private LocalDateTime createTime;
}
