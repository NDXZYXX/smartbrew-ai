#ifndef WIFI_MANAGER_H
#define WIFI_MANAGER_H

#include <Arduino.h>
#include <WiFi.h>

// ============================================================
// WiFiManager — WiFi 连接 + 自动重连
// ============================================================
class WiFiManager {
public:
    WiFiManager(const char* ssid, const char* password);

    bool connect();              // 首次连接（阻塞，带超时）
    bool ensureConnected();      // 非阻塞检车连接，断线自动重连
    bool isConnected() const;
    int32_t getRSSI() const;
    String getLocalIP() const;

private:
    const char* _ssid;
    const char* _password;
    unsigned long _lastRetryMs;
    bool _wasConnected;
};

#endif // WIFI_MANAGER_H
