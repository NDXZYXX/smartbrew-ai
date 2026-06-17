package com.smartbrew.smartbrew.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartbrew.smartbrew.dto.AlarmRecordVO;
import com.smartbrew.smartbrew.dto.ApiResult;
import com.smartbrew.smartbrew.service.AlarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "告警管理", description = "告警记录查询、筛选与手动清除")
@RestController
@RequestMapping("/api/alarm")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @Operation(summary = "告警列表", description = "分页查询告警记录，支持按设备、告警类型、清除状态筛选")
    @GetMapping("/list")
    public ApiResult<Page<AlarmRecordVO>> list(
            @Parameter(description = "设备 ID（可选筛选）") @RequestParam(required = false) String deviceId,
            @Parameter(description = "告警类型：HIGH_TEMP / LOW_TEMP / DEVICE_OFFLINE") @RequestParam(required = false) String alarmType,
            @Parameter(description = "是否已清除：0=未清除, 1=已清除") @RequestParam(required = false) Integer isCleared,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        try {
            Page<AlarmRecordVO> result = alarmService.getAlarmList(deviceId, alarmType, isCleared, page, size);
            return ApiResult.ok(result);
        } catch (Exception e) {
            return ApiResult.fail(500, e.getMessage());
        }
    }

    @Operation(summary = "清除告警", description = "手动清除一条告警记录")
    @PutMapping("/{alarmId}/clear")
    public ApiResult<Void> clear(
            @Parameter(description = "告警记录 ID", example = "1") @PathVariable Long alarmId) {
        try {
            alarmService.clearAlarm(alarmId);
            return ApiResult.ok();
        } catch (IllegalArgumentException e) {
            return ApiResult.fail(400, e.getMessage());
        } catch (Exception e) {
            return ApiResult.fail(500, e.getMessage());
        }
    }
}
