package org.ssl.vpn4j.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.domain.R;
import org.ssl.common.log.annotation.Log;
import org.ssl.common.log.enums.BusinessType;
import org.ssl.common.log.enums.OperatorType;
import org.ssl.vpn4j.domain.SystemClientInfo;
import org.ssl.vpn4j.domain.bo.VpnServiceBo;
import org.ssl.vpn4j.domain.vo.VpnServiceVO;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.service.VpnSystemService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequiredArgsConstructor
public class VpnSystemController {
    final VpnSystemService vpnSystemService;
    private static final ReentrantLock lock = new ReentrantLock();
    /**
     * 获取系统信息
     * @return R<SystemClientInfo>
     */
    @GetMapping("system/info")
    public R<SystemClientInfo> systemInfo(){
        return R.ok(vpnSystemService.getServiceInfo());
    }

    /**
     * 获得Vpn配置文件
     */
    @GetMapping("system/getVpnConfig")
    public R<String> getVpnConfig() {
        String config = vpnSystemService.getVpnConfig();
        return R.ok(null, config);
    }

    /**
     * 更新Vpn配置文件
     */
    @Log(title = "更新Vpn配置文件", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE)
    @PutMapping("system/updateVpnConfig")
    public R<Void> updateVpnConfig(@RequestBody Map<String, String> config) {
        vpnSystemService.updateVpnConfig(config.get("config"));
        return R.ok();
    }

    /**
     * 重新创建服务器证书
     * @param bo 创建证书参数
     * @return R<Void>
     * @throws IOException 创建证书失败
     */
    @Log(title = "创建服务器证书", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE)
    @PostMapping("system/createServerCer")
    public R<Void> createServerCer(@Validated @RequestBody VpnServiceBo bo) throws IOException {
        if (lock.tryLock()) {
            try {
                vpnSystemService.createServerCer(bo);
                return R.ok();
            } finally {
                lock.unlock();  // 只有在获取锁成功时才释放
            }
        } else {
            return R.fail("正在创建证书，请稍后再试");
        }
    }

    /**
     * 发送测试邮件
     */
    @GetMapping("system/sendTestEmail")
    public R<Void> sendTestEmail() {
        vpnSystemService.sendTestEmail();
        return R.ok();
    }

    /**
     * 获取所有配置信息
     * @return R<List<VpnService>>
     */
    @GetMapping("system/getAllConfig")
    private R<List<VpnServiceVO>> getAllConfig(){
        Map<String, String> all = CacheUtils.getAll(SystemConfigEnum.cacheName);

        //过滤密码等关键信息
        List<SystemConfigEnum> filter = Arrays.asList(
                SystemConfigEnum.encrypted_passwords,
                SystemConfigEnum.root_passwd,
                SystemConfigEnum.dh_pem,
                SystemConfigEnum.ca_key,
                SystemConfigEnum.smtp_password
        );

        List<VpnServiceVO> vpnServices = all.entrySet()
                .stream()
                .map(entry ->
                {
                    // 过滤密码等关键信息
                    if (filter.contains(SystemConfigEnum.fromKey(entry.getKey(), false))){
                        return new VpnServiceVO(entry.getKey(), "******", SystemConfigEnum.getDescByKey(entry.getKey()));
                    }
                    return new VpnServiceVO(entry.getKey(), entry.getValue(), SystemConfigEnum.getDescByKey(entry.getKey()));
                }
        ).toList();
        return R.ok(vpnServices);
    }

    /**
     * 获得指定配置信息
     */
    @GetMapping("system/getConfigs")
    public R<List<VpnServiceVO>> getConfigs(@NotNull(message = "参数不能为空") String [] keys) {
        return R.ok(vpnSystemService.getConfigs(keys));
    }

    /**
     * 修改指定配置信息
     */
    @Log(title = "修改指定配置信息", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE)
    @PostMapping("system/updateConfigs")
    public R<Void> updateConfig(@Validated @RequestBody VpnServiceBo bo) {
        vpnSystemService.updateConfigs(bo);
        return R.ok();
    }
}
