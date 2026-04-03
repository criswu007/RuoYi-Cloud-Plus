# `ruoyi-address` 文档导航

## 项目地址

1. 前端：ruoyi-address-ui
2. 后端：ruoyi-modules/ruoyi-address
3. 原型网页：https://ftthtest.wetrytech.com/#/login 账号密码：shazhengbo/Wetry2026!
4. 线上数据库：

     url: jdbc:mysql://82.156.6.111:4000/ftth_cloud_address?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true
     username: address
     password: Wetry2328!

> 当前主线目标是先完成 `ruoyi-address` 标准地址与网格模块的程序设计前置工作。主视角只保留少量活跃文档，旧批次、旧设计稿和过程材料统一收口到 [archive/README.md](./archive/README.md)。

## 程序设计前先看这 5 份

1. [project-baseline.md](./project-baseline.md)：项目范围、阶段、风险、外部依赖总控入口。
2. [historical-schema-baseline.md](./historical-schema-baseline.md)：当前开发必须遵循的库表结构主基线。
3. [interface-checklist.md](./interface-checklist.md)：接口能力边界、关键业务规则、legacy 兼容边界。
4. [historical-inventory/grid-module-structure-baseline.md](./historical-inventory/grid-module-structure-baseline.md)：网格模块历史结构和需求落地入口。
5. [reference/vibe-coding-api-manual.md](./reference/vibe-coding-api-manual.md)：Controller/Service/BO/VO 设计时查参数、报文和实现细节。

如果只想快速进入 `ruoyi-address` 程序设计，上面 5 份已经够用。

## 按场景找文档

| 场景 | 首选文档 | 说明 |
| --- | --- | --- |
| 项目启动 / 范围对齐 | [project-baseline.md](./project-baseline.md) | 看范围、阶段、风险和依赖。 |
| 标准地址结构建模 | [historical-schema-baseline.md](./historical-schema-baseline.md) | 看当前主开发结构基线及禁止事项。 |
| 接口开发 / 联调准备 | [interface-checklist.md](./interface-checklist.md) | 看接口边界、关键规则、兼容性约束。 |
| 联调异常 / 暂存待办 | [todo.md](./todo.md) | 看本轮暂未闭环的问题、规避方式和后续建议。 |
| 需求书章节到实现映射 | [project-baseline.md](./project-baseline.md) | 看 `2.3.5`、`2.4`、`2.5`、`2.6`、`2.7`、`2.8` 如何落到 `ruoyi-address`。 |
| 字段释义 / 表结构速查 | [ftth-cloud-address-schema-handbook.md](./historical-inventory/ftth-cloud-address-schema-handbook.md) | 看字段、来源和候选值。 |
| 证据追溯 | [historical-inventory/README.md](./historical-inventory/README.md) | 看线上盘点结论、证据和后续模块入口。 |
| 网格开发落地 | [grid-module-structure-baseline.md](./historical-inventory/grid-module-structure-baseline.md) | 看网格组织、网格地址、客户、经理的历史结构与需求约束。 |
| 代码落地细节 | [reference/vibe-coding-api-manual.md](./reference/vibe-coding-api-manual.md) | 实现期参考册，含参数、报文、全局实现约束和验收映射。 |
| 旧批次 / 旧设计稿追溯 | [archive/README.md](./archive/README.md) | 已归档，不再作为当前开发主依据。 |

## 需求书章节映射

- `2.3.5.5 标准地址管理`、`2.3.5.6 网格管理`、`2.3.5.7 AI 智能体`、`2.3.7 告警监控` 对 `ruoyi-address` 的实现影响，优先看 [project-baseline.md](./project-baseline.md)。
- `2.3.5.5.*` 和 `2.3.5.6.*` 的接口、字段、兼容边界，优先看 [interface-checklist.md](./interface-checklist.md) 和 [reference/vibe-coding-api-manual.md](./reference/vibe-coding-api-manual.md)。
- `2.4 性能要求`、`2.5 对接要求`、`2.6 部署要求`、`2.7 运维要求` 的工程化约束，优先看 [project-baseline.md](./project-baseline.md)。
- `2.8 标后测试` 中与 `ruoyi-address` 直接相关的功能项和通过门槛，优先看 [project-baseline.md](./project-baseline.md) 与 [reference/vibe-coding-api-manual.md](./reference/vibe-coding-api-manual.md)。

## 当前活跃文档分层

- 核心基线：`project-baseline.md`、`historical-schema-baseline.md`、`interface-checklist.md`
- 待办与异常收口：`todo.md`
- 当前线上盘点：`historical-inventory/online-schema-comment-inventory-report.md`、`historical-inventory/ftth-cloud-address-schema-handbook.md`、`historical-inventory/evidence/online-schema-comment-inventory-evidence.md`
- 网格与历史样例入口：`historical-inventory/grid-module-structure-baseline.md`
- 实现期参考：`reference/vibe-coding-api-manual.md`
- 归档：`archive/*`

## 后续模块入口

- 安装地址：当前先沿用 `ADDR_SET_SEGM` 在线盘点结论，独立专题后续再补。
- 网格模块：从 [grid-module-structure-baseline.md](./historical-inventory/grid-module-structure-baseline.md) 进入。
- 其他关联模块：统一在 [historical-inventory/README.md](./historical-inventory/README.md) 下继续补入口，不再单独散落新文档。

## 维护约定

- 新增正式文档前，优先判断能否并入现有核心基线或盘点索引。
- “当前生效的结构、接口、规则”只写在活跃文档，不写在过程稿里。
- 标准地址 / 安装地址命名收口与代码落地约束，统一以 [project-baseline.md](./project-baseline.md)、[historical-schema-baseline.md](./historical-schema-baseline.md) 与 [reference/vibe-coding-api-manual.md](./reference/vibe-coding-api-manual.md) 为准；若示例仍出现 legacy 占位名，内部主模型仍优先按 `segmId/setAddrId/segmType/levelId/notes` 等推荐命名实现。
- 需求书新增备注、系统级约束和标后测试门槛，必须同步回主线活跃文档，不能只留在临时分析记录里。
- 本轮暂时无法处理的异常问题，必须统一登记到 [todo.md](./todo.md)，不得只留在聊天记录、截图或临时脚本输出里。
- 标准地址模块已启用可视化辅助作为原型交互与页面效果比对手段；后续涉及标准地址原型还原、页面状态核对与交互差异定位时，默认可直接使用，无需重复确认。
- 旧批次盘点、旧设计稿、AI 过程文档统一进 `archive/`，不再占用主入口。
