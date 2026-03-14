package org.ssl.vpn4j.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PropertyPlaceholderHelper;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.common.core.utils.StringUtils;
import org.ssl.common.mybatis.core.page.PageQuery;
import org.ssl.common.mybatis.core.page.TableDataInfo;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.domain.Ccd;
import org.ssl.vpn4j.domain.bo.CcdBo;
import org.ssl.vpn4j.domain.vo.CcdVO;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.mapper.AccountMapper;
import org.ssl.vpn4j.mapper.CcdMapper;
import org.ssl.vpn4j.service.AccountService;
import org.ssl.vpn4j.service.CcdService;
import org.ssl.vpn4j.utils.Tools;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CcdServiceImpl implements CcdService {
    final CcdMapper ccdMapper;
    final AccountMapper accountMapper;

    private static final String ifconfig_push = "ifconfig-push";

    @Override
    public TableDataInfo<CcdVO> getCcd(CcdBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(bo.getId() != null, Account::getId, bo.getId());
        queryWrapper.like(StringUtils.isNotBlank(bo.getUsername()),Account::getUsername, bo.getUsername());
        queryWrapper.like(StringUtils.isNotBlank(bo.getNickname()),Account::getNickname, bo.getNickname());
        queryWrapper.eq(StringUtils.isNotBlank(bo.getStaticIp()),Account::getStaticIp, bo.getStaticIp());
        List<Account> accounts = accountMapper.selectList(queryWrapper);

        Map<String, Account> accountMap = accounts.stream().collect(Collectors.toMap(Account::getUsername, account -> account));

        LambdaQueryWrapper<Ccd> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Ccd::getUsername, accounts.stream().map(Account::getUsername).toList());
        Page<Ccd> ccdPage = ccdMapper.selectPage(pageQuery.build(), wrapper);
        IPage<CcdVO> ccdVOIPage = ccdPage.convert(ccd -> {
            Account account = Optional.ofNullable(accountMap.get(ccd.getUsername())).orElse(new Account());
            CcdVO vo = new CcdVO();
            vo.setId(account.getId());
            vo.setUsername(ccd.getUsername());
            vo.setCcdConfig(ccd.getCcdConfig());
            vo.setCreateTime(ccd.getCreateTime());
            vo.setUpdateTime(ccd.getUpdateTime());
            vo.setNickname(account.getNickname());
            vo.setStaticIp(account.getStaticIp());
            return vo;
        });
        return TableDataInfo.build(ccdVOIPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCcd(CcdBo bo) {
        String username = bo.getUsername();
        String config = CacheUtils.get(SystemConfigEnum.ccd_default_conf);
        LambdaQueryWrapper<Account> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Account::getUsername, username);
        Account account = accountMapper.selectOne(queryWrapper);
        if (account == null) {
            throw new ServiceException("用户不存在数据库中");
        }
        if (StringUtils.isNotBlank(bo.getStaticIp())){
            // 解析静态IP到配置文件
            Tools.checkIpAddress(bo.getStaticIp());
            String subnet_mask = CacheUtils.get(SystemConfigEnum.subnet_mask);
            PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
            config = propertyPlaceholderHelper.replacePlaceholders(config, placeholderName ->
                    switch (placeholderName) {
                        case "virtualIp" -> bo.getStaticIp();
                        case "staticSubnetMask" -> subnet_mask;
                        case "ifconfig-push" -> "ifconfig-push";
                        default -> null;
                    }
            );
        }
        Ccd ccd = new Ccd();
        ccd.setUsername(username);
        ccd.setCcdConfig(config);
        ccdMapper.insert(ccd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCcd(CcdBo bo) {
        String nickname = bo.getNickname();
        String username = bo.getUsername();
        String config = bo.getConfig();
        LambdaQueryWrapper<Ccd> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Ccd::getUsername, username);
        Ccd ccd = ccdMapper.selectOne(queryWrapper);
        String ccdConfig = ccd.getCcdConfig();
        if (Strings.CS.equals(ccdConfig, config)) {
            throw new ServiceException("用户 [ " + nickname + " ] 的CCD配置文件内容未修改，请勿重复提交！");
        }
        ccd.setCcdConfig(config);
        int i = ccdMapper.updateById(ccd);
        if (i > 0) {
            String msg = "更新用户 [ " + nickname + " ] 的CCD配置成功！";
        } else {
            throw new ServiceException("更新用户 [ " + nickname + " ] 的CCD配置失败！");
        }
        // 反向解析ip 静态ip
        String staticIp = null;
        String[] configList = config.split("\n");
        for (String configStr : configList) {

            if (configStr.contains(ifconfig_push) && configStr.trim().startsWith("#")){
                staticIp = null;
            }else if (configStr.startsWith(ifconfig_push)){
                String[] ip = configStr.split(" ");
                if (ip.length > 1) {
                    boolean checkIpAddress = Tools.checkIpAddress(ip[1]);
                    if (!checkIpAddress) {
                        staticIp = ip[1];
                    } else {
                        throw new ServiceException("用户 [ " + nickname + " ] 的CCD配置文件静态IP格式错误！{}", ip[1]);
                    }
                }
            }else if (configStr.contains(ifconfig_push) && !configStr.startsWith(ifconfig_push)){
                throw new ServiceException("用户 [ " + nickname + " ] 的CCD配置文件静态IP格式错误!, ifconfig-push行格式错误");
            }
        }
        AccountService accountService = SpringUtils.getBean(AccountService.class);
        accountService.checkStaticIp(username,staticIp);
        LambdaUpdateWrapper<Account> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Account::getStaticIp, staticIp);
        updateWrapper.eq(Account::getUsername, username);
        int update = accountMapper.update(null, updateWrapper);
        if (update > 0) {
            String msg = "更新用户 [ " + nickname + " ] 的静态IP成功！";
        } else {
            throw new ServiceException("更新用户 [ " + nickname + " ] 的静态IP失败！");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCcd(CcdBo bo) {
        Long id = bo.getId();
        Ccd ccd = ccdMapper.selectById(id);
        if (ccd == null) {
            throw new ServiceException("用户id " + id + " 不存在");
        }
        String vpnUsername = ccd.getUsername();
        int i = ccdMapper.deleteById(id);
        if (i > 0) {
            String msg = "删除用户 [ " + vpnUsername + " ] 的CCD配置成功！";
        } else {
            throw new ServiceException("删除用户 [ " + vpnUsername + " ] 的CCD配置失败！");
        }
    }

    @Override
    public void removeCcds(Long [] ids) {

    }

    @Override
    public void removeCcds(List<String> usernames) {
        usernames.forEach(username -> {
            LambdaQueryWrapper<Ccd> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Ccd::getUsername, username);
            ccdMapper.delete(queryWrapper);
        });
    }
}
