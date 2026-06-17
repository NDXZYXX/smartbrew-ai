package com.smartbrew.smartbrew.controller;

import com.smartbrew.smartbrew.dto.ApiResult;
import com.smartbrew.smartbrew.dto.ControlRequest;
import com.smartbrew.smartbrew.service.ControlService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device")
public class ControlController {

    private final ControlService controlService;

    public ControlController(ControlService controlService) {
        this.controlService = controlService;
    }

    /**
     * 设备控制指令下发
     * POST /api/device/{deviceId}/control
     */
    @PostMapping("/{deviceId}/control")
    public ApiResult<Void> control(@PathVariable String deviceId,
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
