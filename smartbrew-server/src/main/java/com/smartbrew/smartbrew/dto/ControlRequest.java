package com.smartbrew.smartbrew.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ControlRequest {
    /** FAN / HEATER */
    @NotBlank(message = "控制目标不能为空")
    @Pattern(regexp = "FAN|HEATER", message = "控制目标只能是 FAN 或 HEATER")
    private String target;

    /** ON / OFF */
    @NotBlank(message = "指令不能为空")
    @Pattern(regexp = "ON|OFF", message = "指令只能是 ON 或 OFF")
    private String command;
}
