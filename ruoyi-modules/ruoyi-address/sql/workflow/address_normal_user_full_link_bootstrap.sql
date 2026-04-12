SET NAMES utf8mb4;

-- 标准地址模块普通用户全链路联调初始化脚本
-- 目的：
-- 1. 初始化普通录入员/审批员角色；
-- 2. 初始化地址模块 `address:*` 权限菜单；
-- 3. 绑定角色与菜单；
-- 4. 将示例普通用户 `test` / `test1` 绑定到地址角色，便于直接联调。
--
-- 执行前置：
-- 1. 已初始化 `ry-cloud` 基础库（含 `sys_role` / `sys_menu` / `sys_role_menu` / `sys_user_role`）。
-- 2. 若要走“提交审批 -> 审批通过/驳回”闭环，还需先执行：
--    a. `/script/sql/ry-workflow.sql`
--    b. `/ruoyi-modules/ruoyi-address/sql/workflow/address_standard_approval_workflow_install.sql`
--
-- 关键约束：
-- 1. `ruoyi-address-ui` 当前为静态路由，不依赖后台下发菜单树；本脚本主要解决 `getInfo` 权限点、
--    `@SaCheckPermission("address:*")` 与 workflow 审批角色问题。
-- 2. 当前脚本不处理用户 `regionId` 登录态扩展，后续若启用区域数据范围，需要在登录链路补充 token extra。

-- 一、初始化角色

INSERT INTO `sys_role` (
    `role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
    `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_dept`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`
)
SELECT
    2026041101302, '000000', '标准地址录入员', 'address_operator', 10, '1',
    1, 1, '0', '0', 103,
    1, NOW(), NULL, NULL, '标准地址模块普通录入用户'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_role`
    WHERE `tenant_id` = '000000'
      AND `role_key` = 'address_operator'
      AND `del_flag` = '0'
);

INSERT INTO `sys_role` (
    `role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
    `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_dept`,
    `create_by`, `create_time`, `update_by`, `update_time`, `remark`
)
SELECT
    2026040701301, '000000', '标准地址审批员', 'address_approver', 11, '1',
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

-- 二、初始化地址模块权限菜单
-- 说明：
-- 1. 菜单统一挂在隐藏目录“地址模块权限”下，避免污染系统主后台侧边栏；
-- 2. `visible = 1` 表示隐藏，仅用于权限承载和角色授权；
-- 3. `component` 为权限占位路径，当前不用于 `ruoyi-address-ui` 路由渲染。

INSERT IGNORE INTO `sys_menu` (
    `menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
    `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
) VALUES
    (2026041101000, '地址模块权限', 0, 99, 'addressHidden', NULL, '', 1, 1, 'M', '1', '0', '', 'guide', 103, 1, NOW(), NULL, NULL, '标准地址模块隐藏权限目录'),

    (2026041101100, '标准地址列表', 2026041101000, 1, 'standard/list', 'address/standard/list', '', 1, 1, 'C', '1', '0', 'address:standard:list', '#', 103, 1, NOW(), NULL, NULL, '标准地址列表权限'),
    (2026041101101, '标准地址查询', 2026041101100, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:standard:query', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101102, '标准地址新增', 2026041101100, 2, '', '', '', 1, 1, 'F', '1', '0', 'address:standard:add', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101103, '标准地址修改', 2026041101100, 3, '', '', '', 1, 1, 'F', '1', '0', 'address:standard:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101104, '标准地址删除', 2026041101100, 4, '', '', '', 1, 1, 'F', '1', '0', 'address:standard:remove', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101105, '标准地址导出', 2026041101100, 5, '', '', '', 1, 1, 'F', '1', '0', 'address:standard:export', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101106, '标准地址导入', 2026041101100, 6, '', '', '', 1, 1, 'F', '1', '0', 'address:standard:import', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101107, '标准地址合并', 2026041101100, 7, '', '', '', 1, 1, 'F', '1', '0', 'address:standard:merge', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101108, '标准地址拆分', 2026041101100, 8, '', '', '', 1, 1, 'F', '1', '0', 'address:standard:split', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041101200, '待审批地址管理', 2026041101000, 2, 'standard/approvals', 'address/standard/approvals', '', 1, 1, 'C', '1', '0', 'address:standard:list', '#', 103, 1, NOW(), NULL, NULL, '待审批地址管理页入口'),

    (2026041101300, '标准地址合并页', 2026041101000, 3, 'standard/merge', 'address/standard/merge', '', 1, 1, 'C', '1', '0', 'address:standard:merge', '#', 103, 1, NOW(), NULL, NULL, '标准地址合并页入口'),

    (2026041101400, '标准地址拆分页', 2026041101000, 4, 'standard/split', 'address/standard/split', '', 1, 1, 'C', '1', '0', 'address:standard:split', '#', 103, 1, NOW(), NULL, NULL, '标准地址拆分页入口'),

    (2026041101500, '安装地址列表', 2026041101000, 5, 'installation/list', 'address/installation/list', '', 1, 1, 'C', '1', '0', 'address:installation:list', '#', 103, 1, NOW(), NULL, NULL, '安装地址列表权限'),
    (2026041101501, '安装地址查询', 2026041101500, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:installation:query', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101502, '安装地址新增', 2026041101500, 2, '', '', '', 1, 1, 'F', '1', '0', 'address:installation:add', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101503, '安装地址修改', 2026041101500, 3, '', '', '', 1, 1, 'F', '1', '0', 'address:installation:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101504, '安装地址删除', 2026041101500, 4, '', '', '', 1, 1, 'F', '1', '0', 'address:installation:remove', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041101600, '选址平台', 2026041101000, 6, 'selection/tools', 'address/selection/tools', '', 1, 1, 'C', '1', '0', 'address:selection:query', '#', 103, 1, NOW(), NULL, NULL, '选址平台页入口'),
    (2026041101601, '选址平台创建', 2026041101600, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:selection:create', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041101700, '导入记录查询', 2026041101000, 7, 'import/records', 'address/import/records', '', 1, 1, 'C', '1', '0', 'address:import:record:list', '#', 103, 1, NOW(), NULL, NULL, '导入记录页入口'),
    (2026041101701, '导入记录详情', 2026041101700, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:import:record:query', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101702, '导入记录导出', 2026041101700, 2, '', '', '', 1, 1, 'F', '1', '0', 'address:import:record:export', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041101800, '地址操作日志', 2026041101000, 8, 'operation/logs', 'address/operation/logs', '', 1, 1, 'C', '1', '0', 'address:operation:log:list', '#', 103, 1, NOW(), NULL, NULL, '地址操作日志页入口'),
    (2026041101801, '地址操作日志详情', 2026041101800, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:operation:log:query', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041101900, '标签库管理', 2026041101000, 9, 'standard/labels', 'address/standard/labels', '', 1, 1, 'C', '1', '0', 'address:tag:list', '#', 103, 1, NOW(), NULL, NULL, '标签库管理页入口'),
    (2026041101901, '标签查询', 2026041101900, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:tag:query', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101902, '标签新增', 2026041101900, 2, '', '', '', 1, 1, 'F', '1', '0', 'address:tag:add', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101903, '标签修改', 2026041101900, 3, '', '', '', 1, 1, 'F', '1', '0', 'address:tag:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101904, '标签删除', 2026041101900, 4, '', '', '', 1, 1, 'F', '1', '0', 'address:tag:remove', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041101905, '标签绑定', 2026041101900, 5, '', '', '', 1, 1, 'F', '1', '0', 'address:tag:bind', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041102000, '管理站管理', 2026041101000, 10, 'management/station', 'address/management/station', '', 1, 1, 'C', '1', '0', 'address:station:list', '#', 103, 1, NOW(), NULL, NULL, '管理站管理页入口'),
    (2026041102001, '管理站查询', 2026041102000, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:station:query', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102002, '管理站新增', 2026041102000, 2, '', '', '', 1, 1, 'F', '1', '0', 'address:station:add', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102003, '管理站修改', 2026041102000, 3, '', '', '', 1, 1, 'F', '1', '0', 'address:station:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102004, '管理站删除', 2026041102000, 4, '', '', '', 1, 1, 'F', '1', '0', 'address:station:remove', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041102100, '地址属性管理', 2026041101000, 11, 'standard/attributes', 'address/standard/attributes', '', 1, 1, 'C', '1', '0', 'address:attribute:list', '#', 103, 1, NOW(), NULL, NULL, '隐藏地址属性权限页'),
    (2026041102101, '地址属性查询', 2026041102100, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:attribute:query', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102102, '地址属性新增', 2026041102100, 2, '', '', '', 1, 1, 'F', '1', '0', 'address:attribute:add', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102103, '地址属性修改', 2026041102100, 3, '', '', '', 1, 1, 'F', '1', '0', 'address:attribute:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102104, '地址属性删除', 2026041102100, 4, '', '', '', 1, 1, 'F', '1', '0', 'address:attribute:remove', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041102200, '异常地址预警', 2026041101000, 12, 'monitor/records', 'address/monitor/records', '', 1, 1, 'C', '1', '0', 'address:monitor:record:list', '#', 103, 1, NOW(), NULL, NULL, '异常地址预警页入口'),
    (2026041102201, '异常地址预警详情', 2026041102200, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:record:query', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102202, '异常地址预警处理', 2026041102200, 2, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:record:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102203, '异常地址预警删除', 2026041102200, 3, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:record:remove', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041102300, '智能检测配置', 2026041101000, 13, 'monitor/rules', 'address/monitor/rules', '', 1, 1, 'C', '1', '0', 'address:monitor:rule:list', '#', 103, 1, NOW(), NULL, NULL, '智能检测配置页入口'),
    (2026041102301, '智能检测配置详情', 2026041102300, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:rule:query', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102302, '智能检测配置新增', 2026041102300, 2, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:rule:add', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102303, '智能检测配置修改', 2026041102300, 3, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:rule:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102304, '智能检测配置删除', 2026041102300, 4, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:rule:remove', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041102400, '监控任务管理', 2026041101000, 14, 'monitor/task', 'address/monitor/task', '', 1, 1, 'C', '1', '0', 'address:monitor:task:query', '#', 103, 1, NOW(), NULL, NULL, '监控任务管理页入口'),
    (2026041102401, '监控任务新增', 2026041102400, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:task:add', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102402, '监控任务修改', 2026041102400, 2, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:task:edit', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102403, '监控任务执行', 2026041102400, 3, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:task:execute', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041102450, '标准地址工单', 2026041101000, 15, 'monitor/work-orders', 'address/monitor/work-orders', '', 1, 1, 'C', '1', '0', 'address:monitor:workOrder:list', '#', 103, 1, NOW(), NULL, NULL, '标准地址工单页入口'),
    (2026041102451, '标准地址工单详情', 2026041102450, 1, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:workOrder:query', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102452, '标准地址工单新增', 2026041102450, 2, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:workOrder:add', '#', 103, 1, NOW(), NULL, NULL, ''),
    (2026041102453, '标准地址工单处理', 2026041102450, 3, '', '', '', 1, 1, 'F', '1', '0', 'address:monitor:workOrder:edit', '#', 103, 1, NOW(), NULL, NULL, ''),

    (2026041102500, 'ES 运维', 2026041101000, 16, 'ops/search', 'address/ops/search', '', 1, 1, 'C', '1', '0', 'address:search:maintain', '#', 103, 1, NOW(), NULL, NULL, 'ES 运维页入口');

-- 三、绑定角色与菜单
-- 录入员：覆盖当前地址模块所有权限点，便于直接联调地址 UI 全量页面。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT operator_role.`role_id`, menu_ids.`menu_id`
FROM (
    SELECT `role_id`
    FROM `sys_role`
    WHERE `tenant_id` = '000000'
      AND `role_key` = 'address_operator'
      AND `del_flag` = '0'
) operator_role
JOIN (
    SELECT 2026041101000 AS `menu_id`
    UNION ALL SELECT 2026041101100
    UNION ALL SELECT 2026041101101
    UNION ALL SELECT 2026041101102
    UNION ALL SELECT 2026041101103
    UNION ALL SELECT 2026041101104
    UNION ALL SELECT 2026041101105
    UNION ALL SELECT 2026041101106
    UNION ALL SELECT 2026041101107
    UNION ALL SELECT 2026041101108
    UNION ALL SELECT 2026041101200
    UNION ALL SELECT 2026041101300
    UNION ALL SELECT 2026041101400
    UNION ALL SELECT 2026041101500
    UNION ALL SELECT 2026041101501
    UNION ALL SELECT 2026041101502
    UNION ALL SELECT 2026041101503
    UNION ALL SELECT 2026041101504
    UNION ALL SELECT 2026041101600
    UNION ALL SELECT 2026041101601
    UNION ALL SELECT 2026041101700
    UNION ALL SELECT 2026041101701
    UNION ALL SELECT 2026041101702
    UNION ALL SELECT 2026041101800
    UNION ALL SELECT 2026041101801
    UNION ALL SELECT 2026041101900
    UNION ALL SELECT 2026041101901
    UNION ALL SELECT 2026041101902
    UNION ALL SELECT 2026041101903
    UNION ALL SELECT 2026041101904
    UNION ALL SELECT 2026041101905
    UNION ALL SELECT 2026041102000
    UNION ALL SELECT 2026041102001
    UNION ALL SELECT 2026041102002
    UNION ALL SELECT 2026041102003
    UNION ALL SELECT 2026041102004
    UNION ALL SELECT 2026041102100
    UNION ALL SELECT 2026041102101
    UNION ALL SELECT 2026041102102
    UNION ALL SELECT 2026041102103
    UNION ALL SELECT 2026041102104
    UNION ALL SELECT 2026041102200
    UNION ALL SELECT 2026041102201
    UNION ALL SELECT 2026041102202
    UNION ALL SELECT 2026041102203
    UNION ALL SELECT 2026041102300
    UNION ALL SELECT 2026041102301
    UNION ALL SELECT 2026041102302
    UNION ALL SELECT 2026041102303
    UNION ALL SELECT 2026041102304
    UNION ALL SELECT 2026041102400
    UNION ALL SELECT 2026041102401
    UNION ALL SELECT 2026041102402
    UNION ALL SELECT 2026041102403
    UNION ALL SELECT 2026041102450
    UNION ALL SELECT 2026041102451
    UNION ALL SELECT 2026041102452
    UNION ALL SELECT 2026041102453
    UNION ALL SELECT 2026041102500
) menu_ids;

-- 审批员：复用全部地址权限，并额外依赖 `address_approver` 角色参与 workflow 审批节点。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT approver_role.`role_id`, menu_ids.`menu_id`
FROM (
    SELECT `role_id`
    FROM `sys_role`
    WHERE `tenant_id` = '000000'
      AND `role_key` = 'address_approver'
      AND `del_flag` = '0'
) approver_role
JOIN (
    SELECT 2026041101000 AS `menu_id`
    UNION ALL SELECT 2026041101100
    UNION ALL SELECT 2026041101101
    UNION ALL SELECT 2026041101102
    UNION ALL SELECT 2026041101103
    UNION ALL SELECT 2026041101104
    UNION ALL SELECT 2026041101105
    UNION ALL SELECT 2026041101106
    UNION ALL SELECT 2026041101107
    UNION ALL SELECT 2026041101108
    UNION ALL SELECT 2026041101200
    UNION ALL SELECT 2026041101300
    UNION ALL SELECT 2026041101400
    UNION ALL SELECT 2026041101500
    UNION ALL SELECT 2026041101501
    UNION ALL SELECT 2026041101502
    UNION ALL SELECT 2026041101503
    UNION ALL SELECT 2026041101504
    UNION ALL SELECT 2026041101600
    UNION ALL SELECT 2026041101601
    UNION ALL SELECT 2026041101700
    UNION ALL SELECT 2026041101701
    UNION ALL SELECT 2026041101702
    UNION ALL SELECT 2026041101800
    UNION ALL SELECT 2026041101801
    UNION ALL SELECT 2026041101900
    UNION ALL SELECT 2026041101901
    UNION ALL SELECT 2026041101902
    UNION ALL SELECT 2026041101903
    UNION ALL SELECT 2026041101904
    UNION ALL SELECT 2026041101905
    UNION ALL SELECT 2026041102000
    UNION ALL SELECT 2026041102001
    UNION ALL SELECT 2026041102002
    UNION ALL SELECT 2026041102003
    UNION ALL SELECT 2026041102004
    UNION ALL SELECT 2026041102100
    UNION ALL SELECT 2026041102101
    UNION ALL SELECT 2026041102102
    UNION ALL SELECT 2026041102103
    UNION ALL SELECT 2026041102104
    UNION ALL SELECT 2026041102200
    UNION ALL SELECT 2026041102201
    UNION ALL SELECT 2026041102202
    UNION ALL SELECT 2026041102203
    UNION ALL SELECT 2026041102300
    UNION ALL SELECT 2026041102301
    UNION ALL SELECT 2026041102302
    UNION ALL SELECT 2026041102303
    UNION ALL SELECT 2026041102304
    UNION ALL SELECT 2026041102400
    UNION ALL SELECT 2026041102401
    UNION ALL SELECT 2026041102402
    UNION ALL SELECT 2026041102403
    UNION ALL SELECT 2026041102450
    UNION ALL SELECT 2026041102451
    UNION ALL SELECT 2026041102452
    UNION ALL SELECT 2026041102453
    UNION ALL SELECT 2026041102500
) menu_ids;

-- 四、绑定示例普通用户
-- `test`：标准地址录入员
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT user_info.`user_id`, role_info.`role_id`
FROM (
    SELECT `user_id`
    FROM `sys_user`
    WHERE `tenant_id` = '000000'
      AND `user_name` = 'test'
      AND `status` = '0'
      AND `del_flag` = '0'
) user_info
JOIN (
    SELECT `role_id`
    FROM `sys_role`
    WHERE `tenant_id` = '000000'
      AND `role_key` = 'address_operator'
      AND `del_flag` = '0'
) role_info;

-- `test1`：标准地址审批员
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT user_info.`user_id`, role_info.`role_id`
FROM (
    SELECT `user_id`
    FROM `sys_user`
    WHERE `tenant_id` = '000000'
      AND `user_name` = 'test1'
      AND `status` = '0'
      AND `del_flag` = '0'
) user_info
JOIN (
    SELECT `role_id`
    FROM `sys_role`
    WHERE `tenant_id` = '000000'
      AND `role_key` = 'address_approver'
      AND `del_flag` = '0'
) role_info;

-- 五、自检查询（按需单独执行）
-- SELECT role_id, role_name, role_key FROM sys_role WHERE role_key IN ('address_operator', 'address_approver');
-- SELECT COUNT(*) AS address_menu_cnt FROM sys_menu WHERE perms LIKE 'address:%';
-- SELECT u.user_name, r.role_key
-- FROM sys_user u
-- JOIN sys_user_role ur ON ur.user_id = u.user_id
-- JOIN sys_role r ON r.role_id = ur.role_id
-- WHERE u.user_name IN ('test', 'test1')
-- ORDER BY u.user_name, r.role_key;
