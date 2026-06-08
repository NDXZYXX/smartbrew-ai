package com.smartbrew.smartbrew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartbrew.smartbrew.dto.DeviceRegisterRequest;
import com.smartbrew.smartbrew.entity.Device;
import com.smartbrew.smartbrew.mapper.DeviceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceMapper deviceMapper;
    private final DeviceEventService eventService;
    private final RedisTemplate<String, Object> redisTemplate;

    public DeviceService(DeviceMapper deviceMapper,
                         DeviceEventService eventService,
                         RedisTemplate<String, Object> redisTemplate) {
        this.deviceMapper = deviceMapper;
        this.eventService = eventService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设备注册（后台手动注册，ESP32 也可通过心跳自动注册）
     */
    public Device registerDevice(DeviceRegisterRequest req) {
        // 检查是否已存在
        Device existing = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceId, req.getDeviceId()));
        if (existing != null) {
            throw new IllegalArgumentException("设备ID已存在: " + req.getDeviceId());
        }

        Device device = new Device();
        device.setDeviceId(req.getDeviceId());
        device.setDeviceName(req.getDeviceName());
        device.setDeviceSecret(req.getDeviceSecret() != null ? req.getDeviceSecret() : "");
        device.setStatus(0); // 初始离线，等心跳上线
        deviceMapper.insert(device);

        log.info("设备注册成功: id={} name={}", req.getDeviceId(), req.getDeviceName());

        // 记录事件
        eventService.record(device.getDeviceId(), "DEVICE_REGISTER",
                "INFO", "设备注册", "设备 " + req.getDeviceName() + " 注册成功", null);

        return device;
    }

    /**
     * 设备列表（分页，含在线状态）
     */
    public Page<Device> listDevices(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(Device::getDeviceId, keyword)
                    .or()
                    .like(Device::getDeviceName, keyword));
        }
        wrapper.orderByDesc(Device::getStatus)       // 在线优先
               .orderByDesc(Device::getUpdateTime);

        Page<Device> page = deviceMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        // 补充 Redis 在线状态（比 DB 更实时）
        for (Device device : page.getRecords()) {
            String online = (String) redisTemplate.opsForValue()
                    .get("device:online:" + device.getDeviceId());
            if ("1".equals(online) && device.getStatus() != 1) {
                device.setStatus(1); // Redis 显示在线但 DB 未更新
            }
        }

        return page;
    }

    /**
     * 设备详情
     */
    public Device getDevice(String deviceId) {
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceId, deviceId));
        if (device == null) {
            throw new IllegalArgumentException("设备不存在: " + deviceId);
        }
        return device;
    }

    /**
     * 更新设备信息（名称、密钥等）
     */
    public Device updateDevice(String deviceId, String deviceName, String deviceSecret) {
        Device device = getDevice(deviceId);
        if (deviceName != null && !deviceName.isEmpty()) {
            device.setDeviceName(deviceName);
        }
        if (deviceSecret != null) {
            device.setDeviceSecret(deviceSecret);
        }
        deviceMapper.updateById(device);
        return device;
    }
}
