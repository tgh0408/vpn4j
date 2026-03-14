package org.ssl.vpn4j.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.SpringUtils;
import org.ssl.vpn4j.cache.VpnOnlineCache;
import org.ssl.vpn4j.cache.constant.VpnConstant;
import org.ssl.vpn4j.domain.bo.RebootBo;
import org.ssl.vpn4j.domain.vo.VpnConfigVO;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.event.OfflineUserEvent;
import org.ssl.vpn4j.mapper.SyslogMapper;
import org.ssl.vpn4j.service.VpnConfigService;
import org.ssl.vpn4j.utils.Tools;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpnConfigServiceImpl implements VpnConfigService {
    final SyslogMapper syslogMapper;

    @Override
    public VpnConfigVO getConfigList() {
        return new VpnConfigVO(CacheUtils.get(SystemConfigEnum.server_conf));
    }

    @Override
    public String reboot(RebootBo bo) {
        stop();
        startService();
        return "重启成功";
    }

    @Override
    public void start() {
        String status = CacheUtils.get(VpnConstant.VPN_CACHE_NAME, VpnConstant.VPN_STATUS);
        log.info("OpenVPN 服务状态: {}", status);
        if ("inactive".equals(status)){
            Tools.checkCaInfo();
            createConfig();
            startService();
            CacheUtils.evict(VpnConstant.VPN_CACHE_NAME, VpnConstant.VPN_FORCE_STOP);
        }else if ("active".equals(status)){
            log.info("OpenVPN 服务正在运行，无需启动");
        }else {
            log.info("OpenVPN 服务状态未知");
        }
    }

    // 下发配置文件
    private void createConfig() {
        String configPath = CacheUtils.get(SystemConfigEnum.server_conf_path);
        //获取server.conf值
        String serverConf = CacheUtils.get(SystemConfigEnum.server_conf);

        // 将数据库配置文件复制到目标位置
        Tools.copyConfigFile(serverConf, configPath);

        //ca.key
        Tools.copyConfigFile(CacheUtils.get(SystemConfigEnum.ca_key), CacheUtils.get(SystemConfigEnum.ca_key_path));

        //ca ca.crt
        Tools.copyConfigFile(CacheUtils.get(SystemConfigEnum.ca_crt), getParameter(serverConf, "ca"));

        //cert server.crt
        Tools.copyConfigFile(CacheUtils.get(SystemConfigEnum.server_crt), getParameter(serverConf, "cert"));

        //key server.key
        Tools.copyConfigFile(CacheUtils.get(SystemConfigEnum.server_key), getParameter(serverConf, "key"));

        //dh dh.pem
        Tools.copyConfigFile(CacheUtils.get(SystemConfigEnum.dh_pem), getParameter(serverConf, "dh"));

        //tls-auth ta.key
        Tools.copyConfigFile(CacheUtils.get(SystemConfigEnum.ta_key), getParameter(serverConf, "tls-auth"));

        //ifconfig-pool-persist ipp.txt
        Tools.copyConfigFile("", getParameter(serverConf, "ifconfig-pool-persist"));

        //client-connect ccd.sh
        Tools.copyConfigFile(CacheUtils.get(SystemConfigEnum.ccd_auth_value), getParameter(serverConf, "client-connect"));

        //status
        Tools.copyConfigFile("", getParameter(serverConf, "status"));

        //log
        Tools.copyConfigFile("", getParameter(serverConf, "log"));

        //auth-user-pass-verify auth.sh
        Tools.copyConfigFile(CacheUtils.get(SystemConfigEnum.auth_value), getParameter(serverConf, "auth-user-pass-verify"));
    }


    @Override
    public void stop() {
        CacheUtils.put(VpnConstant.VPN_CACHE_NAME, VpnConstant.VPN_FORCE_STOP, true);
        // 移除所有在线用户
        SpringUtils.context().publishEvent(new OfflineUserEvent(VpnOnlineCache.keySet(), false));

        String msg = "停止VPN服务";

        try {
            // 1. 优先尝试直接结束进程 (精确匹配进程名 openvpn，不匹配 jar 命令行)
            log.info("正在尝试停止 OpenVPN 进程...");
            // 不使用 -f，仅匹配进程名 'openvpn'
            Process killProcess = Runtime.getRuntime().exec(new String[]{"pkill", "openvpn"});
            killProcess.waitFor();

            // 2. 等待进程释放资源
            Thread.sleep(2000);

            // 3. 检查是否依然存活
            if (!Strings.CI.equals(Tools.getVpnPid(), "-1")) {
                log.warn("OpenVPN 进程仍未停止，尝试强制结束...");
                // 如果普通 kill 不行，尝试强制 kill
                Runtime.getRuntime().exec(new String[]{"pkill", "-9", "openvpn"});
                Thread.sleep(1000);
            }

            // 4. 清理状态
            CacheUtils.evict(VpnConstant.VPN_CACHE_NAME, VpnConstant.VPN_STATUS);
            CacheUtils.evict(VpnConstant.VPN_CACHE_NAME, VpnConstant.VPN_PID);
            log.info("VPN 服务停止操作执行完毕");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(msg + "失败: 操作被中断");
        } catch (Exception e) {
            stopRoot();
        }
    }

    public void stopRoot() {
        CacheUtils.put(VpnConstant.VPN_CACHE_NAME, VpnConstant.VPN_FORCE_STOP, true);
        // 移除所有在线用户
        SpringUtils.context().publishEvent(new OfflineUserEvent(VpnOnlineCache.keySet(),  false));
        String msg = "停止VPN服务";
        String rootPassword = CacheUtils.get(SystemConfigEnum.root_passwd);

        try {
            // 使用sudo执行终止命令
            String stopCommand = String.format(
                    "echo '%s' | sudo -S pkill openvpn",
                    rootPassword
            );

            Process killProcess = Runtime.getRuntime().exec(
                    new String[]{"/bin/sh", "-c", stopCommand}
            );

            int killExitCode = killProcess.waitFor();

            if (killExitCode == 0) {
                log.info("成功发送终止信号给OpenVPN进程");
            }

            // 等待并检查
            Thread.sleep(3000);

            // 检查是否还有进程存活
            String checkCommand = String.format(
                    "echo '%s' | sudo -S pgrep openvpn",
                    rootPassword
            );

            Process checkProcess = Runtime.getRuntime().exec(
                    new String[]{"/bin/sh", "-c", checkCommand}
            );

            String remainingPids = new String(
                    checkProcess.getInputStream().readAllBytes()
            ).trim();

            if (!remainingPids.isEmpty()) {
                log.warn("仍有进程存活，强制终止: {}", remainingPids);
                String forceKillCommand = String.format(
                        "echo '%s' | sudo -S pkill -9 openvpn",
                        rootPassword
                );
                Runtime.getRuntime().exec(
                        new String[]{"/bin/sh", "-c", forceKillCommand}
                );
            }
            CacheUtils.evict(VpnConstant.VPN_CACHE_NAME,VpnConstant.VPN_STATUS);
            CacheUtils.evict(VpnConstant.VPN_CACHE_NAME,VpnConstant.VPN_PID);
        } catch (SecurityException e) {
            throw new ServiceException(msg + "失败: 权限不足");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(msg + "失败: 操作被中断");
        } catch (Exception e) {
            throw new ServiceException(msg + "失败: " + e.getMessage());
        }

    }

    private void startService() {
        String configPath = CacheUtils.get(SystemConfigEnum.server_conf_path);
        String openVpnBinary = CacheUtils.get(SystemConfigEnum.server_path);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "sudo",
                "-S",
                openVpnBinary,
                "--daemon",
                "--config", configPath
            );
            //输出执行的命令
            log.info("Executing command: {}", pb.command());
            // 合并错误流到标准输出，方便调试
            pb.redirectErrorStream(true);

            Process process = pb.start();
            if (Tools.hasRootPermissions()){
                OutputStream os = process.getOutputStream();
                try{
                    os.write(("%s\n".formatted((String)CacheUtils.get(SystemConfigEnum.root_passwd))).getBytes());
                    os.flush();
                }finally {
                    os.close();
                }
            }
            // 等待进程启动检查（如果是 daemon 模式，通常它会立即返回，或者 fork 后退出父进程）
            // waitFor 返回 0 通常代表成功，但 daemon 模式下行为可能不同，需根据实际情况调整
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (process.isAlive() || process.exitValue() == 0) {
                String vpnPid = Tools.getVpnPid();
                log.info("启动VPN服务成功!, PID: {}", vpnPid);
            } else {
                // 读取启动失败的报错信息
                String errorInfo = new String(process.getInputStream().readAllBytes());
                throw new ServiceException("启动VPN服务失败: " + errorInfo);
            }
            //等的启动后给VPN输入密码
            //waitDoPassword(); // 调用等待密码方法
        } catch (SecurityException e) {
            throw new ServiceException("失败: 权限不足，无法终止或启动进程");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("失败: 系统异常 - " + e.getMessage());
        } catch (IOException e) {
            throw new ServiceException("启动VPN服务失败: " + e.getMessage());
        }
    }

    public static String getParameter(String configContent, String key) {
        // 构建正则：忽略行首注释，匹配key，提取第一个非空且非引号的连续字符串
        // 使用 Pattern.quote 确保 key 中的字符（如横杠）不被当作正则元字符
        String regex = "(?m)^\\s*" + Pattern.quote(key) + "\\s+[\"']?([^\"'\\s#;]+)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(configContent);

        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("server.conf 中缺少 " + key + " 参数");
    }


}
