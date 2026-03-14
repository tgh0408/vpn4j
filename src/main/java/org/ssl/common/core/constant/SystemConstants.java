package org.ssl.common.core.constant;

/**
 * 系统常量信息
 *
  
 */
public interface SystemConstants {

    /**
     * 正常状态
     */
    String NORMAL = "0";

    /**
     * 异常状态
     */
    String DISABLE = "1";

    /**
     * 是否为系统默认（是）
     */
    String YES = "Y";

    /**
     * 是否为系统默认（否）
     */
    String NO = "N";

    /**
     * 超级管理员ID
     */
    Long SUPER_ADMIN_ID = 1L;


    /**
     * 排除敏感属性字段
     */
    String[] EXCLUDE_PROPERTIES = { "clientid","password", "oldPassword", "newPassword", "confirmPassword" };


}
