package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ssl.common.core.validate.QueryGroup;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownLoadConfigBo {
    @NotNull(message = "id不能为空", groups = {QueryGroup.class})
    private Long id;
}
