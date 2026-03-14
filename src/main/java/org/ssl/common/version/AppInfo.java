package org.ssl.common.version;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.mapper.VpnServiceMapper;

@Component
@Getter
@RequiredArgsConstructor
public class AppInfo {
    final VpnServiceMapper vpnServiceMapper;

    @Value("${project.version}")
    private String version;

    public boolean checkVersion() {
        String appVersion = CacheUtils.get(SystemConfigEnum.app_version);
        return Strings.CI.equals(appVersion, version);
    }
}
