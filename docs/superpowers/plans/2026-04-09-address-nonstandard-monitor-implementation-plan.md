# Address Nonstandard Monitor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让标准地址模块的非标地址监控具备“模板化规则配置 + `snailjob` 任务管理 + 管理端异常预警治理”完整闭环。

**Architecture:** 以规则为中心设计检测模板，新增任务定义、任务范围、任务运行日志 3 类持久化模型，使用统一检测执行器承接手动执行与 `snailjob` 调度。异常记录升级为预警快照模型，前端保留 `/monitor/rules`、`/monitor/task`、`/monitor/records` 路由并整体重做交互。

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, SnailJob, Vue 2, Element UI, JUnit 5, Mockito, Vitest

---

### Task 1: 固化接口与数据模型契约

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressAdminApi.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressAdminBo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressAdminVo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressMonitorRuleBo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressMonitorRecordBo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressMonitorRuleVo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressMonitorRecordVo.java`

### Task 2: 增补数据库脚本与实体模型

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/StandardAddressMonitorRule.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/StandardAddressMonitorRecord.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/StandardAddressMonitorTask.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/StandardAddressMonitorTaskRuleRel.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/StandardAddressMonitorTaskScopeRel.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/StandardAddressMonitorTaskRunLog.java`
- Modify: `ruoyi-modules/ruoyi-address/sql/address/*.sql`

### Task 3: 补 Mapper、XML 与查询装配

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/StandardAddressMonitorTaskMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/StandardAddressMonitorTaskRuleRelMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/StandardAddressMonitorTaskScopeRelMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/StandardAddressMonitorTaskRunLogMapper.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/*.xml`

### Task 4: 先写后端失败测试锁定规则与任务行为

**Files:**
- Create or Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressMonitorRuleServiceImplTest.java`
- Create: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressMonitorTaskServiceImplTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressMonitorRecordServiceImplTest.java`

### Task 5: 实现模板化规则配置服务

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/IStandardAddressMonitorRuleService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressMonitorRuleServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressMonitorRuleController.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/enums/MonitorRuleTemplateEnum.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/enums/MonitorSeverityEnum.java`

### Task 6: 实现任务管理与 `snailjob` 编排

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/IStandardAddressMonitorTaskService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressMonitorTaskServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressMonitorTaskController.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/MonitorTaskSchedulerService.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/MonitorTaskSchedulerServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/job/StandardAddressNonStandardMonitorJob.java`

### Task 7: 实现检测执行器与模板 detector

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/monitor/detector/MonitorRuleDetector.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/monitor/detector/FormatStandardDetector.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/monitor/detector/RegionComplianceDetector.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/monitor/detector/ElementCompletenessDetector.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/monitor/detector/SmartSuspectDetector.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/monitor/MonitorExecutionService.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/monitor/impl/MonitorExecutionServiceImpl.java`

### Task 8: 升级异常预警记录与批量治理能力

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/IStandardAddressMonitorRecordService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressMonitorRecordServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressMonitorRecordController.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/StandardAddressMonitorRecordMapper.java`

### Task 9: 统一前端 API 到文档基线合同

**Files:**
- Modify: `ruoyi-address-ui/src/api/address.js`
- Modify: `ruoyi-address-ui/src/api/request.js`（如需兼容统一 `POST` 风格）
- Modify: `ruoyi-address-ui/src/router/index.js`
- Modify: `ruoyi-address-ui/src/App.vue`

### Task 10: 重做智能检测配置页面

**Files:**
- Modify: `ruoyi-address-ui/src/views/MonitorRules.vue`
- Create: `ruoyi-address-ui/src/components/monitor/MonitorRuleEditor.vue`
- Create: `ruoyi-address-ui/src/components/monitor/MonitorRuleTemplateForm.vue`
- Create: `ruoyi-address-ui/src/utils/monitor-rule.js`

### Task 11: 重做监控任务管理页面

**Files:**
- Modify: `ruoyi-address-ui/src/views/MonitorTask.vue`
- Create: `ruoyi-address-ui/src/components/monitor/MonitorTaskEditor.vue`
- Create: `ruoyi-address-ui/src/components/monitor/MonitorTaskRunLogDrawer.vue`
- Create: `ruoyi-address-ui/src/utils/monitor-task.js`

### Task 12: 重做异常地址预警页面

**Files:**
- Modify: `ruoyi-address-ui/src/views/MonitorRecords.vue`
- Create: `ruoyi-address-ui/src/components/monitor/MonitorRecordDetailDrawer.vue`
- Create: `ruoyi-address-ui/src/utils/monitor-record.js`

### Task 13: 补前端测试与后端回归

**Files:**
- Create or Modify: `ruoyi-address-ui/src/views/MonitorRules.test.js`
- Create or Modify: `ruoyi-address-ui/src/views/MonitorTask.test.js`
- Create or Modify: `ruoyi-address-ui/src/views/MonitorRecords.test.js`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java`（仅当公共 Excel/接口改动受影响时）

### Task 14: 本地联调与文档回写

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/docs/reference/vibe-coding-api-manual.md`
- Modify: `ruoyi-modules/ruoyi-address/docs/interface-checklist.md`
- Modify: `docs/operations/address-microservice-production-deployment-guide.md`（若新增表脚本和 `snailjob` 配置要求）
