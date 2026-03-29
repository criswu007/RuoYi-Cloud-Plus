# 标准地址服务项目配置

## 已明确的范围与约束
- 标准地址模块以独立服务存在，整个项目为微服务架构。
- 标准地址层级包含标准地址基本维护中的 19 级。
- 选址平台功能仅要求到第 11 级（建筑、楼栋级）。
- 非标地址监控在本期范围内，工单系统不由本模块开发，但需要完成对接。
- 一二级地址为系统预置不可变数据，不允许通过页面、接口或导入新增/修改/删除/合并/拆分。
- 合并规则：目标地址层级必须高于待合并地址（层级数值更小）。
- 批量新增地址场景复用名称模糊查询接口选择父级地址，前端单次候选限制 `200` 条。
- 标准地址查询口径需兼容 legacy 来源：当查询或返回 `1/2` 级行政区划时优先按 `spec_region` 口径处理，其余层级按 `segm_addr` / 标准地址事实表处理；层级定义以 `segm_addr_type.level_id` 为准。需求书写作 `spec_region`，历史盘点现有结构索引写作 `spc_region`，实现前需核对实际库名。
- `城区/非城区` 与 `城乡属性` 不是同一字段：前者复用 `addr_segm.is_city`，后者复用 `addr_segm.area_type`，枚举来自 `pub_restriction.keyword='AREA_TYPE'`。
- 地址名称变更后，需同步刷新地址拼装名称、地址拼装简拼及关联安装地址快照；简拼规则按“中文转拼音首字母大写、中文括号转英文括号、数字不变”执行。
- 安装地址正常业务口径下必须关联标准地址；未关联安装地址仅保留给历史迁移、脏数据治理和异常清理场景。
- 管理站主数据当前参考历史库 `spc_station` 并按 `region_id` 划分，源数据暂不区分业务类型，系统侧需补齐 `维修/安装/营业` 三类关系角色。
- 管理站类型字典来自 `pub_restriction.keyword='MANAGE_TYPE'`：`2017101=维修`、`2017102=安装`、`2017103=营业`。

## 历史盘点基线
- 当前项目允许合并使用两套历史材料作为标准地址证据基线：
  - `nanjing-standard-address-core-*`：南京历史批次专题盘点，适合看历史事实链路、数据分布、空值率和旧系统字段口径。
  - `online-schema-comment-inventory-*` / `ftth-cloud-address-schema-handbook.md`：当前线上最新非备份表结构盘点，适合看最新字段注释、表结构边界和“是否可直接入模”的判断。
- 合并使用规则：结构定义、字段注释、类型映射优先参考“线上最新非备份表结构”；历史值域、空值率、迁移兼容链路与样本事实补充参考“南京历史批次”；若与 `0323` 需求书正文或备注冲突，以需求书为准。
- 需求书写作 `spec_region`，历史盘点现有结构索引写作 `spc_region`；本项目文档统一按“区域主数据”理解，实现前需核对联调库实际表名。
- `spc_station`、`spc_region` 当前已可与需求书备注合并引用：字段结构与候选字段解释参考历史盘点，业务约束按需求书中“按 `region_id` 划分、类型来自 `MANAGE_TYPE`”执行；若联调库现状与两者不一致，以实表核验结果修正。

## 不确定项与兼容方案
- 新旧系统关系暂未明确，需预留兼容性方案：
  - 支持历史数据迁移的全量导入路径。
  - 支持后续增量同步或双轨运行的扩展点。

## 外部系统对接范围
- BOSS
- 工单系统
- 企业微信
- 大数据平台
- 地图系统

## 项目配置-数据库
- `ruoyi-address.sql` 文件编码为 `UTF-8`，导入时必须显式指定客户端字符集为 `utf8mb4`，否则字段与表的 `comment` 会出现乱码。
- 江苏省一二级标准地址由 SQL 预置生成，不经过业务接口写入。


## 项目开发参考附件
- 原型网址
    ```
    https://ftthtest.wetrytech.com/#/login

    shazhengbo/Wetry2026!
    ```
- 项目需求书
  -  /Users/criswu/Desktop/广电/需求/技术需求书_标准地址_待明确问题0323.docx
- 需兼容的接口文档
  - /Users/criswu/Desktop/广电/开发/旧接口兼容/广电选址平台项目_需求分析说明书_BOSS系统标准地址接口.docx
  - /Users/criswu/Desktop/广电/开发/旧接口兼容/江苏广电标准地址接口规范20230713.docx
- 历史盘点与结构手册
  - /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/docs/historical-inventory/nanjing-standard-address-core-report.md
  - /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/docs/historical-inventory/online-schema-comment-inventory-report.md
  - /Users/criswu/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/docs/historical-inventory/ftth-cloud-address-schema-handbook.md
  - 历史数据库
      ```yaml
        url: jdbc:mysql://82.156.6.111:4000/ftth_cloud_address?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true
        username: address
        password: Wetry2328!ß
      ```
- 性能要求
  - 系统支持同一时间至少3000人共同使用的并发。
  - 用户登录时间<= 3秒，系统页面查询渲染<=5秒，系统内在线事务处理的响应时间<= 5秒。
  - 在业务高峰期间，系统平均响应时间要求不超过非业务高峰期间平均响应时间的1.2倍。
  - 系统可用性需要达到99.99％，MTBF(平均无故障时间)>365天   MTTR(平均修复时间)<30分钟。
  - 在业务高峰期CPU利用率不高于60％；内存利用率不高于60％；存储利用率不高于70％。
  - 系统数据正常处理能力不低于2000条/秒；风暴处理能力不低于6000条/秒。
  - 默认支持7天数据本地存储要求，需要扩展存储时长，视具体的存储空间而定，原则上支持永久存储。
  - 系统支持数据库、存储媒介等重要生产数据的热备份。
  - 系统需使用HTTPS数据传输协议、数据加密传输、具备完善的输入性数据校验。
  - 系统无任何SQL注入、XSS攻击、SSRF漏洞、XXE漏洞、弱口令、代码执行漏洞、文件包含漏洞等渗透风险
