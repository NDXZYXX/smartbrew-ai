package com.smartbrew.smartbrew.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartbrew.smartbrew.dto.SensorDataVO;
import com.smartbrew.smartbrew.entity.Device;
import com.smartbrew.smartbrew.mapper.DeviceMapper;
import com.smartbrew.smartbrew.service.AiService;
import com.smartbrew.smartbrew.service.SensorDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 定时分析任务
 * 每小时扫描一次：对所有在线设备执行 AI 发酵状态分析
 */
@Component
public class AiScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(AiScheduledTask.class);

    private final DeviceMapper deviceMapper;
    private final SensorDataService sensorDataService;
    private final AiService aiService;

    public AiScheduledTask(DeviceMapper deviceMapper,
                           SensorDataService sensorDataService,
                           AiService aiService) {
        this.deviceMapper = deviceMapper;
        this.sensorDataService = sensorDataService;
        this.aiService = aiService;
    }

    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void analyzeAllOnlineDevices() {
        log.info("AI 定时分析任务开始");

        List<Device> onlineDevices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>().eq(Device::getStatus, 1));

        if (onlineDevices.isEmpty()) {
            log.info("无在线设备，跳过 AI 分析");
            return;
        }

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (Device device : onlineDevices) {
            try {
                // 检查最新数据新鲜度：超过 15 分钟无数据则跳过
                SensorDataVO latest = sensorDataService.getLatest(device.getDeviceId());
                if (latest == null || latest.getCreateTime() == null) {
                    log.info("设备 {} 无传感器数据，跳过 AI 分析", device.getDeviceId());
                    skipCount++;
                    continue;
                }

                if (latest.getCreateTime().isBefore(LocalDateTime.now().minusMinutes(15))) {
                    log.info("设备 {} 最新数据过期（{}），跳过 AI 分析", device.getDeviceId(), latest.getCreateTime());
                    skipCount++;
                    continue;
                }

                aiService.triggerAnalysis(device.getDeviceId(), "STATUS_ANALYSIS", null);
                successCount++;
                log.info("设备 {} AI 分析完成", device.getDeviceId());
            } catch (Exception e) {
                log.error("设备 {} AI 分析异常: {}", device.getDeviceId(), e.getMessage());
                failCount++;
            }
        }

        log.info("AI 定时分析任务完成: 成功={} 跳过={} 失败={}", successCount, skipCount, failCount);
    }
}
