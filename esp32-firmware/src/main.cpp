// ============================================================
// Smart Brew AI — ESP32 固件主程序
// Phase 1: DS18B20 + DHT22 采集 + MQTT 上报
// ============================================================

#include <Arduino.h>
#include "config.h"
#include "sensors.h"
#include "wifi_manager.h"
#include "mqtt_handler.h"

// ---------- 硬件对象 ----------
WiFiManager   wifi(WIFI_SSID, WIFI_PASSWORD);
MQTTHandler   mqtt(MQTT_BROKER, MQTT_PORT,
                   MQTT_USERNAME, MQTT_PASSWORD,
                   MQTT_CLIENT_ID, DEVICE_ID);
TankTempSensor tankSensor(PIN_DS18B20);
EnvSensor      envSensor(PIN_DHT22);

// ---------- 定时器 ----------
static unsigned long lastTempReadMs    = 0;
static unsigned long lastEnvReadMs     = 0;
static unsigned long lastHeartbeatMs   = 0;

// 最新读数缓存（无锁共享，单线程循环安全）
static float  g_tankTemp      = 0.0f;
static float  g_envTemp       = 0.0f;
static float  g_envHumidity   = 0.0f;
static bool   g_tankTempValid = false;
static bool   g_envValid      = false;

// ---------- GPIO 控制 ----------
static int g_fanState    = 0;  // 0=关, 1=开
static int g_heaterState = 0;

static void setFan(int state) {
    g_fanState = state;
    digitalWrite(PIN_FAN_RELAY, state ? HIGH : LOW);
    Serial.printf("[GPIO] 风扇 %s\n", state ? "开" : "关");
}

static void setHeater(int state) {
    // 安全互斥：风扇和加热不能同时开
    if (state && g_fanState) {
        setFan(0);
    }
    g_heaterState = state;
    digitalWrite(PIN_HEATER_RELAY, state ? HIGH : LOW);
    Serial.printf("[GPIO] 加热 %s\n", state ? "开" : "关");
}

// ---------- MQTT 控制指令回调 ----------
static void onControlCommand(const char* command, const char* value) {
    bool turnOn = (strcmp(value, "ON") == 0 || strcmp(value, "1") == 0);

    if (strcmp(command, "FAN") == 0) {
        if (turnOn) {
            setHeater(0);  // 确保加热关闭
        }
        setFan(turnOn ? 1 : 0);
    } else if (strcmp(command, "HEATER") == 0) {
        if (turnOn) {
            setFan(0);     // 确保风扇关闭
        }
        setHeater(turnOn ? 1 : 0);
    } else {
        Serial.printf("[Control] 未知指令: %s=%s\n", command, value);
    }
}

// ---------- 设置 ----------
void setup() {
    Serial.begin(115200);
    delay(1000);
    Serial.println();
    Serial.println("========================================");
    Serial.printf(" Smart Brew AI — Firmware %s\n", FIRMWARE_VERSION);
    Serial.printf(" Device ID: %s\n", DEVICE_ID);
    Serial.println("========================================");

    // GPIO 初始化
    pinMode(PIN_FAN_RELAY, OUTPUT);
    pinMode(PIN_HEATER_RELAY, OUTPUT);
    digitalWrite(PIN_FAN_RELAY, LOW);
    digitalWrite(PIN_HEATER_RELAY, LOW);

    // 传感器初始化
    tankSensor.begin();
    envSensor.begin();

    // WiFi 连接（阻塞，最多等 20 秒）
    if (!wifi.connect()) {
        Serial.println("[Main] WiFi 首次连接失败，进入后台重连模式");
    }

    // MQTT 初始化
    mqtt.begin();
    mqtt.setControlCallback(onControlCommand);
}

// ---------- 主循环 ----------
void loop() {
    unsigned long now = millis();

    // 1. 维护 WiFi 连接（非阻塞）
    wifi.ensureConnected();

    // 2. 维护 MQTT 连接（非阻塞）
    bool mqttOk = wifi.isConnected() && mqtt.ensureConnected();

    // 3. MQTT 消息处理（必须高频调用）
    if (mqtt.isConnected()) {
        mqtt.loop();
    }

    // 4. 温度采集（30 秒周期）
    if (now - lastTempReadMs >= TEMP_INTERVAL_MS) {
        lastTempReadMs = now;
        float t;
        if (tankSensor.read(t)) {
            g_tankTemp = t;
            g_tankTempValid = true;
            Serial.printf("[Data] 桶内温度: %.2f ℃\n", t);
        }
    }

    // 5. 环境温湿度采集（60 秒周期）
    if (now - lastEnvReadMs >= ENV_INTERVAL_MS) {
        lastEnvReadMs = now;
        float t, h;
        if (envSensor.read(t, h)) {
            g_envTemp = t;
            g_envHumidity = h;
            g_envValid = true;
            Serial.printf("[Data] 环境温度: %.2f ℃ | 湿度: %.2f %%RH\n", t, h);
        }
    }

    // 6. MQTT 数据上报（跟温度同频，30 秒）
    //    每次都上报全部最新缓存值
    if (mqttOk && g_tankTempValid && (now - lastTempReadMs < 1000)) {
        // 在温度读取后立即尝试上报一次（避免额外定时器）
        mqtt.publishData(g_tankTemp, g_envTemp, g_envHumidity);
    }

    // 7. 心跳上报（60 秒周期）
    if (now - lastHeartbeatMs >= HEARTBEAT_INTERVAL_MS) {
        lastHeartbeatMs = now;
        if (mqttOk) {
            mqtt.publishHeartbeat(
                wifi.getRSSI(),
                ESP.getFreeHeap(),
                FIRMWARE_VERSION,
                wifi.getLocalIP().c_str()
            );
        }
    }
}
