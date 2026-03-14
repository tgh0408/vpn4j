package org.ssl.vpn4j.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum SsePushTypeEnum {
    SYSTEM_MSG(0, "系统消息"),
    SYSTEM_MONITOR(1, "系统占用监控"),
    CPU_MONITOR(2, "CPU监控"),
    SYSTEM_LOG(3, "日志");
    private int type;
    private String desc;
}
