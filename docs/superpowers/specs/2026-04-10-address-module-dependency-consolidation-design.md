# ruoyi-address 外部依赖改动收口设计

## 背景

当前仓库以 `ruoyi-modules/ruoyi-address` 作为标准地址核心后端模块，但围绕该模块的近期开发，已经把一部分地址业务定制改到了外部依赖模块中，主要包括：

- `ruoyi-common/ruoyi-common-excel`
- `ruoyi-common/ruoyi-common-dubbo`

与此同时，仓库内也存在 `ruoyi-modules/ruoyi-workflow`、`ruoyi-address-ui`、`script/*`、Docker 数据目录等其他改动，但本次已经明确：

- `ruoyi-workflow` 改动忽略，不纳入本次迁移
- `ruoyi-address-ui`、`script/*`、运行数据、构建产物不纳入本次迁移
- 本次只处理可物理收口到 `ruoyi-address` 的后端源码与配置文件

本次目标不是简单“能跑就行”，而是把原本只服务于地址模块的定制，从公共模块源码中剥离出来，重新收口到 `ruoyi-address` 内部，降低后续升级、对齐上游和模块维护的成本。

## 目标

- 清理 `ruoyi-common-excel` 中仅服务于地址模块的源码改动
- 清理 `ruoyi-common-dubbo` 中仅服务于地址模块的源码改动
- 保持 `ruoyi-address` 当前功能行为不回归
- 让地址模块后续新增定制优先落在 `ruoyi-address` 私有层，而不是继续侵入公共模块

## 非目标

- 不迁移 `ruoyi-workflow` 中的 standalone 联调能力
- 不处理 `ruoyi-address-ui`、`script/config`、`script/docker`、运行数据目录、`dist` 构建产物
- 不对与本次收口无关的公共能力做额外重构
- 不追求把所有 `Excel` 导出能力都复制到 `ruoyi-address`，只收口当前确有业务必要的部分

## 方案选择

本次对比三种方案：

### 方案 A：`ruoyi-address` 私有适配收口，公共模块回归原状

做法：

- 在 `ruoyi-address` 内新增地址专用 `Excel` 导出支撑类与写处理器
- 在 `ruoyi-address` 配置层显式声明 `dubbo.custom.*` 默认值
- 恢复 `ruoyi-common-excel`、`ruoyi-common-dubbo` 的公共源码改动

优点：

- 公共模块可恢复通用定位
- 地址模块行为控制权回到自身
- 代码重复可控，后续维护边界清晰

缺点：

- `ruoyi-address` 内会新增少量私有包装代码

### 方案 B：完整复制公共能力到 `ruoyi-address`

做法：

- 在 `ruoyi-address` 内复制较完整的 `ExcelUtil` 及相关能力
- 地址模块完全不再依赖公共 `Excel` 实现细节

优点：

- 地址模块隔离度最高

缺点：

- 重复代码过多
- 后续与公共库演进更容易分叉
- 超出当前问题实际需要

### 方案 C：尽量只靠调用点与配置修补

做法：

- `Dubbo` 仅补配置
- `Excel` 不建私有封装，而是在各调用点直接拼 `FastExcel` 写逻辑

优点：

- 文件数量最少

缺点：

- `Excel` 导出逻辑会散落在 controller/service
- 后续再有类似需求时仍会重复实现

本次采用 **方案 A**。

## 总体设计

### 设计原则

- 业务特化逻辑优先沉到 `ruoyi-address`，不再继续侵入 `ruoyi-common-*`
- 公共模块只保留可复用、与业务无关的通用能力
- 能通过模块配置表达的默认行为，不通过改公共源码实现
- 仅迁移当前已被地址模块实际使用的特化能力，避免一次性过度抽象

### 收口范围

本次收口分为两块：

1. `Excel` 收口
2. `Dubbo` 收口

`workflow`、前端、部署脚本与运行数据不在本次设计范围内。

## Excel 收口设计

### 现状判断

当前 `ruoyi-common-excel` 的改动主要集中在两类能力：

- 新增 `DefaultRowHeightWriteHandler`
- 给 `ExcelUtil` 新增支持 `autoWidth` 开关的重载，并在导出流程中挂载默认内容行高处理器

从调用面看，这类改动并不是公共平台层的稳定共性，而是地址模块导入模板导出场景的样式诉求，尤其适合下沉为地址模块私有导出支撑。

### 收口目标

- 地址模块保留“模板导出时可控制默认内容行高与自动列宽”的能力
- 公共 `ExcelUtil` 恢复到通用状态，不再额外承担地址模板导出特例
- 地址模块内形成明确的专用导出入口，后续类似需求继续沿该入口扩展

### 文件落点

建议在 `ruoyi-address` 新增地址专用导出支撑包，例如：

- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/excel/`

该包下建议包含两类文件：

- 地址模块私有 `SheetWriteHandler`
  - 目的：仅负责把头对象上的 `@ContentRowHeight` 同步到工作表默认内容行高
  - 关键约束：只处理地址模块模板导出需要的默认内容行高，不额外承接其他公共职责

- 地址模块私有导出工具类
  - 目的：封装地址模块真正需要的导出能力，例如：
    - 是否启用自动列宽
    - 是否挂载默认内容行高处理器
    - 是否附带下拉框
  - 关键约束：只保留地址模块当前实际用到的导出路径，不复制整份公共 `ExcelUtil`

### 调用调整

本次不要求把 `ruoyi-address` 里所有导出都切到私有工具，而是按“确有特化需求才切”的原则处理：

- 标准地址导入模板下载接口应切换到地址专用导出入口
- 其他普通列表导出、审批导出、失败明细导出，如果不依赖默认内容行高或关闭自动列宽，则继续走公共 `ExcelUtil`

这样做的原因是：

- 可以把本次新增的私有导出层控制在最小范围
- 避免为了一次收口，把所有导出调用点都做无收益改造
- 后续如果新模板也需要相同行为，再继续复用地址专用导出入口

### 公共模块回退

在地址模块完成私有承接后，需要恢复：

- `ruoyi-common-excel/.../ExcelUtil.java`
- `ruoyi-common-excel/.../DefaultRowHeightWriteHandler.java`

回退口径：

- 公共 `ExcelUtil` 删除仅为地址模块引入的 `autoWidth` 特化重载与默认内容行高处理器挂载
- `DefaultRowHeightWriteHandler` 不再保留在公共模块中

## Dubbo 收口设计

### 现状判断

当前 `ruoyi-common-dubbo` 的改动是给 `DubboCustomProperties` 增加默认值：

- `requestLog = false`
- `logLevel = INFO`

这类改动本质上属于“模块运行默认配置”，不是地址模块必须修改公共源码才能成立的能力。既然 `DubboCustomProperties` 已通过配置绑定提供扩展点，更合理的做法是由 `ruoyi-address` 自己在配置层给出明确值。

### 收口目标

- 地址模块不再依赖公共 `DubboCustomProperties` 的源码默认值
- 地址模块在自身配置中显式声明 `dubbo.custom.*`
- 公共 `ruoyi-common-dubbo` 恢复到原始公共行为

### 文件落点

本次不新增地址模块私有 Dubbo Java 实现，直接在地址模块配置层收口，涉及文件包括：

- `ruoyi-modules/ruoyi-address/src/main/resources/application.yml`
- `ruoyi-modules/ruoyi-address/src/main/resources/application-local.yml`
- `ruoyi-modules/ruoyi-address/src/main/resources/runtime/address-local-runtime-standalone.yml`
- `script/config/nacos/ruoyi-address.yml`

其中不要求所有文件都写同一份值，但必须确保地址模块实际运行入口能稳定拿到：

- `dubbo.custom.request-log=false`
- `dubbo.custom.log-level=INFO`

建议策略：

- 本地 `local` / standalone 运行所需配置写入 `application-local.yml` 或 runtime 配置
- 微服务环境所需配置补到 `script/config/nacos/ruoyi-address.yml`
- 如公共 `application.yml` 需要兜底默认值，可在不破坏环境覆盖逻辑的前提下补最小默认配置

### 公共模块回退

在地址模块配置补齐后，需要恢复：

- `ruoyi-common/ruoyi-common-dubbo/src/main/java/org/dromara/common/dubbo/properties/DubboCustomProperties.java`

回退口径：

- 去掉地址项目私有默认值，恢复为公共配置类

## 迁移顺序

### 第一步：先收 Dubbo

先补齐 `ruoyi-address` 模块自身配置，再恢复公共 `DubboCustomProperties`。

原因：

- 改动小
- 风险低
- 可以先验证“配置是否足够替代源码默认值”

### 第二步：再收 Excel

在 `ruoyi-address` 内新增私有导出支撑，并把真正依赖模板样式补丁的调用点切过去，随后恢复公共 `ExcelUtil`。

原因：

- `Excel` 牵涉调用点和导出行为，验证面比 `Dubbo` 更大
- 先收配置，再收代码，回归定位更简单

### 第三步：做回归验证

完成两块收口后，统一验证地址模块核心链路。

## 风险与约束

### Excel 风险

- 如果当前地址模块某些调用点已经隐式依赖公共 `ExcelUtil` 新增重载，公共模块恢复后会直接编译失败
- 如果模板导出样式实际依赖不止“默认内容行高”和“自动列宽”，则需要在地址私有导出层补齐同等行为，不能只做表面替换

### Dubbo 风险

- 若 `dubbo.custom.*` 没有覆盖到地址模块的实际运行入口，则恢复公共类后可能出现空值行为差异
- 需要同时考虑本地 `local` 与实际微服务配置来源，避免环境间表现不一致

### 边界约束

- 本次不因收口目标而顺手重构全部导出链路
- 不允许把 `workflow` 联调能力强行复制进 `ruoyi-address`
- 后续若再出现地址模块专属导出样式需求，必须优先扩展 `ruoyi-address` 私有导出层，而不是继续修改 `ruoyi-common-excel`

## 验证口径

完成迁移后至少验证以下内容：

### Dubbo 验证

- `ruoyi-address` 启动过程中 `DubboCustomProperties` 不因缺省值回退导致异常
- 地址模块在本地与微服务配置来源下都能稳定读取 `dubbo.custom.request-log` 与 `dubbo.custom.log-level`

### Excel 验证

- 标准地址导入模板下载后，默认内容行高与预期一致
- 模板下拉框行为不回归
- 自动列宽行为符合切换后的预期

### 地址模块回归验证

- 标准地址普通导出正常
- 标准地址导入失败明细导出正常
- 标准地址审批记录导出正常
- 受影响调用点编译通过，测试通过

## 预期结果

完成本次收口后，仓库应达到以下状态：

- `ruoyi-common-excel` 不再包含仅服务于地址模块的导出特化
- `ruoyi-common-dubbo` 不再包含仅服务于地址模块的默认值补丁
- `ruoyi-address` 自身承接地址专属导出能力与 Dubbo 默认配置
- 后续地址业务定制可以在 `ruoyi-address` 内持续演进，而不会再次污染公共模块
