#include "mqtt_handler.h"
#include "config.h"
#include <ArduinoJson.h>

MQTTHandler::MQTTHandler(const char* broker, uint16_t port,
                         const char* username, const char* password,
                         const char* clientId, const char* deviceId)
    : _broker(broker)
    , _port(port)
    , _username(username)
    , _password(password)
    , _clientId(clientId)
    , _deviceId(deviceId)
    , _lastRetryMs(0)
    , _controlCb(nullptr)
{
    snprintf(_topicData,      sizeof(_topicData),      TOPIC_DATA,      deviceId);
    snprintf(_topicHeartbeat, sizeof(_topicHeartbeat), TOPIC_HEARTBEAT, deviceId);
    snprintf(_topicStatus,    sizeof(_topicStatus),    TOPIC_STATUS,    deviceId);
    snprintf(_topicControl,   sizeof(_topicControl),   TOPIC_CONTROL,   deviceId);
}

void MQTTHandler::begin() {
    _client.setClient(_wifiClient);
    _client.setServer(_broker, _port);
    _client.setBufferSize(512);

    // 使用静态 lambda 绑定 this 指针
    auto self = this;
    _client.setCallback([self](char* topic, byte* payload, unsigned int length) {
        self->_mqttCallback(topic, payload, length, self);
    });

    Serial.printf("[MQTT] Broker: %s:%d | ClientID: %s\n",
                  _broker, _port, _clientId);
}

bool MQTTHandler::ensureConnected() {
    if (_client.connected()) {
        return true;
    }

    unsigned long now = millis();
    if (now - _lastRetryMs < MQTT_RETRY_INTERVAL) {
        return false;
    }
    _lastRetryMs = now;

    Serial.printf("[MQTT] 连接中: %s:%d ...\n", _broker, _port);
    if (!_client.connect(_clientId, _username, _password)) {
        Serial.printf("[MQTT] 连接失败, rc=%d (1=协议错误 2=ClientID非法 3=服务不可用 4=用户名密码错误 5=未授权)\n",
                      _client.state());
        return false;
    }

    Serial.println("[MQTT] 已连接");
    _subscribeTopics();

    // 发布上线状态
    _client.publish(_topicStatus, "{\"status\":\"online\"}", true);  // retained
    return true;
}

bool MQTTHandler::isConnected() const {
    return _client.connected();
}

void MQTTHandler::_subscribeTopics() {
    _client.subscribe(_topicControl);
    Serial.printf("[MQTT] 已订阅: %s\n", _topicControl);
}

bool MQTTHandler::publishData(float tankTemp, float envTemp, float envHumidity) {
    JsonDocument doc;
    doc["device_id"]  = _deviceId;
    doc["tank_temp"]  = roundf(tankTemp * 100) / 100;
    doc["env_temp"]   = roundf(envTemp * 100) / 100;
    doc["env_humidity"] = roundf(envHumidity * 100) / 100;
    doc["rssi"]       = WiFi.RSSI();
    doc["free_heap"]  = ESP.getFreeHeap();
    doc["fw_version"] = FIRMWARE_VERSION;
    doc["timestamp"]  = millis();

    char buffer[512];
    size_t n = serializeJson(doc, buffer);
    buffer[n] = '\0';

    if (!_client.publish(_topicData, buffer)) {
        Serial.println("[MQTT] 数据发布失败");
        return false;
    }
    Serial.printf("[MQTT] 数据已发布: %s\n", buffer);
    return true;
}

bool MQTTHandler::publishHeartbeat(int32_t rssi, uint32_t freeHeap,
                                    const char* firmwareVer, const char* ip) {
    JsonDocument doc;
    doc["device_id"]       = _deviceId;
    doc["rssi"]            = rssi;
    doc["heap_free"]       = freeHeap;
    doc["firmware_version"] = firmwareVer;
    doc["ip_address"]      = ip;
    doc["uptime_seconds"]  = millis() / 1000;

    char buffer[256];
    size_t n = serializeJson(doc, buffer);
    buffer[n] = '\0';

    if (!_client.publish(_topicHeartbeat, buffer)) {
        Serial.println("[MQTT] 心跳发布失败");
        return false;
    }
    return true;
}

bool MQTTHandler::publishStatus(int fanState, int heaterState) {
    JsonDocument doc;
    doc["fan"]    = fanState;
    doc["heater"] = heaterState;

    char buffer[128];
    size_t n = serializeJson(doc, buffer);
    buffer[n] = '\0';

    if (!_client.publish(_topicStatus, buffer)) {
        Serial.println("[MQTT] 状态发布失败");
        return false;
    }
    Serial.printf("[MQTT] GPIO状态已发布: %s\n", buffer);
    return true;
}

void MQTTHandler::setControlCallback(ControlCallback cb) {
    _controlCb = cb;
}

void MQTTHandler::loop() {
    _client.loop();
}

// 静态 MQTT 回调
void MQTTHandler::_mqttCallback(char* topic, byte* payload, unsigned int length,
                                 MQTTHandler* self) {
    // 只处理控制 Topic
    if (strcmp(topic, self->_topicControl) != 0) {
        return;
    }

    // 解析 JSON
    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, payload, length);
    if (err) {
        Serial.printf("[MQTT] JSON解析失败: %s\n", err.c_str());
        return;
    }

    const char* command = doc["command"] | "";
    const char* value   = doc["value"]   | "";

    Serial.printf("[MQTT] 收到控制指令: command=%s value=%s\n", command, value);

    if (self->_controlCb) {
        self->_controlCb(command, value);
    }
}
