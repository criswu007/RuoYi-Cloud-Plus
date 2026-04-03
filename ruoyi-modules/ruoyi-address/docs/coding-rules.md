# ruoyi-address 编码规则

## 1. 按交互定义查询

- 查询方式先由前端交互决定，而不是先按“大表/小表”分流：
  - 列表页返回分页结果；
  - 候选搜索返回受限结果；
  - 下拉类接口返回列表结果。
- 数据量只影响优化策略，不改变交互语义：高数据量链路要更严格控制扫描范围、关联数量和补数方式。
- 禁止为了返回展示字段，在主查询中直接做无必要的自关联、跨事实表 join，或先查全集再在 Java 侧分页。

推荐模式：

1. 主查询只取当前页必要字段；
2. 从当前页结果中提取关联键；
3. 用批量查询补齐父级名称、层级、展示字段；
4. 在服务层统一回填到 VO。

## 2. 禁止 N+1

- 列表、分页、详情聚合场景禁止逐条调用详情接口或逐条访问 mapper。
- 安装地址回填标准地址名称、标准地址回填父级名称、按类型回填层级，必须走批量查询。
- 对稳定字典数据可以一次性加载后在内存中映射，但不能对当前结果逐条发起 SQL。

## 3. 分页与候选限制

- 常规分页统一使用 `PageQuery.build()`。
- 候选搜索统一使用 `new Page<>(1, limit, false)`，由 MyBatis-Plus 分页插件下推数据库限制。
- 下拉类接口不强行套用分页语义，但必须结合交互上限在服务层或查询层控制返回规模。
- 禁止在 XML 中手写 `limit`、`rownum`、`fetch first` 等数据库方言分页语法。

## 4. 跨库 SQL 兼容

为了同时兼容 Oracle、MySQL 等数据库，Mapper XML 统一遵守以下约束：

- 禁止依赖数据库专有语法、厂商扩展函数或特定方言能力来完成核心业务判断；能用标准 SQL 表达的逻辑必须优先使用标准 SQL；
- 无法通过通用 SQL 稳定表达、且会影响多库兼容性的差异逻辑，统一上移到 Java 服务层或方言适配层处理，不能直接散落在业务 Mapper XML 中；
- 用 `coalesce` 替代 `ifnull`；
- 用 `<bind>` + `like #{keywordLike}` 替代 `concat('%', #{keyword}, '%')`；
- 用 `current_timestamp` 替代 `now()`；
- 详情查询按唯一键过滤，不写 `limit 1`；
- 业务 Mapper 禁止使用 `select *`，必须显式声明列清单；
- 业务 Mapper 禁止使用 `case when`；层级映射、特殊排序和差异判定统一上移到 Java 服务层处理。

## 4.1 一二级区域查询排序

- `spc_region` 承载的一二级标准地址查询、分页查询、候选查询默认不声明排序字段，避免在 Mapper 中额外固化排序语义。
- 标准地址列表页若查询条件命中一二级地址，前端不得提交 `orderByColumn/isAsc`；若其他调用方误传排序参数，后端也必须在区域查询链路中忽略。
- 若后续交互确需自定义排序，必须先确认该排序规则对多库执行计划、索引命中和分页稳定性无副作用，再单独评审后落地。

## 5. 字段返回约束

- 列表接口只返回当前页面真正需要的字段，不为了“以后可能会用”提前多查列。
- 详情接口可以比列表接口多查业务必需字段，但仍然要避免无意义关联。
- VO 中的展示衍生字段如果可通过批量补齐得到，优先在服务层补齐，不放在主查询里直接 join。

## 6. 地址级别统一口径

- 一二级地址是否走区域投影，由 `segm_addr_type.addr_type_id` 统一判定。
- `ADDR_SEGM.segm_type`、级别下拉、搜索过滤、编辑页默认值必须共享同一套字典解释逻辑。
- 禁止前后端再次引入旧 `level_id=1/2` 的业务硬编码分支。
- 前后端交互中的“1-19 级地址”统一使用 `addrLevel` 表示；`levelId` 仅表示数据库真实 `segm_addr_type.level_id`。
- `addrLevel` 必须由 `segm_addr_type.level_id` 升序做稠密映射得到，不能把数据库 `level_id` 直接当作业务级别返回或提交。

## 7. 命名与合同收口

- 区域主数据表名统一写 `spc_region`，不再在代码、文档、SQL、接口字段中引入其他旧别名。
- 标准地址主模型、SQL 列别名、前端状态字段统一优先使用线上库 canonical 命名，例如 `segmId/parentSegmId/segmName/standName/segmType/levelId/addrLevel/notes`。
- 安装地址主模型统一优先使用 `setAddrId/setAddrName/setAddrNo/setType/segmId/segmType/regionId/orgId/notes/bossOp`，避免继续扩散 `standardAddressId/installName/resourceType` 一类泛化命名。
- 合并接口合同固定为 `sourceSegmIds + targetSegmId`，拆分接口合同固定为 `sourceSegmId + splitItems`；`splitItems` 只允许提交 `segmName`，继承字段由服务层从源地址复制。
- 导入批次模型统一使用 `batchId/batchNo/fileName/totalCount/successCount/failCount/updateSupport/errorMsg`，失败明细统一使用 `batchId/rowNum/parentStandName/segmName/segmType/addrLevel/failReason/rawPayload`。
- 兼容层如果必须保留旧命名，必须在注释、Mapper 别名或文档中显式标出其对应的线上物理字段与当前推荐名，禁止在新增代码里继续把旧命名当主命名扩散。

## 8. 测试要求

- 修改核心查询链路时，至少覆盖以下一类测试：
  - 批量补齐 / 避免 N+1；
  - 分页下推到数据库；
  - 核心 XML 不含跨库不兼容片段。
- 如果新增了跨层级、跨表的补齐逻辑，优先补服务层单测锁定行为。

## 9. 暂未闭环异常收口

- 本轮开发、联调、测试中若出现“暂时无法处理”的异常问题，禁止只停留在聊天记录、临时笔记或口头说明中。
- 必须统一登记到 [`todo.md`](./todo.md)，至少写明：
  - 发现日期；
  - 现象与影响范围；
  - 复现方式或复现线索；
  - 临时规避方案；
  - 下一步建议；
  - 当前状态。
- 同一问题后续若有新证据，直接更新原记录，不重复散落到多个文档。
- 对外汇报或阶段性收口时，凡未解决异常都应以 `todo.md` 为唯一清单来源。
