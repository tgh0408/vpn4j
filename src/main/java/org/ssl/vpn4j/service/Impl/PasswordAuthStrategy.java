package org.ssl.vpn4j.service.Impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.constant.CacheConstants;
import org.ssl.common.core.constant.Constants;
import org.ssl.common.core.constant.SystemConstants;
import org.ssl.common.core.domain.model.LoginUser;
import org.ssl.common.core.exception.user.UserException;
import org.ssl.common.core.utils.MessageUtils;
import org.ssl.common.core.utils.ServletUtils;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.common.enums.LoginType;
import org.ssl.common.event.LoginInfoEvent;
import org.ssl.common.satoken.utils.LoginHelper;
import org.ssl.vpn4j.domain.Admin;
import org.ssl.vpn4j.domain.bo.LoginBo;
import org.ssl.vpn4j.domain.vo.LoginVo;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.mapper.AdminMapper;
import org.ssl.vpn4j.service.IAuthStrategy;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 密码认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("password" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class PasswordAuthStrategy implements IAuthStrategy {
    final AdminMapper adminMapper;

    @Override
    public LoginVo login(LoginBo loginBody) {
        String username = loginBody.getUsername();
        String password = loginBody.getPassword();
        String code = loginBody.getCode();
        String uuid = loginBody.getUuid();

        Admin user = loadUserByUsername(username);

//        this.checkLogin(LoginType.PASSWORD, username, () -> !BCrypt.checkpw(password, user.getPassword()));
        this.checkLogin(LoginType.PASSWORD, username, () -> !Strings.CS.equals(password, user.getPassword()));
        // 此处可根据登录用户的数据不同 自行创建 loginUser
        LoginUser loginUser = this.buildLoginUser(user);

        SaLoginParameter model = new SaLoginParameter();
        String tokenTimeOut = CacheUtils.get(SystemConfigEnum.cacheName, SystemConfigEnum.token_timeout.getKey());
        if (tokenTimeOut != null){
            model.setTimeout(Long.parseLong(tokenTimeOut));
        }
        String tokenActiveTimeout = CacheUtils.get(SystemConfigEnum.cacheName, SystemConfigEnum.token_active_timeout.getKey());
        if (tokenActiveTimeout != null){
            model.setActiveTimeout(Long.parseLong(tokenActiveTimeout));
        }
        // 生成token
        LoginHelper.login(loginUser, model);
        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setEmail(loginUser.getEmail());
        return loginVo;
    }

    private LoginUser buildLoginUser(Admin user) {
        LoginUser loginUser = new LoginUser();
        Long userId = user.getId();
        loginUser.setUserId(userId);
        loginUser.setUserType(user.getUserType());
        loginUser.setUsername(user.getAccount());
        loginUser.setNickname(user.getNickname());
        loginUser.setEmail(user.getEmail());
        loginUser.setPhone(user.getPhone());
        loginUser.setAddress(user.getAddress());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setSex(user.getSex());
        loginUser.setDescription(user.getDescription());
        return loginUser;
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    private void validateCaptcha(String tenantId, String username, String code, String uuid) {
//        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtils.blankToDefault(uuid, "");
//        String captcha = RedisUtils.getCacheObject(verifyKey);
//        RedisUtils.deleteObject(verifyKey);
//        if (captcha == null) {
//            recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
//            throw new CaptchaExpireException();
//        }
//        if (!StringUtils.equalsIgnoreCase(code, captcha)) {
//            recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"));
//            throw new CaptchaException();
//        }
    }

    private Admin loadUserByUsername(String username) {
        Admin user = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getAccount, username));
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", username);
            throw new UserException("user.not.exists", username);
        } else if (SystemConstants.DISABLE.equals(user.getDelFlag())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new UserException("user.blocked", username);
        }
        return user;
    }

    /**
     * 登录校验
     */
    public void checkLogin(LoginType loginType, String username,Supplier<Boolean> supplier) {
        //查询封禁时长
        Integer lockTime = Integer.parseInt((String) Optional.ofNullable(CacheUtils.get(SystemConfigEnum.cacheName, SystemConfigEnum.lock_time.getKey())).orElse("0"));

        // 获取用户登录错误次数，默认为0 (可自定义限制策略 例如: key + username + ip)
        Integer errorNumber = Optional.ofNullable((Integer) CacheUtils.get(CacheConstants.PWD_ERR_CNT_KEY, username)).orElse(0);
        Integer maxRetryCount = Integer.parseInt(Optional.ofNullable((String)CacheUtils.get(SystemConfigEnum.cacheName, SystemConfigEnum.max_retry_count.getKey())).orElse("5"));

        if (lockTime > 0){
            //查询用户是否已经封禁
            LocalDateTime o = CacheUtils.get(CacheConstants.PWD_FROZEN_TIME_KEY, username);
            if (o != null){
                //封禁状态查询是否可以解封
                if (o.plusMinutes(lockTime).isAfter(LocalDateTime.now())){
                    //达到解封时间  清空错误次数
                    CacheUtils.evict(CacheConstants.PWD_ERR_CNT_KEY, username);
                }else {
                    //封禁状态直接踢
                    recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
                    throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
                }
            }
        }

        //没设置封禁时间则直接判断用户名密码
        if (supplier.get()) {
            // 错误次数递增
            errorNumber++;
            // 达到规定错误次数 则锁定登录
            if (errorNumber >= maxRetryCount) {
                //放入缓存
                CacheUtils.put(CacheConstants.PWD_FROZEN_TIME_KEY, username, LocalDateTime.now());
                //发送异常信息
                recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
                throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
            } else {
                // 未达到规定错误次数
                //放入缓存
                CacheUtils.put(CacheConstants.PWD_ERR_CNT_KEY, username, errorNumber);
                recordLoginInfo(username, Constants.LOGIN_FAIL, MessageUtils.message(loginType.getRetryLimitCount(), errorNumber));
                throw new UserException(loginType.getRetryLimitCount(), errorNumber);
            }
        }

        // 登录成功 清空错误次数
        CacheUtils.evict(CacheConstants.PWD_ERR_CNT_KEY, username);
    }

    public void recordLoginInfo(String username, String status, String message) {
        LoginInfoEvent event = new LoginInfoEvent();
        event.setUsername(username);
        event.setStatus(status);
        event.setMessage(message);
        event.setRequest(ServletUtils.getRequest());
        SpringUtils.context().publishEvent(event);
    }

}

