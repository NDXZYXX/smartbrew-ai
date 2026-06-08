package com.smartbrew.smartbrew.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;
    private int qos = 1;
    private int keepAliveInterval = 60;
    private int connectionTimeout = 30;
    private Topics topics = new Topics();

    @Data
    public static class Topics {
        private String data      = "smartbrew/+/data";
        private String heartbeat = "smartbrew/+/heartbeat";
        private String status    = "smartbrew/+/status";
    }
}
