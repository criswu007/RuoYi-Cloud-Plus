# 标准地址审批流 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为标准地址新增、编辑、删除、导入、合并、拆分接入 workflow 审批流，并交付审批管理前端页面。

**Architecture:** 采用“审批申请单 + workflow 流转 + 审批通过后正式执行”的双层结构。地址模块负责申请单与正式写入，workflow 模块负责待办/已办/审批动作。

**Tech Stack:** Spring Boot、MyBatis-Plus、Dubbo、Warm-Flow、Vue2、Element UI、Vitest、JUnit5

---

### Task 1: 建立审批数据模型

**Files:**
- Create: `ruoyi-modules/ruoyi-address/sql/address/address_standard_approval.sql`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/StandardAddressApproval.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressApprovalBo.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressApprovalVo.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/StandardAddressApprovalMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/StandardAddressApprovalMapper.xml`

- [ ] 定义申请单表结构与索引
- [ ] 定义实体、查询 BO、展示 VO
- [ ] 定义列表查询与详情查询 Mapper

### Task 2: 接入 workflow 发起能力

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/pom.xml`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressApprovalService.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressApprovalSnapshotService.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressApprovalTitleBuilder.java`

- [ ] 增加 `ruoyi-api-workflow` 依赖
- [ ] 实现六类操作的申请单创建与快照组装
- [ ] 通过 `RemoteWorkflowService.startWorkFlow` 发起审批流程

### Task 3: 改造标准地址写入口

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/IStandardAddressService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressImportService.java`

- [ ] 新增/修改/删除/导入/合并/拆分改为“提交审批”
- [ ] 保留查询接口不变
- [ ] 返回值与提示语适配“已提交审批”

### Task 4: 实现审批通过后的正式执行器

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressApprovalExecutor.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressApprovalImportExecutor.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressCommandService.java`

- [ ] 解析申请单快照并转正式 BO
- [ ] 审批通过后调用正式命令服务
- [ ] 记录执行结果与异常信息

### Task 5: 监听 workflow 事件

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/listener/StandardAddressWorkflowListener.java`

- [ ] 监听 `ProcessEvent` 同步状态
- [ ] 监听 `ProcessTaskEvent` 回填当前任务
- [ ] 监听驳回与删除事件

### Task 6: 新增审批管理后端接口

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressApprovalController.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/IStandardAddressApprovalService.java`

- [ ] 提供“我提交的”分页接口
- [ ] 提供申请详情接口
- [ ] 提供按 `businessId` 查询审批详情聚合接口

### Task 7: 初始化 workflow 流程定义

**Files:**
- Create: `ruoyi-modules/ruoyi-address/sql/workflow/address_standard_approve_v1.json`
- Create: `ruoyi-modules/ruoyi-address/sql/workflow/address_standard_approve_v1.sql`

- [ ] 基于现有 `leave1.json` 生成一级审批流程
- [ ] 补充 `flow_definition / flow_node / flow_skip` 初始化 SQL
- [ ] 约定审批页面 `formPath`

### Task 8: 实现前端审批管理页面

**Files:**
- Modify: `ruoyi-address-ui/src/router/index.js`
- Modify: `ruoyi-address-ui/src/App.vue`
- Modify: `ruoyi-address-ui/src/api/address.js`
- Create: `ruoyi-address-ui/src/views/StandardApprovalRecords.vue`
- Create: `ruoyi-address-ui/src/views/StandardApprovalRecords.test.js`

- [ ] 增加审批管理路由与菜单
- [ ] 新增“我提交的 / 待审批 / 已审批”页面
- [ ] 接 workflow 与地址审批详情接口

### Task 9: 适配现有页面提交流程

**Files:**
- Modify: `ruoyi-address-ui/src/views/StandardList.vue`
- Modify: `ruoyi-address-ui/src/views/StandardMerge.vue`
- Modify: `ruoyi-address-ui/src/views/StandardSplit.vue`
- Modify: `ruoyi-address-ui/src/views/ImportRecords.vue`

- [ ] 提交成功提示改为审批文案
- [ ] 合并/拆分/导入操作完成后跳转或提示进入审批管理
- [ ] 保持现有查询与详情行为不变

### Task 10: 补测试并联调

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressApprovalServiceTest.java`
- Create: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/listener/StandardAddressWorkflowListenerTest.java`
- Modify: `ruoyi-address-ui/src/router/index.test.js`

- [ ] 先写失败测试覆盖申请单与流程事件
- [ ] 实现代码至测试通过
- [ ] 运行 Maven 与 Vitest 验证
- [ ] 启动地址后端、workflow 后端、前端完成联调
