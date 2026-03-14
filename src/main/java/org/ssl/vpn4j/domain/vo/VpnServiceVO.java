package org.ssl.vpn4j.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VpnServiceVO {
    private String key1;
    private String value1;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public VpnServiceVO(String key1, String value1, String description) {
        this.key1 = key1;
        this.value1 = value1;
        this.description = description;
    }
    public VpnServiceVO(String key1, String value1, String description, LocalDateTime updateTime) {
        this.key1 = key1;
        this.value1 = value1;
        this.description = description;
        this.updateTime = updateTime;
    }
}
