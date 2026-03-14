package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ssl.common.core.validate.EditGroup;

@Data
public class UpdateLoginBo {
    @NotBlank(message = "图形验证码启用不能为空", groups = {EditGroup.class})
    private String code;
    @NotNull(message = "登录失败次数不能为空", groups = {EditGroup.class})
    private Long count;
    @NotBlank(message = "登录锁定时长不能为空", groups = {EditGroup.class})
    private String lock;
}
