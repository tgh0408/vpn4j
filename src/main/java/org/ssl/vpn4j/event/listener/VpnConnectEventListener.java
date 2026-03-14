package org.ssl.vpn4j.event.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.ssl.vpn4j.cache.VpnOnlineCache;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.event.ConnectEvent;
import org.ssl.vpn4j.event.VpnStatusInfo;
import org.ssl.vpn4j.mapper.AccountMapper;

import java.util.List;


/**
 * 1. 客户端基本身份信息
 * 这是识别“谁在连接”的关键：
 * common_name=tgh: 证书中的通用名称（CN）。
 * username=tgh: 用户输入的登录账号。
 * trusted_ip=192.168.2.30: 客户端的真实物理 IP 地址。
 * trusted_port=54745: 客户端连接服务器时使用的源端口。
 * <p>
 * 2. 分配的网络参数
 * 连接成功后，服务器分配给该客户端的虚拟网络配置：
 * ifconfig_pool_remote_ip=10.0.10.199: 服务器分配给客户端的 虚拟内部 IP。
 * ifconfig_local=10.0.10.1: VPN 网关（服务器端）的内部虚拟 IP。
 * ifconfig_pool_netmask=255.255.255.0: 虚拟网络的子网掩码。
 * <p>
 * 3. 客户端软件环境 (IV_ 变量)
 * 这些变量（Peer Info）反映了客户端使用的软件版本和能力：
 * IV_PLAT=win: 客户端运行在 Windows 系统上。
 * IV_VER=2.6.12: 客户端 OpenVPN 软件的版本号。
 * IV_GUI_VER=OpenVPN_GUI_11.50.0.0: 客户端使用的 GUI 界面版本。
 * IV_CIPHERS=AES-256-GCM:...: 客户端支持的加密算法列表。
 * <p>
 * 4. 证书与安全验证 (X509)
 * 这部分是 SSL/TLS 握手的详细指纹：
 * tls_id_0 / X509_0_CN=tgh: 客户端证书的信息。
 * tls_id_1 / X509_1_CN=MyCA: 签发该证书的 CA 机构 信息（可以看到你设置的北京、MyOrg 等 O/OU 信息）。
 * tls_serial_hex_0: 客户端证书的唯一 16 进制序列号。
 * tls_digest_sha256_0: 客户端证书的 SHA256 指纹，常用于唯一识别一台设备。
 * <p>
 * 5. 控制与授权文件 (关键路径)
 * 由于你似乎正在开发 Java 后端来管理 OpenVPN，这几个文件非常重要：
 * auth_control_file: 最核心的文件。如果你在脚本中向这个文件写入 1，连接就会被允许；写入 0，连接就会被拒绝。
 * auth_failed_reason_file: 如果你想给客户端返回具体的失败原因（比如“余额不足”或“过期”），可以将原因写进这个临时文件。
 * <p>
 * 6. 服务器运行状态
 * daemon_pid=3467253: 当前 OpenVPN 进程的 PID。
 * config=/data/openvpn/server/server.conf: 正在使用的配置文件路径。
 * proto_1=tcp-server: 说明你的 VPN 运行在 TCP 协议模式。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VpnConnectEventListener {
    final AccountMapper accountMapper;

    @EventListener
    public void handleVpnConnectEvent(ConnectEvent event) {
        VpnStatusInfo vpnClientConnectInfo = new VpnStatusInfo();
        List<String> messages = event.getMessages();
        for (String message : messages) {
            if (message.startsWith(">CLIENT:ENV,common_name=")) {
                vpnClientConnectInfo.setUsername(message.substring(">CLIENT:ENV,common_name=".length()));
            } else if (message.startsWith(">CLIENT:ENV,time_ascii=")) {
                vpnClientConnectInfo.setConnectTime(message.substring(">CLIENT:ENV,time_ascii=".length()));
            } else if (message.startsWith(">CLIENT:ENV,trusted_ip=")) {
                vpnClientConnectInfo.setTrustedIp(message.substring(">CLIENT:ENV,trusted_ip=".length()));
            } else if (message.startsWith(">CLIENT:ENV,trusted_port=")) {
                vpnClientConnectInfo.setTrustedPort(message.substring(">CLIENT:ENV,trusted_port=".length()));
            } else if (message.startsWith(">CLIENT:ENV,ifconfig_pool_remote_ip=")) {
                vpnClientConnectInfo.setPoolRemoteIp(message.substring(">CLIENT:ENV,ifconfig_pool_remote_ip=".length()));
            } else if (message.startsWith(">CLIENT:ENV,ifconfig_local=")) {
                vpnClientConnectInfo.setGateway(message.substring(">CLIENT:ENV,ifconfig_local=".length()));
            } else if (message.startsWith(">CLIENT:ENV,ifconfig_pool_netmask=")) {
                vpnClientConnectInfo.setNetmask(message.substring(">CLIENT:ENV,ifconfig_pool_netmask=".length()));
            } else if (message.startsWith(">CLIENT:ENV,IV_PLAT=")) {
                vpnClientConnectInfo.setIvPlat(message.substring(">CLIENT:ENV,IV_PLAT=".length()));
            } else if (message.startsWith(">CLIENT:ENV,IV_VER=")) {
                vpnClientConnectInfo.setIvVer(message.substring(">CLIENT:ENV,IV_VER=".length()));
            } else if (message.startsWith(">CLIENT:ENV,IV_GUI_VER=")) {
                vpnClientConnectInfo.setIvGuiVer(message.substring(">CLIENT:ENV,IV_GUI_VER=".length()));
            } else if (message.startsWith(">CLIENT:ENV,IV_CIPHERS=")) {
                vpnClientConnectInfo.setIvCiphers(message.substring("CLIENT:ENV,IV_CIPHERS=".length()));
            }
        }
        //假设连接的时候存在,则可能是由于后端重启的原因,需要继承流量信息
        VpnStatusInfo existInfo = VpnOnlineCache.getOnlineCacheInfo(vpnClientConnectInfo.getUsername());
        if (existInfo != null) {
            //结算流量
            VpnOnlineCache.settlement(vpnClientConnectInfo.getUsername());
        }
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Account::getUsername, vpnClientConnectInfo.getUsername());
        Account acc = accountMapper.selectOne(wrapper);
        String nickname = acc.getNickname();
        vpnClientConnectInfo.setNickname(nickname);
        VpnOnlineCache.setOnlineCacheInfo(vpnClientConnectInfo.getUsername(), vpnClientConnectInfo);

        Account account = new Account();
        account.setOnline("1");
        accountMapper.update(account, wrapper);
        log.info("VPN 用户 {} 上线成功, Client Connect Info: {}", vpnClientConnectInfo.getUsername(), vpnClientConnectInfo);
    }
}
