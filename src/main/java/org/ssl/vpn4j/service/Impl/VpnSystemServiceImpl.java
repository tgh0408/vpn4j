package org.ssl.vpn4j.service.Impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.constant.GlobalConstants;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.StringUtils;
import org.ssl.common.mail.config.properties.MailProperties;
import org.ssl.common.mail.utils.MailUtils;
import org.ssl.vpn4j.cache.constant.VpnConstant;
import org.ssl.vpn4j.domain.Account;
import org.ssl.vpn4j.domain.ClientProperties;
import org.ssl.vpn4j.domain.SystemClientInfo;
import org.ssl.vpn4j.domain.VpnService;
import org.ssl.vpn4j.domain.bo.VpnServiceBo;
import org.ssl.vpn4j.domain.vo.VpnServiceVO;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.enums.SystemType;
import org.ssl.vpn4j.mapper.AccountMapper;
import org.ssl.vpn4j.mapper.VpnServiceMapper;
import org.ssl.vpn4j.service.VpnSystemService;
import org.ssl.vpn4j.utils.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

/**
 * 预留代码
 * //        服务器运行时间
 * String sysTime = Files.readString(Path.of("/proc/uptime")).split("\\s+")[0];
 * int sec = Double.valueOf(sysTime).intValue();
 * int day = (int) TimeUnit.SECONDS.toDays(sec);
 * int hour = (int) (TimeUnit.SECONDS.toHours(sec) % 24);
 * int minute = (int) (TimeUnit.SECONDS.toMinutes(sec) % 60);
 * <p>
 * StringBuilder sb = new StringBuilder();
 * if (day > 0) sb.append(day).append("天 ");
 * if (hour > 0) sb.append(hour).append("小时 ");
 * if (minute > 0) sb.append(minute).append("分钟");
 * <p>
 * String uptimeString = sb.toString().trim();
 * if (uptimeString.isEmpty()) {
 * uptimeString = "0分钟";
 * }
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class VpnSystemServiceImpl implements VpnSystemService {
    private static final SystemInfo si = new SystemInfo();
    private static final HardwareAbstractionLayer hal = si.getHardware();
    final VpnServiceMapper vpnServiceMapper;
    final AccountMapper accountMapper;

    /**
     * 获得实时服务信息
     *
     * @return SystemClientInfo
     */
    @Override
    public SystemClientInfo getRealTimeServiceInfo() {
        SystemClientInfo systemClientInfo = new SystemClientInfo();

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long milliseconds = runtimeMXBean.getUptime();
        long seconds = milliseconds / 1000;
        long days = seconds / (24 * 3600);
        long hours = (seconds % (24 * 3600)) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        String runTime = String.format("%d天 %02d:%02d:%02d", days, hours, minutes, secs);

        // 1. 监控内存 (Memory)
        GlobalMemory memory = hal.getMemory();
        long canUserMem = memory.getAvailable() / 1024 / 1024;
        String cMen;
        if (canUserMem > 1024) {
            cMen = String.format("%.2f GB", (double) canUserMem / 1024);
        } else {
            cMen = canUserMem + " MB";
        }

        Long globalMem = CacheUtils.get(GlobalConstants.GLOBAL_SERVER_KEY, "total_mem");
        String gMem;
        if (globalMem == null) {
            globalMem = memory.getTotal() / 1024 / 1024;
            CacheUtils.put(GlobalConstants.GLOBAL_SERVER_KEY, "total_mem", globalMem);
        } else {
            globalMem = CacheUtils.get(GlobalConstants.GLOBAL_SERVER_KEY, "total_mem");
        }
        if (globalMem == null) {
            globalMem = memory.getTotal() / 1024 / 1024;
        }
        if (globalMem > 1024) {
            gMem = String.format("%.2f GB", (double) globalMem / 1024);
        } else {
            gMem = globalMem + " MB";
        }

        //内存使用率
        double memoryUsage = (double) (globalMem - memory.getAvailable() / 1024 / 1024) / globalMem * 100;

        CentralProcessor processor = hal.getProcessor();

        // CPU核心数
        int core = processor.getLogicalProcessorCount();

        systemClientInfo.setOsName(SystemTypeUtil.getSystemType().getValue());
        systemClientInfo.setOsVersion(SystemTypeUtil.getSystemType() == SystemType.LINUX ? LinuxDistroDetector.detectLinuxDistro() : System.getProperty("os.version"));
        systemClientInfo.setProcessId(Tools.getVpnPid());
        String vpnServerStatus = Tools.getVpnServerStatus();
        systemClientInfo.setStatus(vpnServerStatus);
        CacheUtils.put(VpnConstant.VPN_CACHE_NAME, VpnConstant.VPN_STATUS, vpnServerStatus);
        systemClientInfo.setServerIp(GatewayAndLocalIPFinder.getGatewayAndLocalIP().getLocalIP());
        systemClientInfo.setServerFramework("SpringBoot " + SpringBootVersion.getVersion());
        systemClientInfo.setHostName(Tools.getHostName());
        systemClientInfo.setCore(core);
        systemClientInfo.setRunTime(runTime);
        systemClientInfo.setMemoryUsage(Double.parseDouble(new DecimalFormat("0.00").format(memoryUsage)));
        systemClientInfo.setTotalMemory(gMem);
        systemClientInfo.setUsedMemory(cMen);
        systemClientInfo.setDisks(DiskUtils.getDiskSpace(si));
        return systemClientInfo;
    }

    /**
     * CPU 使用率 需要阻塞,单独剥离
     *
     * @return SystemClientInfo
     */
    @Override
    public SystemClientInfo getRealTimeCpuInfo() {
        SystemClientInfo systemClientInfo = new SystemClientInfo();
        CentralProcessor processor = hal.getProcessor();
        // 第一次获取CPU tick计数
        long[] prevTicks = processor.getSystemCpuLoadTicks();

        // 等待一段时间（建议至少1秒）
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new ServiceException("获取CPU使用率等待时发生中断：" + e.getMessage());
        }
        int core = processor.getLogicalProcessorCount();
        // 第二次获取CPU tick计数并计算使用率
        double cpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        systemClientInfo.setCore(core);
        systemClientInfo.setUseCore(Double.parseDouble(new DecimalFormat("0.00").format(cpuUsage * core / 100.0)));
        systemClientInfo.setCpuUsage(Double.parseDouble(new DecimalFormat("0.00").format(cpuUsage)));
        return systemClientInfo;
    }

    /**
     * 获得操作系统信息
     *
     * @return SystemClientInfo
     */
    @Override
    public SystemClientInfo getServiceInfo() {
        // 获取操作系统名称
        String osName = System.getProperty("os.name");
        // 获取操作系统版本
        String osVersion = System.getProperty("os.version");
        // 获取操作系统架构
        String osArch = System.getProperty("os.arch");
        SystemClientInfo systemClientInfo = new SystemClientInfo();
        systemClientInfo.setOsArch(osArch);
        systemClientInfo.setOsName(osName);
        systemClientInfo.setOsVersion(osVersion);
        systemClientInfo.setOsType(SystemTypeUtil.getSystemType());
        return systemClientInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createServerCer(VpnServiceBo bo) throws IOException {
        //优先保存客户端服务端信息
        this.updateConfigs(bo);

        OpenVpnCertificateTool.createServer(
                bo,
                (listMap) -> {
                    vpnServiceMapper.insertOrUpdate(listMap);
                    accountMapper.update(
                            new LambdaUpdateWrapper<Account>()
                                    .set(Account::getClientCrt, "")
                                    .set(Account::getClientKey, "")
                    );
                });

        log.info("✔ 证书写入数据库完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public ClientProperties createUserCer(String username) throws IOException {
        Tools.checkCaInfo();
        return OpenVpnCertificateTool.createClient(username);
    }

    @Override
    public String getVpnConfig() {
        return CacheUtils.get(SystemConfigEnum.server_conf);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVpnConfig(String config) {
        if (StringUtils.isBlank(config)) {
            throw new ServiceException("配置不能为空");
        }
        VpnService vpnService = new VpnService();
        vpnService.setKey1(SystemConfigEnum.server_conf.getKey());
        vpnService.setValue1(config);
        vpnServiceMapper.insertOrUpdate(vpnService);
    }

    @Override
    public void sendTestEmail() {
        if (!Strings.CI.equals(CacheUtils.get(SystemConfigEnum.smtp_enable), "1")) {
            throw new ServiceException("SMTP服务尚未启用");
        }
        MailProperties mailProperties = new MailProperties();
        mailProperties.setHost(CacheUtils.get(SystemConfigEnum.smtp_host));
        mailProperties.setPort(Integer.parseInt(CacheUtils.get(SystemConfigEnum.smtp_port)));
        mailProperties.setAuth(Strings.CI.equals(CacheUtils.get(SystemConfigEnum.smtp_auth), "1"));
        mailProperties.setFrom(CacheUtils.get(SystemConfigEnum.smtp_from));
        mailProperties.setUser(CacheUtils.get(SystemConfigEnum.smtp_user));
        mailProperties.setPassword(CacheUtils.get(SystemConfigEnum.smtp_password));
        mailProperties.setStarttlsEnable(Strings.CI.equals(CacheUtils.get(SystemConfigEnum.smtp_starttls_enable), "1"));
        mailProperties.setSslEnable(Strings.CI.equals(CacheUtils.get(SystemConfigEnum.smtp_ssl_enable), "1"));
        mailProperties.setTimeout(Long.parseLong(CacheUtils.get(SystemConfigEnum.smtp_timeout)));
        mailProperties.setConnectionTimeout(Long.parseLong(CacheUtils.get(SystemConfigEnum.smtp_connection_timeout)));

        MailUtils.setProperties(mailProperties);
        MailUtils.sendText((String) CacheUtils.get(SystemConfigEnum.smtp_from), "VPN4J测试邮件", "VPN4J测试邮件");
    }

    @Override
    public List<VpnServiceVO> getConfigs(String[] keys) {
        return Arrays.stream(keys).map(key -> {
            //特殊适配以下证书创建时间 缓存没存
            if (SystemConfigEnum.server_key_name.getKey().equals(key)) {
                VpnService vpnService = vpnServiceMapper.selectById(SystemConfigEnum.server_key_name.getKey());
                if (vpnService != null) {
                    return new VpnServiceVO(key, vpnService.getValue1(), vpnService.getDescription(), vpnService.getUpdateTime());
                }
            }
            SystemConfigEnum systemConfigEnum = SystemConfigEnum.fromKey(key, true);
            String v = CacheUtils.get(systemConfigEnum);
            return new VpnServiceVO(key, v, systemConfigEnum.getDesc());
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigs(VpnServiceBo bo) {
        List<VpnServiceBo.ServiceItem> items = bo.getItems();
        //校验映射关系
        items.forEach(item -> SystemConfigEnum.fromKey(item.getKey1(),true));
        List<VpnService> list = items.stream().map(item -> new VpnService(item.getKey1(), item.getValue1())).toList();
        vpnServiceMapper.insertOrUpdate(list);
    }

}
