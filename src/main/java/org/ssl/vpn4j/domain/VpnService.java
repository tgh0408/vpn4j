package org.ssl.vpn4j.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("l_vpnservice")
public class VpnService extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "key_1", type = IdType.INPUT)
    private String key1;

    @TableField("value_1")
    private String value1;

    @TableField("description")
    private String description;

    public VpnService() {
    }

    public VpnService(String key1, String value1) {
        this.key1 = key1;
        this.value1 = value1;
    }

    public VpnService(String key1, String value1, String description) {
        this.key1 = key1;
        this.value1 = value1;
        this.description = description;
    }
}
