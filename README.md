# Smart Brew AI — 智能发酵监控系统

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.6-brightgreen?logo=springboot" alt="Spring Boot 3">
  <img src="https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs" alt="Vue 3">
  <img src="https://img.shields.io/badge/ESP32-Arduino-00979D?logo=espressif" alt="ESP32">
  <img src="https://img.shields.io/badge/MQTT-EMQX-00E4FF?logo=mqtt" alt="MQTT">
  <img src="https://img.shields.io/badge/AI-DeepSeek-536DFE?logo=openai" alt="DeepSeek">
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker" alt="Docker">
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="License">
</p>

一个完整的 **IoT + AI 智能发酵监控系统**，基于 ESP32 传感器采集、MQTT 消息中间件、Spring Boot 后端服务和 Vue3 前端看板，集成 DeepSeek AI 进行发酵状态分析与知识问答。

> 适用于：果酒、米酒、葡萄酒等发酵工艺的实时监控与智能控制。

---

## 架构总览

```
┌─────────────────────────────────────────────────────────┐
│                      用户浏览器                            │
│                  http://localhost:80                      │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│              Nginx (:80) — Vue SPA + 反向代理              │
│     / → 静态文件    /api/* → server:8080                  │
│     /ws/* → emqx:8083 (WebSocket)                        │
└──────┬──────────────────────────────────┬───────────────┘
       │                                  │
       ▼                                  ▼
┌──────────────┐                  ┌──────────────┐
│  Spring Boot │                  │     EMQX 5    │
│    :8080     │                  │  :1883 (MQTT) │
│              │                  │  :8083 (WS)   │
│ ┌──────────┐│                  │  :18083 (Dashboard)
│ │REST API  ││                  └──────▲────────┘
│ │12 个接口 ││                         │
│ │AI 分析   ││                  ┌──────┴────────┐
│ │知识库    ││                  │  ESP32 设备    │
│ │温控引擎  ││                  │ DS18B20+DHT22 │
│ │告警规则  ││                  │ 30s 采集周期   │
│ └────┬─────┘│                  └───────────────┘
│      │      │
└──────┼──────┘
       │
  ┌────┴─────┐     ┌──────────────┐
  │  MySQL 8 │     │  Redis 7     │
  │  :3306   │     │  :6379       │
  │ 10 张表  │     │ 设备状态缓存  │
  └──────────┘     └──────────────┘
```

---

## 技术栈

| 层级 | 技术 | 说明 |
|:----:|------|------|
| 设备端 | ESP32 + Arduino + FreeRTOS | DS18B20 温度 + DHT22 温湿度采集 |
| 消息 | EMQX 5 (MQTT Broker) | 设备数据上行 / 控制指令下行 |
| 后端 | Java 21 + Spring Boot 3.2 + MyBatis Plus 3.5 | REST API + MQTT 订阅 + 温控引擎 |
| 缓存 | Redis 7 | 设备在线状态 + 最新数据缓存 |
| 数据库 | MySQL 8.0 | 10 张核心表，自动建表 |
| 前端 | Vue 3 + Element Plus + ECharts 5 | 6 个功能页面，SPA 单页应用 |
| AI | DeepSeek API (deepseek-chat) | 发酵状态分析 + 知识库 RAG 问答 |
| 部署 | Docker + Docker Compose + Nginx | 一键启动 6 个服务 |

---

## 功能模块

| 模块 | 功能 | 前端页面 |
|------|------|:------:|
| 实时监控 | 设备状态、温度仪表盘、数据卡片、30s 自动刷新 | Dashboard |
| 历史曲线 | 温湿度双 Y 轴折线图、时间范围选择、缩放 | HistoryChart |
| 告警中心 | 高温/低温/离线告警、筛选、分页、手动清除 | AlarmCenter |
| 设备控制 | 风扇/加热开关、MQTT 指令下发、操作记录 | ControlPanel |
| 自动温控 | 滞后双阈值规则引擎（>28°C 风扇, <18°C 加热） | — |
| AI 分析 | DeepSeek 发酵状态评估、风险提示、调整建议 | AiAnalysis |
| 知识库 | 发酵知识 RAG 问答（苹果酒/米酒/葡萄酒） | KnowledgeBase |

---

## 快速开始

### 前置要求

- Docker 20.10+ & Docker Compose 2.0+
- 2GB+ 可用内存

### 1. 克隆项目

```bash
git clone https://github.com/your-username/smartbrew-ai.git
cd smartbrew-ai
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入 DeepSeek API Key
nano .env
```

### 3. 一键启动

```bash
docker compose up -d --build
```

### 4. 访问服务

| 服务 | 地址 |
|------|------|
| Web 看板 | http://localhost |
| API 接口 | http://localhost:8080 |
| Swagger 文档 | http://localhost:8080/swagger-ui.html |
| EMQX Dashboard | http://localhost:18083 |

### 5. 停止

```bash
docker compose down           # 保留数据
docker compose down -v        # 清理全部数据
```

---

## REST API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/device/register` | 设备注册 |
| GET | `/api/device/list` | 设备列表（分页+搜索） |
| GET | `/api/device/{deviceId}` | 设备详情 |
| PUT | `/api/device/{deviceId}` | 更新设备 |
| GET | `/api/device/latest` | 最新传感器数据 |
| GET | `/api/device/history` | 历史数据（分页） |
| POST | `/api/device/{deviceId}/control` | 设备控制（FAN/HEATER） |
| GET | `/api/alarm/list` | 告警列表（分页+筛选） |
| PUT | `/api/alarm/{alarmId}/clear` | 清除告警 |
| POST | `/api/ai/analyze` | 触发 AI 分析 |
| GET | `/api/ai/list` | AI 分析记录 |
| GET | `/api/ai/{id}` | AI 分析详情 |
| POST | `/api/knowledge/ask` | 知识库问答 |

> 启动后访问 http://localhost:8080/swagger-ui.html 查看完整交互式文档。

---

## 项目结构

```
smartbrew-ai/
├── esp32-firmware/                  # ESP32 固件 (PlatformIO)
│   └── src/
│       ├── config.h                 # WiFi / MQTT / 引脚配置
│       ├── main.cpp                 # 传感器调度 + MQTT 上报
│       ├── sensors.h/cpp            # DS18B20 + DHT22 驱动
│       ├── wifi_manager.h/cpp       # WiFi 自动重连
│       └── mqtt_handler.h/cpp       # MQTT 连接 + JSON 上报 + 心跳
│
├── smartbrew-server/                # Spring Boot 后端
│   ├── Dockerfile                   # 多阶段构建 (Maven + JRE)
│   ├── pom.xml
│   └── src/main/
│       ├── resources/
│       │   ├── application.yml      # 数据库 / Redis / MQTT 配置
│       │   ├── db/schema.sql        # 10 张表 DDL + 预置数据
│       │   └── knowledge/           # 发酵知识库 Markdown
│       └── java/com/smartbrew/smartbrew/
│           ├── config/              # MQTT / Redis / AI 配置
│           ├── controller/          # 5 个 REST 控制器
│           ├── service/             # 10 个业务服务
│           ├── entity/              # 8 个实体类
│           ├── mapper/              # MyBatis-Plus Mapper
│           ├── dto/                 # 请求/响应 DTO
│           └── task/                # 定时任务 (心跳检测 + AI 分析)
│
├── smartbrew-web/                   # Vue3 前端
│   ├── Dockerfile                   # 多阶段构建 (Node + Nginx)
│   ├── nginx.conf                   # Nginx 反向代理配置
│   ├── package.json
│   └── src/
│       ├── views/
│       │   ├── Dashboard.vue        # 实时监控看板
│       │   ├── HistoryChart.vue     # 历史温湿度曲线
│       │   ├── AlarmCenter.vue      # 告警中心
│       │   ├── ControlPanel.vue     # 设备控制面板
│       │   ├── AiAnalysis.vue       # AI 分析页面
│       │   └── KnowledgeBase.vue    # 知识库问答
│       └── components/             # ECharts 仪表盘 + 设备卡片
│
├── docker-compose.yml               # 6 服务编排
├── .env.example                     # 环境变量模板
├── deploy.sh                        # 一键部署脚本
└── DEPLOY.md                        # 部署文档
```

---

## MQTT 主题设计

| 主题 | 方向 | 说明 |
|------|:--:|------|
| `smartbrew/device/{deviceId}/data` | 上行 | 传感器数据上报 |
| `smartbrew/device/{deviceId}/heartbeat` | 上行 | 设备心跳 |
| `smartbrew/device/{deviceId}/status` | 上行 | 设备状态反馈 |
| `smartbrew/device/{deviceId}/control` | 下行 | 控制指令下发 |
| `smartbrew/device/{deviceId}/ota` | 下行 | OTA 升级 |

---

## 数据库

首次启动时 MySQL 自动执行 `schema.sql`，包含 10 张表：

`device` · `sensor_data` · `fermentation_batch` · `device_heartbeat` · `alarm_record` · `ai_analysis_record` · `device_control_log` · `device_event` · `sys_user` · `system_config`

预置管理员账号：`admin` / `admin123`

---

## 截图

<!-- TODO: 添加以下页面截图 -->
<!--
![Dashboard](docs/screenshots/dashboard.png)
![HistoryChart](docs/screenshots/history.png)
![AlarmCenter](docs/screenshots/alarm.png)
![ControlPanel](docs/screenshots/control.png)
![AiAnalysis](docs/screenshots/ai.png)
![KnowledgeBase](docs/screenshots/knowledge.png)
![Swagger](docs/screenshots/swagger.png)
-->

---

## 许可证

MIT License — 详见 [LICENSE](LICENSE) 文件。

---

---

<p align="center">
  <sub>Built with ESP32 + MQTT + Spring Boot + Vue 3 + DeepSeek AI + Docker</sub>
</p>
