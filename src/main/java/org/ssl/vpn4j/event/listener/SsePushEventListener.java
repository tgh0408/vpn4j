package org.ssl.vpn4j.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.ssl.common.json.utils.JsonUtils;
import org.ssl.common.sse.utils.SseMessageUtils;
import org.ssl.vpn4j.cache.VpnOnlineCache;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.domain.SystemClientInfo;
import org.ssl.vpn4j.enums.SsePushTypeEnum;
import org.ssl.vpn4j.event.SseCpuPushEvent;
import org.ssl.vpn4j.event.SsePushEvent;
import org.ssl.vpn4j.event.VpnStatusInfo;
import org.ssl.vpn4j.service.AccountService;
import org.ssl.vpn4j.service.VpnSystemService;
import org.ssl.vpn4j.utils.TrafficConverter;

import java.util.Collection;
import java.util.List;

/**
 * SSE 实时状态推送事件监听器
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class SsePushEventListener {
    final VpnSystemService vpnSystemService;
    final AccountService accountService;

    @EventListener
    public void onEvent(SsePushEvent event) {
        Collection<VpnStatusInfo> onlineAll = VpnOnlineCache.getOnlineAll();
        SystemClientInfo serviceInfo = vpnSystemService.getRealTimeServiceInfo();
        serviceInfo.setOnlineCount(onlineAll.size());
        serviceInfo.setOnlineUsers(onlineAll);
        //总用户数
        List<Account> userCount = accountService.getUserCount();
        serviceInfo.setTotalUserCount(userCount.size());
        //启用数量
        serviceInfo.setEnableCount(userCount.stream().filter(o -> o.getStatus().equals("1")).count());
        //禁用数量
        serviceInfo.setDisableCount(userCount.stream().filter(o -> o.getStatus().equals("0")).count());

        //总发送
        long totalBytesSent = onlineAll.stream().mapToLong(VpnStatusInfo::getBytesSent).sum();
        serviceInfo.setTotalBytesSent(totalBytesSent);
        serviceInfo.setFormatTotalBytesSent(TrafficConverter.autoConvert(totalBytesSent));

        //总接收
        long totalBytesReceived = onlineAll.stream().mapToLong(VpnStatusInfo::getBytesReceived).sum();
        serviceInfo.setTotalBytesReceived(totalBytesReceived);
        serviceInfo.setFormatTotalBytesReceived(TrafficConverter.autoConvert(totalBytesReceived));

        //总上行 瞬时上行
        long totalAvgBytesSent = onlineAll.stream().mapToLong(VpnStatusInfo::getAvgBytesSent).sum();
        serviceInfo.setTotalAvgBytesSent(totalAvgBytesSent);
        serviceInfo.setFormatTotalAvgBytesSent(TrafficConverter.autoConvert(totalAvgBytesSent));

        //总下行 瞬时下行
        long totalAvgBytesReceived = onlineAll.stream().mapToLong(VpnStatusInfo::getAvgBytesReceived).sum();
        serviceInfo.setTotalAvgBytesReceived(totalAvgBytesReceived);
        serviceInfo.setFormatTotalAvgBytesReceived(TrafficConverter.autoConvert(totalAvgBytesReceived));

        serviceInfo.setSseType(SsePushTypeEnum.SYSTEM_MONITOR);
        SseMessageUtils.sendMessage(JsonUtils.toJsonString(serviceInfo));
    }

    @EventListener
    public void onCpuEvent(SseCpuPushEvent event) {
        SystemClientInfo serviceInfo = vpnSystemService.getRealTimeCpuInfo();
        serviceInfo.setSseType(SsePushTypeEnum.CPU_MONITOR);
        SseMessageUtils.sendMessage(JsonUtils.toJsonString(serviceInfo));
    }
}
