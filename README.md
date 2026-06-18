# 限位挡块资产登记系统

自动化组装车间限位挡块统一登记管理系统，支持基础建档、工位移位、月度清点、报废归档四大核心业务。

---

## 技术架构

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia | Vue3.4 / Vite5 |
| 后端 | Spring Boot 3.3 + MyBatis-Plus + Redis | JDK17 / SpringBoot3.3 |
| 数据库 | MySQL 8.0 + Redis 7 | MySQL8.0 / Redis7 |
| 部署 | Docker + Docker Compose | Compose v3.8 |

---

## 端口分配表（固定端口，禁止复用）

| 服务 | 宿主机端口 | 容器内部端口 | 说明 | 避开默认端口 |
|------|-----------|-------------|------|-------------|
| 前端 Nginx | **3008** | 80 | 静态资源服务 | 避开 80/443/8080 |
| 后端 SpringBoot | **8088** | 8088 | API 服务 | 避开 8080/8081 |
| MySQL | **3309** | 3306 | 数据库 | 避开 3306 |
| Redis | **6380** | 6379 | 缓存 | 避开 6379 |

> **约束：所有对外端口均绑定 `127.0.0.1`，禁止使用 `0.0.0.0` / `localhost` 作为宿主机监听地址。**

---

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 进入项目根目录
cd qd-120

# 一键构建并启动（自动检查端口、打印访问地址）
./start.sh
```

启动成功后自动打印以下访问地址：

```
🌐 前端访问地址：
  ● http://localhost:3008
  ● http://127.0.0.1:3008
```

停止服务：
```bash
./stop.sh
```

### 方式二：Docker Compose 手动启动

```bash
# 构建并启动（首次会下载依赖，后续复用缓存）
docker compose up --build -d

# 查看容器状态
docker compose ps

# 查看实时日志
docker compose logs -f

# 停止服务
docker compose down
```

### 方式三：本地开发

**前端：**
```bash
cd frontend
npm install --registry=https://mirrors.cloud.tencent.com/npm/
npm run dev
# 访问 http://127.0.0.1:3008
```

**后端：**
```bash
cd backend
mvn spring-boot:run
# 服务启动在 http://127.0.0.1:8088
```

> 本地开发需先启动 MySQL(3309) 和 Redis(6380)，可通过 Docker 单独启动：
> ```bash
> docker compose up -d mysql redis
> ```

---

## 核心功能模块

### 1. 限位挡块基础建档
- 录入挡块编号、规格型号、适配设备、存放工位、入库时间、配件图片
- 支持增删改查、分页查询、多条件筛选（关键字/工位/状态）
- 配件图片自适应缩放显示（object-fit: contain）

### 2. 挡块工位移位登记
- 选择挡块，登记目标工位、操作人、移位原因
- 自动记录原工位、操作时间，保证数据一致性
- 事务保证：移位记录落库后自动更新挡块当前工位

### 3. 月度资产清点
- 一键发起月度盘点，自动拉取所有在档挡块
- 逐档标记：正常（已盘） / 差异（缺失/错位）
- 自动统计账面数、实盘数、差异数
- 留存完整盘点台账和差异明细

### 4. 磨损挡块报废归档
- 登记磨损程度、报废原因、操作人
- 自动更新挡块状态为「报废」
- 报废记录永久存档，可追溯

---

## Docker 构建缓存说明

### 分层缓存策略（严格分离依赖层与源码层）

**前端 Dockerfile 分层：**
```
层1: 基础镜像 node:18-alpine
层2: 配置 npm 国内镜像（腾讯云）          ← 无变更复用
层3: COPY package.json + package-lock.json  ← 包文件变更才失效
层4: RUN npm ci                             ← 依赖下载缓存
层5: COPY . (业务源码)                      ← 仅改源码只重跑后续
层6: RUN npm run build                      ← 编译
层7: 基础镜像 nginx:alpine
层8: 复制 dist + nginx.conf
```

**后端 Dockerfile 分层：**
```
层1: 基础镜像 maven:3.9.6-eclipse-temurin-17
层2: COPY settings.xml (阿里云Maven镜像)    ← 无变更复用
层3: COPY pom.xml                           ← pom变更才失效
层4: RUN mvn dependency:go-offline           ← 依赖下载缓存
层5: COPY src (业务源码)                     ← 仅改源码只重跑后续
层6: RUN mvn clean package                   ← 编译打包
层7: 基础镜像 eclipse-temurin:17-jre
层8: 复制 jar 包
```

### 效果验证
| 场景 | 依赖下载 | 编译 | 总耗时 |
|------|---------|------|-------|
| 首次构建 | ✅ 全量下载 | ✅ | 约 3-5 分钟 |
| 仅修改业务代码 | ❌ 复用缓存 | ✅ | 约 20-60 秒 |
| 修改 pom.xml / package.json | ✅ 重新下载 | ✅ | 同首次构建 |

> **约束：仅使用 Docker 原生分层缓存，未引入 `# syntax=docker/dockerfile:*` 语法。**

---

## 国内镜像源配置

### 前端 npm 镜像（腾讯云）
已在 [frontend/Dockerfile](frontend/Dockerfile) 中配置：
```bash
npm config set registry https://mirrors.cloud.tencent.com/npm/
npm config set disturl https://mirrors.cloud.tencent.com/nodejs-release/
```

本地开发使用：
```bash
npm config set registry https://mirrors.cloud.tencent.com/npm/
```

### 后端 Maven 镜像（阿里云）
已在 [backend/settings.xml](backend/settings.xml) 和 [backend/pom.xml](backend/pom.xml) 中配置：
```xml
<mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### Docker 基础镜像仓库统一
所有 Docker 基础镜像通过 `.env` 中的 `DOCKER_REGISTRY` 变量统一控制：
```env
# 修改为私有仓库或镜像加速器地址
DOCKER_REGISTRY=registry.cn-hangzhou.aliyuncs.com
```

修改后所有镜像（node/maven/jre/nginx/mysql/redis）自动从该仓库拉取，无需逐一修改 Dockerfile。

---

## 环境变量配置（.env）

项目根目录 [.env](.env) 文件统一管理所有配置：

| 变量名 | 默认值 | 说明 |
|-------|-------|------|
| `DOCKER_REGISTRY` | docker.io | 全局 Docker 镜像仓库地址 |
| `PROJECT_NAME` | stopper-asset | 项目名称（容器命名前缀） |
| `FRONTEND_PORT` | 3008 | 前端 Nginx 宿主机端口 |
| `BACKEND_PORT` | 8088 | 后端 SpringBoot 宿主机端口 |
| `MYSQL_PORT` | 3309 | MySQL 宿主机端口 |
| `REDIS_PORT` | 6380 | Redis 宿主机端口 |
| `MYSQL_ROOT_PASSWORD` | stopper_asset_2024 | MySQL root 密码 |
| `MYSQL_DATABASE` | stopper_asset | 数据库名 |
| `TZ` | Asia/Shanghai | 时区 |

---

## 容器命名规范

所有容器名称统一使用 `${PROJECT_NAME}-xxx` 格式，与项目名称一致：

| 容器名 | 说明 |
|-------|------|
| `stopper-asset-mysql` | MySQL 数据库 |
| `stopper-asset-redis` | Redis 缓存 |
| `stopper-asset-backend` | SpringBoot 后端 |
| `stopper-asset-frontend` | Nginx 前端 |
| `stopper-asset-net` | 自定义 Bridge 网络 |
| `stopper-asset-mysql-data` | MySQL 数据卷 |
| `stopper-asset-redis-data` | Redis 数据卷 |

---

## 数据库初始化

首次启动 MySQL 容器时，自动执行 [backend/src/main/resources/db/schema.sql](backend/src/main/resources/db/schema.sql) 初始化脚本：
- 创建 5 张业务表（stopper / stopper_shift / stopper_inventory / stopper_inventory_detail / stopper_scrap）
- 为高频查询字段建立索引（规格、工位、状态、编号等，档案查询优化）
- 预置 10 条示例数据，分布在 A/B/C 三个区的不同工位

SQL 脚本为幂等设计（`CREATE TABLE IF NOT EXISTS`、`DROP TABLE IF EXISTS`），反复执行不会报错。

---

## Redis 缓存设计

- **缓存内容**：通用挡块规格列表（`stopper:specs`）
- **过期策略**：24 小时自动过期（TTL）
- **淘汰策略**：`allkeys-lru`（内存不足时淘汰最近最少使用的键）
- **缓存刷新**：新增 / 更新 / 删除挡块时自动清除规格缓存

---

## 端口占用自检

启动前可手动检查端口是否被占用：

```bash
# 检查前端端口
lsof -nP -iTCP:3008 -sTCP:LISTEN

# 检查后端端口
lsof -nP -iTCP:8088 -sTCP:LISTEN

# 检查 MySQL 端口
lsof -nP -iTCP:3309 -sTCP:LISTEN

# 检查 Redis 端口
lsof -nP -iTCP:6380 -sTCP:LISTEN
```

启动成功后自检：
```bash
# 验证 127.0.0.1 和 localhost 返回一致
curl -s http://127.0.0.1:3008 | head
curl -s http://localhost:3008 | head
```

---

## 项目目录结构

```
qd-120/
├── .env                      # 全局环境变量（端口/镜像/密码）
├── .dockerignore             # 全局 Docker 忽略文件
├── .gitignore                # Git 忽略文件
├── docker-compose.yml        # Docker Compose 编排
├── start.sh                  # 一键启动脚本（构建+自检+输出地址）
├── stop.sh                   # 停止脚本
├── README.md                 # 本文件
│
├── frontend/                 # 前端 Vue 3 项目
│   ├── Dockerfile            # 前端多阶段构建（分层缓存）
│   ├── .dockerignore
│   ├── nginx.conf            # Nginx 反向代理配置
│   ├── vite.config.js        # Vite 配置（127.0.0.1 + strictPort）
│   ├── package.json
│   └── src/
│       ├── views/            # 6个业务页面
│       ├── api/              # API 接口封装
│       ├── router/           # 路由配置
│       ├── utils/            # 工具函数（axios封装等）
│       ├── styles/           # 全局样式
│       ├── App.vue
│       └── main.js
│
└── backend/                  # 后端 Spring Boot 项目
    ├── Dockerfile            # 后端多阶段构建（分层缓存）
    ├── .dockerignore
    ├── settings.xml          # Maven 阿里云镜像配置
    ├── pom.xml
    └── src/main/
        ├── java/com/stopper/asset/
        │   ├── controller/   # 4个业务 Controller
        │   ├── service/      # 5个业务 Service
        │   ├── mapper/       # 5个 MyBatis Mapper
        │   ├── entity/       # 6个数据库实体
        │   ├── config/       # Redis/CORS/MyBatis-Plus 配置
        │   ├── common/       # 通用返回类
        │   └── StopperAssetApplication.java
        └── resources/
            ├── application.yml       # 应用配置
            └── db/schema.sql         # 数据库初始化脚本
```

---

## 常见问题

**Q1：首次构建很慢？**
A：首次需要下载基础镜像和全部依赖，属于正常现象。后续构建会复用缓存，仅源码修改时 1 分钟内完成。

**Q2：前端访问 `localhost:18120` 正常但 `127.0.0.1:18120` 异常？**
A：检查 hosts 文件，确保 `localhost` 解析到 `127.0.0.1`，或执行 `./start.sh` 脚本自动完成一致性检查。

**Q3：端口被占用怎么办？**
A：修改 `.env` 中对应端口号，所有服务会自动使用新端口（无需修改其他配置文件）。

**Q4：如何切换到私有镜像仓库？**
A：修改 `.env` 中 `DOCKER_REGISTRY=你的私有仓库地址`，重新执行 `docker compose build` 即可。

**Q5：数据库数据会丢失吗？**
A：MySQL 和 Redis 均使用命名数据卷持久化，`docker compose down` 不会删除数据。如需彻底清理：`docker compose down -v`。
