package org.ssl.vpn4j.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.ssl.vpn4j.enums.SsePushTypeEnum;
import org.ssl.vpn4j.enums.SystemType;
import org.ssl.vpn4j.event.VpnStatusInfo;

import java.util.Collection;
import java.util.List;

/**
 * 系统信息实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemClientInfo {
    private SsePushTypeEnum sseType;

    private String osName;      // 操作系统名称
    private String osVersion;   // 操作系统版本
    private String osArch;      // 操作系统架构
    private SystemType osType;   // 操作系统类型（Windows/Linux/Mac等）

    // 系统使用情况
    private double memoryUsage;
    private double useCore;
    private int core;
    private double cpuUsage;
    private String totalMemory;
    private String usedMemory;
    private String runTime;
    private List<DistInfo> disks;
    private String processId;
    private String status;
    private String serverIp;
    private String serverFramework;
    private String hostName;

    // 连接信息 用户信息
    private Integer onlineCount;
    private Collection<VpnStatusInfo> onlineUsers;

    //总发送
    private long totalBytesSent;
    private String formatTotalBytesSent;
    //总接收
    private long totalBytesReceived;
    private String formatTotalBytesReceived;
    //总上行
    private long totalAvgBytesSent;
    private String formatTotalAvgBytesSent;
    //总下行
    private long totalAvgBytesReceived;
    private String formatTotalAvgBytesReceived;

    //用户数
    private long totalUserCount;
    //启用用户数
    private long enableCount;
    //禁用用户数
    private long disableCount;
}
