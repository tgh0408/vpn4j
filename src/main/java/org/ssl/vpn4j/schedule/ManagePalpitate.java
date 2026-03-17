package org.ssl.vpn4j.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.ssl.vpn4j.runner.ManageRunner;
import org.ssl.vpn4j.schedule.cron.VpnSchedule;

import java.io.PrintWriter;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vpn4j.manager", name = "enable", havingValue = "true") // 核心修改：条件注入
public class ManagePalpitate implements VpnSchedule {
    final ManageRunner manageRunner;

    @Override
    public void run() {
        PrintWriter manageWriter = manageRunner.getManageWriter();
        if (manageWriter != null) {
            manageWriter.println("status 0");
//            log.info("Sent status request to OpenVPN server, {}", "向 OpenVPN 服务器发送状态请求");
        }

    }

    @Override
    public String getScheduleName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getScheduleDescription() {
        return "VPN Manager Palpitate Task";
    }

    @Override
    public String getCron() {
        return "0/3 * * * * ?";
    }
}
