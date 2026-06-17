package com.smartbrew.smartbrew.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private static final String[] KNOWLEDGE_FILES = {
            "knowledge/apple_wine.md",
            "knowledge/rice_wine.md",
            "knowledge/grape_wine.md"
    };

    private static final String[] KNOWLEDGE_TITLES = {
            "苹果酒/梅酒知识",
            "米酒/甜酒酿知识",
            "葡萄酒知识"
    };

    private final Map<String, String> knowledgeBase = new LinkedHashMap<>();
    private final DeepSeekClient deepSeekClient;

    public KnowledgeBaseService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    @PostConstruct
    public void loadKnowledge() {
        for (int i = 0; i < KNOWLEDGE_FILES.length; i++) {
            try {
                ClassPathResource resource = new ClassPathResource(KNOWLEDGE_FILES[i]);
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                knowledgeBase.put(KNOWLEDGE_TITLES[i], content);
                log.info("知识库文件加载成功: {} ({} 字符)", KNOWLEDGE_FILES[i], content.length());
            } catch (IOException e) {
                log.error("知识库文件加载失败: {}", KNOWLEDGE_FILES[i], e);
            }
        }
        log.info("知识库加载完成，共 {} 个文件", knowledgeBase.size());
    }

    public record AskResponse(String answer, String model, long responseTimeMs) {}

    public AskResponse ask(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }

        if (knowledgeBase.isEmpty()) {
            throw new RuntimeException("知识库未加载，请联系管理员");
        }

        String systemPrompt = buildSystemPrompt();

        List<DeepSeekClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.ChatMessage("system", systemPrompt));
        messages.add(new DeepSeekClient.ChatMessage("user", question));

        long startMs = System.currentTimeMillis();
        DeepSeekClient.DeepSeekResponse response = deepSeekClient.chat(messages);
        long elapsed = System.currentTimeMillis() - startMs;

        String answer = "";
        String model = "";
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            DeepSeekClient.Choice choice = response.getChoices().get(0);
            if (choice.getMessage() != null) {
                answer = choice.getMessage().getContent();
            }
        }
        if (response != null && response.getModel() != null) {
            model = response.getModel();
        }

        if (answer == null || answer.isEmpty()) {
            throw new RuntimeException("AI 返回空结果，请稍后重试");
        }

        return new AskResponse(answer, model, elapsed);
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的家庭发酵顾问，精通苹果酒、米酒、葡萄酒的酿造工艺。\n");
        sb.append("请根据以下知识库内容，用通俗易懂的中文回答用户的问题。\n");
        sb.append("如果知识库中没有相关信息，请基于你的发酵专业知识诚实回答。\n");
        sb.append("回答应简洁实用，适合家庭酿造爱好者理解，控制在300字以内。\n\n");

        for (Map.Entry<String, String> entry : knowledgeBase.entrySet()) {
            sb.append("### ").append(entry.getKey()).append("\n");
            sb.append(entry.getValue()).append("\n\n");
        }

        return sb.toString();
    }
}
