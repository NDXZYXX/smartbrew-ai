package com.smartbrew.smartbrew.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartbrew.smartbrew.dto.AiAnalysisRecordVO;
import com.smartbrew.smartbrew.dto.AiAnalysisRequest;
import com.smartbrew.smartbrew.dto.ApiResult;
import com.smartbrew.smartbrew.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 分析", description = "DeepSeek AI 发酵状态分析：手动触发 / 记录查询 / 详情查看")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @Operation(
            summary = "触发 AI 分析",
            description = """
                    手动触发一次 DeepSeek AI 分析。输入最近 24 小时温湿度历史数据，
                    AI 返回发酵状态评估、风险提示、调整建议、预计完成时间。
                    分析结果存储到 ai_analysis_record 表。
                    """
    )
    @PostMapping("/analyze")
    public ApiResult<AiAnalysisRecordVO> analyze(@Valid @RequestBody AiAnalysisRequest request) {
        AiAnalysisRecordVO vo = aiService.triggerAnalysis(
                request.getDeviceId(), request.getAnalysisType(), null);
        return ApiResult.ok(vo);
    }

    @Operation(summary = "AI 分析记录列表", description = "分页查询 AI 分析历史记录，支持按设备和分析类型筛选")
    @GetMapping("/list")
    public ApiResult<Page<AiAnalysisRecordVO>> list(
            @Parameter(description = "设备 ID（可选筛选）") @RequestParam(required = false) String deviceId,
            @Parameter(description = "分析类型：STATUS_ANALYSIS / CYCLE_PREDICT") @RequestParam(required = false) String analysisType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<AiAnalysisRecordVO> result = aiService.list(deviceId, analysisType, page, size);
        return ApiResult.ok(result);
    }

    @Operation(summary = "AI 分析详情", description = "查看单条 AI 分析记录的完整内容（含提示词、分析结果、评估、建议）")
    @GetMapping("/{id}")
    public ApiResult<AiAnalysisRecordVO> detail(
            @Parameter(description = "分析记录 ID", example = "1") @PathVariable Long id) {
        AiAnalysisRecordVO vo = aiService.getById(id);
        return ApiResult.ok(vo);
    }
}
