package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class ServiceCerBo {
    @NotBlank(message = "ca名称不能为空")
    private String caName;
    @NotBlank(message = "服务名称不能为空")
    private String serverName;
    @NotBlank(message = "key不能为空")
    @Pattern(regexp = "^[a-z0-9A-Z]+$", message = "key只能包含字母或数字")
    private String serverKey;
    @NotBlank(message = "邮箱不能为空")
    private String email;
    private String createTime;
    private String expireTime;
}
