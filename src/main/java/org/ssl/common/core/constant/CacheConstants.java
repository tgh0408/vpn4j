package org.ssl.common.core.constant;

/**
 * 缓存的key 常量
 *
  
 */
public interface CacheConstants {
    /**
     * 总用户
     */
    String TOTAL_USER_KEY = "total_user";

    /**
     * 禁用用户
     */
    String TOTAL_DISABLE_USER_KEY = "total_disable_user";

    /**
     * 在线用户 redis key
     */
    String ONLINE_TOKEN_KEY = "online_tokens:";

    /**
     * 参数管理 cache key
     */
    String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    String SYS_DICT_KEY = "sys_dict:";

    /**
     * 登录账户密码错误次数 key
     */
    String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 账号已被封禁用户
     */
    String PWD_FROZEN_TIME_KEY = "pwd_frozen_time:";

}
