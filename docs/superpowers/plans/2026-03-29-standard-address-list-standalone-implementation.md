# 标准地址列表单体联调 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `ruoyi-address` 以本地单体方式启动，并使 `ruoyi-address-ui` 的标准地址列表页按原型交互跑通列表、详情、新增、编辑、删除、批量新增下级地址主链路。

**Architecture:** 后端保持 `ruoyi-address` 单模块 Spring Boot 应用，通过新增本地 standalone profile 绕开 Nacos/完整微服务环境依赖，直接提供标准地址核心接口。前端以 `ruoyi-address-ui` 为独立 Vite 应用，重写标准地址列表页与 API 封装，对齐当前 `POST + segmId` 契约和原型交互。

**Tech Stack:** Spring Boot 3.5, MyBatis-Plus, Vue 2, Element UI, Vite, Axios, JUnit 5, Mockito。

---

### Task 1: 补齐单体运行基线

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/resources/application.yml`
- Create: `ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml`
- Modify: `ruoyi-address-ui/vite.config.js`
- Modify: `ruoyi-address-ui/src/api/request.js`

- [ ] **Step 1: 写失败验证，确认当前后端无法脱离 Nacos 独立启动**

Run: `mvn -pl ruoyi-modules/ruoyi-address spring-boot:run -Dspring-boot.run.profiles=standalone`
Expected: FAIL，提示缺少 standalone profile 或远程配置依赖未满足。

- [ ] **Step 2: 增加 standalone profile 的本地运行配置**

实现要点：
- `application.yml` 保持现有默认行为不变。
- `application-standalone.yml` 提供本地 datasource / 关闭远程配置依赖 / 允许单模块启动的最小配置。
- 前端 `vite.config.js` 默认代理本地 `9206`，支持开发态覆盖。
- `request.js` 增加开发态容错，避免本地无 token 时直接阻塞。

- [ ] **Step 3: 运行后端单体启动命令验证配置生效**

Run: `mvn -pl ruoyi-modules/ruoyi-address spring-boot:run -Dspring-boot.run.profiles=standalone`
Expected: PASS，服务监听 `9206`。

### Task 2: 锁定标准地址列表页接口契约

**Files:**
- Modify: `ruoyi-address-ui/src/api/address.js`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerMappingTest.java`

- [ ] **Step 1: 写失败断言，锁定列表页所需接口使用 POST 与 segmId 契约**

补充断言：
- 列表接口 `POST /address/standard/list`
- 详情接口 `POST /address/standard/{segmId}`
- 编辑接口 `POST /address/standard/update`
- 删除接口 `POST /address/standard/remove/{segmIds}`
- 批量预览接口 `POST /address/standard/batchPreviewChild`
- 批量新增接口 `POST /address/standard/batchAddChild`

- [ ] **Step 2: 前端 API 封装改为对齐当前后端契约**

实现要点：
- 全部改用 `POST`
- 查询类接口按 `application/x-www-form-urlencoded` 或 params 兼容方式提交
- 写接口统一传 canonical 字段：`segmId/parentSegmId/segmName/levelId/...`

- [ ] **Step 3: 运行映射测试确保后端契约未被前端要求带偏**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressControllerMappingTest test`
Expected: PASS

### Task 3: 完成标准地址列表页原型对齐

**Files:**
- Modify: `ruoyi-address-ui/src/views/StandardList.vue`
- Modify: `ruoyi-address-ui/src/router/index.js`
- Reference: `ruoyi-modules/ruoyi-address/docs/reference/vibe-coding-api-manual.md`

- [ ] **Step 1: 根据原型写出页面结构调整清单**

必须落地：
- 列表筛选改为“标准地址 + 级别”为主
- 表格列改为“标准地址、地址简拼、父级地址、当级名称、级别、创建时间、操作”
- 详情弹窗展示标准地址扩展字段与只读信息
- 新增/编辑弹窗字段口径改为 canonical 命名
- “批量新增地址”弹窗增加预览区与二段式操作

- [ ] **Step 2: 先写最小失败验证**

Run: `npm --prefix ruoyi-address-ui run build`
Expected: FAIL 或者虽然 PASS，但页面字段仍明显不匹配当前后端/原型，需要继续调整。

- [ ] **Step 3: 重写 `StandardList.vue` 以对齐原型与当前接口**

实现要点：
- 查询字段使用 `standName`、`levelId`
- 表格显示 `standName/standNo/parentSegmId/segmName/levelId/createDate`
- 行操作支持详情、编辑、删除
- 新增/编辑使用 `segmId/parentSegmId/segmName/segmType/levelId/status/notes`
- 批量新增先预览，再提交
- 所有弹窗与按钮文案统一使用标准地址业务语义

- [ ] **Step 4: 执行前端构建验证**

Run: `npm --prefix ruoyi-address-ui run build`
Expected: PASS

### Task 4: 补齐列表页后端行为缺口

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressQueryService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressCommandService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressQueryServiceTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressCommandServiceTest.java`

- [ ] **Step 1: 为列表页新增失败测试，锁定查询与批量预览行为**

至少覆盖：
- 查询条件 `standName + levelId`
- `1/2` 级走 `spc_region`
- 详情返回 canonical 字段
- 批量预览返回 `segmName/standName/segmNo/standNo/levelId/segmType`

- [ ] **Step 2: 运行测试确认缺口**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressQueryServiceTest,StandardAddressCommandServiceTest test`
Expected: FAIL，列出与页面联调缺口对应的行为问题。

- [ ] **Step 3: 最小实现页面联调所需行为**

实现要点：
- 查询结果字段保证页面无需再做旧别名适配
- 详情字段保证列表页详情弹窗可直接消费
- 批量预览与批量新增返回 / 校验适配前端二段式交互

- [ ] **Step 4: 重跑测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressQueryServiceTest,StandardAddressCommandServiceTest test`
Expected: PASS

### Task 5: 端到端验证列表页整链路

**Files:**
- Verify only

- [ ] **Step 1: 启动后端 standalone**

Run: `mvn -pl ruoyi-modules/ruoyi-address spring-boot:run -Dspring-boot.run.profiles=standalone`
Expected: PASS

- [ ] **Step 2: 启动前端开发服务**

Run: `npm --prefix ruoyi-address-ui run dev -- --host`
Expected: PASS

- [ ] **Step 3: 执行模块级回归**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false test`
Expected: PASS

- [ ] **Step 4: 执行页面手工联调检查**

检查项：
- 列表查询正常
- 分页正常
- 详情正常
- 新增 / 编辑正常
- 删除正常
- 批量预览 / 批量新增正常

