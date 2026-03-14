package org.ssl.vpn4j.utils;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.ssl.common.core.exception.ServiceException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 读取日志工具类
 */
public class ReadLogUtils {

    /**
     * 读取日志文件的指定行数
     *
     * @param logFileName 日志文件名
     * @param lines       指定行数
     * @return 日志文件的指定行数内容
     */
    public static List<String> readLogFile(String logFileName, Integer lines) throws IOException {
        return readLogFile(new File(logFileName), lines);
    }

    /**
     * 读取日志文件的指定行数
     *
     * @param logFile 日志文件
     * @param lines   指定行数
     * @return 日志文件的指定行数内容
     */
    public static List<String> readLogFile(File logFile, Integer lines) throws IOException {
        if (!logFile.exists()) {
            throw new ServiceException("日志文件不存在");
        }

        List<String> logList = new ArrayList<>();
        // ReversedLinesFileReader 会从文件末尾（End of File）开始读取
        try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder().setFile(logFile).setCharset(StandardCharsets.UTF_8).get()) {
            String line;
            // 逐行读取，直到达到请求的行数或文件读完
            while ((line = reader.readLine()) != null && logList.size() < lines) {
                logList.add(line);
            }
        }catch (AccessDeniedException e){
            throw new ServiceException("没有权限读取日志文件，请使用管理员权限运行: sudo chmod 644 " + logFile.getAbsolutePath());
        }

        // 因为 ReversedLinesFileReader 是倒序读取的（最新的在第一条）
        // 前端通常习惯正序排列，所以这里需要反转一下
        Collections.reverse(logList);
        return logList;
    }
}
