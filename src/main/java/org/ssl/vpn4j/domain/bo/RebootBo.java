package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ssl.common.core.validate.EditGroup;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RebootBo {
    @NotBlank(message = "用户名不能为空", groups = {EditGroup.class})
    private String username;
}
