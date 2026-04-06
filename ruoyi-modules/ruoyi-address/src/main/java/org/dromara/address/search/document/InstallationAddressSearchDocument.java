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
 * 安装地址搜索文档。
 * <p>
 * 目的：承载安装地址 ES 检索、筛选与排序所需的核心字段。
 * 关键约束：字段映射需与 `address_installation_search` 索引约定保持一致，确保后续 mapper 与查询网关直接可用。
 * 异常与副作用：仅声明文档结构，不包含持久化、网络调用或数据库副作用。
 * </p>
 */
@Data
@IndexName(value = "address_installation_search", aliasName = "address_installation_search")
public class InstallationAddressSearchDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ES 文档主键。
     */
    @IndexId
    private String documentId;

    /**
     * 安装地址主键。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String setAddrId;

    /**
     * 关联标准地址主键。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String segmId;

    /**
     * 安装位置描述。
     */
    @IndexField(fieldType = FieldType.TEXT, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String setAddrName;

    /**
     * 关联标准地址完整名称。
     */
    @IndexField(fieldType = FieldType.TEXT, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String standName;

    /**
     * 安装地址编号。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String setAddrNo;

    /**
     * 安装地址类型。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String setType;

    /**
     * 组织 ID。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String orgId;

    /**
     * 关联状态。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String associationStatus;

    /**
     * 关联标准地址类型。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String segmType;

    /**
     * 区域 ID。
     */
    @IndexField(fieldType = FieldType.KEYWORD)
    private String regionId;

    /**
     * 创建时间。
     */
    @IndexField(fieldType = FieldType.DATE)
    private Date createDate;
}
