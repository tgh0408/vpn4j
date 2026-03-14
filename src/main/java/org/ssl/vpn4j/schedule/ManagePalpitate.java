package org.ssl.vpn4j.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ssl.vpn4j.runner.ManageRunner;

import java.io.PrintWriter;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vpn4j.manager", name = "enable", havingValue = "true") // 核心修改：条件注入
public class ManagePalpitate {
    final ManageRunner manageRunner;

    @Scheduled(cron = "0/3 * * * * ?")
    @Async("scheduledExecutorService")
    public void palpitate() {
        PrintWriter manageWriter = manageRunner.getManageWriter();
        if (manageWriter != null){
            manageWriter.println("status 0");
//            log.info("Sent status request to OpenVPN server, {}", "向 OpenVPN 服务器发送状态请求");
        }

    }
}
