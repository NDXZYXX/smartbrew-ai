package com.smartbrew.smartbrew.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "设备注册请求")
@Data
public class DeviceRegisterRequest {

    @Schema(description = "设备唯一标识（ESP32 MAC 地址）", example = "A4:CF:12:AB:CD:EF")
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    @Schema(description = "设备名称", example = "1号发酵桶")
    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    @Schema(description = "设备密钥（MQTT 认证用）", example = "a1b2c3d4e5f6")
    private String deviceSecret;
}
