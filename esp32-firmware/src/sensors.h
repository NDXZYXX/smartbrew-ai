#ifndef SENSORS_H
#define SENSORS_H

#include <Arduino.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <DHT.h>

// ============================================================
// DS18B20 桶内温度传感器（单总线）
// ============================================================
class TankTempSensor {
public:
    TankTempSensor(uint8_t pin);
    void begin();
    bool read(float &temperature);  // 返回 true 表示读取成功

private:
    OneWire _oneWire;
    DallasTemperature _sensor;
    unsigned long _lastReadMs;
    bool _lastResult;
    float _lastTemp;
};

// ============================================================
// DHT22 环境温湿度传感器
// ============================================================
class EnvSensor {
public:
    EnvSensor(uint8_t pin);
    void begin();
    bool read(float &temperature, float &humidity);

private:
    DHT _dht;
    unsigned long _lastReadMs;
    bool _lastResult;
    float _lastTemp;
    float _lastHumidity;
};

#endif // SENSORS_H
