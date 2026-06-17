package com.smartbrew.smartbrew.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartbrew.smartbrew.dto.ApiResult;
import com.smartbrew.smartbrew.dto.DeviceRegisterRequest;
import com.smartbrew.smartbrew.dto.SensorDataVO;
import com.smartbrew.smartbrew.entity.Device;
import com.smartbrew.smartbrew.service.DeviceService;
import com.smartbrew.smartbrew.service.SensorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "设备管理", description = "设备注册、列表查询、详情、更新，以及传感器数据查询")
@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;
    private final SensorDataService sensorDataService;

    public DeviceController(DeviceService deviceService, SensorDataService sensorDataService) {
        this.deviceService = deviceService;
        this.sensorDataService = sensorDataService;
    }

    @Operation(summary = "设备注册", description = "注册一个新设备，支持手动注册和 ESP32 心跳自动注册")
    @PostMapping("/register")
    public ApiResult<Device> register(@Valid @RequestBody DeviceRegisterRequest request) {
        try {
            Device device = deviceService.registerDevice(request);
            return ApiResult.ok(device);
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "设备列表", description = "分页查询设备列表，支持按设备名称/ID 关键字搜索")
    @GetMapping("/list")
    public ApiResult<Page<Device>> list(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键字（设备名称/ID）") @RequestParam(required = false) String keyword) {
        Page<Device> result = deviceService.listDevices(page, size, keyword);
        return ApiResult.ok(result);
    }

    @Operation(summary = "设备详情", description = "根据设备 ID 查询单个设备的详细信息")
    @GetMapping("/{deviceId}")
    public ApiResult<Device> detail(
            @Parameter(description = "设备唯一标识（ESP32 MAC 地址）", example = "A4:CF:12:AB:CD:EF")
            @PathVariable String deviceId) {
        try {
            Device device = deviceService.getDevice(deviceId);
            return ApiResult.ok(device);
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(404, e.getMessage());
        }
    }

    @Operation(summary = "更新设备信息", description = "修改设备名称或密钥")
    @PutMapping("/{deviceId}")
    public ApiResult<Device> update(
            @Parameter(description = "设备唯一标识") @PathVariable String deviceId,
            @Parameter(description = "新设备名称") @RequestParam(required = false) String deviceName,
            @Parameter(description = "新设备密钥") @RequestParam(required = false) String deviceSecret) {
        try {
            Device device = deviceService.updateDevice(deviceId, deviceName, deviceSecret);
            return ApiResult.ok(device);
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "最新传感器数据", description = "获取指定设备的最新温湿度数据（Redis 缓存优先，DB 回退）")
    @GetMapping("/latest")
    public ApiResult<SensorDataVO> latest(
            @Parameter(description = "设备唯一标识", required = true) @RequestParam String deviceId) {
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

    @Operation(summary = "历史传感器数据", description = "按时间范围分页查询设备历史温湿度数据")
    @GetMapping("/history")
    public ApiResult<Page<SensorDataVO>> history(
            @Parameter(description = "设备唯一标识", required = true) @RequestParam String deviceId,
            @Parameter(description = "开始时间 (ISO 8601)", example = "2026-06-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间 (ISO 8601)", example = "2026-06-17T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        try {
            Page<SensorDataVO> result = sensorDataService.getHistory(deviceId, startTime, endTime, page, size);
            return ApiResult.ok(result);
        } catch (Exception e) {
            return ApiResult.fail(500, e.getMessage());
        }
    }
}
