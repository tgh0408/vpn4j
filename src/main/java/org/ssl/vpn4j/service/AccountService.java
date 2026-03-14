package org.ssl.vpn4j.service;

import org.ssl.common.mybatis.core.page.PageQuery;
import org.ssl.common.mybatis.core.page.TableDataInfo;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.domain.bo.AccountUpdateBo;
import org.ssl.vpn4j.domain.bo.DisableUserBo;
import org.ssl.vpn4j.domain.bo.DownLoadConfigBo;
import org.ssl.vpn4j.domain.bo.UserListBO;
import org.ssl.vpn4j.domain.vo.AccountVO;
import org.ssl.vpn4j.domain.vo.UserListItem;

import java.io.IOException;
import java.util.List;

public interface AccountService {

    TableDataInfo<UserListItem> getUserList(UserListBO userListBO, PageQuery pageQuery);

    List<Account> getUserCount();

    void updateUser(AccountUpdateBo updateBo);

    void checkStaticIp(String username,String staticIp);

    void addUser(AccountUpdateBo addBo) throws IOException;

    void reCreateUserCer(AccountUpdateBo addBo) throws IOException;

    void download(DownLoadConfigBo bo);

    void deleteUser(Long [] ids);

    void offline(Long[] ids);

    void disable(DisableUserBo disableUserBo);

    AccountVO copyUserPasswd(Long id);

    void sendEmail(Long [] ids);
}
