package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典与约束解释表 `pub_restriction` 实体。
 */
@Data
@TableName("pub_restriction")
public class PubRestriction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典序号。
     */
    @TableId(value = "serial_no")
    private String serialNo;

    /**
     * 字典关键字。
     */
    private String keyword;

    /**
     * 字典名称。
     */
    private String name;

    /**
     * 字典编码。
     */
    private String code;
}
