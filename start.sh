#!/bin/bash

# ========================================================
# 限位挡块资产登记系统 - 构建启动脚本
# 自动构建并启动全部容器，构建完成后打印访问地址
# 用法：./start.sh
# ========================================================

set -e

# 加载 .env 环境变量
if [ -f .env ]; then
    export $(grep -v '^#' .env | grep -v '^$' | xargs)
fi

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

# ============== 端口占用检查 ==============
check_port() {
    local port=$1
    local name=$2
    if lsof -nP -iTCP:${port} -sTCP:LISTEN > /dev/null 2>&1; then
        echo -e "${RED}[错误]${NC} 端口 ${port} (${name}) 已被占用！"
        echo "占用进程信息："
        lsof -nP -iTCP:${port} -sTCP:LISTEN
        exit 1
    fi
}

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  限位挡块资产登记系统 - 启动前检查${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

echo "📋 检查端口占用..."
check_port ${FRONTEND_PORT} "前端Nginx"
check_port ${BACKEND_PORT} "后端SpringBoot"
check_port ${MYSQL_PORT} "MySQL"
check_port ${REDIS_PORT} "Redis"
echo -e "${GREEN}✅ 所有端口均未被占用${NC}"
echo ""

# ============== 启动构建 ==============
echo -e "${CYAN}🚀 开始构建并启动服务...${NC}"
echo ""

docker compose up --build -d

echo ""
echo -e "${YELLOW}⏳ 等待服务就绪...${NC}"

# 等待前端就绪
FRONTEND_READY=0
for i in $(seq 1 60); do
    if curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:${FRONTEND_PORT}" | grep -q "200"; then
        FRONTEND_READY=1
        break
    fi
    sleep 2
    echo "  等待前端服务... (${i}s)"
done

# ============== 服务就绪检查 ==============
echo ""
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}  ✅ 限位挡块资产登记系统 启动成功！${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# 容器状态
echo -e "${BOLD}📦 容器运行状态：${NC}"
docker compose ps
echo ""

# 端口信息
echo -e "${BOLD}🔌 服务端口映射：${NC}"
echo "  前端访问:  127.0.0.1:${FRONTEND_PORT}  →  Nginx(内部${FRONTEND_INTERNAL_PORT})"
echo "  后端API:   127.0.0.1:${BACKEND_PORT}  →  SpringBoot(内部${BACKEND_INTERNAL_PORT})"
echo "  MySQL:     127.0.0.1:${MYSQL_PORT}  →  MySQL(内部${MYSQL_INTERNAL_PORT})"
echo "  Redis:     127.0.0.1:${REDIS_PORT}  →  Redis(内部${REDIS_INTERNAL_PORT})"
echo ""

# ============== 访问地址输出 ==============
echo -e "${BOLD}🌐 前端访问地址（二选一均可访问）：${NC}"
echo -e "  ${GREEN}● http://localhost:${FRONTEND_PORT}${NC}"
echo -e "  ${GREEN}● http://127.0.0.1:${FRONTEND_PORT}${NC}"
echo ""

# 自检
echo -e "${BOLD}🔍 服务自检：${NC}"

# 检查 127.0.0.1
HTTP_127=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:${FRONTEND_PORT}")
if [ "$HTTP_127" = "200" ]; then
    echo -e "  127.0.0.1:${FRONTEND_PORT}  ${GREEN}✅ 正常 (HTTP ${HTTP_127})${NC}"
else
    echo -e "  127.0.0.1:${FRONTEND_PORT}  ${RED}❌ 异常 (HTTP ${HTTP_127})${NC}"
fi

# 检查 localhost
HTTP_LOCAL=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${FRONTEND_PORT}")
if [ "$HTTP_LOCAL" = "200" ]; then
    echo -e "  localhost:${FRONTEND_PORT}   ${GREEN}✅ 正常 (HTTP ${HTTP_LOCAL})${NC}"
else
    echo -e "  localhost:${FRONTEND_PORT}   ${RED}❌ 异常 (HTTP ${HTTP_LOCAL})${NC}"
fi

# 比对 HTML 特征是否一致
TITLE_127=$(curl -s "http://127.0.0.1:${FRONTEND_PORT}" | grep -o '<title>[^<]*</title>' || echo "N/A")
TITLE_LOCAL=$(curl -s "http://localhost:${FRONTEND_PORT}" | grep -o '<title>[^<]*</title>' || echo "N/A")
if [ "$TITLE_127" = "$TITLE_LOCAL" ]; then
    echo -e "  页面标题一致性:  ${GREEN}✅ 一致 (${TITLE_127})${NC}"
else
    echo -e "  页面标题一致性:  ${RED}❌ 不一致${NC}"
    echo "    127.0.0.1: $TITLE_127"
    echo "    localhost:  $TITLE_LOCAL"
fi

echo ""
echo -e "${CYAN}💡 提示：${NC}"
echo "  查看日志:  docker compose logs -f [服务名]"
echo "  停止服务:  docker compose down"
echo "  重启服务:  docker compose restart"
echo ""
