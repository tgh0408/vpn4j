package org.ssl.vpn4j.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DistInfo {
    private String mount;
    private String totalSpace;
    private String freeSpace;
    private String usedSpace;
    private String percentageUsed;
}
