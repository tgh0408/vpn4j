package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginErrorBo {
    @NotNull(message = "错误次数不能为空")
    private Integer errorCount;
    @NotNull(message = "锁定时间不能为空")
    private Long lockTime;

    @NotNull(message = "token过期时间不能为空")
    private Long tokenExpireTime;
    @NotNull(message = "token强制下线时间不能为空")
    private Long tokenForceLoginOut;
}