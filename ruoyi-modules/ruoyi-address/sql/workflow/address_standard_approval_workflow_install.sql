SET NAMES utf8mb4;

DELETE FROM `flow_skip`
WHERE `definition_id` IN (
    SELECT `id`
    FROM (
        SELECT `id`
        FROM `flow_definition`
        WHERE `flow_code` = 'address_standard_approve_v1'
    ) temp
);

DELETE FROM `flow_node`
WHERE `definition_id` IN (
    SELECT `id`
    FROM (
        SELECT `id`
        FROM `flow_definition`
        WHERE `flow_code` = 'address_standard_approve_v1'
    ) temp
);

DELETE FROM `flow_definition`
WHERE `flow_code` = 'address_standard_approve_v1';

INSERT INTO `flow_definition` (
    `id`, `flow_code`, `flow_name`, `model_value`, `category`, `version`, `is_publish`,
    `form_custom`, `form_path`, `activity_status`, `listener_type`, `listener_path`, `ext`,
    `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`, `tenant_id`
) VALUES (
    2026040701001, 'address_standard_approve_v1', '标准地址变更审批', 'CLASSICS', '103', '1', 1,
    'N', '/standard/approvals', 1, NULL, NULL, NULL,
    NOW(), '1', NOW(), '1', '0', '000000'
);

INSERT INTO `flow_node` (
    `id`, `node_type`, `definition_id`, `node_code`, `node_name`, `permission_flag`, `node_ratio`,
    `coordinate`, `any_node_skip`, `listener_type`, `listener_path`, `form_custom`, `form_path`,
    `version`, `create_time`, `create_by`, `update_time`, `update_by`, `ext`, `del_flag`, `tenant_id`
) VALUES
    (2026040701101, 0, 2026040701001, 'stdaddr-start', '开始', NULL, '0.000',
     '200,200|200,200', NULL, NULL, NULL, 'N', '/standard/approvals',
     '1', NOW(), '1', NOW(), '1', '[]', '0', '000000'),
    (2026040701102, 1, 2026040701001, 'stdaddr-apply', '申请人', '', '0.000',
     '360,200|360,200', NULL, '', '', 'N', '/standard/approvals',
     '1', NOW(), '1', NOW(), '1', '[{"code":"ButtonPermissionEnum","value":"back,termination,file,copy"}]', '0', '000000'),
    (2026040701103, 1, 2026040701001, 'stdaddr-approve', '标准地址审批员', 'role:1,role:2026040701301', '0.000',
     '570,200|570,200', NULL, '', '', 'N', '/standard/approvals',
     '1', NOW(), '1', NOW(), '1', '[{"code":"ButtonPermissionEnum","value":"back,termination,copy,transfer,trust,file"}]', '0', '000000'),
    (2026040701104, 2, 2026040701001, 'stdaddr-end', '结束', NULL, '0.000',
     '780,200|780,200', NULL, NULL, NULL, 'N', '/standard/approvals',
     '1', NOW(), '1', NOW(), '1', '[]', '0', '000000');

INSERT INTO `flow_skip` (
    `id`, `definition_id`, `now_node_code`, `now_node_type`, `next_node_code`, `next_node_type`,
    `skip_name`, `skip_type`, `skip_condition`, `coordinate`, `create_time`, `create_by`,
    `update_time`, `update_by`, `del_flag`, `tenant_id`
) VALUES
    (2026040701201, 2026040701001, 'stdaddr-start', 0, 'stdaddr-apply', 1,
     NULL, 'PASS', NULL, '220,200;310,200', NOW(), '1', NOW(), '1', '0', '000000'),
    (2026040701202, 2026040701001, 'stdaddr-apply', 1, 'stdaddr-approve', 1,
     NULL, 'PASS', NULL, '410,200;520,200', NOW(), '1', NOW(), '1', '0', '000000'),
    (2026040701203, 2026040701001, 'stdaddr-approve', 1, 'stdaddr-end', 2,
     NULL, 'PASS', NULL, '620,200;730,200', NOW(), '1', NOW(), '1', '0', '000000');
