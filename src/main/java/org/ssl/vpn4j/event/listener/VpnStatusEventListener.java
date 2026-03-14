package org.ssl.vpn4j.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.ssl.vpn4j.cache.VpnOnlineCache;
import org.ssl.vpn4j.event.OfflineUserEvent;
import org.ssl.vpn4j.event.VpnStatusEvent;
import org.ssl.vpn4j.event.VpnStatusInfo;
import org.ssl.vpn4j.utils.TrafficConverter;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VpnStatusEventListener {
    final ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    public void handleVpnStatusEvent(VpnStatusEvent event) {
        List<String> messages = event.getMessages();
        int index = messages.indexOf("Common Name,Real Address,Bytes Received,Bytes Sent,Connected Since");
        if (index == -1 || index + 1 >= messages.size()) {
            log.warn("解析VPN状态事件时，未找到目标行 {}", "Common Name,Real Address,Bytes Received,Bytes Sent,Connected Since");
            return;
        }
        //日志实时在线列表
        Set<String> realTimeOnline = new HashSet<>();
        //计算流量
        for (int i = index + 1; i < messages.size(); i++) {
            if (Strings.CI.equals(messages.get(i), "ROUTING TABLE")) {
                break;
            }
            String[] info = messages.get(i).split(",");
            VpnStatusInfo vpnStatusInfo = Optional.ofNullable(VpnOnlineCache.getOnlineCacheInfo(info[0])).orElse(new VpnStatusInfo());
            realTimeOnline.add(info[0]);
            vpnStatusInfo.setUsername(info[0]);
            vpnStatusInfo.setConnectTime(info[4]);
            vpnStatusInfo.setTrustedIp(info[1].split(":")[0]);
            vpnStatusInfo.setTrustedPort(info[1].split(":")[1]);

            //设置上次读取的数据
            vpnStatusInfo.setLastBytesReceived(vpnStatusInfo.getBytesReceived());
            vpnStatusInfo.setLastBytesSent(vpnStatusInfo.getBytesSent());
            // 设置总流量
            long received = Long.parseLong(info[2]);
            long sent = Long.parseLong(info[3]);
            vpnStatusInfo.setBytesReceived(received);
            vpnStatusInfo.setBytesSent(sent);
            //格式化总流量
            vpnStatusInfo.setFormatBytesSent(TrafficConverter.autoConvert(sent));
            vpnStatusInfo.setFormatBytesReceived(TrafficConverter.autoConvert(received));

            //计算平均流量
            long avgBytesReceived = (vpnStatusInfo.getBytesReceived() - vpnStatusInfo.getLastBytesReceived()) / 3;
            long avgBytesSent = (vpnStatusInfo.getBytesSent() - vpnStatusInfo.getLastBytesSent()) / 3;
            //设置平均流量
            vpnStatusInfo.setAvgBytesReceived(avgBytesReceived);
            vpnStatusInfo.setAvgBytesSent(avgBytesSent);
            //格式化平均流量
            vpnStatusInfo.setFormatAvgBytesSent(TrafficConverter.autoConvert(avgBytesSent));
            vpnStatusInfo.setFormatAvgBytesReceived(TrafficConverter.autoConvert(avgBytesReceived));

            VpnOnlineCache.setIfAbsent(vpnStatusInfo.getUsername(), vpnStatusInfo);
        }

        Set<String> needOffline = VpnOnlineCache.keySet().stream()
            .filter(user -> !realTimeOnline.contains(user))
            .collect(Collectors.toSet());
        if (!needOffline.isEmpty()) {
            //这里都是需要下线的用户
            applicationEventPublisher.publishEvent(new OfflineUserEvent(needOffline,  false));
        }

        index = messages.indexOf("ROUTING TABLE") + 1;
        if (index == 0) {
            log.warn("解析VPN状态事件时，未找到目标行 {}", "ROUTING TABLE");
            return;
        }
        for (int i = index + 1; i < messages.size(); i++) {
            if (Strings.CI.equals(messages.get(i), "GLOBAL STATS")) {
                break;
            }
            String[] info = messages.get(i).split(",");
            VpnStatusInfo vpnStatusInfo = VpnOnlineCache.getOnlineCacheInfo(info[1]);
            vpnStatusInfo.setPoolRemoteIp(info[0]);
        }
    }
}
