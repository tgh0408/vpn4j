package org.ssl.vpn4j.mybatis.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.vpn4j.domain.VpnService;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.mapper.VpnServiceMapper;

import java.util.Map;
import java.util.Properties;

/**
 * MyBatis 拦截器，自动更新全局缓存,无需 CacheUtils.put(SystemConfigEnum.smtp_server.getKey(), bo.getServer()) 此类操作,仅支持 VpnServiceMapper 的操作
 */

@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class VpnServiceCacheInterceptor implements Interceptor {

    private static final String PATH = VpnServiceMapper.class.getName();


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 先让 MyBatis 执行原始的 SQL 操作
        Object result = invocation.proceed();

        // 2. 获取方法参数
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        // 3. 过滤：只处理 VpnServiceMapper 的操作
        // 注意替换为你的 VpnServiceMapper 的真实包路径
        if (!ms.getId().contains(PATH)) {
            return result;
        }

        SqlCommandType commandType = ms.getSqlCommandType();
        if (commandType == SqlCommandType.INSERT ||
                commandType == SqlCommandType.UPDATE ||
                commandType == SqlCommandType.DELETE) {

            // 4. 保证缓存更新在事务提交之后进行
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        processCacheUpdate(parameter, commandType);
                    }
                });
            } else {
                // 如果当前没有事务，直接刷新
                processCacheUpdate(parameter, commandType);
            }
        }

        return result;
    }

    /**
     * 判断单条还是批量，执行不同的缓存策略
     */
    private void processCacheUpdate(Object parameter, SqlCommandType commandType) {
        // 场景 A：精确的行级更新（参数直接是实体类对象，如 insert(entity), updateById(entity)）
        if (parameter instanceof VpnService entity) {
            handleSingleEntity(entity, commandType);
        }else if (parameter instanceof Map<?, ?> paramMap) {
            // 2. 被 MyBatis-Plus 包装成 ParamMap 的情况 (例如 insertOrUpdate)
            // 尝试从 Map 中提取 entity，MyBatis-Plus 习惯把实体放在 "et" key 中
            if (paramMap.containsKey("et") && paramMap.get("et") instanceof VpnService) {
                handleSingleEntity((VpnService) paramMap.get("et"), commandType);
            }
        }
        // 如果是用 @Param("...") 指定了其他名字，也可以在这里继续添加提取逻辑
        // 场景 B：批量处理或复杂条件更新（参数是 Map、Collection、Wrapper 或是 deleteById 的基本类型）
        else {
            // 无法精确获知改了哪些 key，执行全表缓存刷新 只需要删就行了,查不到的情况下会自动查询
            CacheUtils.clear(SystemConfigEnum.cacheName);
        }
    }

    /**
     * 处理单行缓存更新
     */
    private void handleSingleEntity(VpnService entity, SqlCommandType commandType) {
        // 防御性校验，确保能拿到缓存用的 key
        if (entity.getKey1() == null) {
            // 如果只有 ID 没有 key（例如某些 updateSelective 没传 key），只能降级为全量刷新
            CacheUtils.clear(SystemConfigEnum.cacheName);
            return;
        }

        if (commandType == SqlCommandType.DELETE) {
            CacheUtils.evict(SystemConfigEnum.cacheName, entity.getKey1());
        } else { // INSERT or UPDATE
            // 如果 value 是 null，根据你的业务决定是删除缓存还是存 null
            if (entity.getValue1() != null){
                CacheUtils.put(SystemConfigEnum.cacheName, entity.getKey1(), entity.getValue1());
                log.info("Vpn Service Cache updated for key: {}", entity.getKey1());
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可以在这里接收配置文件中的参数
    }
}
