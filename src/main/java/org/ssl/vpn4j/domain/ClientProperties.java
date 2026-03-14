package org.ssl.vpn4j.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientProperties {
    private String clientKey;
    private String clientCert;
}
