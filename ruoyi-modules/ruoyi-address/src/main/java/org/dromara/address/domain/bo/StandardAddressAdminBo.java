package org.dromara.address.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 标准地址管理补充业务对象集合。
 */
public final class StandardAddressAdminBo {

    private StandardAddressAdminBo() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MonitorTaskBo extends BaseEntity {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 主键ID。
         */
        private Long id;

        /**
         * 任务名称。
         */
        private String taskName;

        /**
         * 任务类型。
         */
        private String taskType;

        /**
         * 执行规则。
         */
        private String executeRule;

        /**
         * 执行时间。
         */
        private Date executeTime;

        /**
         * 监控范围。
         */
        private String monitorScope;

        /**
         * 关联规则ID集合。
         */
        private List<Long> relatedRuleIds;

        /**
         * 关联地址ID集合。
         */
        private List<Long> addressIds;

        /**
         * 任务说明。
         */
        private String taskDesc;

        /**
         * 任务状态。
         */
        private String taskStatus;

        /**
         * 失败原因。
         */
        private String failureReason;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class WorkOrderBo extends BaseEntity {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 主键ID。
         */
        private Long id;

        /**
         * 工单号。
         */
        private String workOrderNo;

        /**
         * 异常地址。
         */
        private String abnormalAddress;

        /**
         * 工单状态。
         */
        private String workOrderStatus;

        /**
         * 异常记录ID集合。
         */
        private List<Long> abnormalWarningIds;

        /**
         * 原始地址。
         */
        private String originalAddress;

        /**
         * 明细地址。
         */
        private String detailAddress;

        /**
         * 网格ID。
         */
        private Long gridId;

        /**
         * 修正地址。
         */
        private String correctedAddress;

        /**
         * 驳回原因。
         */
        private String reason;
    }

    @Data
    public static class SelectionSearchBo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 关键字。
         */
        private String keyword;

        /**
         * 最大层级。
         */
        private Integer levelMax;

        /**
         * 限制条数。
         */
        private Integer limit;
    }

    @Data
    public static class SelectionMapSearchBo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 几何类型。
         */
        private String geometryType;

        /**
         * 坐标集合。
         */
        private List<List<Double>> coordinates;
    }

    @Data
    public static class SelectionPreviewBo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 标准地址ID。
         */
        private Long standardAddressId;

        /**
         * 房间ID集合。
         */
        private List<Long> roomIds;
    }
}
