package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 标准地址导入失败明细实体。
 * 目的：按单条失败数据粒度记录失败原因、原始值和所属批次，支撑失败明细列表与导出。
 * 入参/出参：入库时承载失败记录明细，查询时输出给导入记录页面与导出链路。
 * 关键约束：仅记录失败数据；按批次主记录关联；删除采用逻辑删除。
 * 异常与副作用：持久化失败时回滚当前事务，会写入导入失败明细表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address_standard_import_fail_detail")
public class StandardAddressImportFailDetail extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID。
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 所属导入批次ID。
     */
    private Long batchId;

    /**
     * Excel 行号。
     */
    private Integer rowNum;

    /**
     * 父级标准地址名称。
     */
    private String parentStandName;

    /**
     * 当级名称。
     */
    private String segmName;

    /**
     * 地址类型编码。
     */
    private String segmType;

    /**
     * 业务级别。
     */
    private Integer addrLevel;

    /**
     * 状态。
     */
    private String status;

    /**
     * 失败原因。
     */
    private String failReason;

    /**
     * 原始导入数据快照。
     */
    private String rawPayload;

    /**
     * 删除标志。
     */
    @TableLogic
    private String delFlag;
}
