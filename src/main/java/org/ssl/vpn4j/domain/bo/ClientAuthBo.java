package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ClientAuthBo {
    /**
     * 用户名
     */
    @NotEmpty(message = "用户名不能为空")
    private String username;
    /**
     * 密码
     */
    @NotEmpty(message = "密码不能为空")
    private String password;
    /**
     *common
     */
    @NotEmpty(message = "commonName不能为空")
    private String commonName;
    /**
     * 时间戳
     */
    @NotEmpty(message = "时间戳不能为空")
    private String timestamp;

    /**
     * 客户端ip
     */
    @NotEmpty(message = "客户端IP不能为空")
    private String clientIp;
}
