package com.smartbrew.smartbrew.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartbrew.smartbrew.dto.AlarmRecordVO;
import com.smartbrew.smartbrew.dto.ApiResult;
import com.smartbrew.smartbrew.service.AlarmService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alarm")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    /**
     * 告警列表（分页 + 筛选）
     * GET /api/alarm/list?deviceId=&alarmType=&isCleared=&page=1&size=10
     */
    @GetMapping("/list")
    public ApiResult<Page<AlarmRecordVO>> list(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String alarmType,
            @RequestParam(required = false) Integer isCleared,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<AlarmRecordVO> result = alarmService.getAlarmList(deviceId, alarmType, isCleared, page, size);
            return ApiResult.ok(result);
        } catch (Exception e) {
            return ApiResult.fail(500, e.getMessage());
        }
    }

    /**
     * 手动清除告警
     * PUT /api/alarm/{alarmId}/clear
     */
    @PutMapping("/{alarmId}/clear")
    public ApiResult<Void> clear(@PathVariable Long alarmId) {
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
