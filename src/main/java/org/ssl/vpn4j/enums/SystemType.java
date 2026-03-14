package org.ssl.vpn4j.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum SystemType {
    WINDOWS("Windows"),
    LINUX("Linux"),
    MAC("Mac"),
    OTHER("Other");

    private String value;

    public static SystemType getByValue(String value) {
        for (SystemType type : SystemType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
