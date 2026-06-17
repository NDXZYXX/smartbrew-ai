package com.smartbrew.smartbrew.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_analysis_record")
public class AiAnalysisRecord {
    @TableId(type = IdType.AUTO)
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
