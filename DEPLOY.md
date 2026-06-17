# Smart Brew AI — Docker 部署指南

> 最后更新：2026-06-17

## 环境要求

| 软件 | 最低版本 | 说明 |
|------|:------:|------|
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | 2.0+ | `docker compose` 命令（非 `docker-compose`） |
| 可用内存 | 2 GB+ | MySQL + JVM + EMQX 总计约 1.5GB |
| 可用磁盘 | 5 GB+ | 镜像 + 数据卷 |

## 快速开始

### 1. 配置环境变量

```bash
# 编辑 .env 文件，填入 DeepSeek API Key
nano .env
```

必填项：
- `DEEPSEEK_API_KEY` — 用于 AI 分析功能，从 [DeepSeek 控制台](https://platform.deepseek.com/) 获取

可选修改：
- `MYSQL_ROOT_PASSWORD` — MySQL 密码（默认 `root`）
- `EMQX_DASHBOARD_PASSWORD` — EMQX 管理面板密码（默认 `admin`）

### 2. 一键启动

**Linux / macOS：**

```bash
chmod +x deploy.sh
./deploy.sh
```

**Windows (Git Bash / WSL)：**

```bash
bash deploy.sh
```

**手动启动：**

```bash
docker compose up -d --build
```

### 3. 验证部署

```bash
# 查看服务状态
docker compose ps

# 应看到 5 个容器均为 healthy / running
# smartbrew-mysql    → healthy
# smartbrew-redis    → healthy
# smartbrew-emqx     → healthy
# smartbrew-server   → healthy
# smartbrew-nginx    → running
```

### 4. 访问系统

| 服务 | 地址 | 说明 |
|------|------|------|
| Web 看板 | http://localhost | Vue3 监控面板 |
| API 接口 | http://localhost:8080 | Spring Boot REST API |
| EMQX Dashboard | http://localhost:18083 | MQTT Broker 管理 |
| API 测试 | http://localhost/api/device/list | 设备列表 JSON |

## 管理命令

### deploy.sh 快捷命令

| 命令 | 说明 |
|------|------|
| `./deploy.sh` | 构建并启动全部服务 |
| `./deploy.sh --status` | 查看服务状态 |
| `./deploy.sh --logs` | 查看实时日志 |
| `./deploy.sh --restart` | 重启所有服务 |
| `./deploy.sh --down` | 停止并清理（含数据卷） |

### Docker Compose 原生命令

```bash
# 查看日志
docker compose logs -f server    # 后端日志
docker compose logs -f nginx     # 前端日志

# 重启单个服务
docker compose restart server

# 重新构建并启动
docker compose up -d --build

# 停止所有服务（保留数据）
docker compose down

# 停止并删除数据卷（⚠️ 数据丢失）
docker compose down -v
```

## 服务架构

```
                    Internet
                       │
                       ▼
                 ┌──────────┐
                 │  Nginx   │  :80 (Vue 静态文件 + 反向代理)
                 └────┬─────┘
                      │
          ┌───────────┼───────────┐
          │           │           │
          ▼           ▼           ▼
    ┌─────────┐ ┌─────────┐ ┌─────────┐
    │  Vue    │ │ /api/*  │ │ /ws/*   │
    │  SPA    │ │   ↓     │ │   ↓     │
    └─────────┘ │ Server  │ │  EMQX   │
                │ :8080   │ │ :8083   │
                └────┬────┘ └─────────┘
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
    ┌─────────┐ ┌─────────┐ ┌─────────┐
    │  MySQL  │ │  Redis  │ │  EMQX   │
    │  :3306  │ │  :6379  │ │  :1883  │
    └─────────┘ └─────────┘ └─────────┘
```

## 环境变量参考

以下环境变量由 `docker-compose.yml` 注入到 `server` 容器：

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MYSQL_HOST` | `mysql` | MySQL 服务名（Docker 网络内） |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_USER` | `root` | MySQL 用户名 |
| `MYSQL_PASSWORD` | `root` | MySQL 密码 |
| `REDIS_HOST` | `redis` | Redis 服务名 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `MQTT_HOST` | `emqx` | EMQX 服务名 |
| `MQTT_PORT` | `1883` | MQTT 端口 |
| `DEEPSEEK_API_KEY` | — | DeepSeek API Key（从 .env 读取） |

## 数据库

### 自动建表

首次启动时，MySQL 容器自动执行 `schema.sql`（10 张表 + 预置数据）。

### 预置账号

| 账号 | 密码 | 说明 |
|------|------|------|
| `admin` | `admin123` | 系统管理员 |

### 数据持久化

以下 Docker 命名卷用于数据持久化：

| 卷名 | 挂载路径 | 内容 |
|------|----------|------|
| `mysql_data` | `/var/lib/mysql` | MySQL 数据 |
| `redis_data` | `/data` | Redis AOF 持久化 |
| `emqx_data` | `/opt/emqx/data` | EMQX 数据 |
| `emqx_log` | `/opt/emqx/log` | EMQX 日志 |

## 故障排查

### 服务无法启动

```bash
# 查看所有容器状态
docker compose ps

# 查看具体服务日志
docker compose logs server
docker compose logs mysql
```

### MySQL 连接失败

```bash
# 检查 MySQL 是否就绪
docker exec smartbrew-mysql mysqladmin ping -h localhost -u root -proot

# 检查数据库是否创建
docker exec smartbrew-mysql mysql -u root -proot -e "SHOW DATABASES;"
docker exec smartbrew-mysql mysql -u root -proot -e "USE smartbrew; SHOW TABLES;"
```

### 端口冲突

如果宿主机 80 / 3306 / 6379 / 1883 / 8080 端口被占用，修改 `docker-compose.yml` 中的端口映射：

```yaml
ports:
  - "8081:80"   # 将宿主机端口改为 8081
```

### 镜像拉取慢

```bash
# 配置 Docker 镜像加速（阿里云 / 中科大）
# 编辑 /etc/docker/daemon.json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn"
  ]
}
```

### 完全重置

```bash
# 停止并删除所有容器和数据
docker compose down -v

# 清理构建缓存
docker system prune -f

# 重新部署
./deploy.sh
```

## 生产环境建议

1. **修改默认密码** — 修改 `.env` 中 `MYSQL_ROOT_PASSWORD` 和 `EMQX_DASHBOARD_PASSWORD`
2. **启用 HTTPS** — 在 nginx 前加 SSL 反向代理（如 Caddy / Traefik / Cloudflare Tunnel）
3. **限制端口暴露** — 生产环境建议只暴露 80/443 端口，MySQL/Redis/MQTT 不对外暴露
4. **日志管理** — 配置 Docker 日志驱动为 `json-file` + 日志轮转
5. **资源限制** — 在 `docker-compose.yml` 中为各服务添加 `deploy.resources.limits`
