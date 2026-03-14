package org.ssl.common.utils;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.format.DateTimeFormatter;

public class TimeUtils extends LocalDateTimeUtil {

    public static String timestampToString(Long ts){
        if (ts == null) return null;
        if (ts >= 1_000_000_000L && ts < 10_000_000_000L){
            ts *= 1000;
        }
        return LocalDateTimeUtil.of(ts).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
