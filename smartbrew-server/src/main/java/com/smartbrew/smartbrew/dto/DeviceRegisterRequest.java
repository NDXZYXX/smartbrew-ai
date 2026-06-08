package com.smartbrew.smartbrew.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceRegisterRequest {
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    private String deviceSecret;
}
