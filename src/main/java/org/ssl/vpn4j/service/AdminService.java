package org.ssl.vpn4j.service;

import org.ssl.vpn4j.domain.Admin;
import org.ssl.vpn4j.domain.bo.AdminBo;

import java.util.List;

public interface AdminService {
    List<Admin> getAdminList(String keyword);

    void updatePasswd(AdminBo bo);

    void updateInfo(AdminBo bo);

    Admin getAdminInfo(Long adminId);
}
