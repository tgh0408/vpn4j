package org.ssl.vpn4j.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class SettlementEvent {
    private Map<String, VpnStatusInfo> vpnStatusInfoMap;
}
