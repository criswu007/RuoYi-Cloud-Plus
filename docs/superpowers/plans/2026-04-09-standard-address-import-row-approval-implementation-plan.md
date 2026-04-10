# Standard Address Import Row Approval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让标准地址导入支持“单行单审批 + 保留批次号 + 前置校验落库 + 审批结果回写批次统计”。

**Architecture:** 复用 `address_standard_import_record` 作为批次表，并将 `address_standard_import_fail_detail` 升级为导入行结果表。上传时逐条校验与落行记录，审批监听器再回写单行状态与批次聚合结果。

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, MySQL

---

### Task 1: 补齐导入行结果模型与表结构

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/StandardAddressImportFailDetail.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressImportRecordVo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/StandardAddressImportFailDetailMapper.xml`
- Modify: `ruoyi-modules/ruoyi-address/sql/address/*.sql`（新增或补充迁移脚本）

### Task 2: 先写失败测试锁定上传阶段的逐条前置校验行为

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressApprovalServiceTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerTest.java`

### Task 3: 实现上传阶段单行校验、单行审批与行结果落库

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressApprovalService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressImportService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/StandardAddressImportFailDetailMapper.java`

### Task 4: 先写失败测试锁定审批驳回/执行失败/执行成功后的回写行为

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/listener/StandardAddressWorkflowListenerTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressImportRecordServiceTest.java`（如不存在则新增）

### Task 5: 实现审批监听回写与批次聚合统计

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/listener/StandardAddressWorkflowListener.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressImportRecordServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressImportBatchVo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressImportResultVo.java`

### Task 6: 回归失败导出与真实联调

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/mapper/AddressMapperDialectCompatibilityTest.java`

### Task 7: 打包、重启、接口回归

**Files:**
- Modify: none
