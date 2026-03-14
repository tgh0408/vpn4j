package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SmtpBo {
    @NotEmpty(message = "更新类型不能为空")
    private String type;
    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;
    @NotEmpty(message = "SMTP服务器不能为空")
    private String server;
    @NotEmpty(message = "SMTP用户名不能为空")
    private String user;
    @NotEmpty(message = "SMTP密码不能为空")
    private String password;
    @NotNull(message = "SMTP端口不能为空")
    private Integer port;
    @NotEmpty(message = "邮件标题不能为空")
    private String title;
    private String downLoadAddress;
}
