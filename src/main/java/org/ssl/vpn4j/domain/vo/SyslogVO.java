package org.ssl.vpn4j.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * <p>
 * 系统日志表
 * </p>
 *
 * @author TGH
 * @since 2026-01-08
 */
@Data
public class SyslogVO {

    /**
     * ID
     */
    private Long operId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private String [] operTime;

    private String title;

    private Integer businessType;

}
