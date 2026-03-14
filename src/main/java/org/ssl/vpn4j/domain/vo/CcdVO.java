package org.ssl.vpn4j.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author TGH
 * @since 2026-01-08
 */
@Data
public class CcdVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 使用人
     */
    private String nickname;

    /**
     * vpn用户名
     */
    private String username;

    /**
     * ccd配置
     */
    private String ccdConfig;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;


    /**
     * 静态IP
     */
    private String staticIp;



}
