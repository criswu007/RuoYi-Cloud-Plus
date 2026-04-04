# 地址 ES 运维台与任务化维护设计

## 1. 背景与目标

在“标准地址/安装地址 ES 检索与高并发方案”基础上，地址模块还需要一套可落地的运维入口，解决以下问题：

- 前端缺少 ES 运行态、索引状态与 repair 任务的统一查看入口。
- 标准地址/安装地址全量重建仍是同步长任务，不适合生产运维和联调。
- 需要直接复用仓库现有 `script/docker/docker-compose.yml` 中的真实 `Redis + Elasticsearch + Kibana` 环境做联调，而不是只停留在 mock 接口层。

本设计的目标是：

- 在 `ruoyi-address-ui` 中新增一个 ES 运维模块，作为单页运维驾驶舱。
- 将标准地址、安装地址全量重建改为后台任务化执行。
- 为页面提供索引运行态、任务状态和 repair 列表查询接口。
- 在本地联调和正式分布式部署中统一采用 Redis 作为运维写操作的分布式协调基础。

## 2. 已确认评估口径

以下口径为本轮设计的固定前提，后续如需调整，应直接修改本节：

- 一致性仍沿用此前方案中的“业务上立即一致”口径。
- 在线写链路仍保持“ES 不可用时数据库写入也必须失败”，不引入 DB 成功、ES 延后补偿的降级语义。
- ES 命中后允许按页或按批次回数据库补低频字段，不要求把所有关联字段都冗余进 ES。
- 标准地址与安装地址都必须保留后台列表与导出能力。
- 第一版前端运维模块范围采用“操作台 + 运行态诊断”：
  - 支持标准地址重建
  - 支持安装地址重建
  - 支持 repair 任务查看与手工重放
  - 支持 ES 连通状态、索引别名、物理索引、文档数、最近 repair 摘要、Kibana 跳转
- 高风险操作默认采用“二次确认 + 串行执行 + 明确口令确认”。
- 全量重建采用后台任务化；repair 手工重放第一版保持同步执行，不单独任务化。
- 运维写操作依赖 Redis 分布式协调能力；本地 standalone 联调时也必须开启相关配置，与正式分布式环境保持一致。

## 3. 现状与约束

### 3.1 前端现状

- 前端项目为独立的 `ruoyi-address-ui`，技术栈是 `Vue2 + vue-router + Element UI + Vite`。
- 路由集中在 `ruoyi-address-ui/src/router/index.js`，目前是平铺路由，没有复杂的动态菜单机制。
- 运维类页面风格可复用 `src/views/MonitorTask.vue` 的摘要卡片 + 操作区布局。

### 3.2 后端现状

- 当前已存在 `AddressSearchMaintenanceController` 和 `AddressSearchMaintenanceService`。
- 已支持：
  - 标准地址索引全量重建
  - 安装地址索引全量重建
  - repair 任务重放
- 但这三类入口目前都按同步方式执行，其中全量重建属于明显长任务。

### 3.3 联调约束

- `script/docker/docker-compose.yml` 已提供真实环境：
  - `redis:6379`
  - `elasticsearch:9200`
  - `kibana:5601`
- `script/docker/redis/conf/redis.conf` 已确认密码为 `ruoyi123`。
- `ruoyi-address` 的 `application-standalone.yml` 当前排除了 `Redisson` 与 `Lock4j` 自动配置，联调前必须恢复启用，否则无法验证与生产一致的分布式串行语义。

## 4. 方案对比

### 4.1 方案 A：单页驾驶舱 + 模块内任务表 + 异步执行

做法：

- 前端新增单页运维驾驶舱。
- 地址模块内部新增运维任务表，专门承载重建任务。
- 使用模块内异步执行器完成后台重建。
- repair 队列继续复用现有 `address_search_repair_task`。

优点：

- 最符合当前前端平铺路由结构。
- 不引入额外平台依赖，首轮联调链路最短。
- 可以精确控制进度、阶段、错误信息与口令确认流程。

缺点：

- 需要额外设计任务状态表和任务查询接口。

### 4.2 方案 B：单页驾驶舱 + SnailJob

做法：

- 前端仍然是单页运维台。
- 重建任务交给 SnailJob 编排与执行。

优点：

- 理论上更平台化，后续统一任务调度更自然。

缺点：

- 第一版会把地址模块运维、SnailJob 注册、控制台联动、任务状态映射一起拉进来，复杂度过高。
- 重建进度、阶段与页面展示仍然需要补一层适配。

### 4.3 方案 C：双页运维台 + 模块内任务表

做法：

- 拆成“运行概览页”和“运维任务页”两个页面。

优点：

- 结构清晰，后续扩展更多运维项更自然。

缺点：

- 当前项目没有复杂菜单壳，拆页会扩大改动面。
- 对第一版联调收益不明显。

### 4.4 结论

第一版采用 **方案 A**：

- 前端使用单页驾驶舱。
- 后端新增模块内运维任务表，只对“全量重建”做任务化。
- repair 队列继续复用现有 repair 表与同步重放接口。

## 5. 推荐设计

### 5.1 前端信息架构

左侧导航新增一级分组 `运维支持`，下挂菜单 `ES 运维`，路由建议为 `/ops/search`。

页面分三块：

1. 顶部概览区
   - ES 连通状态
   - 集群名、版本、健康状态
   - 标准地址索引别名、当前物理索引名、文档数
   - 安装地址索引别名、当前物理索引名、文档数
   - repair 待处理数、失败数
   - Kibana 外链入口
2. 中部操作区
   - 标准地址全量重建卡片
   - 安装地址全量重建卡片
   - 每张卡片显示当前运行态、风险提示、口令确认入口
3. 底部任务区
   - 左侧：重建任务列表
   - 右侧：repair 列表与单条重放入口

页面交互约束：

- 页面首次进入时加载概览、最近重建任务和 repair 列表。
- 无运行中任务时仅手动刷新；存在运行中任务时，每 `5s` 轮询概览和当前任务状态。
- 点击重建前必须弹出确认框，并要求输入固定口令：
  - 标准重建：`REBUILD_STANDARD`
  - 安装重建：`REBUILD_INSTALLATION`
- 点击 repair 重放前必须输入 `REPLAY_REPAIR`。
- Kibana 只做新窗口外跳，不嵌入当前页面。

### 5.2 后端任务模型

新增表 `address_search_maintenance_task`，只承载“人工触发的重建任务”，不与 `address_search_repair_task` 混用。

建议核心字段：

- `id`
- `taskType`：`REBUILD_STANDARD` / `REBUILD_INSTALLATION`
- `targetAlias`
- `physicalIndexName`
- `status`：`PENDING / RUNNING / SUCCESS / FAILED`
- `currentPhase`：`PRECHECK / CREATE_INDEX / BULK_INDEX / SWITCH_ALIAS / FINISH`
- `totalCount`
- `processedCount`
- `progressPercent`
- `errorMessage`
- `triggerBy`
- `startedTime`
- `finishedTime`
- `createdTime`
- `updatedTime`

任务状态语义：

- `PENDING`：任务已创建，等待异步执行器领取。
- `RUNNING`：正在执行中。
- `SUCCESS`：完成并已切换别名。
- `FAILED`：失败，错误摘要已落表。

repair 任务仍然沿用现有 `address_search_repair_task`：

- repair 列表直接读取该表。
- 手工重放时仍调用现有重放逻辑。
- repair 执行结果继续回写 `SUCCESS / FAILED` 与 `retryCount`。

### 5.3 分布式串行控制

所有 ES 运维写操作共用一个全局 Redis 协调键，例如 `address:search:maintenance:running`。

第一版串行范围包括：

- 标准地址全量重建
- 安装地址全量重建
- repair 手工重放

原因：

- 三类动作都会写 ES。
- 重建期间涉及新物理索引写入与 alias 切换，若与 repair 并发，容易出现写入目标不一致。

控制策略：

- 提交重建任务时先通过 Redis 分布式协调机制做排他检查。
- 若已有运行中的运维写操作，则直接拒绝提交，并返回明确提示。
- repair 手工重放同样受该串行约束限制。
- Redis 不可用或协调失败时，直接拒绝发起运维写操作。

实现上允许结合 `Lock4j`、`Redisson` 或等价 Redis 互斥原语；设计约束是“依赖 Redis 的分布式串行语义”，而不是限定某一种 API 写法。

### 5.4 重建任务执行流程

标准地址/安装地址重建统一按以下流程执行：

1. 控制器校验权限与口令。
2. 服务层通过 Redis 协调确认当前无其他运维写任务执行中。
3. 创建 `address_search_maintenance_task` 记录，状态置为 `PENDING`。
4. 异步执行器启动任务，状态改为 `RUNNING`。
5. 进入 `PRECHECK`：
   - 校验 ES 可用
   - 校验 alias 读写配置
   - 预估总量并写入 `totalCount`
6. 进入 `CREATE_INDEX`：
   - 创建新的物理索引
7. 进入 `BULK_INDEX`：
   - 按批扫描 MySQL
   - 每批成功后递增 `processedCount`
   - 更新 `progressPercent`
8. 进入 `SWITCH_ALIAS`：
   - 全部批次写入成功后原子切换 alias
9. 进入 `FINISH`：
   - 更新 `SUCCESS`、`finishedTime`
10. 任一阶段失败：
   - 状态改为 `FAILED`
   - 记录错误摘要
   - 释放 Redis 协调标记

### 5.5 运行态与查询接口

建议新增以下接口：

- `GET /address/search/ops/overview`
  - 返回 ES 连通状态、集群信息、两个索引 alias/物理索引/文档数、当前运行中任务摘要、repair 待处理/失败数、Kibana URL
- `POST /address/search/tasks/rebuild/standard`
  - 请求体包含 `confirmationCode`
  - 成功返回 `taskId`
- `POST /address/search/tasks/rebuild/installation`
  - 请求体包含 `confirmationCode`
  - 成功返回 `taskId`
- `GET /address/search/tasks`
  - 查询重建任务列表
- `GET /address/search/tasks/{taskId}`
  - 查询单个重建任务详情与进度
- `GET /address/search/repair/tasks`
  - 查询 repair 列表
- `POST /address/search/repair/{taskId}/execute`
  - 请求体包含 `confirmationCode`
  - 同步执行单条 repair 重放

接口语义约束：

- 口令错误返回业务失败，不创建任务。
- 存在运行中写任务时返回冲突提示。
- `overview` 查询失败时不影响页面加载，但应显式返回 ES 不可用状态，而不是吞掉错误。

### 5.6 本地联调设计

联调直接复用仓库已有 `script/docker/docker-compose.yml` 中的真实服务：

- Redis：`127.0.0.1:6379`，密码 `ruoyi123`
- Elasticsearch：`127.0.0.1:9200`
- Kibana：`http://127.0.0.1:5601`

`ruoyi-address` standalone 模式需同步满足：

- 取消 `application-standalone.yml` 中对 `Redisson`、`Lock4j` 自动配置的排除
- 配置本地 Redis 连接
- 配置 Easy-ES 地址为 `127.0.0.1:9200`
- 保持地址模块 ES alias 配置与检索设计文档一致

前端联调目标：

- 能看见真实 ES 概览
- 能触发标准地址重建并看到进度推进
- 能看到 alias 切换后的索引指向变化与文档数变化
- 能读取 repair 列表并执行单条重放

## 6. 错误处理

- ES 不可达：概览接口返回 `esReachable=false`；重建任务在 `PRECHECK` 阶段快速失败并回写 `FAILED`。
- Redis 协调失败：直接拒绝所有运维写请求，避免产生并发歧义。
- 口令错误：返回明确提示，不创建任务。
- 任务执行异常：状态改为 `FAILED`，页面展示错误摘要；不自动重试。
- repair 重放异常：沿用现有 repair 状态回写逻辑，同时在页面上显示错误信息。

## 7. 测试与验收口径

### 7.1 后端测试

至少补齐以下测试：

- 重建任务创建、状态流转、阶段切换、进度更新
- Redis 串行门禁冲突
- ES 预检查失败后的快速失败
- 概览接口聚合逻辑
- repair 列表查询与手工重放

### 7.2 前端测试

前端已有 `vitest`，第一版测试重点放在：

- API 封装参数与返回映射
- 口令确认与任务状态轮询的状态管理逻辑

页面层面以手工 smoke 为主，不在第一版额外引入重型 UI 测试框架。

### 7.3 联调验收

第一版的最小验收闭环：

1. 通过 docker compose 拉起本地 `Redis + Elasticsearch + Kibana`
2. 地址模块 standalone 成功连接本地 Redis 与 ES
3. 前端 ES 运维页成功展示概览
4. 成功触发一次标准地址重建任务并看到进度变化
5. 任务完成后 alias 正常切换，文档数符合预期
6. repair 列表可见，并能手工重放一条任务
7. 在重建进行中再次触发任意运维写操作时，被 Redis 串行门禁拒绝

## 8. 非目标

以下内容不纳入第一版：

- 嵌入式 Kibana 页面
- 自动告警、消息通知
- 批量 repair 重放
- 索引对账明细或深度比对报表
- 将该运维台抽象为跨模块通用平台
