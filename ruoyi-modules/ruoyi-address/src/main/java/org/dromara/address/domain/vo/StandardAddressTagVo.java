package org.dromara.address.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.address.domain.StandardAddressTag;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 地址标签视图对象。
 * 目的：向前端返回标签基础信息。
 */
@Data
@AutoMapper(target = StandardAddressTag.class)
public class StandardAddressTagVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签编码
     */
    private String code;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;
}
