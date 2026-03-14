package org.ssl.vpn4j.utils;


import java.text.DecimalFormat;

/**
 * 流量单位转换工具类
 * 用于将字节数自动转换为最合适的单位（B、KB、MB、GB、TB、PB）
 */

public class TrafficConverter {

    // 单位定义
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
    private static final double BASE = 1024.0;

    // 格式化为两位小数
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    /**
     * 将字节数自动转换为最合适的单位
     * @param bytes 字节数
     * @return 格式化的字符串，包含数值和单位
     */
    public static String autoConvert(long bytes) {
        if (bytes == 0) {
            return "0 B";
        }

        int unitIndex = 0;
        double value = bytes;

        // 计算最适合的单位
        while (value >= BASE && unitIndex < UNITS.length - 1) {
            value /= BASE;
            unitIndex++;
        }

        // 格式化数值
        String formattedValue = DECIMAL_FORMAT.format(value);
        return formattedValue + " " + UNITS[unitIndex];
    }
}
