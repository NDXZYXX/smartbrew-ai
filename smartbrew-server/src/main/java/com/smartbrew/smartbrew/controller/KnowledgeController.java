package com.smartbrew.smartbrew.controller;

import com.smartbrew.smartbrew.dto.ApiResult;
import com.smartbrew.smartbrew.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "知识库", description = "基于 RAG 的发酵知识问答，本地 Markdown 知识库 + DeepSeek AI")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Operation(
            summary = "知识库问答",
            description = """
                    提交发酵相关问题，系统检索本地知识库（苹果酒/米酒/葡萄酒）并调用 DeepSeek AI 回答。
                    知识库文件位于 src/main/resources/knowledge/ 目录。
                    """
    )
    @PostMapping("/ask")
    public ApiResult<KnowledgeBaseService.AskResponse> ask(@Valid @RequestBody AskRequest request) {
        try {
            KnowledgeBaseService.AskResponse response = knowledgeBaseService.ask(request.question());
            return ApiResult.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(400, e.getMessage());
        } catch (Exception e) {
            return ApiResult.fail(500, "问答失败: " + e.getMessage());
        }
    }

    @Schema(description = "知识库问答请求")
    public record AskRequest(
            @Parameter(description = "发酵相关问题", example = "苹果酒发酵的最佳温度是多少？")
            @NotBlank String question) {}
}
