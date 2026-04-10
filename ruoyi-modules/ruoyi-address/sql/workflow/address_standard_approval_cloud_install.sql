SET NAMES utf8mb4;

INSERT INTO `sys_role` (
    `role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
    `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_dept`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`
)
SELECT
    2026040701301, '000000', '标准地址审批员', 'address_approver', 2, '1',
    1, 1, '0', '0', 103,
    1, NOW(), NULL, NULL, '标准地址审批流程专用角色'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_role`
    WHERE `tenant_id` = '000000'
      AND `role_key` = 'address_approver'
      AND `del_flag` = '0'
);
