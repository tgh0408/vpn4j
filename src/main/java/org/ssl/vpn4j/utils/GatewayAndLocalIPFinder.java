package org.ssl.vpn4j.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class GatewayAndLocalIPFinder {

    /**
     * 主方法：获取网关和对应的本地IP
     */
//    public static void main(String[] args) {
//        try {
//            GatewayInfo gatewayInfo = getGatewayAndLocalIP();
//
//            if (gatewayInfo != null) {
//                System.out.println("网关IP: " + gatewayInfo.getGateway());
//                System.out.println("子网掩码: " + gatewayInfo.getNetmask());
//                System.out.println("对应的本地IP: " + gatewayInfo.getLocalIP());
//                System.out.println("网络接口: " + gatewayInfo.getInterfaceName());
//                System.out.println("网络前缀长度: " + gatewayInfo.getPrefixLength());
//                System.out.println("网络地址: " + gatewayInfo.getNetworkAddress());
//            } else {
//                System.out.println("未找到网关信息");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 获取网关和对应的本地IP信息
     */
    public static GatewayInfo getGatewayAndLocalIP() {
        // 1. 首先获取默认网关
        try {
            String gatewayIP = getDefaultGateway();
            if (gatewayIP == null) {
                return new GatewayInfo();
            }

            // 2. 获取所有网络接口
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // 跳过未启用和回环接口
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                // 3. 获取接口的所有IP地址
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    // 只处理IPv4地址
                    if (address instanceof Inet4Address) {
                        String localIP = address.getHostAddress();

                        // 4. 获取接口的详细地址信息（包含子网掩码）
                        List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();

                        for (InterfaceAddress ifAddr : interfaceAddresses) {
                            InetAddress addr = ifAddr.getAddress();

                            if (addr.equals(address)) {
                                // 5. 计算网络前缀长度和网络地址
                                short prefixLength = ifAddr.getNetworkPrefixLength();

                                // 检查网关是否在同一个子网
                                if (isInSameSubnet(localIP, gatewayIP, prefixLength)) {
                                    // 计算网络地址和子网掩码
                                    String networkAddress = calculateNetworkAddress(localIP, prefixLength);
                                    String netmask = calculateNetmask(prefixLength);

                                    return new GatewayInfo(
                                            gatewayIP,
                                            localIP,
                                            netmask,
                                            prefixLength,
                                            networkAddress,
                                            networkInterface.getName(),
                                            networkInterface.getDisplayName()
                                    );
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取网关和本地IP信息时出错", e);
        }
        return new GatewayInfo();
    }

    /**
     * 检查两个IP是否在同一个子网
     */
    private static boolean isInSameSubnet(String ip1, String ip2, short prefixLength) {
        try {
            long ip1Long = ipToLong(ip1);
            long ip2Long = ipToLong(ip2);
            long mask = prefixLengthToMask(prefixLength);

            return (ip1Long & mask) == (ip2Long & mask);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取默认网关
     */
    private static String getDefaultGateway() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return getWindowsDefaultGateway();
        } else {
            return getUnixDefaultGateway();
        }
    }

    /**
     * Linux/Unix/Mac系统获取默认网关
     */
    private static String getUnixDefaultGateway() throws Exception {
        // 先尝试使用ip命令
        try {
            ProcessBuilder pb = new ProcessBuilder("ip", "route", "show", "default");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.split("\\s+");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("via") && i + 1 < parts.length) {
                        String gateway = parts[i + 1];
                        if (isValidIP(gateway)) {
                            return gateway;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ip命令失败，使用netstat
        }

        // 使用netstat -rn
        ProcessBuilder pb = new ProcessBuilder("netstat", "-rn");
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // 查找默认路由行
            if (line.startsWith("0.0.0.0") || line.startsWith("default")) {
                String[] parts = line.split("\\s+");

                // 网关通常在第二列
                if (parts.length >= 2) {
                    String gateway = parts[1];
                    if (isValidIP(gateway) && !gateway.equals("0.0.0.0")) {
                        return gateway;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Windows系统获取默认网关
     */
    private static String getWindowsDefaultGateway() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("route", "print", "0.0.0.0");
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "GBK"));

        boolean foundTable = false;
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.contains("网络目标") || line.contains("Network Destination")) {
                foundTable = true;
                continue;
            }

            if (foundTable && line.startsWith("0.0.0.0")) {
                String[] parts = line.split("\\s+");
                for (String part : parts) {
                    if (isValidIP(part) && !part.equals("0.0.0.0")) {
                        return part;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 获取所有接口上匹配网关的本地IP列表
     */
    public static List<GatewayInfo> getAllMatchingLocalIPs() throws Exception {
        List<GatewayInfo> results = new ArrayList<>();
        String gatewayIP = getDefaultGateway();

        if (gatewayIP == null) {
            return results;
        }

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                continue;
            }

            List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();

            for (InterfaceAddress ifAddr : interfaceAddresses) {
                InetAddress address = ifAddr.getAddress();

                if (address instanceof Inet4Address) {
                    String localIP = address.getHostAddress();
                    short prefixLength = ifAddr.getNetworkPrefixLength();

                    if (isInSameSubnet(localIP, gatewayIP, prefixLength)) {
                        String networkAddress = calculateNetworkAddress(localIP, prefixLength);
                        String netmask = calculateNetmask(prefixLength);

                        results.add(new GatewayInfo(
                                gatewayIP,
                                localIP,
                                netmask,
                                prefixLength,
                                networkAddress,
                                networkInterface.getName(),
                                networkInterface.getDisplayName()
                        ));
                    }
                }
            }
        }

        return results;
    }

    /**
     * 工具方法：IP字符串转long
     */
    private static long ipToLong(String ipAddress) {
        String[] ipParts = ipAddress.split("\\.");
        long result = 0;

        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= Integer.parseInt(ipParts[i]);
        }

        return result;
    }

    /**
     * 工具方法：前缀长度转掩码long
     */
    private static long prefixLengthToMask(short prefixLength) {
        if (prefixLength < 0 || prefixLength > 32) {
            return 0;
        }
        return 0xFFFFFFFFL << (32 - prefixLength);
    }

    /**
     * 计算网络地址
     */
    private static String calculateNetworkAddress(String ip, short prefixLength) {
        long ipLong = ipToLong(ip);
        long mask = prefixLengthToMask(prefixLength);
        long networkLong = ipLong & mask;

        return longToIP(networkLong);
    }

    /**
     * 计算子网掩码
     */
    private static String calculateNetmask(short prefixLength) {
        long mask = prefixLengthToMask(prefixLength);
        return longToIP(mask);
    }

    /**
     * long转IP字符串
     */
    private static String longToIP(long ip) {
        return ((ip >> 24) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                (ip & 0xFF);
    }

    /**
     * 验证IP地址格式
     */
    private static boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return Pattern.matches("^(\\d{1,3}\\.){3}\\d{1,3}$", ip);
    }

    /**
     * 网关信息封装类
     *
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GatewayInfo {
        private String gateway;
        private String localIP;
        private String netmask;
        private short prefixLength;
        private String networkAddress;
        private String interfaceName;
        private String displayName;
    }
}
