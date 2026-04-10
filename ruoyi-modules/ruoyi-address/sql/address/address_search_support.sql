SET NAMES utf8mb4;

create table if not exists address_search_sync_log (
    id bigint primary key comment '主键ID',
    business_type varchar(32) not null comment '业务类型',
    entity_type varchar(32) not null comment '实体类型',
    entity_id varchar(32) not null comment '实体ID',
    phase varchar(32) not null comment '同步阶段',
    success_flag char(1) not null comment '是否成功',
    error_message varchar(1000) comment '错误信息',
    created_time datetime not null comment '创建时间'
) comment='地址搜索同步日志表';

create table if not exists address_search_repair_task (
    id bigint primary key comment '主键ID',
    entity_type varchar(32) not null comment '实体类型',
    entity_id varchar(32) not null comment '实体ID',
    repair_action varchar(32) not null comment '修复动作',
    payload_json text comment '修复参数JSON',
    status varchar(32) not null comment '任务状态',
    retry_count int not null comment '重试次数',
    created_time datetime not null comment '创建时间',
    updated_time datetime not null comment '更新时间'
) comment='地址搜索修复任务表';

create table if not exists address_search_maintenance_task (
    id bigint primary key comment '主键ID',
    task_type varchar(32) not null comment '维护任务类型',
    target_alias varchar(128) not null comment '目标索引别名',
    physical_index_name varchar(128) comment '物理索引名称',
    status varchar(32) not null comment '任务状态',
    current_phase varchar(32) not null comment '当前阶段',
    total_count bigint not null default 0 comment '总记录数',
    processed_count bigint not null default 0 comment '已处理数量',
    progress_percent int not null default 0 comment '进度百分比',
    error_message varchar(1000) comment '错误信息',
    trigger_by varchar(64) comment '触发人',
    started_time datetime comment '开始时间',
    finished_time datetime comment '结束时间',
    created_time datetime not null comment '创建时间',
    updated_time datetime not null comment '更新时间'
) comment='地址搜索维护任务表';

ALTER TABLE `address_search_sync_log`
    MODIFY COLUMN `id` bigint NOT NULL COMMENT '主键ID',
    MODIFY COLUMN `business_type` varchar(32) NOT NULL COMMENT '业务类型',
    MODIFY COLUMN `entity_type` varchar(32) NOT NULL COMMENT '实体类型',
    MODIFY COLUMN `entity_id` varchar(32) NOT NULL COMMENT '实体ID',
    MODIFY COLUMN `phase` varchar(32) NOT NULL COMMENT '同步阶段',
    MODIFY COLUMN `success_flag` char(1) NOT NULL COMMENT '是否成功',
    MODIFY COLUMN `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
    MODIFY COLUMN `created_time` datetime NOT NULL COMMENT '创建时间',
    COMMENT = '地址搜索同步日志表';

ALTER TABLE `address_search_repair_task`
    MODIFY COLUMN `id` bigint NOT NULL COMMENT '主键ID',
    MODIFY COLUMN `entity_type` varchar(32) NOT NULL COMMENT '实体类型',
    MODIFY COLUMN `entity_id` varchar(32) NOT NULL COMMENT '实体ID',
    MODIFY COLUMN `repair_action` varchar(32) NOT NULL COMMENT '修复动作',
    MODIFY COLUMN `payload_json` text COMMENT '修复参数JSON',
    MODIFY COLUMN `status` varchar(32) NOT NULL COMMENT '任务状态',
    MODIFY COLUMN `retry_count` int NOT NULL COMMENT '重试次数',
    MODIFY COLUMN `created_time` datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN `updated_time` datetime NOT NULL COMMENT '更新时间',
    COMMENT = '地址搜索修复任务表';

ALTER TABLE `address_search_maintenance_task`
    MODIFY COLUMN `id` bigint NOT NULL COMMENT '主键ID',
    MODIFY COLUMN `task_type` varchar(32) NOT NULL COMMENT '维护任务类型',
    MODIFY COLUMN `target_alias` varchar(128) NOT NULL COMMENT '目标索引别名',
    MODIFY COLUMN `physical_index_name` varchar(128) DEFAULT NULL COMMENT '物理索引名称',
    MODIFY COLUMN `status` varchar(32) NOT NULL COMMENT '任务状态',
    MODIFY COLUMN `current_phase` varchar(32) NOT NULL COMMENT '当前阶段',
    MODIFY COLUMN `total_count` bigint NOT NULL DEFAULT 0 COMMENT '总记录数',
    MODIFY COLUMN `processed_count` bigint NOT NULL DEFAULT 0 COMMENT '已处理数量',
    MODIFY COLUMN `progress_percent` int NOT NULL DEFAULT 0 COMMENT '进度百分比',
    MODIFY COLUMN `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
    MODIFY COLUMN `trigger_by` varchar(64) DEFAULT NULL COMMENT '触发人',
    MODIFY COLUMN `started_time` datetime DEFAULT NULL COMMENT '开始时间',
    MODIFY COLUMN `finished_time` datetime DEFAULT NULL COMMENT '结束时间',
    MODIFY COLUMN `created_time` datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN `updated_time` datetime NOT NULL COMMENT '更新时间',
    COMMENT = '地址搜索维护任务表';
