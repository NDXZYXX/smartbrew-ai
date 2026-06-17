package com.smartbrew.smartbrew.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartbrew.smartbrew.dto.AiAnalysisRecordVO;
import com.smartbrew.smartbrew.dto.AiAnalysisRequest;
import com.smartbrew.smartbrew.dto.ApiResult;
import com.smartbrew.smartbrew.service.AiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 手动触发 AI 分析
     */
    @PostMapping("/analyze")
    public ApiResult<AiAnalysisRecordVO> analyze(@Valid @RequestBody AiAnalysisRequest request) {
        AiAnalysisRecordVO vo = aiService.triggerAnalysis(
                request.getDeviceId(), request.getAnalysisType(), null);
        return ApiResult.ok(vo);
    }

    /**
     * 分页查询分析记录
     */
    @GetMapping("/list")
    public ApiResult<Page<AiAnalysisRecordVO>> list(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String analysisType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AiAnalysisRecordVO> result = aiService.list(deviceId, analysisType, page, size);
        return ApiResult.ok(result);
    }

    /**
     * 单条分析详情
     */
    @GetMapping("/{id}")
    public ApiResult<AiAnalysisRecordVO> detail(@PathVariable Long id) {
        AiAnalysisRecordVO vo = aiService.getById(id);
        return ApiResult.ok(vo);
    }
}
