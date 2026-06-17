#!/bin/bash
# ============================================================
# Smart Brew AI — 一键部署脚本
# 用法：
#   chmod +x deploy.sh
#   ./deploy.sh              # 部署
#   ./deploy.sh --down       # 停止并清理
#   ./deploy.sh --restart    # 重启所有服务
# ============================================================

set -e

cd "$(dirname "$0")"

case "${1:-}" in
    --down)
        echo "=== 停止并清理所有容器 ==="
        docker compose down -v
        echo "=== 清理完成 ==="
        exit 0
        ;;
    --restart)
        echo "=== 重启所有服务 ==="
        docker compose restart
        echo "=== 重启完成 ==="
        docker compose ps
        exit 0
        ;;
    --logs)
        echo "=== 查看日志 (Ctrl+C 退出) ==="
        docker compose logs -f
        exit 0
        ;;
    --status)
        docker compose ps
        exit 0
        ;;
esac

# 检查 .env 文件
if [ ! -f ".env" ]; then
    echo "⚠️  .env 文件不存在，使用默认值启动"
    echo "   如需 AI 分析功能，请配置 DEEPSEEK_API_KEY"
fi

echo "=== Smart Brew AI 一键部署 ==="
echo ""

# 拉取最新镜像（MySQL/Redis/EMQX）
echo "[1/3] 拉取基础镜像..."
docker compose pull mysql redis emqx 2>/dev/null || true

# 构建并启动
echo "[2/3] 构建并启动所有服务..."
docker compose up -d --build

# 等待服务就绪
echo "[3/3] 等待服务就绪..."
echo ""

# 显示状态
echo "=== 服务状态 ==="
docker compose ps

echo ""
echo "=== 部署完成 ==="
echo ""
echo "访问地址："
echo "  Web 看板:       http://localhost"
echo "  EMQX Dashboard: http://localhost:18083"
echo "  API 接口:        http://localhost:8080"
echo ""
echo "管理命令："
echo "  ./deploy.sh --status   查看服务状态"
echo "  ./deploy.sh --logs     查看实时日志"
echo "  ./deploy.sh --restart  重启服务"
echo "  ./deploy.sh --down     停止并清理"
