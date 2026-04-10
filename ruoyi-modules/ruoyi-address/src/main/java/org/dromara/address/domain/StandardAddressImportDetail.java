package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 标准地址导入行结果实体。
 * 目的：按单条 Excel 记录粒度记录导入前置校验、审批流转和正式执行结果，支撑导入记录列表、批次统计与失败导出。
 * 入参/出参：入库时承载导入行结果快照，查询时输出给导入记录页面、批次详情和失败导出链路。
 * 关键约束：一条 Excel 行对应一条记录；`status` 表示行生命周期状态；删除采用逻辑删除。
 * 异常与副作用：持久化失败时回滚当前事务，会写入 `address_standard_import_detail`。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address_standard_import_detail")
public class StandardAddressImportDetail extends TenantEntity {

    public static final String STATUS_VALIDATE_FAILED = "VALIDATE_FAILED";
    public static final String STATUS_WAITING_APPROVAL = "WAITING_APPROVAL";
    public static final String STATUS_APPROVED_SUCCESS = "APPROVED_SUCCESS";
    public static final String STATUS_REJECTED_FAILED = "REJECTED_FAILED";
    public static final String STATUS_EXECUTE_FAILED = "EXECUTE_FAILED";

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
     * 审批单ID。
     */
    private Long approvalId;

    /**
     * 审批单号。
     */
    private String approvalNo;

    /**
     * 审批状态。
     */
    private String approvalStatus;

    /**
     * Excel 行号。
     */
    private Integer rowNum;

    /**
     * 导入文件名。
     */
    private String fileName;

    /**
     * 是否允许更新。
     */
    private Boolean updateSupport;

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
     * 行状态。
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
