package org.ssl.vpn4j.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VpnServiceBo {
    @Valid
    @NotNull(message = "配置列表不能为空")
    private List<ServiceItem> items;

    @Data
    public static class ServiceItem{
        @NotBlank(message = "key不能为空")
        private String key1;
        private String value1;
        private String description;
    }
}
