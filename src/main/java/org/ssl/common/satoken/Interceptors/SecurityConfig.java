package org.ssl.common.satoken.Interceptors;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.dev33.satoken.util.SaTokenConsts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.ssl.common.core.constant.HttpStatus;
import org.ssl.common.core.utils.ServletUtils;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.common.satoken.Interceptors.properties.SecurityProperties;
import org.ssl.common.satoken.handler.AllUrlHandler;
import org.ssl.common.satoken.utils.LoginHelper;

import java.util.Objects;

@Slf4j
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {
    final SecurityProperties securityProperties;

    @Value("${sse.path}")
    private String ssePath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册路由拦截器，自定义验证规则
        registry.addInterceptor(new SaInterceptor(handler -> {
                    AllUrlHandler allUrlHandler = SpringUtils.getBean(AllUrlHandler.class);
                    // 登录验证 -- 排除多个路径
                    SaRouter
                            // 获取所有的
                            .match(allUrlHandler.getUrls())
                            // 对未排除的路径进行检查
                            .check(() -> {
                                HttpServletRequest request = Objects.requireNonNull(ServletUtils.getRequest());
                                HttpServletResponse response = Objects.requireNonNull(ServletUtils.getResponse());
                                response.setContentType(SaTokenConsts.CONTENT_TYPE_APPLICATION_JSON);
                                // 检查是否登录 是否有token
                                StpUtil.checkLogin();

                                // 检查 header 与 param 里的 clientid 与 token 里的是否一致
                                String headerCid = request.getHeader(LoginHelper.CLIENT_KEY);
                                String paramCid = ServletUtils.getParameter(LoginHelper.CLIENT_KEY);
                                if (!Strings.CS.equalsAny(headerCid, paramCid)) {
                                    // token 无效
                                    throw NotLoginException.newInstance(StpUtil.getLoginType(),
                                            "-100", "客户端ID与Token不匹配",
                                            StpUtil.getTokenValue());
                                }

                                // 有效率影响 用于临时测试
                                // if (log.isDebugEnabled()) {
                                //     log.info("剩余有效时间: {}", StpUtil.getTokenTimeout());
                                //     log.info("临时有效时间: {}", StpUtil.getTokenActivityTimeout());
                                // }

                            });
                })).addPathPatterns("/**")
                // 排除不需要拦截的路径
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
