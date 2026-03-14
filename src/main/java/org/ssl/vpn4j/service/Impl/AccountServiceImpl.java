package org.ssl.vpn4j.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.constant.CacheConstants;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.ServletUtils;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.common.core.utils.StringUtils;
import org.ssl.common.core.utils.file.FileUtils;
import org.ssl.common.mail.utils.MailUtils;
import org.ssl.common.mybatis.core.page.PageQuery;
import org.ssl.common.mybatis.core.page.TableDataInfo;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.domain.ClientProperties;
import org.ssl.vpn4j.domain.bo.*;
import org.ssl.vpn4j.domain.vo.AccountVO;
import org.ssl.vpn4j.domain.vo.UserListItem;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.event.OfflineUserEvent;
import org.ssl.vpn4j.mapper.AccountMapper;
import org.ssl.vpn4j.service.AccountService;
import org.ssl.vpn4j.service.CcdService;
import org.ssl.vpn4j.service.VpnSystemService;
import org.ssl.vpn4j.utils.Tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    final AccountMapper accountMapper;
    final Environment environment;

    @Override
    public TableDataInfo<UserListItem> getUserList(UserListBO userListBO, PageQuery pageQuery) {
        LambdaQueryWrapper<Account> accountWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isEmpty(userListBO.getKeyword())) {
            accountWrapper.orderByDesc(Account::getId);
        } else {
            accountWrapper.like(Account::getUsername, userListBO.getKeyword());
            accountWrapper.or();
            accountWrapper.like(Account::getNickname, userListBO.getKeyword());
        }
        accountWrapper.in(userListBO.getStatus() != null, Account::getStatus, userListBO.getStatus());
        Page<Account> accountPage = accountMapper.selectPage(pageQuery.build(), accountWrapper);
        //account 转换为 UserListItem
        IPage<UserListItem> userListItemPage = accountPage.convert(account -> {
            UserListItem userListItem = new UserListItem();
            userListItem.setId(account.getId());
            userListItem.setNickname(account.getNickname());
            userListItem.setUsername(account.getUsername());
            userListItem.setStatus(account.getStatus());
            userListItem.setExpiredTime(account.getExpireTime());
            userListItem.setUserEmail(account.getEmail());
            userListItem.setOnline(account.getOnline());
            userListItem.setStaticIp(account.getStaticIp());
            userListItem.setCreateTime(account.getCreateTime());
            userListItem.setUpdateTime(account.getUpdateTime());
            return userListItem;
        });
        return TableDataInfo.build(userListItemPage);
    }

    /**
     * 只需要查就行了 曾山改统一使用mybatis拦截器
     */
    @Override
    @Cacheable(value = CacheConstants.TOTAL_USER_KEY, key = "'user_count'")
    public List<Account> getUserCount() {
        return accountMapper.selectList(null);
    }

    @Override
    public void updateUser(AccountUpdateBo updateBo) {
        String newNickname = updateBo.getNickname();
        String newUsername = updateBo.getUsername();
        String newPassword = updateBo.getPassword();
        String newEmail = updateBo.getEmail();
        String newStatus = updateBo.getStatus();
        LocalDateTime newExpireTime = updateBo.getExpireTime();

        if (StringUtils.isNotBlank(updateBo.getStaticIp())) {
            if (Tools.checkIpAddress(updateBo.getStaticIp())) {
                throw new ServiceException("静态IP地址无效，主机位不能用0,1或者255");
            }
            this.checkStaticIp(updateBo.getUsername(), updateBo.getStaticIp());
        }

        Account account = accountMapper.selectById(updateBo.getId());
        if (account == null) {
            throw new ServiceException("用户不存在");
        }
        String oldNickname = account.getNickname();
        String oldUsername = account.getUsername();
        String oldPassword = account.getPassword();
        String oldEmail = account.getEmail();
        String oldStatus = account.getStatus();
        LocalDateTime oldExpireTime = account.getExpireTime();
        String oldStaticIp = account.getStaticIp();

        //新老配置一样，则不更新
        if (updateBo.getNickname().equals(oldNickname) &&
                updateBo.getUsername().equals(oldUsername) &&
                updateBo.getPassword().equals(oldPassword) &&
                updateBo.getEmail().equals(oldEmail) &&
                updateBo.getStatus().equals(oldStatus) &&
                Objects.equals(updateBo.getExpireTime(), oldExpireTime) &&
                Strings.CS.equals(updateBo.getStaticIp(), oldStaticIp)
        ) {
            return;
        }
        LambdaUpdateWrapper<Account> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Account::getId, updateBo.getId());

        updateWrapper.set(Account::getNickname, newNickname);
        updateWrapper.set(Account::getUsername, newUsername);
        //密码不传则表示不更新密码
        if (StringUtils.isNotBlank(updateBo.getPassword())) {
            updateWrapper.set(Account::getPassword, newPassword);
        }
        updateWrapper.set(Account::getEmail, newEmail);
        updateWrapper.set(Account::getStatus, newStatus);
        updateWrapper.set(Account::getExpireTime, newExpireTime);
        if (!Strings.CS.equals(oldStatus, newStatus)) {
            updateWrapper.set(Account::getStatus, newStatus);
        }
        //数据库操作提前,可回滚，确保数据一致
        accountMapper.update(updateWrapper);
    }

    @Override
    public void checkStaticIp(String username, String staticIp) {
        //查询静态IP是否被占用
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getStaticIp, staticIp);
        Account account = accountMapper.selectOne(queryWrapper, false);
        if (account != null && !Strings.CS.equals(username, account.getUsername())) {
            throw new ServiceException("静态IP地址被用户 {} 占用", account.getUsername());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(AccountUpdateBo addBo) throws IOException {
        //校验用户
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getUsername, addBo.getUsername());
        queryWrapper.or();
        queryWrapper.eq(Account::getNickname, addBo.getNickname());
        List<Account> list = accountMapper.selectList(queryWrapper);
        if (!list.isEmpty()) {
            throw new ServiceException("用户名或昵称已存在");
        }
        Account account = new Account();
        account.setNickname(addBo.getNickname());
        account.setUsername(addBo.getUsername());
        account.setPassword(addBo.getPassword());
        account.setEmail(addBo.getEmail());
        account.setStatus(addBo.getStatus());
        account.setExpireTime(addBo.getExpireTime());
        if (StringUtils.isNotBlank(addBo.getStaticIp())) {
            if (Tools.checkIpAddress(addBo.getStaticIp())) {
                throw new ServiceException("静态IP地址无效，主机位不能用1或者255");
            }
            account.setStaticIp(addBo.getStaticIp());
        }
        //为新用户创建证书
        VpnSystemService vpnSystemService = SpringUtils.getBean(VpnSystemService.class);
        ClientProperties userCer = vpnSystemService.createUserCer(addBo.getUsername());
        account.setClientCrt(userCer.getClientCert());
        account.setClientKey(userCer.getClientKey());
        accountMapper.insert(account);

        //新用户创建CCD
        CcdService ccdService = SpringUtils.getBean(CcdService.class);
        CcdBo ccdBo = new CcdBo();
        ccdBo.setUsername(addBo.getUsername());
        ccdBo.setStaticIp(addBo.getStaticIp());
        ccdService.addCcd(ccdBo);
    }

    public void reCreateUserCer(AccountUpdateBo addBo) throws IOException {
        Account acc = accountMapper.selectById(addBo.getId());
        if (acc == null) {
            throw new ServiceException("用户名或昵称不存在");
        }
        VpnSystemService vpnSystemService = SpringUtils.getBean(VpnSystemService.class);
        ClientProperties userCer = vpnSystemService.createUserCer(acc.getUsername());
        Account account = new Account();
        account.setId(addBo.getId());
        account.setClientCrt(userCer.getClientCert());
        account.setClientKey(userCer.getClientKey());
        accountMapper.updateById(account);
    }

    @Override
    public void download(DownLoadConfigBo bo) {
        Account account = accountMapper.selectById(bo.getId());
        if (account == null) {
            throw new ServiceException("用户不存在");
        }
        String result = Tools.getClientOvpn(account);
        HttpServletResponse response = ServletUtils.getResponse();
        if (response != null) {
            try {
                FileUtils.setAttachmentResponseHeader(response, account.getNickname() + ".ovpn");
                response.setContentType("application/octet-stream");
                response.getOutputStream().write(result.getBytes());
            } catch (IOException e) {
                throw new ServiceException("下载配置文件失败");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("请选择要删除的用户");
        }
        List<Account> accounts = accountMapper.selectByIds(Arrays.asList(ids));
        List<String> removeNames = accounts.stream().map(Account::getUsername).toList();
        //数据库删除
        accountMapper.deleteByIds(Arrays.asList(ids));
        // 删除CCD
        CcdService ccdService = SpringUtils.getBean(CcdService.class);
        ccdService.removeCcds(removeNames);
    }

    @Override
    public void offline(Long[] ids) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Account::getId, Arrays.asList(ids));
        wrapper.eq(Account::getOnline, "1");
        List<Account> accounts = accountMapper.selectList(wrapper);
        Set<String> set = accounts.stream().map(Account::getUsername).collect(Collectors.toSet());
        if (set.isEmpty()) {
            return;
        }
        SpringUtils.context().publishEvent(new OfflineUserEvent(set, true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(DisableUserBo disableUserBo) {
        Long[] ids = disableUserBo.getIds();
        if (disableUserBo.getDisable()) {
            List<Account> accounts = accountMapper.selectByIds(Arrays.asList(ids));
            Set<String> set = accounts.stream().map(Account::getUsername).collect(Collectors.toSet());
            if (set.isEmpty()) {
                return;
            }
            LambdaUpdateWrapper<Account> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(Account::getId, Arrays.asList(ids));
            updateWrapper.set(Account::getStatus, "0");

            accountMapper.update(updateWrapper);
            SpringUtils.context().publishEvent(new OfflineUserEvent(set, true));
        } else {
            LambdaUpdateWrapper<Account> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(Account::getId, Arrays.asList(ids));
            updateWrapper.set(Account::getStatus, "1");
            accountMapper.update(updateWrapper);
        }

    }

    @Override
    public AccountVO copyUserPasswd(Long id) {
        if (id == null) {
            throw new ServiceException("用户ID不能为空");
        }
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new ServiceException("用户不存在");
        }
        AccountVO accountVO = new AccountVO();
        accountVO.setUsername(account.getUsername());
        accountVO.setPassword(account.getPassword());
        return accountVO;
    }

    @Override
    public void sendEmail(Long[] ids) {
        if (!Strings.CI.equals(CacheUtils.get(SystemConfigEnum.smtp_enable), "1")){
            throw new ServiceException("SMTP未启用");
        }
        String serverIp = CacheUtils.get(SystemConfigEnum.server_ip);
        String downLoadUrl = "http://" + serverIp + ":" + environment.getProperty("server.port") + "/api/client/download";

        List<Account> accounts = accountMapper.selectByIds(Arrays.asList(ids));
        for (Account account : accounts) {
            String formatStatus = "1".equals(account.getStatus()) ? "启用" : "禁用";
            String ovpn = Tools.getClientOvpn(account);
            String content = """
                    OpenVPN Web-GUI 提示：<br/>
                    尊敬的 [ %s ] 您好？<br/>
                    管理员已修改了您的VPN用户。<br/>
                    您的新VPN账号是：<b style='color: red'>%s</b><br/>
                    密码是：<b style='color: red'>%s</b><br/>
                    状态是：<b style='color: red'>%s</b><br/>
                    客户端下载地址：[ %s ]
                    附件内容为客户端配置文件，下载后请核对服务器IP地址和端口号，无误后导入VPN客户端即可访问。<br/>
                    请您妥善保管您的用户信息，谢谢！"
                    """.formatted(account.getNickname(), account.getUsername(), account.getPassword(), formatStatus, downLoadUrl);
            byte[] bytes = ovpn.getBytes(StandardCharsets.UTF_8);
            ByteArrayDataSource dataSource = new ByteArrayDataSource(bytes, "application/octet-stream");
            dataSource.setName(account.getNickname() + ".ovpn");
            MailUtils.sendHtml(
                    account.getEmail(),
                    "OpenVPN Web-GUI 提示",
                    content,
                    dataSource
            );
        }
    }

}


