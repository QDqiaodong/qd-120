# ========================================================
# 限位挡块资产登记系统 - 后端 Dockerfile
# 严格使用 Docker 原生分层缓存机制，不引入 dockerfile:* syntax
# 依赖层与源码层严格分离，只改业务代码时不重新下载依赖
# ========================================================

# 接收全局镜像仓库地址，由 docker-compose build args 传入
ARG DOCKER_REGISTRY

# ---------- 阶段一：构建 ----------
# 使用全局统一仓库地址，杜绝直连 DockerHub
FROM ${DOCKER_REGISTRY}/library/maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# 【依赖层】复制 Maven 阿里云镜像配置
COPY settings.xml /root/.m2/settings.xml

# 【依赖层】仅复制 pom.xml，利用 Docker 缓存
# 只要 pom.xml 不变，此层及后续依赖下载完全复用
COPY pom.xml .

# 下载全部依赖到本地仓库（离线模式）
# -B 批处理模式，避免交互提示
# dependency:go-offline 会下载 pom 中声明的所有依赖
RUN mvn dependency:go-offline -B

# 【源码层】复制业务源代码，修改源码时只触发编译，不重新下载依赖
COPY src ./src

# 编译打包，跳过测试
RUN mvn clean package -DskipTests -B

# ---------- 阶段二：运行 ----------
# JRE 运行环境，使用全局统一仓库地址
FROM ${DOCKER_REGISTRY}/library/eclipse-temurin:17-jre-jammy

WORKDIR /app

# 安装时区支持
RUN apt-get update && apt-get install -y --no-install-recommends \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai

# 复制编译好的 jar 包
COPY --from=builder /app/target/stopper-asset-1.0.0.jar app.jar

# 后端容器内部监听端口（由 .env 控制）
EXPOSE 19120

ENTRYPOINT ["java", "-jar", "app.jar"]
