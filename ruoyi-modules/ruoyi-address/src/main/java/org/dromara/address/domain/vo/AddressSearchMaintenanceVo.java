package org.dromara.address.domain.vo;

import lombok.Data;

import java.util.Date;

public final class AddressSearchMaintenanceVo {

    private AddressSearchMaintenanceVo() {
    }

    @Data
    public static class OverviewVo {

        /**
         * ES 是否可达。
         */
        private Boolean esReachable;

        /**
         * 集群名称。
         */
        private String clusterName;

        /**
         * 集群状态。
         */
        private String clusterStatus;

        /**
         * ES 版本。
         */
        private String esVersion;

        /**
         * 标准地址索引概览。
         */
        private IndexSummaryVo standardIndex;

        /**
         * 安装地址索引概览。
         */
        private IndexSummaryVo installationIndex;

        /**
         * 待处理 repair 数量。
         */
        private Long pendingRepairCount;

        /**
         * 失败 repair 数量。
         */
        private Long failedRepairCount;

        /**
         * 运行中任务。
         */
        private RunningTaskVo runningTask;

        /**
         * Kibana 地址。
         */
        private String kibanaUrl;
    }

    @Data
    public static class IndexSummaryVo {

        /**
         * 索引别名。
         */
        private String alias;

        /**
         * 当前物理索引名。
         */
        private String physicalIndexName;

        /**
         * 文档数。
         */
        private Long docCount;
    }

    @Data
    public static class RunningTaskVo {

        /**
         * 任务 ID。
         */
        private Long id;

        /**
         * 任务类型。
         */
        private String taskType;

        /**
         * 任务状态。
         */
        private String status;

        /**
         * 进度百分比。
         */
        private Integer progressPercent;
    }

    @Data
    public static class TaskVo {

        /**
         * 任务 ID。
         */
        private Long id;

        /**
         * 任务类型。
         */
        private String taskType;

        /**
         * 目标别名。
         */
        private String targetAlias;

        /**
         * 当前物理索引名。
         */
        private String physicalIndexName;

        /**
         * 任务状态。
         */
        private String status;

        /**
         * 当前阶段。
         */
        private String currentPhase;

        /**
         * 总处理量。
         */
        private Long totalCount;

        /**
         * 已处理量。
         */
        private Long processedCount;

        /**
         * 进度百分比。
         */
        private Integer progressPercent;

        /**
         * 错误信息。
         */
        private String errorMessage;

        /**
         * 触发人。
         */
        private String triggerBy;

        /**
         * 开始时间。
         */
        private Date startedTime;

        /**
         * 完成时间。
         */
        private Date finishedTime;

        /**
         * 创建时间。
         */
        private Date createdTime;
    }

    @Data
    public static class RepairTaskVo {

        /**
         * repair 任务 ID。
         */
        private Long id;

        /**
         * 实体类型。
         */
        private String entityType;

        /**
         * 实体 ID。
         */
        private String entityId;

        /**
         * repair 动作。
         */
        private String repairAction;

        /**
         * repair 状态。
         */
        private String status;

        /**
         * 重试次数。
         */
        private Integer retryCount;

        /**
         * 最近更新时间。
         */
        private Date updatedTime;
    }
}
