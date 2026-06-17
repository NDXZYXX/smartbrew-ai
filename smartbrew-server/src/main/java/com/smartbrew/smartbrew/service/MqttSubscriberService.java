package com.smartbrew.smartbrew.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbrew.smartbrew.config.MqttProperties;
import com.smartbrew.smartbrew.entity.*;
import com.smartbrew.smartbrew.mapper.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 订阅服务
 * 订阅 smartbrew/+/data、smartbrew/+/heartbeat、smartbrew/+/status
 * 解析并写入 MySQL，同时更新 Redis 缓存
 */
@Service
public class MqttSubscriberService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MqttSubscriberService.class);

    private final MqttProperties props;
    private final SensorDataMapper sensorDataMapper;
    private final DeviceMapper deviceMapper;
    private final DeviceHeartbeatMapper heartbeatMapper;
    private final DeviceEventMapper eventMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AlarmService alarmService;

    private MqttClient client;

    public MqttSubscriberService(MqttProperties props,
                                  SensorDataMapper sensorDataMapper,
                                  DeviceMapper deviceMapper,
                                  DeviceHeartbeatMapper heartbeatMapper,
                                  DeviceEventMapper eventMapper,
                                  RedisTemplate<String, Object> redisTemplate,
                                  ObjectMapper objectMapper,
                                  AlarmService alarmService) {
        this.props = props;
        this.sensorDataMapper = sensorDataMapper;
        this.deviceMapper = deviceMapper;
        this.heartbeatMapper = heartbeatMapper;
        this.eventMapper = eventMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.alarmService = alarmService;
    }

    @Override
    public void run(String... args) throws Exception {
        connectAndSubscribe();
    }

    public void connectAndSubscribe() throws MqttException {
        String broker = props.getBrokerUrl();
        String clientId = props.getClientId();
        log.info("MQTT 连接中: {} (clientId={})", broker, clientId);

        client = new MqttClient(broker, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(props.getUsername());
        options.setPassword(props.getPassword().toCharArray());
        options.setKeepAliveInterval(props.getKeepAliveInterval());
        options.setConnectionTimeout(props.getConnectionTimeout());
        options.setAutomaticReconnect(true);            // 自动重连
        options.setCleanSession(true);

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT 连接断开: {}", cause.getMessage());
                // automaticReconnect=true 会自动重连
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                handleMessage(topic, new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 不需要处理（我们只订阅，不发布）
            }
        });

        client.connect(options);

        // 订阅所有 Topic
        client.subscribe(props.getTopics().getData(), props.getQos());
        client.subscribe(props.getTopics().getHeartbeat(), props.getQos());
        client.subscribe(props.getTopics().getStatus(), props.getQos());

        log.info("MQTT 已连接，已订阅: {}", props.getTopics());
    }

    /**
     * 处理 MQTT 消息：根据 Topic 类型分发
     */
    private void handleMessage(String topic, String payload) {
        try {
            String deviceId = extractDeviceId(topic);
            if (deviceId == null) {
                log.warn("无法从 Topic 解析 deviceId: {}", topic);
                return;
            }

            if (topic.contains("/data")) {
                handleSensorData(deviceId, payload);
            } else if (topic.contains("/heartbeat")) {
                handleHeartbeat(deviceId, payload);
            } else if (topic.contains("/status")) {
                handleStatus(deviceId, payload);
            }
        } catch (Exception e) {
            log.error("MQTT 消息处理异常 | topic={} payload={}", topic, payload, e);
        }
    }

    /**
     * 从 Topic 中提取 deviceId
     * 支持两种格式:
     *   smartbrew/device/{deviceId}/data   → 精确匹配
     *   smartbrew/+/data                   → 通配符订阅（从 topic 第2段取）
     */
    private String extractDeviceId(String topic) {
        String[] parts = topic.split("/");
        // smartbrew / deviceId / data    (3段)
        // 或 smarbbrew / {deviceId} / data (我们用 + 通配符订阅)
        if (parts.length >= 3) {
            return parts[parts.length - 2]; // data 前的一段就是 deviceId
        }
        return null;
    }

    // ---- 传感器数据处理 ----

    private void handleSensorData(String deviceId, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            SensorData data = new SensorData();
            data.setDeviceId(deviceId);

            if (root.has("tank_temp")) {
                data.setTankTemperature(BigDecimal.valueOf(root.get("tank_temp").asDouble()));
            }
            if (root.has("env_temp") && !root.get("env_temp").isNull()) {
                data.setEnvTemperature(BigDecimal.valueOf(root.get("env_temp").asDouble()));
            }
            if (root.has("env_humidity") && !root.get("env_humidity").isNull()) {
                data.setEnvHumidity(BigDecimal.valueOf(root.get("env_humidity").asDouble()));
            }

            // 优先使用设备端时间戳，否则用当前时间
            if (root.has("timestamp")) {
                long ts = root.get("timestamp").asLong();
                data.setCreateTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(ts), ZoneId.systemDefault()));
            } else {
                data.setCreateTime(LocalDateTime.now());
            }

            sensorDataMapper.insert(data);
            log.debug("传感器数据入库: device={} tank={}℃", deviceId, data.getTankTemperature());

            // 检查温度告警
            alarmService.checkTemperature(deviceId, data);

            // 更新 Redis 最新数据缓存
            cacheLatestSensorData(deviceId, data);

        } catch (Exception e) {
            log.error("传感器数据解析失败: device={} payload={}", deviceId, payload, e);
        }
    }

    // ---- 心跳处理 ----

    private void handleHeartbeat(String deviceId, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            DeviceHeartbeat hb = new DeviceHeartbeat();
            hb.setDeviceId(deviceId);
            hb.setRssi(root.get("rssi").asInt());
            hb.setHeapFree(root.get("heap_free").asInt());
            hb.setFirmwareVersion(root.get("firmware_version").asText());
            hb.setIpAddress(root.get("ip_address").asText());
            hb.setUptimeSeconds(root.has("uptime_seconds") ? root.get("uptime_seconds").asInt() : 0);

            heartbeatMapper.insert(hb);
            log.debug("心跳入库: device={} rssi={}", deviceId, hb.getRssi());

            // 更新 device 表在线状态
            String ip = hb.getIpAddress();
            String fwVer = hb.getFirmwareVersion();
            int updated = deviceMapper.updateOnlineByHeartbeat(deviceId, ip, fwVer);

            if (updated == 0) {
                // 设备首次上线，自动注册
                Device device = new Device();
                device.setDeviceId(deviceId);
                device.setDeviceName("ESP32-" + deviceId.substring(Math.max(0, deviceId.length() - 6)));
                device.setDeviceSecret("");
                device.setStatus(1);
                device.setIpAddress(ip);
                device.setFirmwareVersion(fwVer);
                deviceMapper.insert(device);
                log.info("新设备自动注册: {}", deviceId);
            }

            // Redis 在线标记（TTL=120s）
            redisTemplate.opsForValue().set(
                    "device:online:" + deviceId, "1", 120, TimeUnit.SECONDS);

            // 设备上线，自动清除离线告警
            alarmService.clearOfflineAlarms(deviceId);

        } catch (Exception e) {
            log.error("心跳解析失败: device={} payload={}", deviceId, payload, e);
        }
    }

    // ---- 状态处理 ----

    private void handleStatus(String deviceId, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String status = root.has("status") ? root.get("status").asText() : "";
            log.info("设备状态上报: device={} status={}", deviceId, status);
        } catch (Exception e) {
            log.error("状态解析失败: device={} payload={}", deviceId, payload, e);
        }
    }

    // ---- Redis 缓存 ----

    private void cacheLatestSensorData(String deviceId, SensorData data) {
        String key = "device:latest:" + deviceId;
        redisTemplate.opsForHash().put(key, "tank_temp", data.getTankTemperature().toString());
        redisTemplate.opsForHash().put(key, "create_time", data.getCreateTime().toString());
        if (data.getEnvTemperature() != null) {
            redisTemplate.opsForHash().put(key, "env_temp", data.getEnvTemperature().toString());
        }
        if (data.getEnvHumidity() != null) {
            redisTemplate.opsForHash().put(key, "env_humidity", data.getEnvHumidity().toString());
        }
        // 不设TTL，跟随心跳的 device:online TTL
    }
}
