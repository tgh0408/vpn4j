package org.ssl.vpn4j.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.ssl.vpn4j.event.ConnectEvent;
import org.ssl.vpn4j.event.VpnLogEvent;
import org.ssl.vpn4j.event.VpnStatusEvent;

import java.util.List;

/**
 * openvpn 日志事件监听器
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class VpnLogEventListener {
    final ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    @Async
    public void onVpnLogEvent(VpnLogEvent event) {
        List<String> lines = event.getMessages();
//        lines.forEach(System.out::println);
        if (lines.isEmpty()){
            return;
        }

        boolean status = lines.stream().anyMatch(line -> line.contains("OpenVPN CLIENT LIST"));
        if (status){
            applicationEventPublisher.publishEvent(new VpnStatusEvent(lines));
            return;
        }
        boolean connect = lines.stream().anyMatch(line -> line.contains("ESTABLISHED"));
        if (connect){
            //连接成功
            applicationEventPublisher.publishEvent(new ConnectEvent(lines));
        }
    }


}
