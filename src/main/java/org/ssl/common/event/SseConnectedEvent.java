package org.ssl.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SseConnectedEvent {
    private Long userId;
    private String token;
}
