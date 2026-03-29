package org.dromara.address.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 标准地址管理补充视图对象集合。
 */
public final class StandardAddressAdminVo {

    private StandardAddressAdminVo() {
    }

    @Data
    public static class BatchPreviewVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 当级名称。
         */
        private String currentName;

        /**
         * 完整名称。
         */
        private String fullName;

        /**
         * 层级。
         */
        private Integer level;

        /**
         * 当级标准地址名称，面向线上 `ADDR_SEGM.segm_name` 契约。
         */
        private String segmName;

        /**
         * 标准地址全称，面向线上 `ADDR_SEGM.stand_name` 契约。
         */
        private String standName;

        /**
         * 地址编码，面向线上 `ADDR_SEGM.segm_no` 契约。
         */
        private String segmNo;

        /**
         * 标准地址编码，面向线上 `ADDR_SEGM.stand_no` 契约。
         */
        private String standNo;

        /**
         * 线上层级 ID。
         */
        private Integer levelId;

        /**
         * 地址类型编码。
         */
        private String segmType;

        public String getSegmName() {
            return segmName != null ? segmName : currentName;
        }

        public void setSegmName(String segmName) {
            this.segmName = segmName;
            this.currentName = segmName;
        }

        public String getStandName() {
            return standName != null ? standName : fullName;
        }

        public void setStandName(String standName) {
            this.standName = standName;
            this.fullName = standName;
        }

        public Integer getLevelId() {
            return levelId != null ? levelId : level;
        }

        public void setLevelId(Integer levelId) {
            this.levelId = levelId;
            this.level = levelId;
        }
    }

    @Data
    public static class MonitorTaskVo implements Serializable {

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
         * 已处理数量。
         */
        private Integer processedCount;

        /**
         * 异常数量。
         */
        private Integer exceptionCount;

        /**
         * 任务状态。
         */
        private String taskStatus;

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
         * 失败原因。
         */
        private String failureReason;
    }

    @Data
    public static class WorkOrderVo implements Serializable {

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
         * 操作时间。
         */
        private Date operationTime;

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
         * 网格名称。
         */
        private String gridName;

        /**
         * 修正地址。
         */
        private String correctedAddress;

        /**
         * 操作日志集合。
         */
        private List<WorkOrderOperateLogVo> operationLogs;
    }

    @Data
    public static class WorkOrderOperateLogVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 操作时间。
         */
        private Date operationTime;

        /**
         * 操作人。
         */
        private String operator;

        /**
         * 操作内容。
         */
        private String operationContent;
    }

    @Data
    public static class SelectionResultVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 标准地址ID。
         */
        private Long standardAddressId;

        /**
         * 完整标签。
         */
        private String fullLabel;

        /**
         * 层级标签。
         */
        private String levelLabel;

        /**
         * 公司标签。
         */
        private String companyLabel;

        /**
         * 经度。
         */
        private Double lon;

        /**
         * 纬度。
         */
        private Double lat;
    }

    @Data
    public static class SelectionRoomVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 房间ID。
         */
        private Long roomId;

        /**
         * 房间号。
         */
        private String roomNo;

        /**
         * 标准地址ID。
         */
        private Long standardAddressId;
    }

    @Data
    public static class SelectionPreviewVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 标准地址ID。
         */
        private Long standardAddressId;

        /**
         * 完整标签。
         */
        private String fullLabel;

        /**
         * 详情说明。
         */
        private String detail;

        /**
         * 安装地址名称。
         */
        private String installAddressName;

        /**
         * 分段地址信息。
         */
        private String segmentedAddressInfo;

        /**
         * 安装地址ID。
         */
        private Long installationAddressId;
    }
}
