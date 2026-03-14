package org.ssl.vpn4j.service;

import org.ssl.vpn4j.domain.ClientProperties;
import org.ssl.vpn4j.domain.SystemClientInfo;
import org.ssl.vpn4j.domain.bo.VpnServiceBo;
import org.ssl.vpn4j.domain.vo.VpnServiceVO;

import java.io.IOException;
import java.util.List;

public interface VpnSystemService {

    SystemClientInfo getRealTimeServiceInfo();

    SystemClientInfo getRealTimeCpuInfo();

    SystemClientInfo getServiceInfo();

    void createServerCer(VpnServiceBo bo) throws IOException;

    ClientProperties createUserCer(String username) throws IOException;

    String getVpnConfig();

    void updateVpnConfig(String config);

    void sendTestEmail();

    List<VpnServiceVO> getConfigs(String [] keys);

    void updateConfigs(VpnServiceBo bo);
}
