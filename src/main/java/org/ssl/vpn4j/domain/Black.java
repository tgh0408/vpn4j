package org.ssl.vpn4j.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "l_black")
public class Black extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "type")
    private String type;

    @TableField(value = "data_1")
    private String data1;

    @TableField(value = "duration")
    private Integer duration;

    @TableField(value = "release_time")
    private LocalDateTime releaseTime;

    @TableField(value = "notes")
    private String notes;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", exist = false)
    private LocalDateTime updateTime;
}
