package org.ssl.vpn4j.runner;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.utils.StringUtils;
import org.ssl.vpn4j.cache.constant.VpnConstant;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.event.RecoverStatusEvent;
import org.ssl.vpn4j.event.VpnLogEvent;
import org.ssl.vpn4j.mapper.AccountMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 管理 OpenVPN 运行
 */

@Slf4j
@RequiredArgsConstructor
@Component
@Getter
@ConditionalOnProperty(prefix = "vpn4j.manager", name = "enable", havingValue = "true") // 核心修改：条件注入
public class ManageRunner implements ApplicationRunner {
    final AccountMapper accountMapper;
    final ApplicationEventPublisher applicationEventPublisher;
    private PrintWriter manageWriter;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        //发送恢复状态事件
        applicationEventPublisher.publishEvent(new RecoverStatusEvent());
        // 启动一个守护线程，用于管理 OpenVPN 连接
        Thread.ofVirtual().start(() -> {
            for (; ; ) {
                try {
                    if (Strings.CI.equals("active", CacheUtils.get(VpnConstant.VPN_CACHE_NAME, VpnConstant.VPN_STATUS))) {
                        // 这个方法内部应该是一个阻塞式的长连接
                        processVpnManagement();
                    } else {
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    log.error("守护线程管理接口连接异常,等待重连: {}", e.getMessage());
                    try {
                        // 只有在连接失败或断开后，才进入休眠，避免忙等待
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {
                        log.error("Thread sleep interrupted", ie);
                    }
                }
            }
        });
    }

    private void processVpnManagement() {
        String service_path = CacheUtils.get(SystemConfigEnum.server_path);
        log.info("服务路径: {}", service_path);
        String conf_path = CacheUtils.get(SystemConfigEnum.server_conf_path);
        log.info("配置文件路径: {}", conf_path);

        log.info("正在连接 OpenVPN 管理接口...");
        String serverIp = CacheUtils.get(SystemConfigEnum.management_ip);
        log.info("管理接口IP: {}", serverIp);
        String serverPort = Optional.ofNullable((String) CacheUtils.get(SystemConfigEnum.management_port)).orElse("7075");
        log.info("管理接口端口: {}", serverPort);

        //是否认证
        boolean encrypted = StringUtils.isNotEmpty(CacheUtils.get(SystemConfigEnum.encrypted_passwords));

        try (Socket socket = new Socket(serverIp, Integer.parseInt(serverPort));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            log.info("Vpn 管理端口已连接,守护进程启动");
            manageWriter = writer;
            writer.println("state on"); // 开启状态推送

            String line;
            List<String> lines = new ArrayList<>();
            // 循环读取 OpenVPN 的输出日志
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERROR:")) {
                    log.error("[VPN] ERROR: {}", line);
                }
                // 打印日志以便调试
                //log.info("[VPN]: {}", line);
                lines.add(line);
                if (line.contains("END")) {
                    applicationEventPublisher.publishEvent(new VpnLogEvent(List.copyOf(lines)));
                    lines.clear();
                }
                if (encrypted) {
                    log.info("连接成功，等待密码请求...");
                    // 如果检测到状态已经是 CONNECTED 或正处于无需密码的状态
                    if (line.contains("CONNECTED,SUCCESS") || line.contains("END")) {
                        log.info("检测到已认证状态，跳过密码发送。");
                        encrypted = false;
                    }
                    // 核心逻辑：监听密码请求
                    // OpenVPN 通常发送格式： >PASSWORD:Need 'Private Key' password
                    if (line.startsWith(">PASSWORD:Need 'Private Key'")) {
                        log.info(">> 检测到密码请求，正在发送密码...");
                        String privateKeyPassword = CacheUtils.get(SystemConfigEnum.encrypted_passwords);
                        // 发送密码命令，格式：password <类型> <密码>
                        // 类型通常是 'Private Key' (带单引号)
                        String command = "password 'Private Key' " + privateKeyPassword;
                        writer.println(command);
                    }

                    // 监听成功消息
                    if (line.contains("SUCCESS: 'Private Key'")) {
                        log.info(">> 密码验证成功！OpenVPN 启动成功。");
                        // 验证成功后也认为不需要认证了，直接走监听逻辑
                        encrypted = false;
                    }
                }
            }
        } catch (IOException e) {
            log.error("无法连接到 OpenVPN 管理接口。请确认 OpenVPN 已启动并配置了 --management。");
            CacheUtils.clear(VpnConstant.VPN_CACHE_NAME);
        }
    }

}
