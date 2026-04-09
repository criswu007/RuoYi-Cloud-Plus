# 地址运行模式切换设计

## 背景

当前标准地址项目同时存在两类本地运行诉求：

- **单体联调**：`ruoyi-address` / `ruoyi-workflow` 脱离网关、认证中心、注册中心后，本地直接联调地址与审批链路。
- **微服务联调**：按接近生产的最小服务集运行 `ruoyi-gateway`、`ruoyi-auth`、`ruoyi-workflow`、`ruoyi-address`，前端统一经 gateway 访问。

现状问题：

- 地址模块与 workflow 模块的大量本地能力依赖 `standalone` profile。
- 前端代理当前默认直连本地地址/流程服务，不支持按配置切到 gateway。
- 本地切换需要改 profile、改启动命令、改代理地址，操作分散且容易出错。

## 目标

在不破坏现有微服务部署方式的前提下，提供两套可重复切换的本地运行环境：

1. **单体运行环境**
2. **生产式最小微服务运行环境**

切换原则：

- 前后端分别通过**配置文件中的配置项**切换。
- 切换后仅需**重启对应服务**即可生效。
- 不要求通过脚本入口切换。
- 前端与后端切换开关彼此独立。

## 约束与事实

### 后端

- `ruoyi-address` 当前源码默认激活 `standalone` profile。
- `ruoyi-workflow` 已有一整套 `standalone` 本地桩与 mock 登录体系。
- `standalone` 相关能力不仅包含配置文件，还包含大量 `@Profile("standalone")` 代码装配。
- 微服务模式下地址模块需要启用 Dubbo，且配置来源仍应保持与现有 Nacos 体系兼容。

### 前端

- `vite.config.js` 当前默认：
  - `/address` -> `http://localhost:9206`
  - `/workflow` -> `http://localhost:9205`，并去掉 `/workflow` 前缀
- 微服务模式下应统一走 `gateway`，此时 `/workflow` 不应继续重写。

## 方案选择

### 方案 A：新增统一 `local` profile + 运行模式开关（推荐）

- 后端统一使用 `local` profile 作为本地开发入口。
- 在 `local` profile 下通过：
  - `address.runtime.mode=standalone|microservice`
  - `workflow.runtime.mode=standalone|microservice`
  选择实际装配。
- 保留现有 `standalone` profile 作为兼容兜底，但本地主入口改为 `local`。

**优点**

- 满足“同一 profile 下配置开关”的要求。
- 不破坏现有 `dev/prod` 微服务部署方式。
- 可逐步平滑迁移，避免一次性删除全部 `standalone` 资产。

**缺点**

- 需要把 `standalone` 代码装配从纯 profile 判断扩展为“legacy standalone 条件兼容 或 local+standalone mode”。

### 方案 B：继续使用 `standalone/dev` 双 profile，额外补充说明文档

不满足“同一 profile 下配置开关”的要求，放弃。

### 方案 C：仅前端切换代理，后端仍按现有 profile 手工切换

切换责任仍分散，不满足目标，放弃。

## 最终设计

## 1. 后端运行模型

### 1.1 本地统一入口 profile

地址模块与 workflow 模块新增/启用 `local` profile 作为本地开发入口：

- `local + standalone mode`：单体联调
- `local + microservice mode`：最小微服务联调

保留 legacy：

- `standalone` 条件兼容继续保留给已有装配判断与少量显式 `standalone` profile 场景，但不再保留单独的本地资源入口文件。

### 1.2 运行模式配置项

地址模块：

```yaml
address:
  runtime:
    mode: standalone
```

workflow 模块：

```yaml
workflow:
  runtime:
    mode: standalone
```

### 1.3 配置文件组织

地址模块：

- `application.yml`：应用名与默认 profile 入口
- `application-local.yml`：本地开发公共入口，按 `address.runtime.mode` 导入对应运行配置
- `runtime/address-local-runtime-standalone.yml`：单体直连配置
- `runtime/address-local-runtime-microservice.yml`：本地微服务/Nacos 配置

workflow 模块：

- `application.yml`
- `application-local.yml`
- `runtime/workflow-local-runtime-standalone.yml`
- `runtime/workflow-local-runtime-microservice.yml`

### 1.4 代码装配规则

#### standalone 能力

以下能力应在两种情况下生效：

- 激活 legacy `standalone` profile
- 激活 `local` profile 且 `*.runtime.mode=standalone`

涉及：

- 地址模块本地 mock 登录、内存 Sa-Token、字典兜底
- 地址模块 HTTP 版 workflow 远程适配器
- workflow 模块全部 standalone Remote* 桩
- workflow 模块本地 mock 登录与内存 Sa-Token

#### microservice 能力

以下能力应在两种情况下生效：

- 非 `standalone` profile 的正常微服务部署
- `local` profile 且 `*.runtime.mode=microservice`

涉及：

- 地址模块 Dubbo 启动配置
- workflow 正常微服务依赖链

## 2. 前端运行模型

### 2.1 配置项

前端新增独立配置项：

```env
VITE_ADDRESS_RUNTIME_MODE=standalone
VITE_ADDRESS_STANDALONE_BASE=http://127.0.0.1:9206
VITE_WORKFLOW_STANDALONE_BASE=http://127.0.0.1:9205
VITE_GATEWAY_BASE=http://127.0.0.1:8080
```

### 2.2 代理行为

#### standalone

- `/address` -> `VITE_ADDRESS_STANDALONE_BASE`
- `/workflow` -> `VITE_WORKFLOW_STANDALONE_BASE`
- `/workflow` 继续去前缀重写，复用现有页面调用方式

#### microservice

- `/address` -> `VITE_GATEWAY_BASE`
- `/workflow` -> `VITE_GATEWAY_BASE`
- `/workflow` **不重写路径**

### 2.3 页面可视化提示

页头除 `API_BASE` 外，增加当前运行模式展示，避免联调时误判目标环境。

## 3. 测试与验收

### 后端

- 地址模块编译通过
- workflow 模块编译通过
- `local + *.runtime.mode=standalone` 测试不回归
- 新增最小条件装配测试，验证 standalone/microservice 运行条件判断

### 前端

- 补充 `vite.config.js` 代理测试：
  - standalone 下 `/workflow` 会重写
  - microservice 下 `/workflow` 不重写且走 gateway

## 4. 使用方式

### 单体模式

后端配置：

- `spring.profiles.active=local`
- `address.runtime.mode=standalone`
- `workflow.runtime.mode=standalone`

前端配置：

- `VITE_ADDRESS_RUNTIME_MODE=standalone`

### 微服务模式

后端配置：

- `spring.profiles.active=local`
- `address.runtime.mode=microservice`
- `workflow.runtime.mode=microservice`

前端配置：

- `VITE_ADDRESS_RUNTIME_MODE=microservice`

## 5. 风险

- `spring.config.import` 的本地运行时导入路径必须可被稳定解析，否则会导致本地启动期失败。
- workflow 中 standalone 远程桩较多，若条件装配遗漏，可能导致单体模式缺少 provider。
- 前端 `/workflow` 代理重写在两种模式下语义不同，必须由测试兜底。

## 6. 结论

采用 **`local` profile + 运行模式配置项** 的方案，在满足“同一 profile 下通过配置开关切换”的同时，保留 legacy 条件兼容，并把前端代理与后端装配都收口到可重复、可文档化的配置入口；`application-standalone.yml` 这类独立本地资源入口不再保留。
