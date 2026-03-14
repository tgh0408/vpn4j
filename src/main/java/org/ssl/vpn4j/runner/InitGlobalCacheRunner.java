package org.ssl.vpn4j.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.vpn4j.cache.constant.VpnConstant;
import org.ssl.vpn4j.domain.Black;
import org.ssl.vpn4j.domain.VpnService;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.mapper.BlackMapper;
import org.ssl.vpn4j.mapper.VpnServiceMapper;
import org.ssl.vpn4j.utils.Tools;

import java.util.List;

/**
 * 初始化全局缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitGlobalCacheRunner implements ApplicationRunner, Ordered {
    final VpnServiceMapper vpnServiceMapper;
    final BlackMapper blackMapper;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        //初始化全局缓存
        List<VpnService> vpnServices = vpnServiceMapper.selectList(new LambdaQueryWrapper<>());
        for (VpnService vpnService : vpnServices){
            CacheUtils.put(SystemConfigEnum.cacheName, vpnService.getKey1(), vpnService.getValue1());
        }
        log.info("Global cache initialization complete {}", vpnServices.stream().map(VpnService::getKey1).toList());
        //初始化VPN状态 (依赖全局缓存)
        CacheUtils.put(VpnConstant.VPN_CACHE_NAME, VpnConstant.VPN_STATUS, Tools.getVpnServerStatus());
        //初始化黑名单
        List<Black> blacks = blackMapper.selectList(new LambdaQueryWrapper<>());
        for (Black black : blacks){
            CacheUtils.put(VpnConstant.VPN_BLACK_NAME, black.getData1(), black);
            log.info("Black cache initialization complete {}", black.getData1());
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
