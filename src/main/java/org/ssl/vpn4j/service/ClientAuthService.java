package org.ssl.vpn4j.service;

import org.ssl.vpn4j.domain.bo.ClientAuthBo;

public interface ClientAuthService {

    void authenticate(ClientAuthBo clientAuthBo,String serverName);

    String generateCcdConfig(Long id);

    String generateCcdConfig(String username);
}
