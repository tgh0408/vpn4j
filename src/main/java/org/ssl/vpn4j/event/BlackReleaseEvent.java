package org.ssl.vpn4j.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ssl.vpn4j.domain.Black;

@Data
@AllArgsConstructor
public class BlackReleaseEvent {
    private Black release;
}
