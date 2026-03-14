package org.ssl.vpn4j.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ssl.common.core.validate.EditGroup;

@Data
public class UpdateEmailBo {
//    {"smtp_server":"smtp.163.com","smtp_user":"linux_support@163.com","smtp_password":"QQKISZZVBYLEOKXT","smtp_port":465,"sender_name":"Linuxcc.cn"}
    @NotBlank(message = "smtpServer不能为空", groups = {EditGroup.class})
    private String smtpServer;
    @NotBlank(message = "smtpUser不能为空", groups = {EditGroup.class})
    private String smtpUser;
    @NotBlank(message = "smtpPassword不能为空", groups = {EditGroup.class})
    private String smtpPassword;
    @NotNull(message = "smtpPort不能为空", groups = {EditGroup.class})
    private Long smtpPort;
    @NotBlank(message = "senderName不能为空", groups = {EditGroup.class})
    private String senderName;
}
