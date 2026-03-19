package org.ssl.common.satoken.Interceptors.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 排除路径
     */
    private String[] excludes = new String[0];

    /**
     * 本机指定路径放行
     */
    private String[] localExcludes = new String[0];

}
