#include "wifi_manager.h"
#include "config.h"

WiFiManager::WiFiManager(const char* ssid, const char* password)
    : _ssid(ssid)
    , _password(password)
    , _lastRetryMs(0)
    , _wasConnected(false)
{
}

bool WiFiManager::connect() {
    Serial.printf("[WiFi] 连接中: %s ...\n", _ssid);
    WiFi.mode(WIFI_STA);
    WiFi.begin(_ssid, _password);

    unsigned long start = millis();
    while (WiFi.status() != WL_CONNECTED) {
        if (millis() - start > 20000) {  // 20 秒超时
            Serial.println("[WiFi] 首次连接超时（20s）");
            return false;
        }
        delay(500);
        Serial.print(".");
    }

    Serial.println();
    Serial.printf("[WiFi] 已连接 | IP: %s | RSSI: %d dBm\n",
                  WiFi.localIP().toString().c_str(), WiFi.RSSI());
    _wasConnected = true;
    return true;
}

bool WiFiManager::ensureConnected() {
    if (WiFi.status() == WL_CONNECTED) {
        if (!_wasConnected) {
            // 恢复连接
            Serial.printf("[WiFi] 已恢复 | IP: %s | RSSI: %d dBm\n",
                          WiFi.localIP().toString().c_str(), WiFi.RSSI());
            _wasConnected = true;
        }
        return true;
    }

    // 断线状态
    if (_wasConnected) {
        Serial.println("[WiFi] 连接已断开，开始重连...");
        _wasConnected = false;
    }

    unsigned long now = millis();
    if (now - _lastRetryMs < WIFI_RETRY_INTERVAL) {
        return false;
    }
    _lastRetryMs = now;

    Serial.printf("[WiFi] 重连中: %s ...\n", _ssid);
    WiFi.reconnect();

    return false;
}

bool WiFiManager::isConnected() const {
    return WiFi.status() == WL_CONNECTED;
}

int32_t WiFiManager::getRSSI() const {
    return WiFi.RSSI();
}

String WiFiManager::getLocalIP() const {
    return WiFi.localIP().toString();
}
