# ========================================================
# 限位挡块资产登记系统 - 前端 Dockerfile
# 严格使用 Docker 原生分层缓存机制，不引入 dockerfile:* syntax
# 依赖层与源码层严格分离，只改业务代码时不重新下载依赖
# ========================================================

# 接收全局镜像仓库地址，由 docker-compose build args 传入
ARG DOCKER_REGISTRY=docker.io

# ---------- 阶段一：构建 ----------
# 使用全局统一仓库地址，杜绝直连 DockerHub
FROM ${DOCKER_REGISTRY}/library/node:18-alpine AS builder

WORKDIR /app

# 配置 npm 使用国内镜像源（腾讯云镜像），无需 VPN
RUN npm config set registry https://registry.npmmirror.com/

# 【依赖层】仅复制包管理文件，利用 Docker 缓存
# 只要 package.json / package-lock.json 不变，此层完全复用
COPY package.json package-lock.json* ./

# 使用 npm ci 严格按 lockfile 安装，版本确定性更高，缓存命中率更好
# package-lock.json 不存在时回退到 npm install
RUN if [ -f package-lock.json ]; then \
        npm ci; \
    else \
        npm install; \
    fi

# 【源码层】复制业务源代码，修改源码时只触发此层之后的步骤
COPY . .

# 执行生产构建
RUN npm run build

# ---------- 阶段二：运行 ----------
# Nginx 运行静态资源，使用全局统一仓库地址
FROM ${DOCKER_REGISTRY}/library/nginx:alpine

# 设置时区
RUN apk add --no-cache tzdata
ENV TZ=Asia/Shanghai

# 复制构建产物到 Nginx 静态资源目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Nginx 默认监听 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
