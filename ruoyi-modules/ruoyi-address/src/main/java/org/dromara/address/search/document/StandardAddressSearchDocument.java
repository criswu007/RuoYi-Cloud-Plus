package org.dromara.address.search.document;

import lombok.Data;
import org.dromara.easyes.annotation.IndexField;
import org.dromara.easyes.annotation.IndexId;
import org.dromara.easyes.annotation.IndexName;
import org.dromara.easyes.annotation.rely.FieldType;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 标准地址搜索文档。
 * <p>
 * 目的：承载标准地址 ES 检索、筛选与排序所需的核心字段。
 * 关键约束：字段映射需与 `address_standard_search` 索引约定保持一致，避免后续读写链路使用默认索引元数据。
 * 异常与副作用：仅作为文档模型声明，不直接执行 I/O、数据库或远程调用。
 * </p>
 */
@Data
@IndexName(value = "address_standard_search", aliasName = "address_standard_search")
public class StandardAddressSearchDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ES 文档主键。
     */
    @IndexId
    private String documentId;

    /**
     * 标准地址主键。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String segmId;

    /**
     * 父标准地址主键。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String parentSegmId;

    /**
     * 当级名称。
     */
    @IndexField(fieldType = FieldType.TEXT, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String segmName;

    /**
     * 标准地址全称。
     */
    @IndexField(fieldType = FieldType.TEXT, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String standName;

    /**
     * 地址编码。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String segmNo;

    /**
     * 标准地址编码。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String standNo;

    /**
     * 区域 ID。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String regionId;

    /**
     * 业务地址层级。
     */
    @IndexField(fieldType = FieldType.INTEGER)
    private Integer addrLevel;

    /**
     * 地址类型编码。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String segmType;

    /**
     * 行政区 ID。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String districtId;

    /**
     * 服务区域 ID。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String serviceRegionId;

    /**
     * 状态（0正常 1停用）。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String status;

    /**
     * 创建时间。
     */
    @IndexField(fieldType = FieldType.DATE)
    private Date createDate;
}
