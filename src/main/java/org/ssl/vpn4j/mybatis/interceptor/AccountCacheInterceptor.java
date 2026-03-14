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
import org.ssl.common.core.constant.CacheConstants;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.mapper.AccountMapper;

import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class AccountCacheInterceptor implements Interceptor {

    private static final String PATH = AccountMapper.class.getName();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 先让 MyBatis 执行原始的 SQL 操作
        Object result = invocation.proceed();

        // 2. 获取方法参数
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        // 3. 过滤：只处理 AccountMapper 的操作
        // 注意替换为你的 AccountMapper 的真实包路径
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
        if (parameter instanceof Account entity) {
            handleSingleEntity(entity, commandType);
        }else if (parameter instanceof Map<?, ?> paramMap) {
            // 2. 被 MyBatis-Plus 包装成 ParamMap 的情况 (例如 insertOrUpdate)
            // 尝试从 Map 中提取 entity，MyBatis-Plus 习惯把实体放在 "et" key 中
            if (paramMap.containsKey("et") && paramMap.get("et") instanceof Account) {
                handleSingleEntity((Account) paramMap.get("et"), commandType);
            }
        }
        // 如果是用 @Param("...") 指定了其他名字，也可以在这里继续添加提取逻辑
        // 场景 B：批量处理或复杂条件更新（参数是 Map、Collection、Wrapper 或是 deleteById 的基本类型）
        else {
            // 无法精确获知改了哪些 key，执行全表缓存刷新 只需要删就行了,查不到的情况下会自动查询
            CacheUtils.clear(CacheConstants.TOTAL_USER_KEY);
        }
    }

    /**
     * 处理单行缓存更新 缓存颗粒度未到行级，因此单行更新也执行全表刷新
     */
    private void handleSingleEntity(Account entity, SqlCommandType commandType) {
        CacheUtils.clear(CacheConstants.TOTAL_USER_KEY);
        log.info("Account Cache updated");
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
