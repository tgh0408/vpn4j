package org.ssl.vpn4j.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.utils.file.FileUtils;
import org.ssl.common.version.AppInfo;
import org.ssl.vpn4j.domain.VpnService;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.mapper.VpnServiceMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * nginx 初始化下发前端配置文件
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class NginxInitRunner implements ApplicationRunner, Ordered {
    final AppInfo appInfo;
    final VpnServiceMapper vpnServiceMapper;

    /**
     * 支持从输入流解压文件
     *
     * @param inputStream 源文件的输入流
     * @param destDir     目标解压目录
     */
    private static void unzip(InputStream inputStream, String destDir) throws IOException {
        File destDirectory = new File(destDir);
        if (!destDirectory.exists()) {
            destDirectory.mkdirs();
        }

        String canonicalDestPath = destDirectory.getCanonicalPath();

        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            byte[] buffer = new byte[4096];

            while ((zipEntry = zis.getNextEntry()) != null) {
                // 1. 兼容性处理：将 \ 和 / 全部替换为当前系统的路径分隔符
                // 这样在 Windows 下会统一成 \，在 Linux 下会统一成 /
                String entryName = zipEntry.getName()
                        .replace("\\", File.separator)
                        .replace("/", File.separator);

                File newFile = new File(destDirectory, entryName);

                // 2. 安全校验 (Zip Slip)
                String canonicalFilePath = newFile.getCanonicalPath();
                if (!canonicalFilePath.startsWith(canonicalDestPath + File.separator)
                        && !canonicalFilePath.equals(canonicalDestPath)) {
                    throw new IOException("检测到非法 ZIP 条目，可能存在路径穿越攻击: " + zipEntry.getName());
                }

                // 3. 处理目录逻辑
                // 有些压缩包的条目可能没有显式的目录条目，而是直接给出了文件路径
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("无法创建目录: " + newFile);
                    }
                } else {
                    // 4. 无论如何，在写文件前都确保其父目录存在（关键点）
                    File parent = newFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        if (!parent.mkdirs()) {
                            throw new IOException("无法创建父目录: " + parent);
                        }
                    }

                    // 5. 写入文件
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        initOrUpdateNginxConfig();
        initOrUpdateWeb();
        restartOrStartNginx();
    }

    private void initOrUpdateNginxConfig() {
        log.info(">>> [Nginx] 正在初始化 Nginx 配置");
        String nginxPath = CacheUtils.get(SystemConfigEnum.nginx_config_path);
        File nginx = new File(nginxPath);
        if (FileUtils.exist(nginx)) {
            log.info(">>> [Nginx] Nginx 配置文件已存在");
            return;
        }
        log.info(">>> [Nginx] Nginx 配置文件路径: {}", nginxPath);
        String nginxConf = CacheUtils.get(SystemConfigEnum.nginx_conf);

        FileUtils.writeString(nginxConf, new File(nginxPath), StandardCharsets.UTF_8);
        log.info(">>> [Nginx] nginx.conf 配置文件写入完成");
    }

    public void restartOrStartNginx() {
        String nginxPath = CacheUtils.get(SystemConfigEnum.nginx_config_path);
        if (isNginxRunning()) {
            log.info(">>> [Nginx] 检测到进程存在，准备执行热重启 (Reload)...");
            reloadNginx(nginxPath);
        } else {
            log.info(">>> [Nginx] 未检测到运行中的进程，准备执行全新启动...");
            startNginx(nginxPath);
        }
    }

    private boolean isNginxRunning() {
        try {
            // 使用 pgrep 检查名为 nginx 的进程
            Process process = new ProcessBuilder("pgrep", "nginx").start();
            return process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 平滑重启 (Reload)
     * 优势：不会断开现有连接
     */
    private void reloadNginx(String CONF_PATH) {
        executeCommand("nginx -s reload -c " + CONF_PATH, "热重启成功");
    }

    /**
     * 全新启动
     */
    private void startNginx(String CONF_PATH) {
        executeCommand("nginx -c " + CONF_PATH, "启动成功");
    }

    /**
     * 通用命令执行器
     */
    private void executeCommand(String command, String successMsg) {
        try {
            String nginxPath = CacheUtils.get(SystemConfigEnum.nginx_config_path);
            // 校验配置文件语法
            Process check = new ProcessBuilder("nginx", "-t", "-c", nginxPath).start();
            if (check.waitFor() != 0) {
                log.error("❌ [Nginx] 配置文件语法错误，放弃操作。");
                return;
            }

            ProcessBuilder pb = new ProcessBuilder(command.split(" "));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug(">>> [Nginx Shell] {}", line);
                }
            }

            if (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                log.info("✅ [Nginx] {}", successMsg);
            } else {
                log.error("❌ [Nginx] 执行失败 [{}]，错误码: {}", command, process.exitValue());
            }
        } catch (Exception e) {
            log.error(">>> [Nginx] 操作系统调用失败: {}", e.getMessage());
        }
    }

    private void initOrUpdateWeb() {
        try {
            String [] path = {
                    "classpath:dist/dist.zip",
                    "file:./dist.zip"
            };
            Resource resource = null;
            for (String s : path) {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources(s);
                if (resources.length > 0) {
                    resource = resources[0];
                    break;
                }
            }

            if (resource == null) {
                log.warn(">>> [Nginx] 未找到前端文件 dist.zip");
                return;
            }

            log.info(">>> [Nginx] web文件包路径: {}", resource.getURL().getPath());
            if (appInfo.checkVersion()) {
                log.info(">>> [Nginx] 版本号一致: {} ", appInfo.getVersion());
                return;
            }

            log.info(">>> [Nginx] 正在清理旧的 dist 目录");
            File distFolder = new File((String) CacheUtils.get(SystemConfigEnum.web_path), "dist");
            if (distFolder.exists()) {
                log.info(">>> [Nginx] 正在清理旧的 dist 目录: {}", distFolder.getAbsolutePath());
                FileSystemUtils.deleteRecursively(distFolder);
            }

            log.info(">>> [Nginx] 准备下发web文件包: {}", (String) CacheUtils.get(SystemConfigEnum.web_path));

            // 复制新文件
            try (InputStream is = resource.getInputStream()) {
                unzip(is, CacheUtils.get(SystemConfigEnum.web_path));
            }
            log.info(">>> [Nginx] web文件包解压完成: {}", CacheUtils.get(SystemConfigEnum.web_path) + File.separator + "dist");

            vpnServiceMapper.insertOrUpdate(
                    new VpnService(
                            SystemConfigEnum.app_version.getKey(),
                            appInfo.getVersion(),
                            SystemConfigEnum.app_version.getDesc()
                    )
            );
        } catch (Exception e) {
            log.error(">>> [Nginx] 解压web文件包失败: {}, 请手动更新客户端", e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
