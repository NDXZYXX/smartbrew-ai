package com.smartbrew.smartbrew.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.deepseek")
public class AiProperties {

    /** DeepSeek API Key */
    private String apiKey;

    /** DeepSeek API Base URL */
    private String baseUrl = "https://api.deepseek.com";

    /** 模型名称 */
    private String model = "deepseek-chat";
}
