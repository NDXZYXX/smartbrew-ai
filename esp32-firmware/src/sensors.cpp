#include "sensors.h"

// ============================================================
// TankTempSensor — DS18B20 桶内温度
// ============================================================

TankTempSensor::TankTempSensor(uint8_t pin)
    : _oneWire(pin)
    , _sensor(&_oneWire)
    , _lastReadMs(0)
    , _lastResult(false)
    , _lastTemp(-127.0f)
{
}

void TankTempSensor::begin() {
    _sensor.begin();
    // DS18B20 分辨率设为 11bit（0.125℃ 精度，375ms 转换时间）
    _sensor.setResolution(11);
    Serial.println("[Sensor] DS18B20 初始化完成");
}

bool TankTempSensor::read(float &temperature) {
    _sensor.requestTemperatures();
    float tmp = _sensor.getTempCByIndex(0);

    // DS18B20 读取失败时返回 -127，返回上一次的有效值
    if (tmp == DEVICE_DISCONNECTED_C || tmp < -55.0f || tmp > 125.0f) {
        Serial.println("[Sensor] DS18B20 读取失败，使用上次缓存值");
        if (_lastResult) {
            temperature = _lastTemp;
        }
        return _lastResult;
    }

    _lastTemp = tmp;
    _lastResult = true;
    temperature = tmp;
    return true;
}

// ============================================================
// EnvSensor — DHT22 环境温湿度
// ============================================================

EnvSensor::EnvSensor(uint8_t pin)
    : _dht(pin, DHT22)
    , _lastReadMs(0)
    , _lastResult(false)
    , _lastTemp(0.0f)
    , _lastHumidity(0.0f)
{
}

void EnvSensor::begin() {
    _dht.begin();
    Serial.println("[Sensor] DHT22 初始化完成");
}

bool EnvSensor::read(float &temperature, float &humidity) {
    float h = _dht.readHumidity();
    float t = _dht.readTemperature();

    // DHT22 读取失败时返回 NaN
    if (isnan(h) || isnan(t)) {
        Serial.println("[Sensor] DHT22 读取失败，使用上次缓存值");
        if (_lastResult) {
            temperature = _lastTemp;
            humidity = _lastHumidity;
        }
        return _lastResult;
    }

    // 合理性校验
    if (t < -40.0f || t > 80.0f || h < 0.0f || h > 100.0f) {
        Serial.printf("[Sensor] DHT22 数值异常 t=%.1f h=%.1f，丢弃\n", t, h);
        if (_lastResult) {
            temperature = _lastTemp;
            humidity = _lastHumidity;
        }
        return _lastResult;
    }

    _lastTemp = t;
    _lastHumidity = h;
    _lastResult = true;
    temperature = t;
    humidity = h;
    return true;
}
