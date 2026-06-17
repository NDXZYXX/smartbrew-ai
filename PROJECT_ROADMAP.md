# Smart Brew AI 项目开发路线图（V1.0）

> **最后更新**：2026-06-17
> **配套文件**：`PROJECT_STATUS.md`（每日进度日志）
> **下次恢复**：当前进度 9/11 Phase，下一步是 Phase 10（知识库）

---

## ⚠️ AI 防失忆机制（开始开发前必读）

**本文件 + `PROJECT_STATUS.md` = 项目的"长期记忆"**

每次新的AI会话开始时，请按以下顺序恢复上下文：

| 步骤 | 读取文件 | 作用 |
|:--:|----------|------|
| 1 | `PROJECT_ROADMAP.md`（本文件） | 了解项目全貌、当前进度、下一步做什么 |
| 2 | `PROJECT_STATUS.md` | 了解昨天做了什么、Git提交记录、遇到的问题 |
| 3 | `数据库列表.md` | 了解数据库表结构和字段 |
| 4 | `功能需求列表.md` | 了解完整功能需求规格 |

**开发过程中**：
- 每完成一个子任务 → 标记 Phase 内的复选框 `[x]`
- 每完成一个 Phase → 修改 Phase 状态为 `✅ 已完成`，更新概要表格
- 每天结束 → 在 `PROJECT_STATUS.md` 末尾追加日志

---

## 进度总览

| Phase | 名称 | 状态 | 完成日期 | 进度 |
|:-----:|------|:----:|:--------:|:----:|
| 1 | 基础设备接入 | ✅ 已完成 | 2026-06-08 | 5/5 |
| 2 | Spring Boot 后端 | ✅ 已完成 | 2026-06-08 | 4/4 |
| 3 | 数据库建设 | ✅ 已完成 | 2026-06-08 | 2/2 |
| 4 | 设备管理模块 | ✅ 已完成 | 2026-06-08 | 4/4 |
| 5 | 监控看板 | ✅ 已完成 | 2026-06-09 | 4/4 |
| 6 | 告警系统 | ✅ 已完成 | 2026-06-17 | 3/3 |
| 7 | 设备控制 | ✅ 已完成 | 2026-06-17 | 2/2 |
| 8 | 自动温控 | ✅ 已完成 | 2026-06-17 | 2/2 |
| 9 | AI分析 | ✅ 已完成 | 2026-06-17 | 3/3 |
| 10 | 知识库 | ⬜ 未开始 | - | 0/3 |
| 11 | Docker部署 | ⬜ 未开始 | - | 0/6 |

> 图例：⬜ 未开始 | 🔄 进行中 | ✅ 已完成 | ⏸️ 暂停

### 已完成 Phase 摘要

| Phase | 产出 | 关键文件 |
|:-----:|------|----------|
| 1 | ESP32 固件（DS18B20+DHT22+MQTT） | `esp32-firmware/src/` |
| 2 | Spring Boot 后端骨架 + MQTT 订阅 + 数据入库 | `smartbrew-server/` |
| 3 | V1 全部 10 张表 DDL + ER 图 | `schema.sql` + `数据库列表.md` |
| 4 | 设备 REST API + 心跳超时 + 事件记录 | `DeviceController.java` 等 |
| 5 | Vue3 监控看板 + ECharts 历史曲线 + 实时/历史 API | `smartbrew-web/` + `SensorDataService.java` |
| 6 | 告警规则引擎 + 告警记录API + Vue3告警中心 | `AlarmService.java` + `AlarmController.java` + `AlarmCenter.vue` |
| 7 | MQTT控制指令下发 + Vue3控制面板 | `ControlService.java` + `ControlController.java` + `ControlPanel.vue` |
| 8 | 自动温控规则引擎 + ESP32 GPIO反馈 | `TemperatureControlService.java` + `mqtt_handler.cpp` |
| 9 | DeepSeek API 封装 + AI 分析存储/查询 + Vue3 展示 | `AiService.java` + `AiController.java` + `AiAnalysis.vue` |

---

## 项目简介

Smart Brew AI 是一个基于 ESP32、MQTT、Spring Boot、Vue3 和 DeepSeek API 的智能发酵监控系统。

项目目标：

构建一个完整的 IoT + AI 应用系统，实现：

- 发酵数据采集
- MQTT设备接入
- 数据可视化
- 自动温控
- AI分析
- 发酵知识库

本项目主要用于：

- IoT工程实践
- 嵌入式开发能力展示
- 校招/社招简历项目
- GitHub开源项目

---

技术栈

设备端

- ESP32
- PlatformIO
- FreeRTOS
- MQTT Client

---

服务端

- Java 21
- Spring Boot 3
- MyBatis Plus
- MySQL 8
- Redis

---

消息中间件

- EMQX

---

前端

- Vue3
- Element Plus
- ECharts
- Axios

---

AI模块

- DeepSeek API

---

部署

- Docker
- Docker Compose
- Nginx

---

项目阶段规划

Phase 1：基础设备接入 &nbsp; ✅ 已完成

> **目标**：实现ESP32设备接入平台。
> **完成标准**：设备数据成功上传至MQTT Broker。

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 1.1 | DS18B20 温度采集（30s周期） | [x] |
| 1.2 | DHT22 环境温湿度采集（60s周期） | [x] |
| 1.3 | WiFi 自动联网 + 自动重连 | [x] |
| 1.4 | MQTT 连接 EMQX（含认证） | [x] |
| 1.5 | MQTT 数据上传（JSON格式） | [x] |

> 状态：[ ] 未完成 | [x] 已完成

---

Phase 2：Spring Boot后端 &nbsp; ✅ 已完成

> **目标**：完成设备数据接收与存储。
> **完成标准**：数据库成功保存设备数据。

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 2.1 | 搭建 Spring Boot 3 项目骨架（含 MyBatis Plus） | [x] |
| 2.2 | MQTT 订阅服务（订阅 smartbrew/+/data 等 Topic） | [x] |
| 2.3 | 数据解析 + 写入 sensor_data 表 | [x] |
| 2.4 | Redis 缓存最新设备数据 | [x]

---

Phase 3：数据库建设 &nbsp; ✅ 已完成

> **目标**：建立完整数据库结构。
> **完成标准**：完成 ER 图设计及 V1 全部10张表建表。
> **参考文件**：`数据库列表.md`

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 3.1 | 绘制 ER 图 | [x] |
| 3.2 | 执行 DDL 脚本，创建 V1 全部 10 张表 | [x]

---

Phase 4：设备管理模块 &nbsp; ✅ 已完成

> **完成标准**：设备上线状态实时显示。

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 4.1 | 设备注册接口（POST /api/device/register） | [x] |
| 4.2 | 心跳超时检测（120s无心跳 → 离线） | [x] |
| 4.3 | 设备事件记录（上下线/发酵起止等） | [x] |
| 4.4 | 设备列表查询 + 在线状态展示 | [x]

---

Phase 5：监控看板 &nbsp; ✅ 已完成

> **完成标准**：实现实时监控页面（Vue3 + ECharts）。

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 5.1 | 实时数据接口 GET /api/device/latest | [x] |
| 5.2 | 历史数据接口 GET /api/device/history | [x] |
| 5.3 | Vue3 实时监控面板（温度/湿度/在线状态/发酵天数） | [x] |
| 5.4 | ECharts 温度 & 湿度历史曲线 | [x]

---

Phase 6：告警系统 &nbsp; ✅ 已完成

> **完成标准**：异常状态自动记录，前端告警中心可查。

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 6.1 | 告警规则引擎（高温/低温/离线判定） | [x] |
| 6.2 | 告警记录写入 + 查询接口 | [x] |
| 6.3 | Vue3 告警中心页面 | [x]

---

Phase 7：设备控制 &nbsp; ✅ 已完成

> **完成标准**：网页通过 MQTT 下发指令成功控制设备。

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 7.1 | MQTT 控制指令下发接口 | [x] |
| 7.2 | Vue3 控制面板（风扇开关 + 加热开关） | [x]

---

Phase 8：自动温控 &nbsp; ✅ 已完成

> **完成标准**：温度闭环控制，无需人工干预。

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 8.1 | 服务端温控规则引擎（>28℃ 开风扇, <18℃ 开加热，带滞后） | [x] |
| 8.2 | ESP32 接收 MQTT 指令并控制 GPIO + 状态反馈 | [x]

---

Phase 9：AI分析 &nbsp; ✅ 已完成

> **完成标准**：调用 DeepSeek API 生成 AI 分析报告并在前端展示。
> **输入**：温度历史 + 湿度历史 + 发酵周期
> **输出**：发酵状态评估 + 风险提示 + 调整建议 + 预计完成时间

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 9.1 | DeepSeek API 调用封装（含 Prompt 模板） | [x] |
| 9.2 | AI 分析结果存储 + 查询接口 | [x] |
| 9.3 | Vue3 AI 分析结果展示组件 | [x]

---

Phase 10：知识库 &nbsp; ⬜ 未开始

> **完成标准**：支持发酵知识问答，AI 返回建议。

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 10.1 | 编写知识库 Markdown 文件（apple_wine / rice_wine / grape_wine） | [ ] |
| 10.2 | 知识库检索 + DeepSeek 问答接口 | [ ] |
| 10.3 | Vue3 知识库问答页面 | [ ]

---

Phase 11：Docker部署 &nbsp; ⬜ 未开始

> **完成标准**：docker-compose 一键启动全部服务。

| # | 子任务 | 状态 |
|:-:|--------|:--:|
| 11.1 | Spring Boot Dockerfile | [ ] |
| 11.2 | Vue3 Nginx Dockerfile | [ ] |
| 11.3 | docker-compose.yml（Nginx + Vue + Spring Boot + MySQL + Redis + EMQX） | [ ] |
| 11.4 | MySQL 初始化 SQL 自动挂载 | [ ] |
| 11.5 | 环境变量管理（.env 文件） | [ ] |
| 11.6 | 部署文档 + 一键启动脚本 | [ ]

---

MQTT Topic设计

设备数据上传

smartbrew/device/{deviceId}/data

设备心跳

smartbrew/device/{deviceId}/heartbeat

设备状态

smartbrew/device/{deviceId}/status

设备控制

smartbrew/device/{deviceId}/control

OTA升级

smartbrew/device/{deviceId}/ota

---

开发原则

1. 先实现MVP
2. 不提前优化
3. 不训练模型
4. AI仅调用API
5. 每完成一个模块立即提交Git
6. 每完成一个阶段录制演示视频
7. 所有接口必须有文档
8. 所有数据库变更必须记录

---

Git提交规范

feat: 新功能

fix: 修复问题

docs: 文档更新

refactor: 重构

test: 测试

chore: 其他修改

---

项目最终目标

实现一个可运行的：

ESP32 + MQTT + Spring Boot + Vue3 + AI

完整IoT智能发酵监控系统。

用于：

- GitHub展示
- 技术博客展示
- 面试项目展示
- IoT岗位求职