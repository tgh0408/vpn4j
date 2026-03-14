package org.ssl.vpn4j.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ssl.vpn4j.enums.SsePushTypeEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SseSendMsg {
    private SsePushTypeEnum sseType;
    private String data;
}
