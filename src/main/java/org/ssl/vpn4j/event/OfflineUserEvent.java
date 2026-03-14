package org.ssl.vpn4j.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OfflineUserEvent {
    private Set<String> offlineUsers;
    // 是否强制下线
    private boolean force;
}
