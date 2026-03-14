package org.ssl.vpn4j.utils;

import lombok.Getter;
import org.ssl.vpn4j.enums.SystemType;

public class SystemTypeUtil {

    @Getter
    static SystemType systemType;

    static {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("windows")) {
            systemType = SystemType.WINDOWS;
        } else if (osName.toLowerCase().contains("linux")) {
            systemType = SystemType.LINUX;
        } else if (osName.toLowerCase().contains("mac")) {
            systemType = SystemType.MAC;
        } else {
            systemType = SystemType.OTHER;
        }
    }

}
