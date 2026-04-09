# 标准地址生产参数清单模板

> 本文件只保留占位符，不提交真实生产敏感信息。

## 1. 基础信息

- 部署环境：`prod`
- 部署主机：`<HOST_IP>`
- 公网域名：`<DOMAIN>`
- 前端访问地址：`https://<DOMAIN>`
- Gateway 访问地址：`http://<GATEWAY_HOST>:8080`

## 2. 镜像仓库

- 镜像仓库：`<REGISTRY>`
- 镜像版本：`<TAG>`

## 3. MySQL

- 主机：`<MYSQL_HOST>`
- 端口：`3306`
- root 用户：`<MYSQL_ROOT_USER>`
- root 密码：`<MYSQL_ROOT_PASSWORD>`
- `ry-config` 用户：`<CONFIG_DB_USER>`
- `ry-config` 密码：`<CONFIG_DB_PASSWORD>`
- `ry-cloud` 用户：`<CLOUD_DB_USER>`
- `ry-cloud` 密码：`<CLOUD_DB_PASSWORD>`
- `ry-job` 用户：`<JOB_DB_USER>`
- `ry-job` 密码：`<JOB_DB_PASSWORD>`
- `ry-workflow` 用户：`<WORKFLOW_DB_USER>`
- `ry-workflow` 密码：`<WORKFLOW_DB_PASSWORD>`
- `ry-seata` 用户：`<SEATA_DB_USER>`
- `ry-seata` 密码：`<SEATA_DB_PASSWORD>`
- `ftth_cloud_address` 用户：`<ADDRESS_DB_USER>`
- `ftth_cloud_address` 密码：`<ADDRESS_DB_PASSWORD>`

## 4. Nacos

- 主机：`<NACOS_HOST>`
- 端口：`8848`
- namespace：`prod`
- discovery group：`DEFAULT_GROUP`
- config group：`DEFAULT_GROUP`
- 用户名：`<NACOS_USER>`
- 密码：`<NACOS_PASSWORD>`

## 5. Redis

- 主机：`<REDIS_HOST>`
- 端口：`6379`
- 密码：`<REDIS_PASSWORD>`
- database：`0`

## 6. RabbitMQ

- 主机：`<RABBITMQ_HOST>`
- 端口：`5672`
- 管理端口：`15672`
- 用户名：`<RABBITMQ_USER>`
- 密码：`<RABBITMQ_PASSWORD>`

## 7. MinIO

- 主机：`<MINIO_HOST>`
- API 端口：`9000`
- Console 端口：`9001`
- 用户名：`<MINIO_USER>`
- 密码：`<MINIO_PASSWORD>`

## 8. Elasticsearch / Kibana

- ES 主机：`<ES_HOST>`
- ES 端口：`9200`
- Kibana 主机：`<KIBANA_HOST>`
- Kibana 端口：`5601`
- 标准地址索引别名：`address_standard_search`
- 安装地址索引别名：`address_installation_search`
- 首次上线是否全量重建 ES：`<YES/NO>`
- 标准地址重建确认口令：`REBUILD_STANDARD`
- 安装地址重建确认口令：`REBUILD_INSTALLATION`
- repair 执行确认口令：`REPLAY_REPAIR`
- 标准地址重建执行人：`<NAME>`
- 安装地址重建执行人：`<NAME>`

## 9. Seata / Dubbo

- Seata 主机：`<SEATA_HOST>`
- Seata 端口：`8091`
- 宿主机注册 IP：`<HOST_IP>`
- Dubbo 注册 IP：`<HOST_IP>`

## 10. 前端变量

- `VITE_ADDRESS_RUNTIME_MODE=microservice`
- `VITE_API_BASE=/prod-api`
- `VITE_GATEWAY_BASE=http://<GATEWAY_HOST>:8080`

## 11. 地址服务 Nacos 配置

- `datasource.address-master.url=jdbc:mysql://<MYSQL_HOST>:3306/ftth_cloud_address?...`
- `datasource.address-master.username=<ADDRESS_DB_USER>`
- `datasource.address-master.password=<ADDRESS_DB_PASSWORD>`
- `easy-es.address=<ES_HOST>:9200`
- `address.search.kibana-url=http://<KIBANA_HOST>:5601`
- `address.search.refresh-policy=wait_for`
- `address.search.rebuild-batch-size=500`
- `address.search.pit-keep-alive=1m`
- `address.search.standard.read-enabled=true`
- `address.search.standard.write-enabled=true`
- `address.search.installation.read-enabled=true`
- `address.search.installation.write-enabled=true`

## 12. ES 迁移执行记录

- ES / Kibana 健康检查完成时间：`<YYYY-MM-DD HH:mm:ss>`
- 标准地址全量重建任务号：`<TASK_ID>`
- 安装地址全量重建任务号：`<TASK_ID>`
- 标准地址索引文档量：`<COUNT>`
- 安装地址索引文档量：`<COUNT>`
- ES 别名核验完成时间：`<YYYY-MM-DD HH:mm:ss>`
- ES 抽样核验负责人：`<NAME>`

## 13. 发布记录

- 数据库初始化完成时间：`<YYYY-MM-DD HH:mm:ss>`
- Nacos 导入完成时间：`<YYYY-MM-DD HH:mm:ss>`
- 镜像构建完成时间：`<YYYY-MM-DD HH:mm:ss>`
- Compose 启动完成时间：`<YYYY-MM-DD HH:mm:ss>`
- 前端发布完成时间：`<YYYY-MM-DD HH:mm:ss>`
- 上线验收负责人：`<NAME>`
