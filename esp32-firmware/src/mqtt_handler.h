#ifndef MQTT_HANDLER_H
#define MQTT_HANDLER_H

#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFi.h>

// ============================================================
// MQTTHandler — MQTT 连接、数据发布、心跳上报
// ============================================================

// 回调函数指针类型：收到控制指令时调用
typedef void (*ControlCallback)(const char* command, const char* value);

class MQTTHandler {
public:
    MQTTHandler(const char* broker, uint16_t port,
                const char* username, const char* password,
                const char* clientId, const char* deviceId);

    void begin();
    bool ensureConnected();
    bool isConnected() const;

    // 发布数据（JSON 格式）
    bool publishData(float tankTemp, float envTemp, float envHumidity);
    // 发布心跳
    bool publishHeartbeat(int32_t rssi, uint32_t freeHeap,
                          const char* firmwareVer, const char* ip);
    // 发布 GPIO 状态反馈
    bool publishStatus(int fanState, int heaterState);

    // 控制指令回调
    void setControlCallback(ControlCallback cb);
    void loop();  // 必须定期调用以处理 MQTT 消息

private:
    WiFiClient   _wifiClient;
    PubSubClient _client;
    const char* _broker;
    uint16_t _port;
    const char* _username;
    const char* _password;
    const char* _clientId;
    const char* _deviceId;

    char _topicData[64];
    char _topicHeartbeat[64];
    char _topicStatus[64];
    char _topicControl[64];

    unsigned long _lastRetryMs;
    ControlCallback _controlCb;

    void _subscribeTopics();

    static void _mqttCallback(char* topic, byte* payload, unsigned int length,
                              MQTTHandler* self);
};

#endif // MQTT_HANDLER_H
