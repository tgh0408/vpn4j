package org.ssl.vpn4j.enums;

import lombok.Getter;

@Getter
public enum BlackTypeEnum {
    USERNAME("username"),
    IP("ip"),
    MAC("mac");

    private final String type;

    BlackTypeEnum(String type) {
        this.type = type;
    }
}
