package org.ssl.vpn4j.utils;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PropertyPlaceholderHelper;
import org.ssl.common.cache.utils.CacheUtils;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.file.FileUtils;
import org.ssl.vpn4j.domain.ClientProperties;
import org.ssl.vpn4j.domain.VpnService;
import org.ssl.vpn4j.domain.bo.VpnServiceBo;
import org.ssl.vpn4j.enums.SystemConfigEnum;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OpenVpnCertificateTool {

    // 初始化 SLF4J 日志记录器
    private static final Logger log = LoggerFactory.getLogger(OpenVpnCertificateTool.class);

    private static final String dh_pem = SystemConfigEnum.dh_pem.getKey();
    private static final String ca_key = SystemConfigEnum.ca_key.getKey();
    private static final String ca_crt = SystemConfigEnum.ca_crt.getKey();
    private static final String server_key = SystemConfigEnum.server_key.getKey();
    private static final String server_crt = SystemConfigEnum.server_crt.getKey();
    private static final String ta_key = SystemConfigEnum.ta_key.getKey();


    // 从数据库读取 ca 创建客户端证书
    public static ClientProperties createClient(String username) throws IOException {
        String shell = """
                # 1. 生成用户私钥 (client.key)
                openssl genrsa -out "${userDir}client.key" 2048
                # 2. 生成请求 (client.csr)
                openssl req -new -key "${userDir}client.key" -out "${userDir}client.csr" -subj "/CN=${username}" -sha256
                
                # 创建扩展配置
                cat > "${userDir}client.ext" <<EOF
                basicConstraints=CA:FALSE
                keyUsage = digitalSignature, keyEncipherment
                extendedKeyUsage = clientAuth
                subjectKeyIdentifier=hash
                authorityKeyIdentifier=keyid,issuer
                EOF
                
                # 3. 使用 CA 签发 (client.crt)
                openssl x509 -req -in "${userDir}client.csr" -CA "${caDir}ca.crt" -CAkey "${caDir}ca.key" -CAcreateserial -out "${userDir}client.crt" -days ${caExpire} -extfile "${userDir}client.ext" -sha256
                
                # 清理临时文件
                rm -f "${userDir}client.ext"
                chmod 644 "${userDir}client.key" "${userDir}client.crt"
                """;
        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
        String uDir = CacheUtils.get(SystemConfigEnum.user_ca_path) + "/" + username + "/";
        Path dir = Paths.get(uDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        String result = propertyPlaceholderHelper.replacePlaceholders(shell, placeholderName ->
                switch (placeholderName) {
                    case "username" -> username;
                    case "userDir" -> uDir;
                    case "caDir" -> CacheUtils.get(SystemConfigEnum.ssl_path) + "/";
                    case "caExpire" -> CacheUtils.get(SystemConfigEnum.ca_expire);
                    default -> "";
                }
        );
        Tools.doShell(result, "user_cert");
        String crt = Files.readString(Paths.get(uDir + "/client.crt"));
        String key = Files.readString(Paths.get(uDir + "/client.key"));
        return new ClientProperties(key, crt);
    }

    /**
     * 创建系统证书
     */
    public static void createServer(VpnServiceBo bo, Consumer<List<VpnService>> saveDataBase) throws IOException {
        Map<String, String> boMap = bo.getItems().stream().collect(Collectors.toMap(VpnServiceBo.ServiceItem::getKey1, VpnServiceBo.ServiceItem::getValue1));
        String targetDirectory = CacheUtils.get(SystemConfigEnum.ssl_path);
        if (targetDirectory == null) {
            throw new ServiceException("请先配置CA证书路径");
        }

        // 确保路径以 / 结尾
        String dir = targetDirectory.endsWith("/") ? targetDirectory : targetDirectory + "/";

        FileUtils.mkdir(dir);

        String shell = getShell(dir);

        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
        String result = propertyPlaceholderHelper.replacePlaceholders(shell, placeholderName ->
                switch (placeholderName) {
                    case "dir" -> dir;
                    case "email" -> boMap.get(SystemConfigEnum.system_email.getKey());
                    case "openvpnPath" -> (String) CacheUtils.get(SystemConfigEnum.server_path);
                    case "caName" -> boMap.get(SystemConfigEnum.ca_name.getKey());
                    case "serverName" -> boMap.get(SystemConfigEnum.server_key_name.getKey());
                    case "serverKey" -> boMap.get(SystemConfigEnum.encrypted_passwords.getKey());
                    case "caExpire" -> boMap.get(SystemConfigEnum.ca_expire.getKey());
                    default -> "";
                }
        );
//        log.info("create vpn cert shell : \n {}", result);
        Tools.doShell(result, "vpn_cert_gen");
        //读取本地文件
        List<VpnService> vpnServices = new ArrayList<>();
        vpnServices.add(new VpnService(ca_key, Files.readString(Paths.get(dir + "ca.key"))));
        vpnServices.add(new VpnService(server_key, Files.readString(Paths.get(dir + "server.key"))));
        vpnServices.add(new VpnService(server_crt, Files.readString(Paths.get(dir + "server.crt"))));
        vpnServices.add(new VpnService(ta_key, Files.readString(Paths.get(dir + "ta.key"))));
        vpnServices.add(new VpnService(ca_crt, Files.readString(Paths.get(dir + "ca.crt"))));

        Path path = Paths.get(dir + "dh.pem");
        if (Files.exists(path)) {
            vpnServices.add(new VpnService(dh_pem, Files.readString(path)));
        }
        saveDataBase.accept(vpnServices);
    }

    private static @NonNull String getShell(String dir) {
        String dh = CacheUtils.get(SystemConfigEnum.dh_pem);
        String dhShell;
        if (dh == null) {
            dhShell = """
                    # 4. 生成其它参数 dh.pem
                    sudo openssl dhparam -out "${dir}dh.pem" 2048
                    """;
        } else {
            dhShell = "";
            FileUtils.writeString(dh, Paths.get(dir + "dh.pem").toFile(), StandardCharsets.UTF_8);
        }

        String shell = """
                # 1. 创建CA扩展配置
                cat > "${dir}ca.ext" <<EOF
                basicConstraints = critical,CA:TRUE
                keyUsage = cRLSign, keyCertSign
                subjectKeyIdentifier=hash
                authorityKeyIdentifier=keyid:always,issuer
                EOF
                
                # 生成 CA (为了简化，CA 暂时不设密码，如需设密码逻辑同下)
                sudo openssl genrsa -out "${dir}ca.key" 2048
                sudo openssl req -new -key "${dir}ca.key" -out "${dir}ca.csr" -subj "/C=CN/ST=Beijing/L=Beijing/O=MyOrg/OU=MyOrgOU/CN=${caName}/emailAddress=${email}" -sha256
                sudo openssl x509 -req -days ${caExpire} -in "${dir}ca.csr" -signkey "${dir}ca.key" -out "${dir}ca.crt" -extfile "${dir}ca.ext" -sha256
                
                # 2. 创建服务器扩展配置
                cat > "${dir}server.ext" <<EOF
                basicConstraints=CA:FALSE
                keyUsage = digitalSignature, keyEncipherment
                extendedKeyUsage = serverAuth
                subjectKeyIdentifier=hash
                authorityKeyIdentifier=keyid,issuer
                EOF
                
                # 3. 创建服务器证书 (使用密码加密私钥)
                # 使用 -aes256 算法加密私钥，并通过 -passout 传入密码
                sudo openssl genrsa -aes256 -passout "pass:${serverKey}" -out "${dir}server.key" 2048
                
                # 生成请求时需要使用 -passin 校验私钥密码
                sudo openssl req -new -key "${dir}server.key" -passin "pass:${serverKey}" -out "${dir}server.csr" -subj "/C=CN/ST=Beijing/L=Beijing/O=MyOrg/OU=MyOrgOU/CN=${serverName}/emailAddress=${email}" -sha256
                
                # 签署证书
                sudo openssl x509 -req -days ${caExpire} -in "${dir}server.csr" -CA "${dir}ca.crt" -CAkey "${dir}ca.key" -CAcreateserial -out "${dir}server.crt" -extfile "${dir}server.ext" -sha256
                sudo "${openvpnPath}" --genkey secret "${dir}ta.key"
                
                """;
        shell += dhShell;
        shell += """
                  # 5. 清理与权限
                sudo rm -f "${dir}ca.csr" "${dir}server.csr" "${dir}ca.srl" "${dir}ca.ext" "${dir}server.ext"
                
                sudo chmod 777 "${dir}server.key" "${dir}ca.key"
                sudo chmod 777 "${dir}ca.crt" "${dir}server.crt" "${dir}ta.key"
                sudo chmod 777 "${dir}dh.pem"
                
                """;
         /*
          权限改了之后配置文件无法在启动时覆盖

         */
        return shell;
    }
}
