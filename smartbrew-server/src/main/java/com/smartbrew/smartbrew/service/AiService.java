package com.smartbrew.smartbrew.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbrew.smartbrew.config.AiProperties;
import com.smartbrew.smartbrew.dto.AiAnalysisRecordVO;
import com.smartbrew.smartbrew.dto.SensorDataVO;
import com.smartbrew.smartbrew.entity.AiAnalysisRecord;
import com.smartbrew.smartbrew.mapper.AiAnalysisRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiAnalysisRecordMapper aiAnalysisRecordMapper;
    private final SensorDataService sensorDataService;
    private final DeepSeekClient deepSeekClient;
    private final AiProperties aiProperties;

    public AiService(AiAnalysisRecordMapper aiAnalysisRecordMapper,
                     SensorDataService sensorDataService,
                     DeepSeekClient deepSeekClient,
                     AiProperties aiProperties) {
        this.aiAnalysisRecordMapper = aiAnalysisRecordMapper;
        this.sensorDataService = sensorDataService;
        this.deepSeekClient = deepSeekClient;
        this.aiProperties = aiProperties;
    }

    /**
     * 触发 AI 分析
     */
    public AiAnalysisRecordVO triggerAnalysis(String deviceId, String analysisType, Long batchId) {
        // 1. 获取传感器数据
        SensorDataVO latest = sensorDataService.getLatest(deviceId);
        if (latest == null) {
            throw new IllegalArgumentException("设备 " + deviceId + " 无传感器数据，无法分析");
        }

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(6);
        Page<SensorDataVO> historyPage = sensorDataService.getHistory(deviceId, start, end, 1, 1000);
        List<SensorDataVO> historyRecords = historyPage != null ? historyPage.getRecords() : List.of();

        // 2. 计算趋势
        String trendSummary = computeTrendSummary(historyRecords);

        // 3. 构建 input snapshot
        String inputSnapshot = buildInputSnapshot(latest, trendSummary, historyRecords.size());

        // 4. 构建 Prompt
        String systemPrompt = buildSystemPrompt();
        String userMessage = buildUserMessage(latest, trendSummary);

        // 5. 调用 DeepSeek API
        long startMs = System.currentTimeMillis();
        DeepSeekClient.DeepSeekResponse response;
        try {
            List<DeepSeekClient.ChatMessage> messages = new ArrayList<>();
            messages.add(new DeepSeekClient.ChatMessage("system", systemPrompt));
            messages.add(new DeepSeekClient.ChatMessage("user", userMessage));
            response = deepSeekClient.chat(messages);
        } catch (Exception e) {
            log.error("AI 分析调用失败: deviceId={}", deviceId, e);
            throw new RuntimeException("AI 分析失败: " + e.getMessage(), e);
        }

        int responseTimeMs = (int) (System.currentTimeMillis() - startMs);

        // 6. 提取响应文本
        String analysisResult = "";
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            DeepSeekClient.Choice choice = response.getChoices().get(0);
            if (choice.getMessage() != null) {
                analysisResult = choice.getMessage().getContent();
            }
        }

        if (analysisResult == null || analysisResult.isEmpty()) {
            throw new RuntimeException("AI 返回空结果");
        }

        // 7. 解析结构化字段
        String statusAssessment = null;
        String riskWarning = null;
        String suggestion = null;
        LocalDateTime predictedEndTime = null;

        try {
            String jsonStr = extractJson(analysisResult);
            JsonNode root = objectMapper.readTree(jsonStr);
            if (root.has("statusAssessment")) {
                statusAssessment = root.get("statusAssessment").asText();
            }
            if (root.has("riskWarning")) {
                riskWarning = root.get("riskWarning").asText();
            }
            if (root.has("suggestion")) {
                suggestion = root.get("suggestion").asText();
            }
            if (root.has("predictedEndTime") && !root.get("predictedEndTime").isNull()) {
                String timeStr = root.get("predictedEndTime").asText();
                try {
                    predictedEndTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                    log.warn("无法解析 predictedEndTime: {}", timeStr);
                }
            }
        } catch (Exception e) {
            log.warn("AI 返回非 JSON 格式，使用原始文本。deviceId={} error={}", deviceId, e.getMessage());
            statusAssessment = "AI返回格式异常，详见原始结果";
        }

        // 8. 构建 user message 的完整文本作为 prompt 存储
        String fullPrompt = "[System]\n" + systemPrompt + "\n\n[User]\n" + userMessage;

        // 9. 存储记录
        AiAnalysisRecord record = new AiAnalysisRecord();
        record.setDeviceId(deviceId);
        record.setBatchId(batchId);
        record.setAnalysisType(analysisType);
        record.setInputSnapshot(inputSnapshot);
        record.setPrompt(fullPrompt);
        record.setAnalysisResult(analysisResult);
        record.setStatusAssessment(statusAssessment);
        record.setRiskWarning(riskWarning);
        record.setSuggestion(suggestion);
        record.setPredictedEndTime(predictedEndTime);
        record.setAiModel(aiProperties.getModel());
        record.setResponseTimeMs(responseTimeMs);

        aiAnalysisRecordMapper.insert(record);
        log.info("AI 分析记录已保存: id={} deviceId={} type={} time={}ms",
                record.getId(), deviceId, analysisType, responseTimeMs);

        return toVO(record);
    }

    /**
     * 分页查询分析记录
     */
    public Page<AiAnalysisRecordVO> list(String deviceId, String analysisType, int pageNum, int pageSize) {
        LambdaQueryWrapper<AiAnalysisRecord> wrapper = new LambdaQueryWrapper<>();
        if (deviceId != null && !deviceId.isEmpty()) {
            wrapper.eq(AiAnalysisRecord::getDeviceId, deviceId);
        }
        if (analysisType != null && !analysisType.isEmpty()) {
            wrapper.eq(AiAnalysisRecord::getAnalysisType, analysisType);
        }
        wrapper.orderByDesc(AiAnalysisRecord::getCreateTime);

        Page<AiAnalysisRecord> page = aiAnalysisRecordMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        Page<AiAnalysisRecordVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(this::toVO)
                .toList());
        return voPage;
    }

    /**
     * 查询单条详情
     */
    public AiAnalysisRecordVO getById(Long id) {
        AiAnalysisRecord record = aiAnalysisRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("分析记录不存在: " + id);
        }
        return toVO(record);
    }

    private String buildSystemPrompt() {
        return "你是一位专业的发酵工艺监控专家。请根据传感器数据分析发酵状态。\n"
                + "严格按JSON格式输出（不要包含markdown代码块标记）：\n"
                + "{\"statusAssessment\":\"状态评估\",\"riskWarning\":\"风险提示\","
                + "\"suggestion\":\"调整建议\",\"predictedEndTime\":\"ISO8601格式时间或null\","
                + "\"confidence\":0.8}\n"
                + "statusAssessment: 对当前发酵状态的简要评估\n"
                + "riskWarning: 是否存在风险，无风险时填\"无\"\n"
                + "suggestion: 具体的调整建议，无需调整时填\"无需调整\"\n"
                + "predictedEndTime: 预测发酵结束时间(ISO8601)，无法预测时填null\n"
                + "confidence: 置信度 0.0-1.0";
    }

    private String buildUserMessage(SensorDataVO latest, String trendSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append("请分析以下发酵设备传感器数据：\n\n");
        sb.append("当前数据：\n");
        sb.append("- 桶内温度: ").append(latest.getTankTemperature()).append("℃\n");
        sb.append("- 环境温度: ").append(latest.getEnvTemperature()).append("℃\n");
        sb.append("- 环境湿度: ").append(latest.getEnvHumidity()).append("%\n");
        sb.append("- 数据时间: ").append(latest.getCreateTime()).append("\n\n");
        sb.append("近6小时趋势：\n").append(trendSummary);
        return sb.toString();
    }

    private String buildInputSnapshot(SensorDataVO latest, String trendSummary, int historyCount) {
        try {
            var snapshot = new java.util.LinkedHashMap<String, Object>();
            snapshot.put("tankTemperature", latest.getTankTemperature());
            snapshot.put("envTemperature", latest.getEnvTemperature());
            snapshot.put("envHumidity", latest.getEnvHumidity());
            snapshot.put("dataTime", latest.getCreateTime() != null ? latest.getCreateTime().toString() : null);
            snapshot.put("historyCount", historyCount);
            snapshot.put("trendSummary", trendSummary);
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.warn("构建 inputSnapshot JSON 失败", e);
            return "{}";
        }
    }

    private String computeTrendSummary(List<SensorDataVO> records) {
        if (records == null || records.isEmpty()) {
            return "暂无足够历史数据计算趋势";
        }

        BigDecimal tankMin = null, tankMax = null, tankSum = BigDecimal.ZERO;
        BigDecimal envMin = null, envMax = null, envSum = BigDecimal.ZERO;
        BigDecimal humMin = null, humMax = null, humSum = BigDecimal.ZERO;
        int validTank = 0, validEnv = 0, validHum = 0;

        for (SensorDataVO d : records) {
            if (d.getTankTemperature() != null) {
                BigDecimal v = d.getTankTemperature();
                if (tankMin == null || v.compareTo(tankMin) < 0) tankMin = v;
                if (tankMax == null || v.compareTo(tankMax) > 0) tankMax = v;
                tankSum = tankSum.add(v);
                validTank++;
            }
            if (d.getEnvTemperature() != null) {
                BigDecimal v = d.getEnvTemperature();
                if (envMin == null || v.compareTo(envMin) < 0) envMin = v;
                if (envMax == null || v.compareTo(envMax) > 0) envMax = v;
                envSum = envSum.add(v);
                validEnv++;
            }
            if (d.getEnvHumidity() != null) {
                BigDecimal v = d.getEnvHumidity();
                if (humMin == null || v.compareTo(humMin) < 0) humMin = v;
                if (humMax == null || v.compareTo(humMax) > 0) humMax = v;
                humSum = humSum.add(v);
                validHum++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("共 ").append(records.size()).append(" 条历史数据：\n");

        if (validTank > 0) {
            sb.append("桶内温度: 最低").append(tankMin).append("℃, 最高").append(tankMax)
                    .append("℃, 平均").append(tankSum.divide(new BigDecimal(validTank), 2, RoundingMode.HALF_UP))
                    .append("℃\n");
        }
        if (validEnv > 0) {
            sb.append("环境温度: 最低").append(envMin).append("℃, 最高").append(envMax)
                    .append("℃, 平均").append(envSum.divide(new BigDecimal(validEnv), 2, RoundingMode.HALF_UP))
                    .append("℃\n");
        }
        if (validHum > 0) {
            sb.append("环境湿度: 最低").append(humMin).append("%, 最高").append(humMax)
                    .append("%, 平均").append(humSum.divide(new BigDecimal(validHum), 2, RoundingMode.HALF_UP))
                    .append("%\n");
        }

        return sb.toString();
    }

    /**
     * 从 AI 返回文本中提取 JSON 字符串
     */
    private String extractJson(String text) {
        if (text == null || text.isEmpty()) return "{}";
        String trimmed = text.trim();
        // 去掉可能的 markdown 代码块标记
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("\n");
            if (start > 0) {
                int end = trimmed.lastIndexOf("```");
                if (end > start) {
                    trimmed = trimmed.substring(start, end).trim();
                }
            }
        }
        // 找到第一个 { 和最后一个 }
        int jsonStart = trimmed.indexOf('{');
        int jsonEnd = trimmed.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1);
        }
        return trimmed;
    }

    private AiAnalysisRecordVO toVO(AiAnalysisRecord record) {
        AiAnalysisRecordVO vo = new AiAnalysisRecordVO();
        vo.setId(record.getId());
        vo.setDeviceId(record.getDeviceId());
        vo.setBatchId(record.getBatchId());
        vo.setAnalysisType(record.getAnalysisType());
        vo.setInputSnapshot(record.getInputSnapshot());
        vo.setPrompt(record.getPrompt());
        vo.setAnalysisResult(record.getAnalysisResult());
        vo.setStatusAssessment(record.getStatusAssessment());
        vo.setRiskWarning(record.getRiskWarning());
        vo.setSuggestion(record.getSuggestion());
        vo.setPredictedEndTime(record.getPredictedEndTime());
        vo.setAiModel(record.getAiModel());
        vo.setResponseTimeMs(record.getResponseTimeMs());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}
