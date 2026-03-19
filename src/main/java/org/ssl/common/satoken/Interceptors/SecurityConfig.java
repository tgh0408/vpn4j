package org.ssl.common.satoken.Interceptors;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.dev33.satoken.util.SaTokenConsts;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.ssl.common.core.constant.HttpStatus;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.ServletUtils;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.common.satoken.Interceptors.properties.SecurityProperties;
import org.ssl.common.satoken.handler.AllUrlHandler;
import org.ssl.common.satoken.utils.LoginHelper;

@Slf4j
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {
    final SecurityProperties securityProperties;
    final String[] localIps = new String[]{"127.0.0.1", "0:0:0:0:0:0:0:1"};

    @Value("${sse.path}")
    private String ssePath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    AllUrlHandler allUrlHandler = SpringUtils.getBean(AllUrlHandler.class);
                    // --- 1. 本机特权路径处理 ---
                    SaRouter.match(securityProperties.getLocalExcludes())
                            .check(() -> {
                                if (ArrayUtils.contains(localIps, ServletUtils.getClientIP())) {
                                    // 如果是本机访问特权路径，直接停止后续匹配（即：跳过登录校验，实现放行）
                                    SaRouter.stop();
                                } else {
                                    // 如果不是本机访问特权路径，直接拒绝
                                    throw new ServiceException("该路径仅限本机访问");
                                }
                            });
                    // --- 2. 常规登录验证 ---
                    SaRouter.match(allUrlHandler.getUrls())
                            .check(() -> {
                                // 使用 SaHolder 获取参数，更符合 Sa-Token 规范
                                SaRequest request = SaHolder.getRequest();

                                // 检查是否登录
                                StpUtil.checkLogin();

                                // 检查 ClientID 是否一致
                                String headerCid = request.getHeader(LoginHelper.CLIENT_KEY);
                                String paramCid = request.getParam(LoginHelper.CLIENT_KEY);

                                if (!Strings.CS.equalsAny(headerCid, paramCid)) {
                                    throw NotLoginException.newInstance(StpUtil.getLoginType(),
                                            "-100", "客户端ID与Token不匹配",
                                            StpUtil.getTokenValue());
                                }
                            });
                })).addPathPatterns("/**")
                .excludePathPatterns(securityProperties.getExcludes())
                .excludePathPatterns(ssePath);
    }

    /**
     * 对 actuator 健康检查接口 做账号密码鉴权
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        String username = SpringUtils.getProperty("spring.boot.admin.client.username");
        String password = SpringUtils.getProperty("spring.boot.admin.client.password");
        return new SaServletFilter()
                .addInclude("/actuator", "/actuator/**")
                .setAuth(obj -> {
                    SaHttpBasicUtil.check(username + ":" + password);
                })
                .setError(e -> {
                    HttpServletResponse response = ServletUtils.getResponse();
                    response.setContentType(SaTokenConsts.CONTENT_TYPE_APPLICATION_JSON);
                    return SaResult.error(e.getMessage()).setCode(HttpStatus.UNAUTHORIZED);
                });
    }
}
