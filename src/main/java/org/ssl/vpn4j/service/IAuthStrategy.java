package org.ssl.vpn4j.service;

import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.vpn4j.domain.bo.LoginBo;
import org.ssl.vpn4j.domain.vo.LoginVo;

public interface IAuthStrategy {
    String BASE_NAME = "AuthStrategy";

    /**
     * 登录
     *
     * @param body      登录对象
     * @param grantType 授权类型
     * @return 登录验证信息
     */
    static LoginVo login(LoginBo body, String grantType) {
        // 授权类型和客户端id
        String beanName = grantType + BASE_NAME;
        if (!SpringUtils.containsBean(beanName)) {
            throw new ServiceException("授权类型不正确!");
        }
        IAuthStrategy instance = SpringUtils.getBean(beanName);
        return instance.login(body);
    }

    /**
     * 登录
     *
     * @param body   登录对象
     * @return 登录验证信息
     */
    LoginVo login(LoginBo body);
}
