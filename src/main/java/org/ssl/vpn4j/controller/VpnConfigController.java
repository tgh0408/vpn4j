package org.ssl.vpn4j.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.ssl.common.core.domain.R;
import org.ssl.common.core.validate.EditGroup;
import org.ssl.common.log.annotation.Log;
import org.ssl.common.log.enums.BusinessType;
import org.ssl.common.log.enums.OperatorType;
import org.ssl.common.ratelimiter.annotation.RateLimiter;
import org.ssl.common.ratelimiter.enums.LimitType;
import org.ssl.vpn4j.domain.bo.RebootBo;
import org.ssl.vpn4j.domain.vo.VpnConfigVO;
import org.ssl.vpn4j.service.VpnConfigService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class VpnConfigController {
    final VpnConfigService vpnConfigService;

    @GetMapping("config/list")
    public R<VpnConfigVO> list() throws IOException {
        return R.ok(vpnConfigService.getConfigList());
    }

    @Log(title = "VPN重启", businessType = BusinessType.RESTART, operatorType = OperatorType.MANAGE)
    @RateLimiter(key = "VPN", shareKey = true, time = 6, count = 1, limitType = LimitType.DEFAULT, message = "VPN重启操作过于频繁，请5秒后再试")
    @PostMapping("config/reboot")
    public R<String> reboot(@Validated(EditGroup.class) @RequestBody RebootBo bo) {
        return R.ok(vpnConfigService.reboot(bo));
    }

    @Log(title = "VPN启动", businessType = BusinessType.START, operatorType = OperatorType.MANAGE)
    @RateLimiter(key = "VPN", shareKey = true, time = 6, count = 1, limitType = LimitType.DEFAULT, message = "VPN启动操作过于频繁，请5秒稍后再试")
    @GetMapping("config/start")
    public R<Void> start() {
        vpnConfigService.start();
        return R.ok();
    }

    @Log(title = "VPN停止", businessType = BusinessType.STOP, operatorType = OperatorType.MANAGE)
    @RateLimiter(key = "VPN", shareKey = true, time = 6, count = 1, limitType = LimitType.DEFAULT, message = "VPN停止操作过于频繁，请5秒稍后再试")
    @GetMapping("config/stop")
    public R<Void> stop() {
        vpnConfigService.stop();
        return R.ok();
    }
}
