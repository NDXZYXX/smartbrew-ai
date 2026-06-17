package com.smartbrew.smartbrew.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartbrew.smartbrew.config.AiProperties;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    public DeepSeekClient(RestTemplate restTemplate, AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
    }

    public DeepSeekResponse chat(List<ChatMessage> messages) {
        String url = aiProperties.getBaseUrl() + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        ChatRequest request = new ChatRequest();
        request.setModel(aiProperties.getModel());
        request.setMessages(messages);
        request.setTemperature(0.3);
        request.setMaxTokens(1000);

        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        try {
            long start = System.currentTimeMillis();
            ResponseEntity<DeepSeekResponse> response = restTemplate.postForEntity(url, entity, DeepSeekResponse.class);
            long elapsed = System.currentTimeMillis() - start;

            DeepSeekResponse body = response.getBody();
            if (body != null && body.getUsage() != null) {
                log.info("DeepSeek API 调用成功: model={} tokens={} time={}ms",
                        body.getModel(),
                        body.getUsage().getTotalTokens(),
                        elapsed);
            }
            return body;
        } catch (RestClientException e) {
            log.error("DeepSeek API 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }

    @Data
    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    public static class ChatRequest {
        private String model;
        private List<ChatMessage> messages;
        private double temperature;
        @JsonProperty("max_tokens")
        private int maxTokens;
    }

    @Data
    public static class DeepSeekResponse {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
    }

    @Data
    public static class Choice {
        private int index;
        private ChatMessage message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
