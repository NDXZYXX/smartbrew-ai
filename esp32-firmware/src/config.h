#ifndef CONFIG_H
#define CONFIG_H

// ============================================================
// Smart Brew AI — 设备配置文件
// 修改此文件以匹配你的 WiFi 和 MQTT 配置
// ============================================================

// ---------- WiFi 配置 ----------
#define WIFI_SSID           "你的WiFi名称"
#define WIFI_PASSWORD       "你的WiFi密码"
#define WIFI_RETRY_INTERVAL 10000   // WiFi 重连间隔（毫秒）

// ---------- MQTT 配置 ----------
#define MQTT_BROKER         "192.168.1.100"  // EMQX 服务器 IP
#define MQTT_PORT           1883
#define MQTT_USERNAME       "smartbrew"
#define MQTT_PASSWORD       "smartbrew2024"
#define MQTT_CLIENT_ID      "esp32_brew_001"
#define MQTT_RETRY_INTERVAL 5000    // MQTT 重连间隔（毫秒）
#define MQTT_KEEPALIVE      60      // MQTT KeepAlive（秒）

// ---------- MQTT Topic ----------
#define TOPIC_DATA          "smartbrew/device/%s/data"
#define TOPIC_HEARTBEAT     "smartbrew/device/%s/heartbeat"
#define TOPIC_STATUS        "smartbrew/device/%s/status"
#define TOPIC_CONTROL       "smartbrew/device/%s/control"

// ---------- 传感器引脚 ----------
#define PIN_DS18B20         4    // DS18B20 数据引脚（GPIO4）
#define PIN_DHT22           5    // DHT22  数据引脚（GPIO5）
#define PIN_FAN_RELAY       16   // 风扇继电器控制（GPIO16）
#define PIN_HEATER_RELAY    17   // 加热继电器控制（GPIO17）

// ---------- 采集周期 ----------
#define TEMP_INTERVAL_MS    30000   // 温度采集间隔（30秒）
#define ENV_INTERVAL_MS     60000   // 环境采集间隔（60秒）
#define HEARTBEAT_INTERVAL_MS 60000 // 心跳上报间隔（60秒）

// ---------- 固件信息 ----------
#define FIRMWARE_VERSION    "1.0.0"
#define DEVICE_ID           "esp32_brew_001"

// ---------- 温控阈值 ----------
#define TEMP_HIGH_THRESHOLD 25.0f   // 超过此温度自动开风扇
#define TEMP_LOW_THRESHOLD  18.0f   // 低于此温度自动开加热

#endif // CONFIG_H
