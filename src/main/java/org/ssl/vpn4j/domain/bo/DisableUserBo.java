package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DisableUserBo {
    @NotNull(message = "用户ID不能为空")
    private Long[] ids;
    @NotNull(message = "禁用状态不能为空")
    private Boolean disable;
}
