package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminBo {
    @NotNull(message = "类型不能为空")
    private String type;
    @NotNull(message = "ID不能为空")
    private Long id;

    private String newPassword;
    private String confirmPassword;

    private String email;
    private String phone;
    private String address;
    private String description;
    private String username;
    private String nickname;
    private String sex;
}
