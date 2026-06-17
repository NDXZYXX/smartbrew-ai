package com.smartbrew.smartbrew.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartBrewOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Brew AI — 智能发酵监控系统 API")
                        .description("""
                                基于 ESP32 + MQTT + Spring Boot + Vue3 + DeepSeek AI 的智能发酵监控系统。

                                **功能模块：**
                                - 设备管理（注册/列表/详情/更新）
                                - 传感器数据（最新数据/历史曲线）
                                - 告警系统（规则引擎/告警列表/清除）
                                - 设备控制（风扇/加热 MQTT 下发）
                                - AI 分析（DeepSeek 状态评估/风险提示）
                                - 知识库（RAG 发酵知识问答）
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Smart Brew AI Team"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
