package org.ssl.vpn4j.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;

import java.io.File;

@Slf4j
public class SpringBootJarHelper {

    /**
     * 获取当前 Jar 包（或项目）所在的【绝对路径】
     * 示例：G:/VPNSpace/vpn4j/ruoyi-admin/target/ruoyi-admin.jar
     */
    public static String getJarPath() {
        // 传入当前类或者启动类都可以
        ApplicationHome home = new ApplicationHome(SpringBootJarHelper.class);
        File source = home.getSource();
        // 如果是在 IDE 中运行，source 可能会是 null 或者 指向 target/classes
        // 如果是 java -jar 运行，source 指向 xxx.jar
        return source != null ? source.getAbsolutePath() : "";
    }

    /**
     * 获取当前 Jar 包所在的【文件夹路径】
     * 示例：G:/VPNSpace/vpn4j/ruoyi-admin/target
     */
    public static String getJarDir() {
        ApplicationHome home = new ApplicationHome(SpringBootJarHelper.class);
        File source = home.getSource();
        if (source == null) {
            return userDir(); // 容错处理
        }
        // 如果 source 是文件（xx.jar），返回其父目录
        // 如果 source 是目录（target/classes），也返回其父目录或自身，视需求而定
        return source.isFile() ? source.getParent() : source.getAbsolutePath();
    }

    // 兜底方案：获取工作目录
    private static String userDir() {
        return System.getProperty("user.dir");
    }

}
