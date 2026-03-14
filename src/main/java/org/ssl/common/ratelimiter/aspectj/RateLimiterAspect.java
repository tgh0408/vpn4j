package org.ssl.common.ratelimiter.aspectj;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.MessageUtils;
import org.ssl.common.core.utils.ServletUtils;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.common.core.utils.StringUtils;
import org.ssl.common.ratelimiter.annotation.RateLimiter;
import org.ssl.common.ratelimiter.enums.LimitType;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流处理
 *
 * @author Lion Li
 */
@Slf4j
@Aspect
public class RateLimiterAspect {

    /**
     * 定义spel表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();
    /**
     * 定义spel解析模版
     */
    private final ParserContext parserContext = new TemplateParserContext();
    /**
     * 方法参数解析器
     */
    private final ParameterNameDiscoverer pnd = new DefaultParameterNameDiscoverer();

    private final ConcurrentHashMap<Integer, Cache<String, AtomicInteger>> cacheMap = new ConcurrentHashMap<>();


    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) {
        int time = rateLimiter.time();
        int count = rateLimiter.count();

        Cache<String, AtomicInteger> limitCache = getCache(time);
        String combineKey = getCombineKey(rateLimiter, point);

        // 【优化点 1】原子化获取计数器，不需要在 compute 里直接设为 1
        AtomicInteger counter = limitCache.get(combineKey, k -> new AtomicInteger(0));

        // 【优化点 2】先增加再判断，保证并发下的精确性
        int currentCount = counter.incrementAndGet();

        if (currentCount > count) {
            log.warn("限制令牌 => {}, 当前已触发 => {}, 缓存key => '{}'", count, currentCount, combineKey);
            // 国际化消息处理逻辑（如果有）建议在这里加上
            String message = rateLimiter.message();
            if (Strings.CI.startsWith(message, "{") && Strings.CI.endsWith(message, "}")) {
                message = MessageUtils.message(StringUtils.substring(message, 1, message.length() - 1));
            }
            throw new ServiceException(message);
        }
    }

    /**
     * 动态获取/创建 Cache，支持不同的 rateLimiter.time()
     */
    private Cache<String, AtomicInteger> getCache(int seconds) {
        return cacheMap.computeIfAbsent(seconds, s -> Caffeine.newBuilder()
                .expireAfterWrite(s, TimeUnit.SECONDS) // 写入后指定秒数过期
                .maximumSize(10000) // 防止内存溢出
                .build());
    }

    private String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        String key = rateLimiter.key();

        // 1. 处理 SpEL 表达式
        if (StringUtils.isNotBlank(key) && StringUtils.containsAny(key, "#")) {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method targetMethod = signature.getMethod();
            Object[] args = point.getArgs();

            MethodBasedEvaluationContext context =
                    new MethodBasedEvaluationContext(null, targetMethod, args, pnd);
            context.setBeanResolver(new BeanFactoryResolver(SpringUtils.getBeanFactory()));

            Expression expression;
            // 判断是否是模板形式如 #{#user.id}
            if (key.startsWith(parserContext.getExpressionPrefix())
                    && key.endsWith(parserContext.getExpressionSuffix())) {
                expression = parser.parseExpression(key, parserContext);
            } else {
                expression = parser.parseExpression(key);
            }
            key = expression.getValue(context, String.class);
        }

        // 2. 构造本地缓存 Key
        StringBuilder sb = new StringBuilder("local_rate_limit:"); // 改为本地前缀
        if (!rateLimiter.shareKey()){
            sb.append(Objects.requireNonNull(ServletUtils.getRequest()).getRequestURI()).append(":");
        }
        // 3. 根据限流类型区分
        if (rateLimiter.limitType() == LimitType.IP) {
            sb.append(ServletUtils.getClientIP()).append(":");
        }

        return sb.append(StringUtils.defaultString(key)).toString();
    }
}
