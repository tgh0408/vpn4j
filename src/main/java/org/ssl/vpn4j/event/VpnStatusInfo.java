package org.ssl.vpn4j.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VpnStatusInfo {
    //上次接收字节总数
    private long lastBytesReceived;
    //上次发送字节总数
    private long lastBytesSent;
    // 接收字节总数
    private long bytesReceived;
    // 发送字节总数
    private long bytesSent;
    //平均字节接收速率
    private long avgBytesReceived;
    //平均字节发送速率
    private long avgBytesSent;

    /**
     * 以下数据连接时传递
     */
    //用户名
    private String nickname;
    //账号
    private String username;
    //连接时间
    private String connectTime;
    //子网掩码
    private String netmask;
    //分配的IP
    private String poolRemoteIp;
    //网关
    private String gateway;
    //本地IP 真实IP
    private String trustedIp;
    //本地端口 真实端口
    private String trustedPort;
    //客户端软件平台 win/mac/linux
    private String ivPlat;
    //客户端软件版本
    private String ivVer;
    //客户端GUI版本
    private String ivGuiVer;
    //客户端加密算法
    private String ivCiphers;

    //格式化后数据
    private String formatBytesReceived;
    private String formatBytesSent;
    private String formatAvgBytesReceived;
    private String formatAvgBytesSent;
}
