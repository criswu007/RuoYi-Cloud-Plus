# 仓库指南

## 项目结构
- `ruoyi-auth`, `ruoyi-gateway`, `ruoyi-modules/*`：核心微服务与业务模块。
- `ruoyi-common/*`：跨服务共享的基础库与 Starter。
- `ruoyi-api/*`：共享 API 约定与 BOM。
- `ruoyi-visual/*`：可视化与平台组件（监控、Nacos、Seata、SnailJob）。
- `ruoyi-example/*`：示例与 MQ 案例模块。
- `script/`：Docker 与环境搭建资源（见 `script/docker`、`script/config`）。

## 构建、测试与开发命令
- 构建全部模块（本仓库默认跳过测试）：`mvn -T1C -DskipTests package`。
- 构建单个服务及依赖：`mvn -pl ruoyi-auth -am package`。
- 本地运行服务：`mvn -pl ruoyi-auth -am spring-boot:run`。
- 显式执行测试（覆盖默认跳过）：`mvn -DskipTests=false test`。
- Docker 环境与编排文件在 `script/docker`。

## 编码风格与命名
- Java 遵循 Alibaba Java 规范，缩进 4 空格。
- 包名与类名尽量与模块对齐（示例模块：`ruoyi-system`）。
- 配置放在 `src/main/resources`，按环境区分 `bootstrap*.yml` 或 `application*.yml`。
- 项目广泛使用 Lombok，除非必要不要手写样板代码。

## 测试指南
- 根目录配置 Maven Surefire，测试目录遵循 `src/test/java` 与 `src/test/resources`。
- 推荐使用 `spring-boot-starter-test` 编写单元与切片测试。
- 测试类命名使用 `*Test` 或 `*Tests` 以便 Surefire 识别。

## 提交与 PR 指南
- 近期提交风格常见前缀：`fix`、`update`、`refactor`、版本发布。
  示例：`fix 修复 xxx`。
- 提交应聚焦且描述清晰，必要时注明模块范围。
- PR 需包含变更摘要、关联 Issue 与配置或迁移说明；
  仅在涉及 UI 变更时附截图。

## 配置与环境提示
- 默认 profile 为 `dev`（见根 `pom.xml`）；运行前确认 Nacos 与 Logstash 地址。
- 禁止提交密钥与敏感配置，使用环境配置文件或 Nacos 管理。
