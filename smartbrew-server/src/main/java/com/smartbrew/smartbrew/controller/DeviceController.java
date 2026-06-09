package com.smartbrew.smartbrew.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartbrew.smartbrew.dto.ApiResult;
import com.smartbrew.smartbrew.dto.DeviceRegisterRequest;
import com.smartbrew.smartbrew.dto.SensorDataVO;
import com.smartbrew.smartbrew.entity.Device;
import com.smartbrew.smartbrew.service.DeviceService;
import com.smartbrew.smartbrew.service.SensorDataService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;
    private final SensorDataService sensorDataService;

    public DeviceController(DeviceService deviceService, SensorDataService sensorDataService) {
        this.deviceService = deviceService;
        this.sensorDataService = sensorDataService;
    }

    /**
     * 设备注册
     * POST /api/device/register
     */
    @PostMapping("/register")
    public ApiResult<Device> register(@Valid @RequestBody DeviceRegisterRequest request) {
        try {
            Device device = deviceService.registerDevice(request);
            return ApiResult.ok(device);
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(400, e.getMessage());
        }
    }

    /**
     * 设备列表（分页 + 搜索）
     * GET /api/device/list?page=1&size=10&keyword=
     */
    @GetMapping("/list")
    public ApiResult<Page<Device>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Page<Device> result = deviceService.listDevices(page, size, keyword);
        return ApiResult.ok(result);
    }

    /**
     * 设备详情
     * GET /api/device/{deviceId}
     */
    @GetMapping("/{deviceId}")
    public ApiResult<Device> detail(@PathVariable String deviceId) {
        try {
            Device device = deviceService.getDevice(deviceId);
            return ApiResult.ok(device);
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(404, e.getMessage());
        }
    }

    /**
     * 更新设备信息
     * PUT /api/device/{deviceId}
     */
    @PutMapping("/{deviceId}")
    public ApiResult<Device> update(
            @PathVariable String deviceId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceSecret) {
        try {
            Device device = deviceService.updateDevice(deviceId, deviceName, deviceSecret);
            return ApiResult.ok(device);
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(400, e.getMessage());
        }
    }

    /**
     * 获取设备最新传感器数据
     * GET /api/device/latest?deviceId=xxx
     */
    @GetMapping("/latest")
    public ApiResult<SensorDataVO> latest(@RequestParam String deviceId) {
        try {
            SensorDataVO data = sensorDataService.getLatest(deviceId);
            if (data == null) {
                return ApiResult.fail(404, "暂无该设备的传感器数据");
            }
            return ApiResult.ok(data);
        } catch (Exception e) {
            return ApiResult.fail(500, e.getMessage());
        }
    }

    /**
     * 查询设备历史传感器数据（分页）
     * GET /api/device/history?deviceId=xxx&startTime=...&endTime=...&page=1&size=20
     */
    @GetMapping("/history")
    public ApiResult<Page<SensorDataVO>> history(
            @RequestParam String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<SensorDataVO> result = sensorDataService.getHistory(deviceId, startTime, endTime, page, size);
            return ApiResult.ok(result);
        } catch (Exception e) {
            return ApiResult.fail(500, e.getMessage());
        }
    }
}
