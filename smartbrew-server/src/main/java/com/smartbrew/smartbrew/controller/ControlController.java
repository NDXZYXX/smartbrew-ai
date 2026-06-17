package com.smartbrew.smartbrew.controller;

import com.smartbrew.smartbrew.dto.ApiResult;
import com.smartbrew.smartbrew.dto.ControlRequest;
import com.smartbrew.smartbrew.service.ControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "设备控制", description = "通过 MQTT 向设备下发控制指令（风扇/加热开关）")
@RestController
@RequestMapping("/api/device")
public class ControlController {

    private final ControlService controlService;

    public ControlController(ControlService controlService) {
        this.controlService = controlService;
    }

    @Operation(
            summary = "设备控制指令下发",
            description = """
                    向指定设备发送 MQTT 控制指令，控制风扇或加热器开关。
                    支持手动控制（MANUAL）和自动温控（AUTO）两种触发来源。
                    设备必须在线才能接收指令，指令会记录到 device_control_log 表。
                    """
    )
    @PostMapping("/{deviceId}/control")
    public ApiResult<Void> control(
            @Parameter(description = "设备唯一标识", required = true) @PathVariable String deviceId,
            @Valid @RequestBody ControlRequest request) {
        try {
            controlService.sendControl(deviceId, request.getTarget(), request.getCommand());
            return ApiResult.ok();
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(400, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResult.fail(409, e.getMessage());
        } catch (Exception e) {
            return ApiResult.fail(500, "控制指令下发失败: " + e.getMessage());
        }
    }
}
