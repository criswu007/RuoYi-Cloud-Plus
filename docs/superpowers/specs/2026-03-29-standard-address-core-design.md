# 标准地址核心主链路设计

## 1. 背景与目标

本设计仅覆盖 `ruoyi-address` 标准地址模块第一批可编码主链路，直接基于线上数据库表结构实现，不再参考仓库内历史自建表模型。首批目标是尽快落成可运行、可验证、可继续扩展的标准地址核心能力，为后续导入、日志、标签、合并、拆分和 legacy 兼容能力提供稳定基线。

本轮目标能力：

- 标准地址列表
- 标准地址详情
- 标准地址新增
- 标准地址修改
- 标准地址删除
- 批量下级地址预览
- 批量新增下级地址
- 标准地址导出

本轮不纳入实现范围：

- legacy 外部兼容接口
- BOSS / 工单 / 地图 / 企业微信联调
- 标签、导入、操作日志、合并、拆分
- 非标地址监控、工单、网格

## 2. 已确认决策

### 2.1 数据源决策

- 标准地址主事实表使用 `ADDR_SEGM`
- 地址类型与层级解释使用 `segm_addr_type`
- 字典值解释使用 `pub_restriction`
- 区域主数据统一使用 `spc_region`
- 管理站展示补充统一使用 `spc_station`

### 2.2 查询分流决策

- 当标准地址层级查询条件为 `1` 或 `2` 时，查询 `spc_region`
- 当标准地址层级查询条件为其他值时，查询 `ADDR_SEGM`
- 对 `ADDR_SEGM` 的层级条件，直接作用于 `ADDR_SEGM.segm_type`
- `segm_addr_type` 不作为查询主入口，而作为层级顺序解释、展示映射和写操作校验依据

### 2.3 命名决策

- 标准地址主模型统一使用 `segmId/parentSegmId/segmName/segmNo/standName/standNo/segmType/levelId/regionId/districtId/serviceRegionId/status/notes`
- 主键统一按 `varchar(24)` 字符串处理，不再使用 `Long`
- `levelId` 为投影字段，不在 `ADDR_SEGM` 主表直接落库
- `status` 统一按 `ADDR_SEGM_STATUS` 字典值处理，不再抽象成 `0/1`

## 3. 实现范围

### 3.1 列表与详情

- 列表支持按名称、父级、层级、状态、区域等条件查询
- 详情支持返回基础地址信息、只读标记、管理站名称补充字段
- `1/2` 级详情按 `spc_region` 投影返回
- `3+` 级详情按 `ADDR_SEGM` 返回

### 3.2 写操作

- 新增、修改、删除、批量下级预览、批量新增全部只作用于 `ADDR_SEGM`
- `1/2` 级地址统一视为外部基础数据，只允许查询，不允许写操作
- 写操作全部围绕真实 `segm_type` 和其对应层级规则执行

### 3.3 导出

- 导出字段与列表字段口径保持一致
- 首批至少支持按当前筛选条件导出标准地址结果
- “导出本页 / 导出选中 / 自选字段导出”接口协议预留在服务层和导出模型中，但具体交互细节可在第二阶段补齐

## 4. 领域模型设计

### 4.1 `ADDR_SEGM` 主模型

首批实现直接围绕以下主字段建模：

- `segm_id -> segmId`
- `parent_segm_id -> parentSegmId`
- `segm_name -> segmName`
- `segm_no -> segmNo`
- `stand_name -> standName`
- `stand_no -> standNo`
- `segm_type -> segmType`
- `region_id -> regionId`
- `district_id -> districtId`
- `service_region_id -> serviceRegionId`
- `status -> status`
- `notes -> notes`
- `create_date -> createDate`

首批预留但不强制参与写入的扩展字段：

- `is_city -> isCity`
- `station_id -> stationId`
- `installstation_id -> installStationId`
- `busstation_id -> busStationId`
- `addr_in_type_ftth -> addrInTypeFtth`
- `ftth_pon_type -> ftthPonType`
- `addr_in_type_lan -> addrInTypeLan`
- `area_type -> areaType`
- `place_type -> placeType`
- `cover_num -> coverNum`
- `post_code -> singleProjectCode`
- `segm_name_fir -> supportingFeeCommunityFlag`

### 4.2 `levelId` 投影规则

- `ADDR_SEGM` 实际存储的是 `segm_type`
- `levelId` 来自 `segm_addr_type.level_id`
- 服务层需要提供 `segmType -> levelId` 的映射能力
- 写操作校验父子层级关系时，统一基于 `levelId` 判断

### 4.3 `spc_region` 投影规则

`spc_region` 不作为标准地址主实体落库，仅作为 `1/2` 级查询投影：

- `region_id -> segmId`
- `super_region_id -> parentSegmId`
- `region_name -> segmName`
- `region_name -> standName`
- `region_no -> segmNo` 或附加编码展示字段
- `notes -> notes`

`1/2` 级查询结果需补充：

- `levelId`
- `readOnlyFlag`
- `canEdit/canDelete/canMerge/canSplit = false`

## 5. 服务架构设计

### 5.1 分层

- `controller`：只负责协议入参、出参与参数绑定
- `application service`：负责按层级条件分流 `spc_region` 与 `ADDR_SEGM`
- `domain service`：负责名称生成、层级校验、只读保护、删除校验
- `mapper`：负责 `ADDR_SEGM`、`segm_addr_type`、`spc_region`、`spc_station` 的数据访问

### 5.2 查询链路

建议拆分为两条查询服务：

- `SpcRegionQueryService`
  - 负责 `1/2` 级列表与详情查询
  - 只读
  - 结果投影到统一标准地址 VO

- `AddrSegmQueryService`
  - 负责 `3+` 级列表与详情查询
  - 负责写操作需要的主表查询

对外统一由 `StandardAddressService` 聚合：

- 若 `levelId in (1,2)`，转 `SpcRegionQueryService`
- 否则转 `AddrSegmQueryService`
- 未传层级时，默认查询 `ADDR_SEGM`

### 5.3 写链路

写链路统一由 `StandardAddressCommandService` 承担：

- 新增：校验父级、推导 `segmType/levelId` 合法性、生成 `standName/standNo`
- 修改：校验只读保护、校验层级变化、必要时级联刷新下级 `standName/standNo`
- 删除：先校验下级，再校验关联资源，再校验安装地址并支持二次确认
- 批量下级预览/新增：基于父级地址和命名规则生成待写入项

## 6. 核心业务规则

### 6.1 只读保护

- `1/2` 级地址视为外部基础数据
- 一律不允许新增、修改、删除、合并、拆分
- 只读判断统一基于真实层级语义，不依赖旧表字段

### 6.2 层级规则

- 底层结构不按固定 `14` 段写死
- 校验统一以 `segm_addr_type.level_id` 为准
- `segm_type` 为保存字段，`levelId` 为展示和校验字段
- “楼栋级”仅是业务语义，不等于固定数字层级

### 6.3 名称与简拼规则

- `segmName` 表示当级名称
- `standName` 表示完整标准地址全称
- `segmNo` 表示当级简拼
- `standNo` 表示完整标准地址简拼
- 修改名称或父级后，必须级联刷新下级地址的 `standName/standNo`
- 简拼生成规则统一为“中文转拼音首字母大写、中文括号转英文括号、数字不变”

### 6.4 删除规则

- 先校验是否存在下级地址
- 再校验是否存在关联资源
- 然后校验是否有关联安装地址
- 有安装地址关联时，通过 `confirm` 执行二次确认删除

## 7. 对外接口设计

首批接口仍维持文档中已冻结的管理端协议形式：

- `POST /address/standard/list`
- `POST /address/standard/{segmId}`
- `POST /address/standard`
- `POST /address/standard/update`
- `POST /address/standard/remove/{segmIds}`
- `POST /address/standard/batchPreviewChild`
- `POST /address/standard/batchAddChild`
- `POST /address/standard/export`

接口返回统一使用仓库现有：

- `R<T>`
- `TableDataInfo<T>`
- `PageQuery`

## 8. 返回模型设计

统一标准地址 VO 至少包含：

- `segmId`
- `parentSegmId`
- `segmName`
- `segmNo`
- `standName`
- `standNo`
- `segmType`
- `levelId`
- `regionId`
- `districtId`
- `serviceRegionId`
- `status`
- `notes`
- `readOnlyFlag`
- `canEdit`
- `canDelete`
- `canMerge`
- `canSplit`
- `stationId/installStationId/busStationId`
- `stationName/installStationName/busStationName`
- `createDate`

## 9. 测试策略

### 9.1 查询分流测试

- `levelId=1` 时命中 `spc_region`
- `levelId=2` 时命中 `spc_region`
- `levelId=3+` 时命中 `ADDR_SEGM`
- 未传层级时默认命中 `ADDR_SEGM`

### 9.2 规则测试

- 一二级只读保护
- `segm_type` 对应层级合法性校验
- 名称与简拼生成
- 修改后的级联刷新
- 删除前下级 / 关联资源 / 安装地址拦截

### 9.3 集成测试

- `spc_region` 投影结果正确性
- `ADDR_SEGM` 按 `segm_type` 查询正确性
- 导出字段与列表一致

## 10. 风险与后续扩展

### 10.1 当前接受的限制

- 首批不实现 legacy 外部兼容接口
- 首批不实现标签、导入、日志、合并、拆分
- 首批不实现完整管理站关系维护

### 10.2 第二阶段建议

- 导入与导入回滚
- 操作日志与审计
- 标签能力
- 合并与拆分
- 管理站与地址属性完整维护
- legacy 协议兼容

## 11. 开工顺序建议

1. 先统一 `ruoyi-address` 文档中的 `spec_region` 为 `spc_region`
2. 建立基于线上库的标准地址实体、BO、VO 和 Mapper
3. 完成查询分流和只读保护
4. 完成列表、详情、新增、修改、删除
5. 完成批量下级预览 / 新增、导出
6. 补测试和回归验证
