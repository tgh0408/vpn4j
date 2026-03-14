package org.ssl.vpn4j.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.ssl.common.core.validate.AddGroup;
import org.ssl.common.core.validate.EditGroup;

import java.time.LocalDateTime;

@Data
public class AccountUpdateBo {
    @NotNull(groups = {EditGroup.class})
    private Long id;

    @NotBlank(groups = {AddGroup.class})
    private String email;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @NotBlank(groups = {AddGroup.class})
    private String nickname;

    @NotBlank(groups = {AddGroup.class})
    @Pattern(
            regexp = "^$|^.{6,20}$",  // 匹配空字符串或6-20位的任意字符
            message = "密码长度在6到20个字符之间",
            groups = {AddGroup.class, EditGroup.class}
    )
    private String password;

    private String staticIp;

    @NotBlank(groups = {AddGroup.class})
    @Pattern(regexp = "^[01]$", message = "状态值只能是0或1", groups = {AddGroup.class})
    private String status;

    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "用户名只能包含字母或数字", groups = {AddGroup.class, EditGroup.class})
    private String username;
}
