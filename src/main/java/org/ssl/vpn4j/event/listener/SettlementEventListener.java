package org.ssl.vpn4j.event.listener;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.ssl.vpn4j.event.SettlementEvent;

/**
 * 结算流量
 */

@Component
@Slf4j
@AllArgsConstructor
public class SettlementEventListener {

    @EventListener
    public void handleSettlementEvent(SettlementEvent event) {
        log.info("SettlementEvent: 结算流量 {}", event);
    }
}
