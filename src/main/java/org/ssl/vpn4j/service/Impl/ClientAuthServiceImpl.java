package org.ssl.vpn4j.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.vpn4j.cache.constant.VpnConstant;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.domain.Black;
import org.ssl.vpn4j.domain.Ccd;
import org.ssl.vpn4j.domain.bo.ClientAuthBo;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.event.BlackReleaseEvent;
import org.ssl.vpn4j.mapper.AccountMapper;
import org.ssl.vpn4j.mapper.CcdMapper;
import org.ssl.vpn4j.service.ClientAuthService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAuthServiceImpl implements ClientAuthService {
    final AccountMapper accountMapper;
    final CcdMapper ccdMapper;

    @Override
    public void authenticate(ClientAuthBo clientAuthBo, String serverName) {
        // 1. 记录日志 (注意：生产环境不要打印 request.getPassword())
        log.info("接收到认证请求 | 用户: {} | IP: {} | Server: {}",
                clientAuthBo.getUsername(), clientAuthBo.getClientIp(), serverName);

        // 2. 参数基本校验
        if (clientAuthBo.getUsername() == null || clientAuthBo.getPassword() == null) {
            throw new ServiceException("Username or password missing");
        }
        boolean forceStop = Boolean.TRUE.equals(CacheUtils.get(VpnConstant.VPN_CACHE_NAME, VpnConstant.VPN_FORCE_STOP));
        if (forceStop) {
            throw new ServiceException("VPN service is stopping");
        }
        try {
            validUserNameBlack(clientAuthBo,serverName);
            validIpBlack(clientAuthBo,serverName);
            // ---------------------------------------------------------
            // 比如查询 MySQL 数据库、连接 LDAP/AD 或 调用 Redis
            // ---------------------------------------------------------
            boolean isAuthenticated = checkUserCredentials(clientAuthBo.getUsername(), clientAuthBo.getPassword());

            if (isAuthenticated) {
                log.info("认证成功: {}", clientAuthBo.getUsername());
            } else {
                log.warn("认证失败: {} (密码错误)", clientAuthBo.getUsername());
                throw new ServiceException("fail to authenticate");
            }

        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public String generateCcdConfig(Long id) {
        Account account = accountMapper.selectById(id);
        if (account == null){
            throw new ServiceException("用户不存在");
        }
        return this.generateCcdConfig(account.getUsername());
    }

    private void validUserNameBlack(ClientAuthBo clientAuthBo, String serverName) {

        Black black = CacheUtils.get(VpnConstant.VPN_BLACK_NAME, clientAuthBo.getUsername());
        if (black == null) {
            return;
        }else if (black.getReleaseTime().isBefore(LocalDateTime.now())){
            // 解冻
            SpringUtils.context().publishEvent(new BlackReleaseEvent(black));
            return;
        }
        throw new ServiceException("用户名被临时冻结,{}", black.getNotes());
    }

    private void validIpBlack(ClientAuthBo clientAuthBo, String serverName) {

        Black black = CacheUtils.get(VpnConstant.VPN_BLACK_NAME, clientAuthBo.getClientIp());
        if (black == null) {
            return;
        }else if (black.getReleaseTime().isBefore(LocalDateTime.now())){
            // 解冻
            SpringUtils.context().publishEvent(new BlackReleaseEvent(black));
            return;
        }
        throw new ServiceException("IP被临时冻结,{}", black.getNotes());
    }

    private boolean checkUserCredentials(String username, String password) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Account::getUsername, username);
        wrapper.eq(Account::getPassword, password);
        wrapper.eq(Account::getStatus, "1");
        Account account = accountMapper.selectOne(wrapper);
        return account != null;
    }

    @Override
    public String generateCcdConfig(String username) {
        try {
            LambdaQueryWrapper<Ccd> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Ccd::getUsername, username);
            Ccd ccd = ccdMapper.selectOne(wrapper);
            if (ccd != null) {
                return ccd.getCcdConfig();
            }else {
                //为用户生成CCD配置文件
                String ccdDefaultConf = CacheUtils.get(SystemConfigEnum.ccd_default_conf);
                Ccd newCcd = new Ccd();
                newCcd.setUsername(username);
                newCcd.setCcdConfig(ccdDefaultConf);
                int insert = ccdMapper.insert(newCcd);
                // === 场景 4：普通用户 ===
                // 返回 null 或 空字符串，OpenVPN 会自动从 ipp.txt 或地址池里分配动态 IP
                if (insert > 0){
                    return newCcd.getCcdConfig();
                }else {
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("VPN 获取用户 {} CCD 出错 : {}", username, e.getMessage());
            return null;
        }
    }
}
