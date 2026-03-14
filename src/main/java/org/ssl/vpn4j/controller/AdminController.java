package org.ssl.vpn4j.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.ssl.common.core.domain.R;
import org.ssl.common.core.domain.model.LoginUser;
import org.ssl.common.core.utils.DateUtils;
import org.ssl.common.json.utils.JsonUtils;
import org.ssl.common.log.annotation.Log;
import org.ssl.common.log.enums.BusinessType;
import org.ssl.common.log.enums.OperatorType;
import org.ssl.common.satoken.utils.LoginHelper;
import org.ssl.common.sse.utils.SseMessageUtils;
import org.ssl.vpn4j.domain.Admin;
import org.ssl.vpn4j.domain.SseSendMsg;
import org.ssl.vpn4j.domain.bo.AdminBo;
import org.ssl.vpn4j.domain.bo.LoginBo;
import org.ssl.vpn4j.domain.vo.LoginVo;
import org.ssl.vpn4j.enums.SsePushTypeEnum;
import org.ssl.vpn4j.service.AdminService;
import org.ssl.vpn4j.service.IAuthStrategy;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class AdminController {
    final AdminService adminService;

    final ScheduledExecutorService scheduledExecutorService;

    @Log(title = "登录管理", businessType = BusinessType.LOGIN, operatorType = OperatorType.MANAGE, isSaveResponseData = false)
    @PostMapping("auth/login")
    @SaIgnore
    public R<LoginVo> login(@Validated @RequestBody LoginBo loginBody) {
        // 登录
        LoginVo loginVo = IAuthStrategy.login(loginBody, loginBody.getGrantType());
        scheduledExecutorService.schedule(() ->
        {
            String msg = DateUtils.getTodayHour(new Date()) + "好，欢迎登录 VPN 后台管理系统";
            SseMessageUtils.sendMessage(JsonUtils.toJsonString(new SseSendMsg(SsePushTypeEnum.SYSTEM_MSG, msg)));
        }, 5, TimeUnit.SECONDS);
        return R.ok(loginVo);
    }

    @GetMapping("user/info")
    public R<Admin> getUserInfo() {
        LoginUser user = LoginHelper.getLoginUser();
        if (user == null) {
            return R.fail("用户不存在或者未登陆");
        }
        Admin admin = adminService.getAdminInfo(user.getUserId());
        if (admin == null) {
            return R.fail("用户不存在");
        }
        return R.ok(admin);
    }

    @GetMapping("admin/list")
    public R<List<Admin>> list(@RequestParam(value = "keyword", required = false) String keyword) {
        return R.ok(adminService.getAdminList(keyword));
    }

    @PostMapping("admin/update")
    public R<String> update(@Validated @RequestBody AdminBo bo) {
        if (Strings.CI.equals(bo.getType(), "passwd")) {
            adminService.updatePasswd(bo);
        } else if (Strings.CI.equals(bo.getType(), "info")) {
            adminService.updateInfo(bo);
        } else {
            return R.fail("未知操作类型");
        }
        return R.ok();
    }
}
