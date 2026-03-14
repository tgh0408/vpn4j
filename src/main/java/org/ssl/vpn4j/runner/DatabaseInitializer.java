package org.ssl.vpn4j.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库初始化
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 匹配 CREATE TABLE "tableName" 或 CREATE TABLE tableName
     * 兼容引号、反引号、空格和大小写
     */
    private static final Pattern CREATE_TABLE_PATTERN =
            Pattern.compile("CREATE\\s+TABLE\\s+[\"`]?(\\w+)[\"`]?", Pattern.CASE_INSENSITIVE);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 确保只在主容器初始化时运行，避免多次触发
        if (event.getApplicationContext().getParent() == null) {
            runIntegrityCheck();
        }
    }

    private void runIntegrityCheck() {
        log.info(">>> [数据库检查] 开始扫描 SQL 脚本以验证表结构...");
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:sql/*.sql");

            if (resources.length == 0) {
                log.warn(">>> [数据库检查] 未在 classpath:sql/ 目录下找到任何脚本。");
                return;
            }

            // 批量操作前关闭外键约束
            jdbcTemplate.execute("PRAGMA foreign_keys = OFF;");

            for (Resource resource : resources) {
                String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                String tableName = extractTableName(sql);

                if (tableName == null) {
                    log.debug(">>> [跳过] 脚本 {} 未包含标准建表语句。", resource.getFilename());
                    continue;
                }

                if (isTableMissing(tableName)) {
                    log.info(">>> [补漏] 发现缺失表 [{}], 正在执行: {}", tableName, resource.getFilename());
                    executeScript(resource);
                }
            }

            // 恢复外键约束
            jdbcTemplate.execute("PRAGMA foreign_keys = ON;");
            log.info(">>> [数据库检查] 完成。所有表已就绪。");

        } catch (Exception e) {
            log.error(">>> [严重错误] 数据库初始化逻辑崩溃: ", e);
            throw new RuntimeException("Database Check Failed", e);
        }
    }

    private String extractTableName(String sql) {
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(sql);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean isTableMissing(String tableName) {
        String query = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, tableName);
        return count == null || count == 0;
    }

    private void executeScript(Resource resource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.addScript(resource);
        populator.setIgnoreFailedDrops(true);
        populator.setCommentPrefixes("^^");
        populator.execute(dataSource);
    }

    @Override
    public int getOrder() {
        // 保证在业务 Mapper 启动查询前执行完毕
        return Ordered.HIGHEST_PRECEDENCE;
    }
}