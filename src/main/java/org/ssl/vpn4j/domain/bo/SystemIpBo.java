package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemIpBo {
    @NotBlank(message = "系统IP不能为空")
    private String systemIp;
    @NotBlank(message = "系统端口不能为空")
    private String systemPort;
    @NotBlank(message = "系统协议不能为空")
    private String systemProtocol;
    @NotBlank(message = "系统名称不能为空")
    private String systemName;
}
