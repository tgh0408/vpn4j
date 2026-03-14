package org.ssl.vpn4j.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * VPN用户表
 * </p>
 *
 * @author TGH
 * @since 2026-01-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("l_account")
public class Account extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 使用人
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码
     */
    @TableField("`password`")
    private String password;

    /**
     * client crt
     */
    @TableField("client_crt")
    private String clientCrt;

    /**
     * client key
     */
    @TableField("client_key")
    private String clientKey;

    /**
     * 联系方式
     */
    @TableField("email")
    private String email;

    /**
     * 0不在线，1在线
     */
    @TableField("online")
    private String online;

    /**
     * 状态1启用，0禁用
     */
    @TableField("`status`")
    private String status;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 到期日期
     */
    @TableField(value = "expire_time", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime expireTime;

    /**
     * 静态IP
     */
    @TableField("static_ip")
    private String staticIp;

}
