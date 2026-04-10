SET NAMES utf8mb4;

-- 标准地址非标监控升级脚本
-- 目的：补齐模板化规则、任务主表与异常记录快照字段，支撑“规则配置 -> 任务管理 -> 异常预警”闭环。
-- 关键约束：本脚本仅覆盖当前实现已使用的主表字段，关系表与运行日志表后续按实现阶段继续扩展。

CREATE TABLE IF NOT EXISTS `address_standard_monitor_rule` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    `name` varchar(128) NOT NULL COMMENT '规则名称',
    `rule_code` varchar(64) DEFAULT NULL COMMENT '规则编码',
    `rule_template` varchar(64) DEFAULT NULL COMMENT '规则模板',
    `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `severity` varchar(32) DEFAULT NULL COMMENT '严重等级',
    `priority` int DEFAULT NULL COMMENT '优先级',
    `dedup_hours` int DEFAULT NULL COMMENT '去重窗口小时数',
    `config_json` longtext COMMENT '模板配置JSON',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_monitor_rule_code` (`rule_code`),
    KEY `idx_monitor_rule_status` (`status`),
    KEY `idx_monitor_rule_template` (`rule_template`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='标准地址监控规则表';

DROP PROCEDURE IF EXISTS `add_column_if_missing`;

ALTER TABLE `address_standard_monitor_rule`
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    MODIFY COLUMN `name` varchar(128) NOT NULL COMMENT '规则名称',
    MODIFY COLUMN `rule_code` varchar(64) DEFAULT NULL COMMENT '规则编码',
    MODIFY COLUMN `rule_template` varchar(64) DEFAULT NULL COMMENT '规则模板',
    MODIFY COLUMN `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    MODIFY COLUMN `severity` varchar(32) DEFAULT NULL COMMENT '严重等级',
    MODIFY COLUMN `priority` int DEFAULT NULL COMMENT '优先级',
    MODIFY COLUMN `dedup_hours` int DEFAULT NULL COMMENT '去重窗口小时数',
    MODIFY COLUMN `config_json` longtext COMMENT '模板配置JSON',
    MODIFY COLUMN `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    MODIFY COLUMN `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    MODIFY COLUMN `create_by` bigint DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    MODIFY COLUMN `update_by` bigint DEFAULT NULL COMMENT '更新者',
    MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    COMMENT = '标准地址监控规则表';

ALTER TABLE `address_standard_monitor_task`
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    MODIFY COLUMN `task_name` varchar(128) NOT NULL COMMENT '任务名称',
    MODIFY COLUMN `task_type` varchar(32) DEFAULT NULL COMMENT '任务类型',
    MODIFY COLUMN `execute_rule` varchar(128) DEFAULT NULL COMMENT '执行规则',
    MODIFY COLUMN `monitor_scope` varchar(32) DEFAULT NULL COMMENT '监控范围',
    MODIFY COLUMN `task_status` varchar(32) DEFAULT NULL COMMENT '任务状态',
    MODIFY COLUMN `task_desc` varchar(500) DEFAULT NULL COMMENT '任务说明',
    MODIFY COLUMN `snail_job_task_id` bigint DEFAULT NULL COMMENT 'snailjob任务ID',
    MODIFY COLUMN `last_execute_time` datetime DEFAULT NULL COMMENT '最近执行时间',
    MODIFY COLUMN `last_success_time` datetime DEFAULT NULL COMMENT '最近成功时间',
    MODIFY COLUMN `last_failure_reason` varchar(500) DEFAULT NULL COMMENT '最近失败原因',
    MODIFY COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    MODIFY COLUMN `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    MODIFY COLUMN `create_by` bigint DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    MODIFY COLUMN `update_by` bigint DEFAULT NULL COMMENT '更新者',
    MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    COMMENT = '标准地址监控任务主表';

ALTER TABLE `address_standard_monitor_task_rule_rel`
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    MODIFY COLUMN `task_id` bigint NOT NULL COMMENT '任务ID',
    MODIFY COLUMN `rule_id` bigint NOT NULL COMMENT '规则ID',
    MODIFY COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    MODIFY COLUMN `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    MODIFY COLUMN `create_by` bigint DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    MODIFY COLUMN `update_by` bigint DEFAULT NULL COMMENT '更新者',
    MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    COMMENT = '标准地址监控任务规则关系表';

ALTER TABLE `address_standard_monitor_task_scope_rel`
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    MODIFY COLUMN `task_id` bigint NOT NULL COMMENT '任务ID',
    MODIFY COLUMN `scope_type` varchar(32) NOT NULL COMMENT '范围类型',
    MODIFY COLUMN `scope_value` varchar(64) NOT NULL COMMENT '范围值',
    MODIFY COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    MODIFY COLUMN `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    MODIFY COLUMN `create_by` bigint DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    MODIFY COLUMN `update_by` bigint DEFAULT NULL COMMENT '更新者',
    MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    COMMENT = '标准地址监控任务范围关系表';

ALTER TABLE `address_standard_monitor_task_run_log`
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    MODIFY COLUMN `task_id` bigint NOT NULL COMMENT '任务ID',
    MODIFY COLUMN `trigger_mode` varchar(32) DEFAULT NULL COMMENT '触发方式',
    MODIFY COLUMN `execute_status` varchar(32) DEFAULT NULL COMMENT '执行状态',
    MODIFY COLUMN `execute_message` varchar(500) DEFAULT NULL COMMENT '执行消息',
    MODIFY COLUMN `scanned_count` bigint DEFAULT 0 COMMENT '扫描数量',
    MODIFY COLUMN `hit_count` bigint DEFAULT 0 COMMENT '命中数量',
    MODIFY COLUMN `created_count` bigint DEFAULT 0 COMMENT '新增异常数量',
    MODIFY COLUMN `started_time` datetime DEFAULT NULL COMMENT '开始时间',
    MODIFY COLUMN `finished_time` datetime DEFAULT NULL COMMENT '结束时间',
    MODIFY COLUMN `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    MODIFY COLUMN `create_by` bigint DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    MODIFY COLUMN `update_by` bigint DEFAULT NULL COMMENT '更新者',
    MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    COMMENT = '标准地址监控任务运行日志表';

ALTER TABLE `address_standard_monitor_record`
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    MODIFY COLUMN `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    MODIFY COLUMN `standard_address_id` bigint DEFAULT NULL COMMENT '标准地址ID',
    MODIFY COLUMN `rule_id` bigint DEFAULT NULL COMMENT '规则ID',
    MODIFY COLUMN `stand_name_snapshot` varchar(500) DEFAULT NULL COMMENT '标准地址名称快照',
    MODIFY COLUMN `region_id_snapshot` varchar(64) DEFAULT NULL COMMENT '区域快照',
    MODIFY COLUMN `rule_name_snapshot` varchar(128) DEFAULT NULL COMMENT '规则名称快照',
    MODIFY COLUMN `rule_template_snapshot` varchar(64) DEFAULT NULL COMMENT '规则模板快照',
    MODIFY COLUMN `task_id` bigint DEFAULT NULL COMMENT '来源任务ID',
    MODIFY COLUMN `task_run_log_id` bigint DEFAULT NULL COMMENT '来源任务运行日志ID',
    MODIFY COLUMN `task_name_snapshot` varchar(128) DEFAULT NULL COMMENT '来源任务名称快照',
    MODIFY COLUMN `severity` varchar(32) DEFAULT NULL COMMENT '严重等级',
    MODIFY COLUMN `hit_detail_json` longtext COMMENT '命中详情JSON',
    MODIFY COLUMN `dedup_key` varchar(256) DEFAULT NULL COMMENT '去重键',
    MODIFY COLUMN `first_detected_time` datetime DEFAULT NULL COMMENT '首次发现时间',
    MODIFY COLUMN `last_detected_time` datetime DEFAULT NULL COMMENT '最近发现时间',
    MODIFY COLUMN `hit_count` int DEFAULT 1 COMMENT '命中次数',
    MODIFY COLUMN `status` char(1) DEFAULT '0' COMMENT '状态（0待处理 1已忽略 2已处理）',
    MODIFY COLUMN `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `process_by` varchar(64) DEFAULT NULL COMMENT '处理人',
    MODIFY COLUMN `process_time` datetime DEFAULT NULL COMMENT '处理时间',
    MODIFY COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    MODIFY COLUMN `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    MODIFY COLUMN `create_by` bigint DEFAULT NULL COMMENT '创建者',
    MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    MODIFY COLUMN `update_by` bigint DEFAULT NULL COMMENT '更新者',
    MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    COMMENT = '标准地址监控异常记录表';
DELIMITER $$
CREATE PROCEDURE `add_column_if_missing`(
    IN p_table_name varchar(64),
    IN p_column_name varchar(64),
    IN p_alter_sql text
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = p_alter_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL `add_column_if_missing`(
    'address_standard_monitor_rule',
    'rule_code',
    'ALTER TABLE `address_standard_monitor_rule` ADD COLUMN `rule_code` varchar(64) DEFAULT NULL COMMENT ''规则编码'' AFTER `name`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_rule',
    'rule_template',
    'ALTER TABLE `address_standard_monitor_rule` ADD COLUMN `rule_template` varchar(64) DEFAULT NULL COMMENT ''规则模板'' AFTER `rule_code`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_rule',
    'severity',
    'ALTER TABLE `address_standard_monitor_rule` ADD COLUMN `severity` varchar(32) DEFAULT NULL COMMENT ''严重等级'' AFTER `status`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_rule',
    'priority',
    'ALTER TABLE `address_standard_monitor_rule` ADD COLUMN `priority` int DEFAULT NULL COMMENT ''优先级'' AFTER `severity`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_rule',
    'dedup_hours',
    'ALTER TABLE `address_standard_monitor_rule` ADD COLUMN `dedup_hours` int DEFAULT NULL COMMENT ''去重窗口小时数'' AFTER `priority`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_rule',
    'config_json',
    'ALTER TABLE `address_standard_monitor_rule` ADD COLUMN `config_json` longtext COMMENT ''模板配置JSON'' AFTER `dedup_hours`'
);

CREATE TABLE IF NOT EXISTS `address_standard_monitor_task` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    `task_name` varchar(128) NOT NULL COMMENT '任务名称',
    `task_type` varchar(32) DEFAULT NULL COMMENT '任务类型',
    `execute_rule` varchar(128) DEFAULT NULL COMMENT '执行规则',
    `monitor_scope` varchar(32) DEFAULT NULL COMMENT '监控范围',
    `task_status` varchar(32) DEFAULT NULL COMMENT '任务状态',
    `task_desc` varchar(500) DEFAULT NULL COMMENT '任务说明',
    `snail_job_task_id` bigint DEFAULT NULL COMMENT 'snailjob任务ID',
    `last_execute_time` datetime DEFAULT NULL COMMENT '最近执行时间',
    `last_success_time` datetime DEFAULT NULL COMMENT '最近成功时间',
    `last_failure_reason` varchar(500) DEFAULT NULL COMMENT '最近失败原因',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_monitor_task_status` (`task_status`),
    KEY `idx_monitor_task_scope` (`monitor_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='标准地址监控任务主表';

CREATE TABLE IF NOT EXISTS `address_standard_monitor_task_rule_rel` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    `task_id` bigint NOT NULL COMMENT '任务ID',
    `rule_id` bigint NOT NULL COMMENT '规则ID',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_monitor_task_rule` (`task_id`, `rule_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='标准地址监控任务规则关系表';

CREATE TABLE IF NOT EXISTS `address_standard_monitor_task_scope_rel` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    `task_id` bigint NOT NULL COMMENT '任务ID',
    `scope_type` varchar(32) NOT NULL COMMENT '范围类型',
    `scope_value` varchar(64) NOT NULL COMMENT '范围值',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_monitor_task_scope` (`task_id`, `scope_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='标准地址监控任务范围关系表';

CREATE TABLE IF NOT EXISTS `address_standard_monitor_task_run_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    `task_id` bigint NOT NULL COMMENT '任务ID',
    `trigger_mode` varchar(32) DEFAULT NULL COMMENT '触发方式',
    `execute_status` varchar(32) DEFAULT NULL COMMENT '执行状态',
    `execute_message` varchar(500) DEFAULT NULL COMMENT '执行消息',
    `scanned_count` bigint DEFAULT 0 COMMENT '扫描数量',
    `hit_count` bigint DEFAULT 0 COMMENT '命中数量',
    `created_count` bigint DEFAULT 0 COMMENT '新增异常数量',
    `started_time` datetime DEFAULT NULL COMMENT '开始时间',
    `finished_time` datetime DEFAULT NULL COMMENT '结束时间',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_monitor_task_run_log` (`task_id`, `started_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='标准地址监控任务运行日志表';

CREATE TABLE IF NOT EXISTS `address_standard_monitor_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
    `standard_address_id` bigint DEFAULT NULL COMMENT '标准地址ID',
    `rule_id` bigint DEFAULT NULL COMMENT '规则ID',
    `stand_name_snapshot` varchar(500) DEFAULT NULL COMMENT '标准地址名称快照',
    `region_id_snapshot` varchar(64) DEFAULT NULL COMMENT '区域快照',
    `rule_name_snapshot` varchar(128) DEFAULT NULL COMMENT '规则名称快照',
    `rule_template_snapshot` varchar(64) DEFAULT NULL COMMENT '规则模板快照',
    `task_id` bigint DEFAULT NULL COMMENT '来源任务ID',
    `task_run_log_id` bigint DEFAULT NULL COMMENT '来源任务运行日志ID',
    `task_name_snapshot` varchar(128) DEFAULT NULL COMMENT '来源任务名称快照',
    `severity` varchar(32) DEFAULT NULL COMMENT '严重等级',
    `hit_detail_json` longtext COMMENT '命中详情JSON',
    `dedup_key` varchar(256) DEFAULT NULL COMMENT '去重键',
    `first_detected_time` datetime DEFAULT NULL COMMENT '首次发现时间',
    `last_detected_time` datetime DEFAULT NULL COMMENT '最近发现时间',
    `hit_count` int DEFAULT 1 COMMENT '命中次数',
    `status` char(1) DEFAULT '0' COMMENT '状态（0待处理 1已忽略 2已处理）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `process_by` varchar(64) DEFAULT NULL COMMENT '处理人',
    `process_time` datetime DEFAULT NULL COMMENT '处理时间',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
    `create_by` bigint DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_monitor_record_status` (`status`),
    KEY `idx_monitor_record_task` (`task_id`),
    KEY `idx_monitor_record_rule` (`rule_id`),
    KEY `idx_monitor_record_dedup` (`dedup_key`),
    KEY `idx_monitor_record_region_status` (`region_id_snapshot`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='标准地址监控异常记录表';

CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'stand_name_snapshot',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `stand_name_snapshot` varchar(500) DEFAULT NULL COMMENT ''标准地址名称快照'' AFTER `standard_address_id`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'region_id_snapshot',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `region_id_snapshot` varchar(64) DEFAULT NULL COMMENT ''区域快照'' AFTER `stand_name_snapshot`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'rule_name_snapshot',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `rule_name_snapshot` varchar(128) DEFAULT NULL COMMENT ''规则名称快照'' AFTER `rule_id`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'rule_template_snapshot',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `rule_template_snapshot` varchar(64) DEFAULT NULL COMMENT ''规则模板快照'' AFTER `rule_name_snapshot`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'task_id',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `task_id` bigint DEFAULT NULL COMMENT ''来源任务ID'' AFTER `rule_template_snapshot`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'task_run_log_id',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `task_run_log_id` bigint DEFAULT NULL COMMENT ''来源任务运行日志ID'' AFTER `task_id`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'task_name_snapshot',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `task_name_snapshot` varchar(128) DEFAULT NULL COMMENT ''来源任务名称快照'' AFTER `task_run_log_id`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'severity',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `severity` varchar(32) DEFAULT NULL COMMENT ''严重等级'' AFTER `task_name_snapshot`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'hit_detail_json',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `hit_detail_json` longtext COMMENT ''命中详情JSON'' AFTER `severity`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'dedup_key',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `dedup_key` varchar(256) DEFAULT NULL COMMENT ''去重键'' AFTER `hit_detail_json`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'first_detected_time',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `first_detected_time` datetime DEFAULT NULL COMMENT ''首次发现时间'' AFTER `dedup_key`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'last_detected_time',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `last_detected_time` datetime DEFAULT NULL COMMENT ''最近发现时间'' AFTER `first_detected_time`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'hit_count',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `hit_count` int DEFAULT 1 COMMENT ''命中次数'' AFTER `last_detected_time`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'process_by',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `process_by` varchar(64) DEFAULT NULL COMMENT ''处理人'' AFTER `remark`'
);
CALL `add_column_if_missing`(
    'address_standard_monitor_record',
    'process_time',
    'ALTER TABLE `address_standard_monitor_record` ADD COLUMN `process_time` datetime DEFAULT NULL COMMENT ''处理时间'' AFTER `process_by`'
);

DROP PROCEDURE IF EXISTS `add_column_if_missing`;
