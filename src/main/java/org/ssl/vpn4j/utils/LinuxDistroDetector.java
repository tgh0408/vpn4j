package org.ssl.vpn4j.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LinuxDistroDetector {

    public static String detectLinuxDistro() {
        // 检查常见的发行版标识文件
        String[] distroFiles = {
                "/etc/os-release",      // 现代标准
                "/etc/lsb-release",     // Ubuntu/Debian
                "/etc/debian_version",  // Debian
                "/etc/redhat-release",  // RedHat/CentOS/Fedora
                "/etc/centos-release",  // CentOS
                "/etc/fedora-release",  // Fedora
                "/etc/SuSE-release",    // openSUSE/SLES
                "/etc/arch-release",    // Arch Linux
                "/etc/alpine-release"   // Alpine Linux
        };

        for (String filePath : distroFiles) {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                try {
                    return readDistroFromFile(filePath);
                } catch (IOException e) {
                    // 继续检查下一个文件
                }
            }
        }
        return "Unknown Linux";
    }

    private static String readDistroFromFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        // 根据文件内容判断发行版
        String contentStr = content.toString().toLowerCase();

        if (filePath.contains("os-release")) {
            if (contentStr.contains("ubuntu")) return "Ubuntu";
            if (contentStr.contains("debian")) return "Debian";
            if (contentStr.contains("centos")) return "CentOS";
            if (contentStr.contains("rhel") || contentStr.contains("redhat")) return "RedHat";
            if (contentStr.contains("fedora")) return "Fedora";
            if (contentStr.contains("opensuse") || contentStr.contains("suse")) return "openSUSE";
            if (contentStr.contains("alpine")) return "Alpine Linux";
            if (contentStr.contains("arch")) return "Arch Linux";
        } else if (filePath.contains("centos-release")) {
            return "CentOS";
        } else if (filePath.contains("redhat-release")) {
            return "RedHat";
        } else if (filePath.contains("debian_version")) {
            return "Debian";
        }

        return "Linux (具体发行版未知)";
    }
}
