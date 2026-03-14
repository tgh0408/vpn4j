package org.ssl.common.event.listener;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.ssl.common.core.utils.ServletUtils;
import org.ssl.common.event.LoginInfoEvent;
import org.ssl.common.satoken.utils.LoginHelper;

@Slf4j
@Component
public class LogListener {
    @Async
    @EventListener
    public void recordLogininfor(LoginInfoEvent logininforEvent) {
        HttpServletRequest request = logininforEvent.getRequest();
        final UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        final String ip = ServletUtils.getClientIP(request);
        // 客户端信息
        String clientId = request.getHeader(LoginHelper.CLIENT_KEY);

        StringBuilder s = new StringBuilder();
        s.append(getBlock(logininforEvent.getUsername()));
        s.append(getBlock(logininforEvent.getStatus()));
        s.append(getBlock(logininforEvent.getMessage()));
        // 打印信息到日志
        log.info(s.toString(), logininforEvent.getArgs());
        // 获取客户端操作系统
        String os = userAgent.getOs().getName();
        // 获取客户端浏览器
        String browser = userAgent.getBrowser().getName();
        // 封装对象
//        SysLogininforBo logininfor = new SysLogininforBo();
//        logininfor.setTenantId(logininforEvent.getTenantId());
//        logininfor.setUserName(logininforEvent.getUsername());
//        if (ObjectUtil.isNotNull(client)) {
//            logininfor.setClientKey(client.getClientKey());
//            logininfor.setDeviceType(client.getDeviceType());
//        }
//        logininfor.setIpaddr(ip);
//        logininfor.setLoginLocation(address);
//        logininfor.setBrowser(browser);
//        logininfor.setOs(os);
//        logininfor.setMsg(logininforEvent.getMessage());
//        // 日志状态
//        if (StringUtils.equalsAny(logininforEvent.getStatus(), Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER)) {
//            logininfor.setStatus(Constants.SUCCESS);
//        } else if (Constants.LOGIN_FAIL.equals(logininforEvent.getStatus())) {
//            logininfor.setStatus(Constants.FAIL);
//        }
//        // 插入数据
//        insertLogininfor(logininfor);
    }

    private String getBlock(Object msg) {
        if (msg == null) {
            msg = "";
        }
        return "[" + msg + "]";
    }
}
