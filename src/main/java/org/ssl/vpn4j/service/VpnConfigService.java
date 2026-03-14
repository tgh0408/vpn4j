package org.ssl.vpn4j.service;

import org.ssl.vpn4j.domain.bo.RebootBo;
import org.ssl.vpn4j.domain.vo.VpnConfigVO;

import java.io.IOException;

public interface VpnConfigService {
    VpnConfigVO getConfigList() throws IOException;

    String reboot(RebootBo bo);

    void start();

    void stop();
}
