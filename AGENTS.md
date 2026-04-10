# Repository Guidelines

## 项目结构与模块组织
`ruoyi-auth`、`ruoyi-gateway`、`ruoyi-modules/*` 是核心服务与业务模块；`ruoyi-address-ui` 是独立前端；`ruoyi-common/*` 放公共组件；`ruoyi-api/*` 放共享接口与 BOM；`ruoyi-visual/*` 放平台配套；`script/docker`、`script/config` 放环境资源。测试代码位于各模块 `src/test/java`。

## 构建、测试与开发命令
- `mvn -T1C -DskipTests package`：全量构建，仓库默认跳过测试。
- `mvn -pl ruoyi-auth -am spring-boot:run`：启动单个后端服务。
- `mvn -pl ruoyi-modules/ruoyi-address -am package`：只构建标准地址相关模块。
- `mvn -DskipTests=false test`：显式执行 Maven 测试。
- `cd ruoyi-address-ui && npm run dev`：启动前端开发环境。
- `cd ruoyi-address-ui && npm run build` / `npm test`：构建或执行 Vitest。

## 编码风格与命名
Java 遵循 Alibaba Java 规范，统一 4 空格缩进；包名小写，类名与模块职责对齐。配置文件放在 `src/main/resources`，按环境使用 `bootstrap-*.yml` 或 `application-*.yml`。说明与注释统一中文，命令和专有名词保留原文。项目已广泛使用 Lombok，除非必要不要手写样板代码。
- 后端接口路径禁止使用中划线命名；优先采用多级路径（如 `/address/import/batch`），确需单段路径时使用驼峰命名。

## 注释与测试要求
`ruoyi-modules/ruoyi-address` 下的类、接口、方法注释需写明目的、入参/出参、关键约束、异常与副作用；核心业务方法和对外接口必须写详细注释，DTO/VO 仅保留字段注释。后端测试优先使用 `spring-boot-starter-test`，测试类命名采用 `*Test` 或 `*Tests`；前端使用 Vitest。改动查询链路、跨库兼容或性能敏感逻辑时，必须补测试。

## 提交与 PR 指南
近期提交常见前缀有 `fix`、`docs`、`refactor`，也存在直接用模块名作主题的提交；建议统一为清晰、可检索的风格，例如 `fix ruoyi-address: 优化标准地址查询`。PR 应包含变更摘要、影响范围、配置或数据迁移说明；涉及页面变更时附截图，并关联 Issue。

## 配置与安全提示
根 `pom.xml` 默认启用 `dev` profile，启动前确认 Nacos、Logstash 等外部配置可用。禁止提交密码、密钥和线上地址等敏感信息，优先通过本地环境配置或 Nacos 管理。进入 `ruoyi-modules/ruoyi-address` 深度开发时，还需同时遵守该目录下的模块级 `AGENTS.md`。
