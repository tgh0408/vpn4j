package org.ssl.vpn4j.runner;

import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.mail.config.properties.MailProperties;
import org.ssl.common.mail.utils.MailUtils;
import org.ssl.vpn4j.enums.SystemConfigEnum;

/**
 * JavaMail 配置
 */
@Component
public class MailRunner implements ApplicationRunner, Ordered {
    /**
     * smtp:
     *   enabled: false
     *   host: smtp.163.com
     *   port: 465
     *   # 是否需要用户名密码验证
     *   auth: true
     *   # 发送方，遵循RFC-822标准
     *   from: xxx@163.com
     *   # 用户名（注意：如果使用foxmail邮箱，此处user为qq号）
     *   user: xxx@163.com
     *   # 密码（注意，某些邮箱需要为SMTP服务单独设置密码，详情查看相关帮助）
     *   pass: xxxxxxxxxx
     *   # 使用 STARTTLS安全连接，STARTTLS是对纯文本通信协议的扩展。
     *   starttlsEnable: true
     *   # 使用SSL安全连接
     *   sslEnable: true
     *   # SMTP超时时长，单位毫秒，缺省值不超时
     *   timeout: 0
     *   # Socket连接超时值，单位毫秒，缺省值不超时
     *   connectionTimeout: 0
     */


    @Override
    public void run(@NonNull ApplicationArguments args) {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setHost(CacheUtils.get(SystemConfigEnum.smtp_host));
        mailProperties.setPort(Integer.parseInt(CacheUtils.get(SystemConfigEnum.smtp_port)));
        mailProperties.setAuth(Strings.CI.equals(CacheUtils.get(SystemConfigEnum.smtp_auth), "1"));
        mailProperties.setFrom(CacheUtils.get(SystemConfigEnum.smtp_from));
        mailProperties.setUser(CacheUtils.get(SystemConfigEnum.smtp_user));
        mailProperties.setPassword(CacheUtils.get(SystemConfigEnum.smtp_password));
        mailProperties.setStarttlsEnable(Strings.CI.equals(CacheUtils.get(SystemConfigEnum.smtp_starttls_enable), "1"));
        mailProperties.setSslEnable(Strings.CI.equals(CacheUtils.get(SystemConfigEnum.smtp_ssl_enable), "1"));
        mailProperties.setTimeout(Long.parseLong(CacheUtils.get(SystemConfigEnum.smtp_timeout)));
        mailProperties.setConnectionTimeout(Long.parseLong(CacheUtils.get(SystemConfigEnum.smtp_connection_timeout)));

        MailUtils.setProperties(mailProperties);
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
