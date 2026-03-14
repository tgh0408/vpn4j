package org.ssl.vpn4j.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.domain.R;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.mybatis.core.page.PageQuery;
import org.ssl.common.mybatis.core.page.TableDataInfo;
import org.ssl.vpn4j.domain.Syslog;
import org.ssl.vpn4j.domain.vo.SyslogVO;
import org.ssl.vpn4j.enums.SystemConfigEnum;
import org.ssl.vpn4j.service.SysLogService;
import org.ssl.vpn4j.utils.ReadLogUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LogController {
    final SysLogService sysLogService;

    @GetMapping("/log/history")
    public R<List<String>> getHistory(@RequestParam(defaultValue = "100") int lines) throws IOException {
        // 建议使用绝对路径或从配置中获取，防止找不到文件
        File logFile = new File("./logs/sys-console.log");
        List<String> logList = ReadLogUtils.readLogFile(logFile, lines);
        return R.ok(logList);
    }


    @GetMapping("/log/getVpnHistory")
    public R<List<String>> getVpnHistory(@RequestParam(defaultValue = "100") int lines) throws IOException {
        // 从配置中获取，正则匹配,防止找不到文件
        String serverConf = CacheUtils.get(SystemConfigEnum.server_conf);
        Pattern pattern = Pattern.compile("^log\\s+(\\S+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(serverConf);

        String logPath;
        if (matcher.find()) {
            logPath = matcher.group(1);
            // 输出: /data/openvpn/syslog/openvpn.log
        } else {
            throw new ServiceException("无法从serverConf中找到日志路径 Path: " + SystemConfigEnum.server_conf);
        }
        List<String> logList = ReadLogUtils.readLogFile(logPath, lines);
        return R.ok(logList);
    }

    @GetMapping("/log/getOperationLog")
    public TableDataInfo<Syslog> getOperationLog(@Validated SyslogVO syslog, PageQuery pageQuery) {
        return sysLogService.getOperationLog(syslog, pageQuery);
    }

}
