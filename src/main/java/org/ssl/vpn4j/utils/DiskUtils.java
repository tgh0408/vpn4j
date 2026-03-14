package org.ssl.vpn4j.utils;

import org.ssl.vpn4j.domain.DistInfo;
import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.text.DecimalFormat;
import java.util.List;

/**
 * 磁盘空间工具类
 */
public class DiskUtils {

    public static List<DistInfo> getDiskSpace(SystemInfo si) {
        OperatingSystem os = si.getOperatingSystem();
        FileSystem fileSystem = os.getFileSystem();// 可以根据实际情况修改路径

        List<OSFileStore> fileStores = fileSystem.getFileStores();
        return fileStores.stream().map(fs -> {
            DistInfo result = new DistInfo();
            result.setMount(fs.getMount());
            result.setTotalSpace(formatSize(fs.getTotalSpace()));
            result.setFreeSpace(formatSize(fs.getFreeSpace()));
            result.setUsedSpace(formatSize(fs.getUsableSpace()));
            result.setPercentageUsed(new DecimalFormat("0.00%").format(fs.getUsableSpace() / (double)fs.getTotalSpace()));
            return result;
        }).toList();
    }

    // 辅助方法：格式化字节大小为可读格式
    private static String formatSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
