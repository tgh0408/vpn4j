package org.ssl.vpn4j.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ssl.common.core.exception.ServiceException;

@Getter
@AllArgsConstructor
public enum SystemConfigEnum {
    /**
     * 关键概念
     * 公私钥对：每个.crt证书都有对应的.key私钥
     * 信任链：ca.crt → 验证 → server.crt
     * 安全等级：
     * 最高：ca.key（绝对不能泄露）
     * 高：server.key
     * 中：ta.key
     * 使用场景
     * 客户端只需要：ca.crt + client.crt + client.key + ta.key（可选）
     * 服务器需要全部文件（除了客户端证书）
     */
    ssl_path("ssl.path", "SSL路径", "/data/openvpn/ssl"),
    ta_key("ta.key", "TLS-auth密钥", ""),
    ca_key("ca.key", "CA私钥", ""),
    ca_crt("ca.crt", "根证书/CA证书", ""),
    server_key_name("server.key.name", "服务器证书名称", ""),
    server_crt("server.crt", "服务器证书", ""),
    server_key("server.key", "服务器私钥", ""),
    dh_pem("dh.pem", "DH参数", """
            -----BEGIN DH PARAMETERS-----
            MIIBCAKCAQEAykl88O34MvPK2NW8EYH8bqvSRAcGZT79ROYkIUiO2Zq5+qlbtHBL
            ZP1Ss+cJySba87S/p1TjRcaz15k1cuI94+GeWlKQZ4ZkzVZTTFmjt5IVxXl36rAn
            LBGsAMd6IL50M/6mzScvJ1MCPgjKU2scMEKQWFN0B3WRlN2WosJUWi+Pyn8qgCnc
            eYBiLXHmgKG/YJ7YoLi6Je/22NMLL20EYebA5hQS0DcbfLWTLeyUerdfXa4AELGO
            2Xa300rOV1Xefxu6oF2ZwcBhIvAeCD1f+zD6dy3QGFcdMvj+WNrzneKZrLkkmxMi
            nJtNT7yPVjy1mrK25CXZfhuaULXVzWyJrwIBAg==
            -----END DH PARAMETERS-----
            
            """),
    ca_name("ca.name", "CA证书名称", ""),
    ca_key_path("ca.key.path", "CA证书路径", "/data/openvpn/ssl/ca.key"),
    server_port("server.port", "服务器端口", "1194"),
    server_name("server.name", "服务器名称", ""),
    ca_expire("ca.expire", "CA证书有效期", "3650"),
    server_proto("server.proto", "服务器协议", "tcp"),
    encrypted_passwords("encrypted.passwords", "证书加密密码", ""),
    server_ip("server.ip", "服务器IP", ""),
    user_ca_path("user.ca.path", "用户CA证书路径", "/data/openvpn/ssl/userClient"),
    client_ovpn("client.ovpn", "客户端配置文件", """
            client
            dev tun
            proto ${proto}
            # 远程服务端IP 端口号根据实际应用修改，其它都不用做任何修改！
            # 注意预留空格
            remote ${server} ${port}
            resolv-retry infinite
            nobind
            setenv FRIENDLY_NAME ${username}
            # 和性能有关系
            sndbuf 0
            rcvbuf 0
            
            # 传输队列长度，默认是500，不建议设置太大。
            # 仅Linux客户端支持此配置项。
            #txqueuelen 1000
            
            persist-key
            persist-tun
            remote-cert-tls server
            cipher AES-256-GCM
            auth SHA256
            verb 3
            auth-nocache
            auth-user-pass
            
            # 忽略服务端推送的路由
            #route-nopull
            
            #--- 对接爱快路由附加配置开始 ---#
            reneg-sec 0
            key-direction 1
            
            # --- CA证书部分 ---
            <ca>
            ${ca.crt}
            </ca>
            
            # --- TLS认证密钥部分（如果有的话）---
            # 如果你有TLS认证密钥，取消注释以下部分并添加密钥
            <tls-auth>
            ${tls-auth}
            </tls-auth>
            
            <cert>
            ${cert}
            </cert>
            
            <key>
            ${key}
            </key>
            """),
    management_ip("management.ip", "管理IP", "127.0.0.1"),
    management_port("management.port", "管理端口", "7505"),
    server_path("server.path", "openvpn服务路径", "/usr/sbin/openvpn"),
    server_conf("server.conf", "服务器配置文件", """
            # 特别注意：行首的“#”号为注释符号，若想让此行配置生效只需删除行首的“#”号即可。
            # 监听端口号，注意：Docker方式部署不需要修改
            port 1194
            # 使用 TCP or UDP server? 建议使用TCP协议, 这样更稳定
            proto tcp
            # 使用tun设备,不建议修改
            dev tun
            # 服务端证书及私钥等其它安全文件
            ca /data/openvpn/ssl/ca.crt
            cert /data/openvpn/ssl/server.crt
            key /data/openvpn/ssl/server.key
            dh /data/openvpn/ssl/dh.pem
            # 提供额外的安全保障 通过SSL/TLS，创建“HMAC防火墙”
            tls-auth /data/openvpn/ssl/ta.key 0
            # Network topology; 有序分配
            topology subnet
            push "topology subnet"
            # 定义VPN隧道网络地址
            server 10.0.10.0 255.255.255.0
            # 维护客户端<->虚拟IP地址的记录
            ifconfig-pool-persist /data/openvpn/server/ipp.txt
            # 向客户端推送路由信息, 注意请使用英文双引号
            #push "route 172.16.10.0 255.255.255.0"
            # 重定向客户端的所有流量，表示客户端所有流量通过VPN服务器转发，取消下面3行开头注释符号“#”。
            #push "redirect-gateway def1"
            #push "dhcp-option DNS 223.5.5.5" # 建议填写VPN服务器上面配置的DNS地址（cat /data/resolv.conf）
            #push "dhcp-option DNS 8.8.8.8" # 备用DNS服务器地址
            # 客户端与客户端之间互访
            client-to-client
            # 指定CCD配置文件目录
            # client-config-dir /data/openvpn/ccd
            # ccd 从服务端获取,不再本地存储
            client-connect /data/openvpn/ccd/ccd.sh
            # 允许一个用户被多个客户端同时使用（不建议这样使用，不安全）
            #duplicate-cn
            # keepalive指令,类ping消息通过链接，以便双方都知道对方何时断开链接。
            # 每隔20秒Ping一次，假设远程如果在120秒的时间段内没有接收到ping，则认为对端关闭。
            keepalive 20 120
            # v2.4之后自动采用AES-256-GCM in TLS mode，客户端配置文件中也有配置
            cipher AES-256-GCM
            auth SHA256
            # 客户端最大链接数量
            max-clients 253
            # 降级使用，这样更安全，以 sslvpn 账户运行程序，Windows系统不支持。
            user root
            group root
            # 通过keepalive检测超时后，重新启动VPN，不重新读取keys，保留第一次使用的keys
            persist-key
            # 通过keepalive检测超时后，重新启动VPN，一直保持tun或者tap设备是linkup的。否则网络连接，会先linkdown然后再linkup
            persist-tun
            # 把openvpn的一些状态信息写到文件中，比如客户端获得的IP地址
            status /data/openvpn/status/openvpn-status.log
            # 记录日志文件
            log /data/openvpn/logs/openvpn.log
            # 记录日志记录级别，0-9 级别越高，记录越多
            verb 2
            # 指定私钥密码文件位置
            # askpass /data/openvpn/auth.passwd
            # 开启管理端口,获得实时流量查询
            management 127.0.0.1 7505
            management-query-passwords
            # 下面4行是使用密码验证配置
            script-security 3
            auth-user-pass-verify /data/openvpn/auth/auth.sh via-env
            username-as-common-name
            #verify-client-cert none # 启用后将不再验证客户端证书
            # 默认值3600， 也就是一个小时进行一次TSL重新协商。
            # 这个参数在服务端和客户端设置都有效 如果两边都设置了，那马就按照时间短的设定优先。当两边同时设置成0，表示禁用TSL重协商。
            reneg-sec 0
            # 不缓存用户认证信息
            auth-nocache
            # 和性能有关系
            sndbuf 0
            rcvbuf 0
            # 传输队列长度，默认是500，不建议设置太大。
            txqueuelen 1000
            
            mssfix 1200
            """),
    server_conf_path("server.conf.path", "服务器配置文件路径", "/data/openvpn/server/server.conf"),
    root_passwd("root.passwd", "root密码", ""),
    system_email("system.email", "系统邮箱", ""),
    max_retry_count("max.retry.count", "登陆最多错误次数", "3"),
    lock_time("lock.time", "禁止登录时长", "300"),
    token_timeout("token.timeout", "token强制超时时间", "3600"),
    token_active_timeout("token.active.timeout", "token最少活跃时间", "600"),
    ccd_default_conf("ccd.default.conf", "ccd默认配置文件", """
            # 警告，请认真阅读以下四条
            # 1、配置CCD时请特别注意，不清楚，请不要随便使用此功能。
            # 2、“#”号开头的行为注释，配置不当可能导致VPN服务起不来。
            # 3、下面准备了4个配置项模板，请酌情修改。
            # 4、Author by Linuxcc Email: linux_support@163.com
            
            # 给客户端指定静态IP地址(优先级高于用户列表中已分配IP)
            # 格式：ipconfig-push <客户端IP> <子网掩码>
            # 警告：ifconfig-push 参数只能有一个，并且必须是在第10行。
            ifconfig-push 10.0.200.10 255.255.255.0
            
            # 指定客户端可访问的 IP 范围
            #iroute 192.168.1.0 255.255.255.0
            
            # 将特定的路由推送给客户端。
            #push "route 172.16.10.0 255.255.255.0"
            
            # 重定向客户端的所有流量
            #push "redirect-gateway def1"
            """),
    subnet_mask("subnet.mask", "子网掩码", "255.255.255.0"),
    smtp_title("smtp.title", "SMTP标题", ""),
    smtp_download_address("smtp.download.address", "SMTP下载地址", ""),
    ccd_auth_value("ccd.auth.value", "ccd授权shell值", """
            #!/bin/bash
            # /data/openvpn/ccd/ccd.sh
            
            # ================= 配置区 =================
            LOG_FILE="/data/openvpn/ccd_debug.log"
            # 将标准输出和错误都重定向到日志，方便排查
            exec 1>>$LOG_FILE 2>&1
            
            SPRINGBOOT_URL="http://localhost:8080/api/openvpn/ccd"
            TIMEOUT=5
            CONFIG_FILE="$1"
            
            # 强制成功函数
            finish_success() {
                echo "[$0] Done."
                exit 0
            }
            # ==========================================
            
            echo "[$0] Start: User=$username"
            
            # 1. 检查必要条件
            if [ -z "$username" ]; then
                echo "No username."
                finish_success
            fi
            
            if ! command -v jq >/dev/null 2>&1; then
                echo "Error: jq not found. Please install jq (yum install jq -y)."
                finish_success
            fi
            
            # 2. 构造请求 JSON
            # 使用 jq 构造能完美处理用户名里的特殊字符
            JSON_DATA=$(jq -n --arg un "$username" '{username: $un}')
            
            # 3. 发送请求
            # || true 保证即使 curl 失败脚本也不会崩溃退出
            RESPONSE=$(curl -s -X POST \\
                -H "Content-Type: application/json" \\
                -d "$JSON_DATA" \\
                --max-time $TIMEOUT \\
                "$SPRINGBOOT_URL" || true)
            
            echo "Response received: $RESPONSE"
            
            # 4. 核心解析逻辑 (适配你的返回值)
            # jq -r .data 会直接把 JSON 里的 "\\n" 渲染成真正的换行符
            # jq -r .code 提取状态码
            HTTP_CODE=$(echo "$RESPONSE" | jq -r '.code // 500')
            
            if [ "$HTTP_CODE" == "200" ]; then
                # 提取 data 内容
                CONFIG_CONTENT=$(echo "$RESPONSE" | jq -r '.data // empty')
            
                # 写入 OpenVPN 临时文件
                if [ -n "$CONFIG_CONTENT" ] && [ "$CONFIG_CONTENT" != "null" ]; then
                    echo "$CONFIG_CONTENT" > "$CONFIG_FILE"
                    echo "Config written to $CONFIG_FILE"
                else
                    echo "Data is null or empty, skipping."
                fi
            else
                echo "Backend returned code $HTTP_CODE, skipping config."
            fi
            
            # 5. 必须返回 0
            finish_success
            """),
    auth_value("auth.value", "登录授权shell值", """
            #!/bin/bash
            # /data/openvpn/auth/auth.sh
            
            # ================= 配置区 =================
            # Spring Boot 服务地址
            SPRINGBOOT_URL="http://localhost:8080/api/openvpn/auth"
            # 超时时间（秒）
            TIMEOUT=10
            # 日志文件
            LOG_FILE="/data/openvpn/auth/auth.log"
            # ==========================================
            
            TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
            
            # 1. 简单日志记录（记录尝试连接的用户）
            echo "[$TIMESTAMP] Auth request - User: $username, IP: $untrusted_ip" >> $LOG_FILE
            
            # 2. 检查环境变量（由 OpenVPN 注入）
            if [ -z "$username" ] || [ -z "$password" ]; then
                echo "[$TIMESTAMP] ERROR: Missing username or password" >> $LOG_FILE
                exit 1
            fi
            
            # 3. 构造 JSON 数据
            # 优先使用 jq 构造（防止密码含特殊字符导致 JSON 格式错误），如果没有 jq 则回退到手动构造
            if command -v jq >/dev/null 2>&1; then
                JSON_DATA=$(jq -n \\
                              --arg un "$username" \\
                              --arg pw "$password" \\
                              --arg ip "$untrusted_ip" \\
                              --arg cn "$common_name" \\
                              --arg ts "$TIMESTAMP" \\
                              '{username: $un, password: $pw, clientIp: $ip, commonName: $cn, timestamp: $ts}')
            else
                # 手动构造时的转义处理（防止密码里的 " 或 \\ 破坏 JSON）
                safe_pw=$(echo "$password" | sed 's/\\\\/\\\\\\\\/g' | sed 's/"/\\\\"/g')
                safe_un=$(echo "$username" | sed 's/\\\\/\\\\\\\\/g' | sed 's/"/\\\\"/g')
                JSON_DATA="{\\"username\\": \\"$safe_un\\", \\"password\\": \\"$safe_pw\\", \\"clientIp\\": \\"$untrusted_ip\\", \\"commonName\\": \\"$common_name\\", \\"timestamp\\": \\"$TIMESTAMP\\"}"
            fi
            
            # 4. 发送 HTTP 请求
            # -s: 静默模式
            # 2>/dev/null: 隐藏 curl 的错误输出，由脚本逻辑处理
            RESPONSE=$(curl -s -X POST \\
                -H "Content-Type: application/json" \\
                -H "Accept: application/json" \\
                -H "X-OpenVPN-Server: $(hostname)" \\
                -d "$JSON_DATA" \\
                --max-time $TIMEOUT \\
                --connect-timeout 5 \\
                "$SPRINGBOOT_URL")
            
            CURL_EXIT_CODE=$?
            
            # 5. 检查网络连接是否成功
            if [ $CURL_EXIT_CODE -ne 0 ]; then
                echo "[$TIMESTAMP] ERROR: Curl connection failed (code: $CURL_EXIT_CODE)" >> $LOG_FILE
                exit 1
            fi
            
            # 6. 解析业务返回结果
            # 目标格式: { "code": 200, "data": null, "msg": "操作成功" }
            
            # 使用 grep 正则匹配 "code": 200 或 "code":200
            # grep -q 表示静默查找，找到返回 0，找不到返回 1
            if echo "$RESPONSE" | grep -qE '"code"\\s*:\\s*200'; then
                # === 认证成功 ===
                echo "[$TIMESTAMP] SUCCESS: User $username authenticated" >> $LOG_FILE
                exit 0
            else
                # === 认证失败 ===
                # 尝试提取 "msg": "xxx" 中的内容
                # 逻辑：匹配 "msg": "内容" -> 提取双引号里的内容
                ERROR_MSG=$(echo "$RESPONSE" | sed -n 's/.*"msg"\\s*:\\s*"\\([^"]*\\)".*/\\1/p')
            
                # 如果提取为空（可能是 JSON 格式不对），给一个默认值
                if [ -z "$ERROR_MSG" ]; then
                    ERROR_MSG="Unknown error (Raw response: $RESPONSE)"
                fi
            
                echo "[$TIMESTAMP] FAILED: User $username denied. Msg: $ERROR_MSG" >> $LOG_FILE
                exit 1
            fi
            """),
    web_path("web.path", "Web路径", "/data/openvpn/web"),
    nginx_config_path("nginx.config.path", "nginx路径", "/data/openvpn/web/nginx/nginx.conf"),
    nginx_conf("nginx.conf", "nginx配置文件", """
            # 必须包含 events 块，即使是空的
            events {
                worker_connections 1024;
            }
            
            http {
                # 包含必要的 MIME 类型映射
                include       /etc/nginx/mime.types;
                default_type  application/octet-stream;
            	server {
            		listen       80;
            		server_name  localhost;
            
            		# 对应 docker-compose.yml 中的容器内挂载路径
            		root   /data/openvpn/web/dist;
            		index  index.html;
            
            		# ==========================================
            		# Gzip 压缩配置
            		# ==========================================
            		gzip on;
            		gzip_static on;\s
            		gzip_min_length 1k;
            		gzip_comp_level 6;
            		gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
            		gzip_vary on;
            
            		# ==========================================
            		# 静态资源缓存配置
            		# ==========================================
            		location ~* \\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|eot|ttf|otf)$ {
            			expires 30d;
            			add_header Cache-Control "public, no-transform";
            			try_files $uri =404;
            		}
            
            		# ==========================================
            		# 前端路由 (处理 Vue Router 的 history 模式)
            		# ==========================================
            		location / {
            			try_files $uri $uri/ /index.html;
            		}
            
            		# ==========================================
            		# 后端 API 接口反向代理
            		# ==========================================
            		location /api/ {
            			rewrite ^/api/(.*)$ /$1 break;
            
            			# -----------------------------------------------------------
            			# 【情况 A】如果 Nginx 和后端确确实实在同一个容器内部运行：
            			proxy_pass http://127.0.0.1:8080;
            			# -----------------------------------------------------------
            
            			# -----------------------------------------------------------
            			# 【情况 B】如果后端其实是 docker-compose 里的另一个服务（比如 vpn-server）：
            			# 请注释掉上面的 127.0.0.1，打开下面这行：
            			# proxy_pass http://vpn-server:8080;
            			# -----------------------------------------------------------
            
            			proxy_set_header Host $host;
            			proxy_set_header X-Real-IP $remote_addr;
            			proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            			proxy_set_header X-Forwarded-Proto $scheme;
            		}
            	}
            }
            """),
    //mail
    smtp_enable("smtp.enable", "SMTP启用", "0"),
    smtp_host("smtp.host", "SMTP服务器", "smtp.163.com"),
    smtp_port("smtp.port", "SMTP端口", "465"),
    smtp_auth("smtp.auth", "SMTP授权", "1"),
    smtp_from("smtp.from", "发送方，遵循RFC-822标准", "admin@163.com"),
    smtp_user("smtp.user", "SMTP用户", "admin@163.com"),
    smtp_password("smtp.password", "SMTP密码", ""),
    smtp_starttls_enable("smtp.starttls.enable", "SMTP启动TLS", "1"),
    smtp_ssl_enable("smtp.ssl.enable", "SMTP SSL", "1"),
    smtp_timeout("smtp.timeout", "SMTP超时时间", "0"),
    smtp_connection_timeout("smtp.connection.timeout", "SMTP连接超时时间", "0"),
    user_client_download_url("user.client.download.url", "用户客户端下载地址", "api/client/download"),
    app_version("app.version", "应用版本", "1.0.0");
    public static final String cacheName = "Global:Vpn:Config";
    private final String key;
    private final String desc;
    private final String defaultValue;

    public static SystemConfigEnum fromKey(String key, boolean throwException) {
        for (SystemConfigEnum e : SystemConfigEnum.values()) {
            if (e.getKey().equals(key)) { // 建议使用 getter 访问
                return e;
            }
        }
        if (throwException){
            throw new ServiceException("未知的配置项 {}", key);
        }
        return null;
    }

    public static String getDescByKey(String key) {
        for (SystemConfigEnum e : SystemConfigEnum.values()) {
            if (e.getKey().equals(key)) { // 建议使用 getter 访问
                return e.getDesc();
            }
        }
        // 建议不要直接抛异常，或者抛出更具体的自定义异常
        return null;
    }
}
