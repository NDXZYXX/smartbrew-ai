package com.smartbrew.smartbrew.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "设备控制指令请求")
@Data
public class ControlRequest {

    @Schema(description = "控制目标：FAN（风扇）或 HEATER（加热器）", example = "FAN")
    @NotBlank(message = "控制目标不能为空")
    @Pattern(regexp = "FAN|HEATER", message = "控制目标只能是 FAN 或 HEATER")
    private String target;

    @Schema(description = "指令：ON（开启）或 OFF（关闭）", example = "ON")
    @NotBlank(message = "指令不能为空")
    @Pattern(regexp = "ON|OFF", message = "指令只能是 ON 或 OFF")
    private String command;
}
