# vpn4j 🛡️

基于 Java 25 和 Spring Boot 4.x 构建的轻量级、高性能 VPN 管理与监控系统。该系统不仅提供了完善的 OpenVPN 账号管理、证书颁发与生命周期监控，还基于虚拟线程和 SSE 技术实现了极致的实时数据观测体验。

---

## 📖 项目简介

vpn4j 是一款集中式管理和监控 VPN 服务的后端平台。我们致力于让复杂的 VPN 配置、证书管理以及日常巡检变得简单且自动化。
系统底层全面拥抱 **Java 25** 的虚拟线程 (Virtual Threads) 和 **Spring Boot 4.x** 的新特性，在 I/O 密集型的网络处理和证书生成任务中，提供了卓越的并发处理能力和超低的资源消耗。

## ✨ 核心特性

- **⚡ 高效的底层架构**：基于 Java 25 虚拟线程，大幅优化网络和磁盘 I/O 并发性能。
- **📜 OpenVPN 证书生命周期管理**：内置 `OpenVpnCertificateTool` 证书工具。支持动态创建、分发 SSL/TLS 证书，管理 CCD (Client Config Dir) 专属路由，以及提供定期的账号过期检测与自动禁用机制。
- **📡 实时状态与日志流推送**：整合 SSE (Server-Sent Events) 服务。不仅实时推送后端日志流和 OpenVPN 在线用户状态，更可将宿主机的系统硬件信息（CPU、内存等）投射至前端大屏。
- **⏱️ 动态定时任务调度**：集成动态任务调度器 (`DynamicTaskScheduler`)。支持在不重启服务的情况下，动态修改校验脚本、Cron 表达式并即刻生效。
- **💾 全局配置与高速缓存**：借助 Caffeine 极速本地缓存以及 MyBatis-Plus 拦截器，自动感知数据库级数据更新并同步刷新对应的本地缓存，确保全局状态和管理策略的强一致性。
- **🔒 灵活且安全的权限管理**：Sa-Token + JWT 无状态鉴权。支持灵活的并发登录配置和严格的“挤下线”多地登录控制。

## 🛠️ 技术栈

- **核心框架**: Java 25, Spring Boot 4.0.3
- **持久层 & 数据库**: MyBatis-Plus 3.5.16, SQLite (默认内置)
- **权限认证**: Sa-Token, JWT
- **系统监控与日志**: Oshi (系统状态信息), P6Spy (SQL 性能分析), Logback
- **通用工具类**: Hutool, Apache Commons Lang3, Commons IO

---

## 🚀 安装部署与运行指南

为了保证环境纯净、高灵活性并避开繁杂的配置，我们通过提供了原生的 `install.sh` 脚本和 **Docker** 化的一键部署方案。所有必需的环境（包括 OpenVPN 本身、Java 运行环境以及 Nginx）都已经在脚本内整合。

### 环境准备 (Prerequisites)

在部署服务器上，您只需要：

1. **Docker Engine**: 需要安装 Docker。
2. **Docker Compose**: 需要安装 `docker compose` (V2) 或 `docker-compose` (V1)。

### 1. 编译打包项目

首先在您的开发机上拉取代码，并使用 Maven 进行编译。由于项目采用了 Java 25 的语法特性，**请确保您编译时所用的 JDK 版本为 25 或以上**。

```bash
# 进入项目根目录
cd vpn4j

# 使用 Maven 编译并跳过测试
mvn clean package -DskipTests
```

编译成功后，将在 `target/` 目录下生成 `openvpn4j-<version>.jar` 文件（例如 `openvpn4j-1.0.1.jar`）。

### 2. 服务器部署 (配合 install.sh)

将打包好的 `openvpn4j-*.jar` 文件以及代码根目录下的 `install.sh` 一并上传到服务器的某一个目录下（例如 `/opt/vpn4j`）。

```bash
# 赋予脚本执行权限
chmod +x install.sh

# 一键安装并启动容器
./install.sh
```

**`install.sh` 脚本执行流程解析：**

1. **准备目录**：自动在宿主机创建相关的 Nginx / Web 代理目录结构（`openvpn/web/nginx`, `openvpn/web/dist`）。
2. **自动构建 Docker 镜像**：如果当前不存在基础镜像，它可以：
   - 如果目录下存在 `openvpn4j.tar` 离线镜像包，直接离线 load 导入。
   - 如果没有，则通过自动生成的 `Dockerfile`，拉取 `debian:bookworm-slim` 并在线安装 Java 25 解压包、`openvpn`、`nginx` 等核心依赖包，生成名为 `openvpn4j:jdk25` 的内置镜像。
3. **生成 Compose 并启动编排**：自动在当前目录生成 `docker-compose.yml`。容器内部自动执行 `openvpn4j-*.jar` 包并拉起服务。

### 3. 网络与端口映射

安装脚本自动暴露并映射了以下服务端口，如需修改，请调整生成后的 `docker-compose.yml` 文件：

- **`80` / `443`**: Nginx Web/HTTP(S) 访问端口 (前端入口)
- **`8080`**: Spring Boot vpn4j 后端 API 接口通讯端口
- **`1194/udp` & `1194/tcp`**: OpenVPN 核心监听端口
- **`5005`**: 默认开启的额外调试/扩展探测端口

### 4. 数据及高阶配置

vpn4j 默认使用轻量且易于移动的 `SQLite` 作为本地数据库（存放于您运行目录下的 `openvpn4j.db`）。同时容器**挂载了当前路径到 `/data`** 下，因此生成的证书、日志、数据库文件都可以直接在宿主机进行查看和持久化保存。

重启并应用更改：

```bash
docker compose down
docker compose up -d
```

---

> ⚠️ **提示**：在使用 SSE 推送以及部署 Web 页面时，请确保相应的路由/防火墙规则不拦截 `8080` 及 `1194` 端口的数据传输。
