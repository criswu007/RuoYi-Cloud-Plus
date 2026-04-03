package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标准地址标签关联表对象 address_standard_tag_rel
 *
 * @author Lion Li
 */
@Data
@TableName("address_standard_tag_rel")
public class StandardAddressTagRel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标准地址ID
     */
    private String standardAddressId;

    /**
     * 标签ID
     */
    private Long tagId;

}
