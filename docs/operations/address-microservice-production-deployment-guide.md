# 标准地址微服务生产迁移与上线手册

## 1. 目标与范围

本文档用于将当前本地已联通的标准地址方案迁移到生产环境，部署形态固定为：

- `Docker Compose`
- `microservice`
- 前端 `ruoyi-address-ui`
- 后端 `ruoyi-gateway`、`ruoyi-auth`、`ruoyi-system`、`ruoyi-resource`、`ruoyi-workflow`、`ruoyi-address`
- 中间件 `MySQL`、`Nacos`、`Redis`、`RabbitMQ`、`MinIO`、`Seata`、`Elasticsearch`

本文档覆盖：

- 生产资源准备
- 数据库初始化与数据迁移
- Nacos 配置导入
- 镜像构建与 Compose 部署
- 前端发布
- 正式上线步骤与上线后核验

本文档不覆盖：

- 灰度发布
- 双活或多机房部署
- 自动化 CI/CD 编排
- 完整回滚演练

当前上线口径默认按以下约束执行：

- `ftth_cloud_address` 是地址事实源，ES 索引以数据库全量重建为准。
- 首次上线按“立刻一致”评估，地址写链路依赖 ES；若 ES 不可用，不允许放行仅数据库成功的写请求。
- 首次上线完成 ES 重建前，不要对外开放真实业务流量。

## 2. 仓库现有资产映射

生产迁移直接基于以下仓库资产执行：

- 编排基线：[script/docker/docker-compose.yml](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/script/docker/docker-compose.yml)
- Nacos 配置基线：[script/config/nacos](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/script/config/nacos)
- 平台基础库脚本：[script/sql](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/script/sql)
- 地址模块补充脚本：[ruoyi-modules/ruoyi-address/sql](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/sql)
- 前端构建入口：[ruoyi-address-ui/package.json](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-address-ui/package.json)
- 前端开发代理基线：[ruoyi-address-ui/vite.config.js](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-address-ui/vite.config.js)
- nginx 静态站点与反向代理基线：[script/docker/nginx/conf/nginx.conf](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/script/docker/nginx/conf/nginx.conf)

上线前必须注意以下现状差异：

1. 当前 Compose 文件偏本地联调，内含固定 IP、默认密码和大量 `host` 网络配置，不能直接原样上生产。
2. 当前 `datasource.yml` 的 `address-master` 指向 `ry-address` 示例库；标准地址项目生产必须改为 `ftth_cloud_address`。
3. [ruoyi-modules/ruoyi-address/Dockerfile](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/Dockerfile) 已补齐，但镜像构建前仍需确认目标 jar 与 JVM 参数符合生产要求。
4. [script/config/nacos/ruoyi-address.yml](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/script/config/nacos/ruoyi-address.yml) 已补齐为基线模板，导入 `prod` namespace 前仍需替换 ES、Kibana 与数据库真实配置。

## 3. 推荐生产拓扑

### 3.1 必需服务

- `mysql`
- `nacos`
- `redis`
- `rabbitmq`
- `minio`
- `seata-server`
- `elasticsearch`
- `kibana`
- `ruoyi-gateway`
- `ruoyi-auth`
- `ruoyi-system`
- `ruoyi-resource`
- `ruoyi-workflow`
- `ruoyi-address`
- `nginx-web`

### 3.2 可选服务

- `ruoyi-monitor`
- `ruoyi-snailjob-server`
- `ruoyi-job`
- `ruoyi-gen`
- `skywalking`
- `prometheus`

### 3.3 网络建议

本手册按“单机生产”编写，建议继续沿用当前编排里 Java 微服务的 `network_mode: host` 思路，避免 Nacos、Dubbo、Seata 在容器桥接网络下注册到错误 IP。若要改为纯 bridge 网络，需要同步重做以下配置：

- `SPRING_CLOUD_NACOS_DISCOVERY_IP`
- `SPRING_CLOUD_NACOS_SERVER_ADDR`
- `DUBBO_IP_TO_REGISTRY`
- `SEATA_IP`
- nginx upstream 地址

## 4. 生产资源准备

### 4.1 服务器与目录

建议在部署机准备统一目录，例如：

```bash
mkdir -p /opt/ruoyi-prod/{compose,nacos,nginx/html,nginx/conf,logs,mysql,redis,minio,elk,backup}
```

### 4.2 软件版本

- Docker Engine 24+
- Docker Compose Plugin 2.20+
- JDK 17
- Maven 3.9+
- Node.js 18+

### 4.3 端口规划

建议至少开放：

- `80/443`：nginx
- `3306`：MySQL
- `6379`：Redis
- `8848/9848/9849`：Nacos
- `8080`：Gateway
- `9210`：Auth
- `9201`：System
- `9204`：Resource
- `9205`：Workflow
- `9206`：Address
- `9200/9300`：Elasticsearch
- `5601`：Kibana
- `5672/15672`：RabbitMQ
- `9000/9001`：MinIO
- `7091/8091`：Seata

## 5. 数据库初始化与数据迁移

## 5.1 目标库清单

生产 MySQL 至少应包含以下 schema：

- `ry-config`
- `ry-cloud`
- `ry-job`
- `ry-workflow`
- `ry-seata`
- `ftth_cloud_address`

说明：

- `ftth_cloud_address` 是标准地址服务的主业务库。
- 地址模块中的 `ADDR_SEGM`、`ADDR_SET_SEGM`、`spc_region`、`spc_station`、`address_standard_approval`、`address_search_*` 都应落在 `ftth_cloud_address`。
- 不要继续使用 `datasource.yml` 示例里的 `ry-address` 作为地址服务生产库名，除非代码与 SQL 已整体改造。

## 5.2 基础库初始化策略

推荐使用“两段式”初始化：

1. 平台基础库用仓库脚本初始化。
2. 地址业务库用本地现有数据整体导出后导入生产。

### SQL 字符集防护

所有手工执行的 MySQL 导入命令都必须显式追加 `--default-character-set=utf8mb4`。

原因：MySQL 客户端在部分环境下默认仍会使用 `latin1` 连接字符集。即使目标库和表本身是 `utf8mb4`，只要导入连接不是 `utf8mb4`，带中文的表注释、字段注释和流程定义文本仍可能被写成乱码。

统一要求：

- 执行 `.sql` 文件时，命令行必须带 `--default-character-set=utf8mb4`
- 地址模块仓库内的 SQL 脚本已补充 `SET NAMES utf8mb4;`，但执行命令仍保持显式字符集参数，不能省略
- 如果上线后已出现中文注释乱码，先停止继续导入，再按专项修复脚本处理，避免二次覆盖

### 平台基础库初始化

按以下顺序导入：

```bash
mysql -h <MYSQL_HOST> -P 3306 -u <MYSQL_USER> -p<MYSQL_PASSWORD> --default-character-set=utf8mb4 ry-config < script/sql/ry-config.sql
mysql -h <MYSQL_HOST> -P 3306 -u <MYSQL_USER> -p<MYSQL_PASSWORD> --default-character-set=utf8mb4 ry-cloud  < script/sql/ry-cloud.sql
mysql -h <MYSQL_HOST> -P 3306 -u <MYSQL_USER> -p<MYSQL_PASSWORD> --default-character-set=utf8mb4 ry-job    < script/sql/ry-job.sql
mysql -h <MYSQL_HOST> -P 3306 -u <MYSQL_USER> -p<MYSQL_PASSWORD> --default-character-set=utf8mb4 ry-seata  < script/sql/ry-seata.sql
mysql -h <MYSQL_HOST> -P 3306 -u <MYSQL_USER> -p<MYSQL_PASSWORD> --default-character-set=utf8mb4 ry-workflow < script/sql/ry-workflow.sql
```

### 地址业务库迁移

如果生产要继承当前本地已导入的地址数据，直接从本地 MySQL 导出 `ftth_cloud_address`：

```bash
docker exec mysql mysqldump \
  -uroot -p'<LOCAL_MYSQL_ROOT_PASSWORD>' \
  --single-transaction --set-gtid-purged=OFF --routines --triggers \
  --default-character-set=utf8mb4 \
  ftth_cloud_address > /opt/ruoyi-prod/backup/ftth_cloud_address.sql
```

再导入生产：

```bash
mysql -h <MYSQL_HOST> -P 3306 -u <MYSQL_USER> -p<MYSQL_PASSWORD> \
  --default-character-set=utf8mb4 \
  ftth_cloud_address < /opt/ruoyi-prod/backup/ftth_cloud_address.sql
```

如果生产要完全继承当前本地环境的用户、菜单、角色和工作流定义，建议连同以下库一起整体导出导入：

- `ry-config`
- `ry-cloud`
- `ry-job`
- `ry-workflow`
- `ry-seata`
- `ftth_cloud_address`

## 5.3 地址模块附加 SQL 执行顺序

若地址业务库不是从本地整库恢复，而是基于已有生产事实库补表，则按以下顺序执行：

1. [address_search_support.sql](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/sql/address/address_search_support.sql)
2. [address_standard_approval.sql](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/sql/address/address_standard_approval.sql)
3. 若已存在旧版审批表，再执行 [address_standard_approval_guard_upgrade.sql](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/sql/address/address_standard_approval_guard_upgrade.sql)

执行目标库均为 `ftth_cloud_address`。

```bash
mysql -h <MYSQL_HOST> -P 3306 -u <MYSQL_USER> -p<MYSQL_PASSWORD> \
  --default-character-set=utf8mb4 \
  ftth_cloud_address < ruoyi-modules/ruoyi-address/sql/address/address_search_support.sql

mysql -h <MYSQL_HOST> -P 3306 -u <MYSQL_USER> -p<MYSQL_PASSWORD> \
  --default-character-set=utf8mb4 \
  ftth_cloud_address < ruoyi-modules/ruoyi-address/sql/address/address_standard_approval.sql
```

## 5.4 审批流初始化注意事项

[address_standard_approve_v1.sql](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/sql/workflow/address_standard_approve_v1.sql) 当前同时包含：

- `sys_role` 写入
- `flow_definition / flow_node / flow_skip` 写入

在微服务拆库部署下，这个脚本不能直接对单个库整体执行，必须拆分：

- `sys_role` 相关语句执行到 `ry-cloud`
- `flow_definition / flow_node / flow_skip` 相关语句执行到 `ry-workflow`

如果上线后出现流程中文乱码，再按需执行 [address_standard_approve_v1_repair_mojibake.sql](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/sql/workflow/address_standard_approve_v1_repair_mojibake.sql)，执行目标库为 `ry-workflow`。

## 6. Nacos 配置导入

## 6.1 创建生产 namespace

在 Nacos 中创建 `prod` namespace，然后将所有生产配置导入该 namespace。

建议最少导入：

- `application-common.yml`
- `datasource.yml`
- `ruoyi-gateway.yml`
- `ruoyi-auth.yml`
- `ruoyi-system.yml`
- `ruoyi-resource.yml`
- `ruoyi-workflow.yml`
- `ruoyi-address.yml`（需新增）

如果启用对应服务，再导入：

- `ruoyi-monitor.yml`
- `ruoyi-job.yml`
- `ruoyi-snailjob-server.yml`
- `seata-server.properties`

导入基线目录见：[script/config/nacos](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/script/config/nacos)

## 6.2 必改配置

### `application-common.yml`

必须改为生产值：

- `spring.cloud.nacos.discovery.metadata.username`
- `spring.cloud.nacos.discovery.metadata.userpassword`
- `spring.rabbitmq.*`
- `spring.data.redis.*`
- `dubbo.metadata-report.parameters.backup`
- `sa-token.jwt-secret-key`

### `datasource.yml`

必须改为生产值，尤其是：

```yaml
datasource:
  address-master:
    url: jdbc:mysql://<MYSQL_HOST>:3306/ftth_cloud_address?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true
    username: <ADDRESS_DB_USER>
    password: <ADDRESS_DB_PASSWORD>
```

### `ruoyi-address.yml`

仓库已提供基线文件 [ruoyi-address.yml](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/script/config/nacos/ruoyi-address.yml)，生产 namespace 中导入前建议按以下内容核对：

```yaml
spring:
  datasource:
    dynamic:
      primary: address
      datasource:
        address:
          type: ${spring.datasource.type}
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: ${datasource.address-master.url}
          username: ${datasource.address-master.username}
          password: ${datasource.address-master.password}

easy-es:
  enable: true
  compatible: true
  address: <ES_HOST>:9200
  schema: http

address:
  search:
    enabled: true
    kibana-url: http://<KIBANA_HOST>:5601
    maintenance-lock-key: address:search:maintenance:running
    refresh-policy: wait_for
    rebuild-batch-size: 500
    pit-keep-alive: 1m
    standard:
      alias: address_standard_search
      read-enabled: true
      write-enabled: true
    installation:
      alias: address_installation_search
      read-enabled: true
      write-enabled: true
```

说明：

- `ruoyi-address` 在微服务模式下通过 Dubbo 调 workflow，不需要再配置 `address.workflow.base-url`。
- 如生产阶段暂不启用 ES 写入，可先把 `write-enabled` 设为 `false`，但 `read-enabled` 与索引数据必须匹配。
- 当前代码中的运维确认口令为固定值：标准地址重建 `REBUILD_STANDARD`、安装地址重建 `REBUILD_INSTALLATION`、repair 执行 `REPLAY_REPAIR`。

## 6.3 ES 数据迁移与索引初始化

### 原则

- 生产首次上线不建议直接迁移本地 ES 数据卷，也不建议把本地联调索引快照直接恢复到生产。
- 推荐以 `ftth_cloud_address` 作为事实源，在生产 ES 上重新全量构建 `address_standard_search` 与 `address_installation_search`。
- 若本次上线要求地址写入与 ES 保持立刻一致，则在 ES 索引完成重建前，不应放行业务写流量。

### 首次上线顺序

1. 启动 `elasticsearch` 与 `kibana`，确认集群健康。
2. 在 Nacos `prod` namespace 导入 `ruoyi-address.yml`，确认 `easy-es.*` 与 `address.search.*` 指向生产值。
3. 启动 `ruoyi-address`，但先不要在网关或 nginx 放开真实业务流量。
4. 通过前端“地址搜索运维”页面，或调用运维接口触发两类索引全量重建。
5. 等待重建任务完成后，校验 ES 别名、文档量和抽样查询结果。
6. 确认标准地址列表、安装地址列表、导出接口正常，再开放生产流量。

### 运维接口

可直接访问地址服务，或经 gateway 转发后访问以下接口：

- `POST /address/search/tasks/rebuild/standard`
- `POST /address/search/tasks/rebuild/installation`
- `GET /address/search/tasks`
- `GET /address/search/tasks/{taskId}`
- `GET /address/search/ops/overview`

示例：

```bash
curl -X POST "http://<ADDRESS_HOST>:9206/address/search/tasks/rebuild/standard" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"confirmationCode":"REBUILD_STANDARD"}'

curl -X POST "http://<ADDRESS_HOST>:9206/address/search/tasks/rebuild/installation" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"confirmationCode":"REBUILD_INSTALLATION"}'
```

### 重建完成核验

```bash
curl -s http://<ES_HOST>:9200/_cat/indices/address_*?v
curl -s http://<ES_HOST>:9200/_cat/aliases/address_*?v
curl -s http://<ES_HOST>:9200/address_standard_search/_count
curl -s http://<ES_HOST>:9200/address_installation_search/_count
```

同时建议对照数据库做抽样：

- 校验标准地址列表的区域收敛查询结果是否正常。
- 校验安装地址模糊查询是否能在 1 秒级返回。
- 校验导出链路是否仍可在 30 秒级内完成。

### 失败处理

- 若重建任务失败，先通过 `/address/search/tasks/{taskId}` 和地址服务日志定位失败批次，再重跑全量重建。
- 若创建了错误的物理索引但尚未切换别名，可删除错误索引后重新重建，不要手工改写别名到未知索引。
- 若首次上线尚未完成 ES 重建，不要临时放开“仅数据库成功”的写路径；当前生产评估口径下应保持地址写入关闭，直至 ES 恢复并重建完成。

## 7. 后端构建与镜像制作

## 7.1 后端打包

在代码仓库根目录执行：

```bash
mvn -T1C -DskipTests package
```

输出 jar 位置示例：

- `ruoyi-auth/target/ruoyi-auth.jar`
- `ruoyi-gateway/target/ruoyi-gateway.jar`
- `ruoyi-modules/ruoyi-system/target/ruoyi-system.jar`
- `ruoyi-modules/ruoyi-resource/target/ruoyi-resource.jar`
- `ruoyi-modules/ruoyi-workflow/target/ruoyi-workflow.jar`
- `ruoyi-modules/ruoyi-address/target/ruoyi-address.jar`

## 7.2 `ruoyi-address` Dockerfile

仓库已提供基线文件 [Dockerfile](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/ruoyi-modules/ruoyi-address/Dockerfile)，镜像制作前可按下述内容核对：

```dockerfile
FROM bellsoft/liberica-openjdk-rocky:17.0.16-cds

LABEL maintainer="RuoYi Address"

RUN mkdir -p /ruoyi/address/logs \
    /ruoyi/address/temp \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/address

ENV SERVER_PORT=9206 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-address.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
```

## 7.3 镜像构建

示例：

```bash
docker build -t <REGISTRY>/ruoyi-auth:<TAG> ruoyi-auth
docker build -t <REGISTRY>/ruoyi-gateway:<TAG> ruoyi-gateway
docker build -t <REGISTRY>/ruoyi-system:<TAG> ruoyi-modules/ruoyi-system
docker build -t <REGISTRY>/ruoyi-resource:<TAG> ruoyi-modules/ruoyi-resource
docker build -t <REGISTRY>/ruoyi-workflow:<TAG> ruoyi-modules/ruoyi-workflow
docker build -t <REGISTRY>/ruoyi-address:<TAG> ruoyi-modules/ruoyi-address
```

如使用私有仓库，再执行：

```bash
docker push <REGISTRY>/ruoyi-auth:<TAG>
docker push <REGISTRY>/ruoyi-gateway:<TAG>
docker push <REGISTRY>/ruoyi-system:<TAG>
docker push <REGISTRY>/ruoyi-resource:<TAG>
docker push <REGISTRY>/ruoyi-workflow:<TAG>
docker push <REGISTRY>/ruoyi-address:<TAG>
```

## 8. 前端构建与发布

## 8.1 生产环境变量

新增 `ruoyi-address-ui/.env.production`：

```env
VITE_ADDRESS_RUNTIME_MODE=microservice
VITE_API_BASE=/prod-api
VITE_GATEWAY_BASE=http://<GATEWAY_HOST>:8080
```

说明：

- 生产静态站点实际走 `VITE_API_BASE=/prod-api`
- `VITE_GATEWAY_BASE` 主要用于本地 dev proxy，不参与正式静态发布

## 8.2 前端构建

```bash
cd ruoyi-address-ui
npm ci
npm run build
```

构建产物位于 `ruoyi-address-ui/dist`。

## 8.3 nginx 发布

将 `dist` 内容发布到 nginx 静态目录：

```bash
rsync -av --delete ruoyi-address-ui/dist/ /opt/ruoyi-prod/nginx/html/
```

nginx 反向代理可基于 [script/docker/nginx/conf/nginx.conf](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/script/docker/nginx/conf/nginx.conf) 调整，关键是保留：

- `/` 指向静态前端
- `/prod-api/` 代理到 gateway `8080`

## 9. Compose 文件改造要点

不要直接在生产使用 [script/docker/docker-compose.yml](/Users/yuantiansheng/IdeaProjects/RuoYi-Cloud-Plus/script/docker/docker-compose.yml) 原文件，建议复制为独立生产文件，例如 `/opt/ruoyi-prod/compose/docker-compose.prod.yml`，并完成以下改造：

1. 所有硬编码 IP 改为生产 IP 或域名。
2. 所有默认密码改为生产密钥。
3. 所有卷目录改为生产挂载目录。
4. 对不需要的服务删除或注释。
5. 补充 `ruoyi-address` 服务。

建议新增的地址服务段如下：

```yaml
  ruoyi-address:
    image: <REGISTRY>/ruoyi-address:<TAG>
    container_name: ruoyi-address
    environment:
      TZ: Asia/Shanghai
      SPRING_PROFILES_ACTIVE: prod
      SPRING_CLOUD_NACOS_DISCOVERY_IP: <HOST_IP>
      SPRING_CLOUD_NACOS_SERVER_ADDR: <NACOS_HOST>:8848
      DUBBO_IP_TO_REGISTRY: <HOST_IP>
      JAVA_OPTS: "-Xms1g -Xmx1g"
    ports:
      - "9206:9206"
    volumes:
      - /opt/ruoyi-prod/logs/ruoyi-address:/ruoyi/address/logs
      - /opt/ruoyi-prod/skywalking/agent:/ruoyi/skywalking/agent
    restart: always
    network_mode: "host"
```

## 10. 正式上线步骤

按以下顺序执行：

1. 准备生产服务器目录、证书、镜像仓库登录信息。
2. 导入基础库和地址业务库，完成数据迁移。
3. 在 Nacos 创建 `prod` namespace 并导入全部配置。
4. 启动中间件：
   - `mysql`
   - `nacos`
   - `redis`
   - `rabbitmq`
   - `minio`
   - `elasticsearch`
   - `kibana`
   - `seata-server`
5. 检查中间件健康状态。
6. 启动基础平台服务：
   - `ruoyi-gateway`
   - `ruoyi-auth`
   - `ruoyi-system`
   - `ruoyi-resource`
7. 启动业务服务：
   - `ruoyi-workflow`
   - `ruoyi-address`
8. 发布前端静态文件到 nginx，并重载 nginx。
9. 完成登录、列表、审批、ES 查询与导出验证。

Compose 示例命令：

```bash
docker compose -f /opt/ruoyi-prod/compose/docker-compose.prod.yml up -d mysql nacos redis rabbitmq minio elasticsearch kibana seata-server
docker compose -f /opt/ruoyi-prod/compose/docker-compose.prod.yml up -d ruoyi-gateway ruoyi-auth ruoyi-system ruoyi-resource
docker compose -f /opt/ruoyi-prod/compose/docker-compose.prod.yml up -d ruoyi-workflow ruoyi-address nginx-web
docker compose -f /opt/ruoyi-prod/compose/docker-compose.prod.yml ps
```

## 11. 上线后核验

### 11.1 容器核验

```bash
docker compose -f /opt/ruoyi-prod/compose/docker-compose.prod.yml ps
docker logs --tail=200 ruoyi-gateway
docker logs --tail=200 ruoyi-workflow
docker logs --tail=200 ruoyi-address
```

### 11.2 Nacos 核验

- `prod` namespace 中配置全部存在
- `ruoyi-gateway`
- `ruoyi-auth`
- `ruoyi-system`
- `ruoyi-resource`
- `ruoyi-workflow`
- `ruoyi-address`

### 11.3 数据库核验

```sql
SHOW DATABASES;
USE ftth_cloud_address;
SHOW TABLES LIKE 'ADDR_SEGM';
SHOW TABLES LIKE 'address_search_%';
SHOW TABLES LIKE 'address_standard_approval';
SELECT COUNT(1) FROM ADDR_SEGM;
```

### 11.4 ES 核验

```bash
curl -s http://<ES_HOST>:9200/_cat/indices/address_*?v
curl -s http://<ES_HOST>:9200/_cat/aliases/address_*?v
curl -s http://<ES_HOST>:9200/address_standard_search/_count
curl -s http://<ES_HOST>:9200/address_installation_search/_count
```

### 11.5 前端与业务核验

- 打开前端首页，确认可正常加载。
- 登录后确认能打开“标准地址列表”。
- 验证 `/address/standard/list` 分页查询正常。
- 验证“待审批地址管理”页面可打开，并能读取 workflow 待办。
- 验证 ES 运维页能读取索引状态。
- 验证导出接口在大数据量下可正常返回。

## 12. 常见问题

### 12.1 地址服务启动后查不到数据

优先检查 `datasource.address-master.url` 是否仍错误指向 `ry-address`。标准地址项目生产必须指向 `ftth_cloud_address`。

### 12.2 `ruoyi-address` 在 Nacos 中没有实例

检查：

- 容器是否传入 `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_CLOUD_NACOS_SERVER_ADDR` 是否正确
- `SPRING_CLOUD_NACOS_DISCOVERY_IP` 是否为宿主机可访问 IP

### 12.3 地址服务能启动，但 ES 查询失败

检查：

- `ruoyi-address.yml` 是否已导入 Nacos
- `easy-es.address` 是否指向生产 ES
- `address.search.standard.read-enabled`
- `address.search.installation.read-enabled`
- `address_search_*` 表是否已创建
- 首次上线是否已完成标准地址、安装地址两类索引重建
- ES 别名是否已正确指向最新物理索引

### 12.4 前端页面空白或接口 404

检查：

- `VITE_API_BASE` 是否为 `/prod-api`
- nginx 是否保留 `/prod-api/` 代理
- gateway 路由配置中是否存在 `/address/**` 与 `/workflow/**`

### 12.5 审批流程初始化失败

当前 `address_standard_approve_v1.sql` 不能直接整文件执行到单个库，必须按 `ry-cloud` 与 `ry-workflow` 拆分执行。

## 13. 上线核对表

- 已准备生产主机、目录、证书、镜像仓库账号
- 已创建 `prod` namespace
- 已导入 Nacos 配置
- 已把 `datasource.address-master` 指向 `ftth_cloud_address`
- 已创建 `ruoyi-address.yml`
- 已补齐 `ruoyi-address` Dockerfile
- 已构建并推送 `ruoyi-address` 镜像
- 已导入 `ftth_cloud_address` 全量数据
- 已执行地址模块附加 SQL
- 已完成 workflow 审批定义初始化
- 已完成 ES 全量重建与别名核验
- 已完成前端 `dist` 发布
- 已完成登录、地址列表、审批、ES 核验
