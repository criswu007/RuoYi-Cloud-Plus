# 地址运行模式切换 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为地址项目提供“单体 standalone 联调”和“最小微服务联调”两套可通过配置文件切换的本地运行环境。

**Architecture:** 后端新增统一 `local` profile，并在该 profile 下通过 `address.runtime.mode` / `workflow.runtime.mode` 切换 standalone 与 microservice 运行方式；前端通过 `VITE_ADDRESS_RUNTIME_MODE` 切换直连本地服务或统一走 gateway。legacy standalone 条件兼容保留，但不再保留单独的 `application-standalone.yml` 本地资源入口。

**Tech Stack:** Spring Boot、Spring Config Data、Dubbo、Vite、Vitest

---

### Task 1: 补齐前端运行模式切换

**Files:**
- Modify: `ruoyi-address-ui/vite.config.js`
- Modify: `ruoyi-address-ui/src/App.vue`
- Modify: `ruoyi-address-ui/src/viteProxy.test.js`
- Create: `ruoyi-address-ui/.env.development`

- [ ] 新增前端运行模式配置解析
- [ ] 让 `/workflow` 代理按模式决定是否重写
- [ ] 在页头展示当前运行模式
- [ ] 补齐 Vitest 代理测试

### Task 2: 增加地址模块 local 运行入口

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/resources/application.yml`
- Create: `ruoyi-modules/ruoyi-address/src/main/resources/application-local.yml`
- Create: `ruoyi-modules/ruoyi-address/src/main/resources/runtime/address-local-runtime-standalone.yml`
- Create: `ruoyi-modules/ruoyi-address/src/main/resources/runtime/address-local-runtime-microservice.yml`

- [ ] 引入 `local` profile 作为本地统一入口
- [ ] 把 standalone 本地配置下沉到 local runtime 配置文件
- [ ] 补齐 microservice 模式下的本地 Nacos 导入配置

### Task 3: 收口地址模块运行条件判断

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/condition/AddressRuntimeMode.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/condition/AddressStandaloneRuntimeCondition.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/condition/AddressMicroserviceRuntimeCondition.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/StandaloneLocalDevelopmentConfiguration.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/AddressDubboBootstrapConfiguration.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/workflow/StandaloneRemoteWorkflowService.java`

- [ ] 为地址模块新增运行模式条件类
- [ ] 让 standalone 本地能力同时兼容 legacy standalone 条件判断与 local+standalone
- [ ] 让 Dubbo 启动配置在 microservice 模式下生效

### Task 4: 增加 workflow 模块 local 运行入口

**Files:**
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/resources/application.yml`
- Create: `ruoyi-modules/ruoyi-workflow/src/main/resources/application-local.yml`
- Create: `ruoyi-modules/ruoyi-workflow/src/main/resources/runtime/workflow-local-runtime-standalone.yml`
- Create: `ruoyi-modules/ruoyi-workflow/src/main/resources/runtime/workflow-local-runtime-microservice.yml`

- [ ] 引入 workflow 本地统一入口 profile
- [ ] 将 standalone 本地运行配置下沉为 local runtime 配置
- [ ] 补齐 microservice 模式下本地 Nacos 导入配置

### Task 5: 收口 workflow standalone 条件装配

**Files:**
- Create: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/config/condition/WorkflowStandaloneRuntimeCondition.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/config/StandaloneLocalDevelopmentConfiguration.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteClientService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteDataScopeService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteDeptService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteDictService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteFileService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteLogService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteMailService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteMessageService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemotePermissionService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemotePostService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteRoleService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteTaskAssigneeService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneRemoteUserService.java`
- Modify: `ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/standalone/StandaloneSystemJdbcSupport.java`

- [ ] 新增 workflow standalone 运行条件
- [ ] 将全部 standalone Remote* 与 JDBC 支撑改为条件装配

### Task 6: 验证与文档补充

**Files:**
- Modify: `ruoyi-address-ui/src/viteProxy.test.js`
- Optional Modify: `ruoyi-modules/ruoyi-address/docs/project-baseline.md`

- [ ] 运行前端代理测试
- [ ] 运行地址模块定向测试与编译
- [ ] 运行 workflow 模块编译
- [ ] 补充本地切换说明
