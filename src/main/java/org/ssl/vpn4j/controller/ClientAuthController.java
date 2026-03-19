package org.ssl.vpn4j.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.ssl.common.core.domain.R;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.ServletUtils;
import org.ssl.common.core.utils.file.FileUtils;
import org.ssl.common.log.annotation.Log;
import org.ssl.common.log.enums.BusinessType;
import org.ssl.common.log.enums.OperatorType;
import org.ssl.vpn4j.domain.bo.CcdBo;
import org.ssl.vpn4j.domain.bo.ClientAuthBo;
import org.ssl.vpn4j.service.ClientAuthService;

import java.io.IOException;

/**
 * openvpn 连接验证使用
 *
 * @author tgh
 */

@Slf4j
@RestController
@RequiredArgsConstructor
public class ClientAuthController {
    final ClientAuthService clientAuthService;

    @Log(title = "VPN连接", businessType = BusinessType.GRANT, operatorType = OperatorType.MANAGE)
    @PostMapping("openvpn/auth")
    public R<Void> auth(@Validated @RequestBody ClientAuthBo clientAuthBo, @RequestHeader(value = "X-OpenVPN-Server", required = false) String serverName) {
        clientAuthService.authenticate(clientAuthBo, serverName);
        return R.ok();
    }

    //    @Log(title = "VPN获取ccd", businessType = BusinessType.GRANT, operatorType = OperatorType.MANAGE)
    @PostMapping("openvpn/ccd")
    public R<String> ccd(@RequestBody CcdBo request) {
        String username = request.getUsername();
        String configContent = clientAuthService.generateCcdConfig(username);
        return R.ok(null, configContent);
    }

    @Log(title = "下载客户端", businessType = BusinessType.DOWNLOAD, operatorType = OperatorType.MANAGE)
    @GetMapping("client/download")
    @SaIgnore
    public void getClient(HttpServletResponse response) {
        if (response != null) {
            try {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

                String [] path = {
                        "classpath:client/vpn-client-for-windows.zip",
                        "file:./vpn-client-for-windows.zip"
                };
                Resource resource = null;
                for (String s : path) {
                    Resource[] resources = resolver.getResources(s);
                    if (resources.length > 0) {
                        resource = resources[0];
                        break;
                    }
                }
                if (resource == null) {
                    throw new ServiceException("下载客户端失败,找不到客户端资源");
                }
                FileUtils.setAttachmentResponseHeader(response, "vpn-client-for-windows.zip");
                response.setContentType("application/octet-stream");
                ServletUtils.write(response, resource.getInputStream());
            } catch (IOException e) {
                throw new ServiceException("下载客户端失败");
            }
        }
    }

}
