#!/bin/bash

# 配置变量
IMAGE_NAME="openvpn4j:jdk25"
DOCKER_FILE="Dockerfile"
COMPOSE_FILE="docker-compose.yml"
OFFLINE_TAR="openvpn4j.tar"

# --- 兼容性处理：自动侦测使用哪种 Compose 命令 ---
if docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD="docker compose"
elif docker-compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD="docker-compose"
else
    echo "❌ 错误: 未检测到 docker compose 或 docker-compose，请先安装。"
    exit 1
fi

echo "✅ 使用命令: $DOCKER_COMPOSE_CMD"
# ---------------------------------------------

# 1. 目录准备
mkdir -p ./openvpn/web/nginx
mkdir -p ./openvpn/web/dist

# 2. 镜像安装逻辑 (优先使用离线包)
if [[ "$(docker images -q $IMAGE_NAME 2> /dev/null)" == "" ]]; then
    if [ -f "$OFFLINE_TAR" ]; then
        echo "📦 发现离线镜像包 $OFFLINE_TAR，正在加载..."
        docker load -i "$OFFLINE_TAR"
    else
        echo "🛠️ 未发现离线包，开始生成 Dockerfile 并构建镜像..."
        cat <<EOF > $DOCKER_FILE
FROM debian:bookworm-slim
ENV JAVA_HOME=/opt/jdk-25
ENV PATH="\${JAVA_HOME}/bin:\${PATH}"
WORKDIR /data
RUN apt-get update && apt-get install -y --no-install-recommends \\
    curl jq openvpn openssl nginx ca-certificates sudo iproute2 procps \\
    && rm -rf /var/lib/apt/lists/*
RUN set -eux; \\
    ARCH=\$(uname -m); \\
    case "\$ARCH" in \\
        x86_64)  JDK_URL="https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz" ;; \\
        aarch64) JDK_URL="https://download.oracle.com/java/25/latest/jdk-25_linux-aarch64_bin.tar.gz" ;; \\
        *) echo "Unsupported architecture: \$ARCH"; exit 1 ;; \\
    esac; \\
    mkdir -p "\$JAVA_HOME"; \\
    curl -fSL "\$JDK_URL" | tar -xzC "\$JAVA_HOME" --strip-components=1;
CMD ["/bin/bash"]
EOF
        docker build -t $IMAGE_NAME -f $DOCKER_FILE .
    fi
fi

# 3. 生成 docker-compose.yml
if [ ! -f "$COMPOSE_FILE" ]; then
    cat <<EOF > $COMPOSE_FILE
services:
  vpn-server:
    image: $IMAGE_NAME
    container_name: openvpn4j
    privileged: true
    restart: always
    ports:
      - "80:80"
      - "443:443"
      - "8080:8080"
      - "1194:1194/udp"
      - "1194:1194/tcp"
      - "5005:5005"
    volumes:
      - .:/data
    environment:
      - TZ=Asia/Shanghai
      - LANG=C.UTF-8
      - SA_TOKEN_JWT_SECRET_KEY=\${JWT_PASS:-secret}
    working_dir: /data
    command: >
      sh -c "LATEST_JAR=\$\$(ls openvpn4j-*.jar 2>/dev/null | sort -V | tail -n 1);
             if [ -n \"\$\$LATEST_JAR\" ]; then
               exec java -jar \"\$\$LATEST_JAR\";
             else
               echo '❌ Error: No JAR found';
               exit 1;
             fi"
    sysctls:
      - net.ipv4.ip_forward=1
EOF
fi

# 4. 检查并运行 (使用兼容变量)
LATEST_JAR=$(ls openvpn4j-*.jar 2>/dev/null | sort -V | tail -n 1)
if [ -z "$LATEST_JAR" ]; then
    echo "⚠️  环境已就绪，请放入 openvpn4j-*.jar 后重新运行。"
else
    $DOCKER_COMPOSE_CMD up -d
    echo "🎉 容器已启动"
fi