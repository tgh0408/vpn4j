package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.ssl.common.core.validate.AddGroup;
import org.ssl.common.core.validate.DelGroup;
import org.ssl.common.core.validate.EditGroup;

@Data
public class CcdBo {
    @NotBlank(message = "配置内容不能为空", groups = {EditGroup.class})
    private String config;

    @NotBlank(message = "昵称不能为空", groups = {EditGroup.class})
    private String nickname;

    @NotBlank(message = "用户名不能为空", groups = {AddGroup.class, EditGroup.class})
    private String username;

    @NotBlank(message = "ID不能为空", groups = {DelGroup.class})
    private Long id;

    private String staticIp;
}
