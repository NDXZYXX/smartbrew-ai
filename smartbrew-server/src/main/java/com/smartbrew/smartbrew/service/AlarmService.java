package com.smartbrew.smartbrew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartbrew.smartbrew.dto.AlarmRecordVO;
import com.smartbrew.smartbrew.entity.AlarmRecord;
import com.smartbrew.smartbrew.entity.SensorData;
import com.smartbrew.smartbrew.entity.SystemConfig;
import com.smartbrew.smartbrew.mapper.AlarmRecordMapper;
import com.smartbrew.smartbrew.mapper.SystemConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AlarmService {

    private static final Logger log = LoggerFactory.getLogger(AlarmService.class);

    private final AlarmRecordMapper alarmRecordMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final DeviceEventService eventService;

    public AlarmService(AlarmRecordMapper alarmRecordMapper,
                        SystemConfigMapper systemConfigMapper,
                        DeviceEventService eventService) {
        this.alarmRecordMapper = alarmRecordMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.eventService = eventService;
    }

    /**
     * 检查温度是否超出阈值，超出则产生告警
     */
    public void checkTemperature(String deviceId, SensorData data) {
        BigDecimal tankTemp = data.getTankTemperature();
        if (tankTemp == null) return;

        BigDecimal highThreshold = getConfigAsDecimal("alarm.temp.high", new BigDecimal("30.00"));
        BigDecimal lowThreshold = getConfigAsDecimal("alarm.temp.low", new BigDecimal("15.00"));

        if (tankTemp.compareTo(highThreshold) > 0) {
            insertAlarm(deviceId, data.getId(), "HIGH_TEMP", "WARN",
                    "桶内高温告警",
                    String.format("桶内温度 %.2f℃ 超过高温阈值 %.2f℃", tankTemp, highThreshold),
                    tankTemp, highThreshold);
            // 如果高温，同时清除之前的低温告警（温度不可能同时高和低）
            clearAlarmAuto(deviceId, "LOW_TEMP");
        } else if (tankTemp.compareTo(lowThreshold) < 0) {
            insertAlarm(deviceId, data.getId(), "LOW_TEMP", "WARN",
                    "桶内低温告警",
                    String.format("桶内温度 %.2f℃ 低于低温阈值 %.2f℃", tankTemp, lowThreshold),
                    tankTemp, lowThreshold);
            clearAlarmAuto(deviceId, "HIGH_TEMP");
        } else {
            // 温度恢复正常，清除所有温度相关告警
            clearAlarmAuto(deviceId, "HIGH_TEMP");
            clearAlarmAuto(deviceId, "LOW_TEMP");
        }
    }

    /**
     * 设备离线告警
     */
    public void checkOffline(String deviceId) {
        insertAlarm(deviceId, null, "DEVICE_OFFLINE", "ERROR",
                "设备离线",
                "心跳超时，设备已自动标记为离线",
                null, null);
    }

    /**
     * 设备上线时，自动清除该设备所有未清除的 DEVICE_OFFLINE 告警
     */
    public void clearOfflineAlarms(String deviceId) {
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<AlarmRecord>()
                .eq(AlarmRecord::getDeviceId, deviceId)
                .eq(AlarmRecord::getAlarmType, "DEVICE_OFFLINE")
                .eq(AlarmRecord::getIsCleared, 0);

        var alarms = alarmRecordMapper.selectList(wrapper);
        for (AlarmRecord alarm : alarms) {
            alarm.setIsCleared(1);
            alarm.setClearedTime(LocalDateTime.now());
            alarmRecordMapper.updateById(alarm);
            log.info("设备上线，自动清除离线告警: device={} alarmId={}", deviceId, alarm.getId());
        }
    }

    /**
     * 插入告警（带判重逻辑）
     */
    private void insertAlarm(String deviceId, Long sensorDataId, String alarmType,
                             String alarmLevel, String alarmTitle, String alarmMessage,
                             BigDecimal alarmValue, BigDecimal thresholdValue) {
        // 判重：同 deviceId + 同 alarmType + isCleared=0 已存在则跳过
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<AlarmRecord>()
                .eq(AlarmRecord::getDeviceId, deviceId)
                .eq(AlarmRecord::getAlarmType, alarmType)
                .eq(AlarmRecord::getIsCleared, 0);

        if (alarmRecordMapper.selectCount(wrapper) > 0) {
            log.debug("告警已存在，跳过: device={} type={}", deviceId, alarmType);
            return;
        }

        AlarmRecord record = new AlarmRecord();
        record.setDeviceId(deviceId);
        record.setSensorDataId(sensorDataId);
        record.setAlarmType(alarmType);
        record.setAlarmLevel(alarmLevel);
        record.setAlarmTitle(alarmTitle);
        record.setAlarmMessage(alarmMessage);
        record.setAlarmValue(alarmValue);
        record.setThresholdValue(thresholdValue);
        record.setIsCleared(0);

        alarmRecordMapper.insert(record);
        log.info("告警产生: device={} type={} level={} title={}", deviceId, alarmType, alarmLevel, alarmTitle);

        // 同步记录设备事件
        eventService.record(deviceId, alarmType, alarmLevel, alarmTitle, alarmMessage, null);
    }

    /**
     * 自动清除某设备某类型的所有未清除告警
     */
    private void clearAlarmAuto(String deviceId, String alarmType) {
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<AlarmRecord>()
                .eq(AlarmRecord::getDeviceId, deviceId)
                .eq(AlarmRecord::getAlarmType, alarmType)
                .eq(AlarmRecord::getIsCleared, 0);

        var alarms = alarmRecordMapper.selectList(wrapper);
        for (AlarmRecord alarm : alarms) {
            alarm.setIsCleared(1);
            alarm.setClearedTime(LocalDateTime.now());
            alarmRecordMapper.updateById(alarm);
            log.info("告警自动清除: device={} type={} alarmId={}", deviceId, alarmType, alarm.getId());
        }
    }

    /**
     * 告警列表分页查询
     */
    public Page<AlarmRecordVO> getAlarmList(String deviceId, String alarmType,
                                             Integer isCleared, int pageNum, int pageSize) {
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        if (deviceId != null && !deviceId.isEmpty()) {
            wrapper.eq(AlarmRecord::getDeviceId, deviceId);
        }
        if (alarmType != null && !alarmType.isEmpty()) {
            wrapper.eq(AlarmRecord::getAlarmType, alarmType);
        }
        if (isCleared != null) {
            wrapper.eq(AlarmRecord::getIsCleared, isCleared);
        }
        wrapper.orderByDesc(AlarmRecord::getCreateTime);

        Page<AlarmRecord> page = alarmRecordMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        Page<AlarmRecordVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(this::toVO)
                .toList());
        return voPage;
    }

    /**
     * 手动清除单条告警
     */
    public void clearAlarm(Long alarmId) {
        AlarmRecord alarm = alarmRecordMapper.selectById(alarmId);
        if (alarm == null) {
            throw new IllegalArgumentException("告警记录不存在: " + alarmId);
        }
        alarm.setIsCleared(1);
        alarm.setClearedTime(LocalDateTime.now());
        alarmRecordMapper.updateById(alarm);
        log.info("告警手动清除: alarmId={}", alarmId);
    }

    private AlarmRecordVO toVO(AlarmRecord record) {
        AlarmRecordVO vo = new AlarmRecordVO();
        vo.setId(record.getId());
        vo.setDeviceId(record.getDeviceId());
        vo.setAlarmType(record.getAlarmType());
        vo.setAlarmLevel(record.getAlarmLevel());
        vo.setAlarmTitle(record.getAlarmTitle());
        vo.setAlarmMessage(record.getAlarmMessage());
        vo.setAlarmValue(record.getAlarmValue());
        vo.setThresholdValue(record.getThresholdValue());
        vo.setIsCleared(record.getIsCleared());
        vo.setClearedTime(record.getClearedTime());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private BigDecimal getConfigAsDecimal(String key, BigDecimal defaultValue) {
        SystemConfig config = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigKey, key));
        if (config != null && config.getConfigValue() != null) {
            try {
                return new BigDecimal(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("配置 {} 值格式错误: {}，使用默认值 {}", key, config.getConfigValue(), defaultValue);
            }
        }
        return defaultValue;
    }
}
