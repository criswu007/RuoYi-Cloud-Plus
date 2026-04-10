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
    public static class LevelOptionVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 地址类型编码，面向线上 `segm_addr_type.addr_type_id` 契约。
         */
        private String addrTypeId;

        /**
         * 地址类型名称，面向线上 `segm_addr_type.name` 契约。
         */
        private String name;

        /**
         * 地址业务级别（1-19），用于前后端级别交互。
         */
        private Integer addrLevel;

        /**
         * 数据库真实层级 ID，面向线上 `segm_addr_type.level_id` 契约。
         */
        private Integer levelId;
    }

    @Data
    public static class RestrictionOptionVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 字典值，面向线上 `pub_restriction.serial_no` 契约。
         */
        private String value;

        /**
         * 字典中文名称，面向线上 `pub_restriction.desc_china` 契约。
         */
        private String label;
    }

    @Data
    public static class FormOptionsVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 标准地址状态字典。
         */
        private List<RestrictionOptionVo> statusOptions;

        /**
         * 光纤接入方式字典。
         */
        private List<RestrictionOptionVo> addrInTypeFtthOptions;

        /**
         * 光纤接入能力字典。
         */
        private List<RestrictionOptionVo> ftthPonTypeOptions;

        /**
         * 电缆接入方式字典。
         */
        private List<RestrictionOptionVo> addrInTypeLanOptions;

        /**
         * 城乡属性字典。
         */
        private List<RestrictionOptionVo> areaTypeOptions;

        /**
         * 房屋属性字典。
         */
        private List<RestrictionOptionVo> placeTypeOptions;
    }

    @Data
    public static class StationOptionVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 管理站 ID，面向线上 `spc_station.station_id` 契约。
         */
        private String stationId;

        /**
         * 管理站名称，接口展示字段，来源线上 `spc_station.china_name`。
         */
        private String stationName;

        /**
         * 区域 ID，面向线上 `spc_station.region_id` 契约。
         */
        private String regionId;

        /**
         * 管理站类型，面向线上 `spc_station.manage_type` 契约。
         */
        private String manageType;
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
         * 地址业务级别（1-19）。
         */
        private Integer addrLevel;

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
         * 数据库真实层级 ID。
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

        public Integer getAddrLevel() {
            return addrLevel != null ? addrLevel : level;
        }

        public void setAddrLevel(Integer addrLevel) {
            this.addrLevel = addrLevel;
            this.level = addrLevel;
        }

        public Integer getLevelId() {
            return levelId;
        }

        public void setLevelId(Integer levelId) {
            this.levelId = levelId;
        }

        public void setLevel(Integer level) {
            this.level = level;
            this.addrLevel = level;
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
         * 最近执行时间。
         */
        private Date lastExecuteTime;

        /**
         * 最近成功时间。
         */
        private Date lastSuccessTime;

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
         * 关联区域ID集合。
         */
        private List<String> regionIds;

        /**
         * 任务说明。
         */
        private String taskDesc;

        /**
         * 失败原因。
         */
        private String failureReason;

        /**
         * 最近失败原因。
         */
        private String lastFailureReason;

        /**
         * snailjob任务ID。
         */
        private Long snailJobTaskId;
    }

    @Data
    public static class MonitorTaskRunLogVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 主键ID。
         */
        private Long id;

        /**
         * 任务ID。
         */
        private Long taskId;

        /**
         * 触发方式。
         */
        private String triggerMode;

        /**
         * 执行状态。
         */
        private String executeStatus;

        /**
         * 执行消息。
         */
        private String executeMessage;

        /**
         * 扫描数量。
         */
        private Long scannedCount;

        /**
         * 命中数量。
         */
        private Long hitCount;

        /**
         * 新增异常数量。
         */
        private Long createdCount;

        /**
         * 开始时间。
         */
        private Date startedTime;

        /**
         * 结束时间。
         */
        private Date finishedTime;
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
