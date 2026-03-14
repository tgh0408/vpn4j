package org.ssl.vpn4j.utils;


import cn.hutool.core.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.util.PropertyPlaceholderHelper;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.StringUtils;
import org.ssl.common.core.utils.file.FileUtils;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.enums.SystemType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.ServerException;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class Tools {

    public static String getVpnServerStatus() {
        // 模糊匹配的关键字：只要进程命令行包含这个片段即可
        String partialPath = CacheUtils.get(SystemConfigEnum.server_conf_path);
        boolean isRunning = isProcessRunning("openvpn", partialPath);
        return isRunning ? "active" : "inactive";
    }

    public static boolean isProcessRunning(String processName, String pathKeyword) {
        SystemType systemType = SystemTypeUtil.getSystemType();
        try {
            if (systemType == SystemType.WINDOWS) {
                return checkWindows(processName, pathKeyword);
            } else if (systemType == SystemType.LINUX) {
                return checkLinux(processName, pathKeyword);
            }else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkLinux(String processName, String keyword) throws Exception {
        // 使用 pgrep -a 可以列出完整命令行，然后再进行过滤
        ProcessBuilder pb = new ProcessBuilder("pgrep", "-af", processName);
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String output = reader.lines().collect(Collectors.joining("\n"));
            return output.contains(keyword);
        }
    }

    private static boolean checkWindows(String processName, String keyword) throws Exception {
        // Windows 使用 wmic 来获取包含完整参数的进程列表
        // 注意：新版 Windows 建议用 PowerShell，但 wmic 兼容性在旧版更好
        ProcessBuilder pb = new ProcessBuilder("wmic", "process", "where",
            "name like '%" + processName + "%'", "get", "commandline");
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "GBK"))) {
            String output = reader.lines().collect(Collectors.joining("\n"));
            // 兼容 Windows 路径的反斜杠问题，统一转义匹配
            String sanitizedKeyword = keyword.replace("/", "\\");
            return output.contains(keyword) || output.contains(sanitizedKeyword);
        }
    }

    public static String getVpnPid() {
        return ProcessHandle.allProcesses()
                .filter(process -> {
                    // 获取进程的命令行信息
                    ProcessHandle.Info info = process.info();
                    Optional<String> commandLine = info.commandLine();
                    // 模拟原 Shell 的过滤逻辑：包含 openvpn 且包含 --daemon
                    if (commandLine.isPresent()) {
                        String cmd = commandLine.get();
                        return cmd.contains("openvpn") && cmd.contains("--daemon");
                    }
                    // 某些系统上 commandLine 可能不可见，尝试检查可执行文件路径
                    Optional<String> command = info.command();
                    if (command.isPresent()) {
                        String cmdPath = command.get();
                        // 检查路径是否包含 openvpn，且参数中包含 --daemon
                        return cmdPath.contains("openvpn") &&
                                process.info().arguments()
                                        .map(args -> String.join(" ", args).contains("--daemon"))
                                        .orElse(false);
                    }
                    return false;
                })
                .findFirst() // 找到第一个匹配的进程
                .map(handle -> String.valueOf(handle.pid()))
                .orElse("-1");
    }

    public static boolean checkIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return true;
        }
        String[] split = ipAddress.split("\\.");
        if (split.length != 4) {
            return true;
        }
        return Strings.CS.equalsAny(split[3], "0","1") || Strings.CS.equals(split[3], "255");
    }

    public static String getHostName(){
        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "Unknown";
        }
        return hostName;
    }

    public static boolean hasRootPermissions() {
        try {
            // 获取当前用户名
            String username = System.getProperty("user.name");

            // 在Unix/Linux系统上，root用户的用户名是"root"
            // 在Windows系统上，管理员账户可能不是"root"，但可以检查权限
            if (SystemTypeUtil.getSystemType() == SystemType.WINDOWS) {
                // Windows系统
                return !isWindowsAdmin();
            } else if (SystemTypeUtil.getSystemType() == SystemType.LINUX) {
                // Unix/Linux系统
                return !"root".equals(username);
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean isWindowsAdmin() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("net", "session");
            int exitCode = processBuilder.start().waitFor();
            return exitCode == 0; // 如果命令成功执行，说明是管理员
        } catch (Exception e) {
            return false;
        }
    }

    public static void checkCaInfo() {
        String caInfo = CacheUtils.get(SystemConfigEnum.ca_crt);
        if (StringUtils.isEmpty(caInfo)){
            throw new ServiceException("CA证书不存在, 请先配置CA证书");
        }
        String caKey = CacheUtils.get(SystemConfigEnum.ca_key);
        if (StringUtils.isEmpty(caKey)){
            throw new ServiceException("CA私钥不存在, 请先配置CA私钥");
        }
        String serverCrt = CacheUtils.get(SystemConfigEnum.server_crt);
        if (StringUtils.isEmpty(serverCrt)){
            throw new ServiceException("服务器证书不存在, 请先配置服务器证书");
        }
        String serverKey = CacheUtils.get(SystemConfigEnum.server_key);
        if (StringUtils.isEmpty(serverKey)){
            throw new ServiceException("服务器私钥不存在, 请先配置服务器私钥");
        }
        String dhPem = CacheUtils.get(SystemConfigEnum.dh_pem);
        if (StringUtils.isEmpty(dhPem)){
            throw new ServiceException("DH参数不存在, 请先配置DH参数");
        }
        String serverPort = CacheUtils.get(SystemConfigEnum.server_port);
        if (StringUtils.isEmpty(serverPort)){
            throw new ServiceException("服务器端口不存在, 请先配置服务器端口");
        }
        String serverProto = CacheUtils.get(SystemConfigEnum.server_proto);
        if (StringUtils.isEmpty(serverProto)){
            throw new ServiceException("服务器协议不存在, 请先配置服务器协议");
        }
        String serverIp = CacheUtils.get(SystemConfigEnum.server_ip);
        if (StringUtils.isEmpty(serverIp)){
            throw new ServiceException("服务器IP不存在, 请先配置服务器IP");
        }
    }

    public static String getClientOvpn(Account account) {
        String serverIp = CacheUtils.get(SystemConfigEnum.server_ip);
        String serverPort = CacheUtils.get(SystemConfigEnum.server_port);
        String serverProto = CacheUtils.get(SystemConfigEnum.server_proto);
        String clientOvpn = CacheUtils.get(SystemConfigEnum.client_ovpn);
        if (StringUtils.isEmpty(serverIp)) {
            throw new ServiceException("VPN客户端ip地址不存在, 请先配置VPN客户端");
        }
        if (StringUtils.isEmpty(serverPort)) {
            throw new ServiceException("VPN客户端端口号不存在, 请先配置VPN客户端");
        }
        if (StringUtils.isEmpty(serverProto)) {
            throw new ServiceException("VPN客户端协议不存在, 请先配置VPN客户端");
        }
        if (StringUtils.isEmpty(clientOvpn)) {
            throw new ServiceException("VPN客户端配置文件不存在, 请先配置vpn-client.ovpn");
        }
        if (StringUtils.isEmpty(account.getClientCrt())) {
            throw new ServiceException("用户证书不存在, 请先生成用户证书");
        }
        if (StringUtils.isEmpty(account.getClientKey())) {
            throw new ServiceException("用户密钥不存在, 请先生成用户证书");
        }
        //模板解析
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
        return propertyPlaceholderHelper.replacePlaceholders(clientOvpn, placeholderName ->
                switch (placeholderName) {
                    case "username" -> account.getUsername();
                    case "password" -> StringUtils.trim(account.getPassword());
                    case "server" -> StringUtils.trim(serverIp);
                    case "port" -> StringUtils.trim(serverPort);
                    case "proto" -> StringUtils.trim(serverProto);
                    case "ca.crt" -> StringUtils.trim(CacheUtils.get(SystemConfigEnum.ca_crt));
                    case "tls-auth" -> StringUtils.trim(CacheUtils.get(SystemConfigEnum.ta_key));
                    case "cert" -> StringUtils.trim(account.getClientCrt());
                    case "key" -> StringUtils.trim(account.getClientKey());
                    default -> null;
                }
        );
    }

    public static void copyConfigFile(String config, String configPath) {
        try {
            Path path = Paths.get(configPath);

            // 1. 自动创建不存在的父目录 (类似 mkdir -p)
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            // 2. 将字符串写入文件 (如果文件存在则覆盖)
            Files.writeString(path, config, CharsetUtil.CHARSET_UTF_8);

            log.info("配置文件已成功写入: {}", configPath);
            //授权
            if (FileUtils.isFile(configPath)){
                doShell("chmod 755 " + configPath, "chmod_script");
            }
        } catch (IOException e) {
            log.error("写入配置文件失败: {}", e.getMessage());
            throw new ServiceException("写入配置文件失败", e);
        }
    }

    public static void doShell(String result, String tempName) throws IOException {
        File tempScript = File.createTempFile(tempName, ".sh");
        try {
            Files.write(tempScript.toPath(), result.getBytes());
            tempScript.setExecutable(true);

            // 尝试直接执行
            boolean directSuccess = false;
            try {
                directSuccess = executeScript(tempScript, false);
            } catch (IOException e) {
                // 直接执行异常，记录下来，准备尝试sudo
                log.warn("直接执行脚本失败，将尝试sudo: {}", e.getMessage());
            }

            if (!directSuccess) {
                // 尝试使用sudo执行
                boolean sudoSuccess = executeScript(tempScript, true);
                if (!sudoSuccess) {
                    throw new ServerException("脚本执行失败（包括sudo尝试）");
                }
            }

        } finally {
            tempScript.delete();
        }
    }

    private static boolean executeScript(File scriptFile, boolean useSudo) throws IOException {
        ProcessBuilder pb;
        if (useSudo) {
            String command = String.format("echo '%s' | sudo -S bash %s",
                    CacheUtils.get(SystemConfigEnum.root_passwd),
                    scriptFile.getAbsolutePath());
            pb = new ProcessBuilder("bash", "-c", command);
        } else {
            pb = new ProcessBuilder("bash", scriptFile.getAbsolutePath());
        }

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 读取输出
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return true;
            } else {
                log.warn("脚本执行退出码非0: {}{}", exitCode, useSudo ? " (sudo)" : "");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("脚本执行被中断", e);
        }
    }
}
