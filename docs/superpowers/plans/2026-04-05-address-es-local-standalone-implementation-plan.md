# 地址 ES 本地 standalone 联调收口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐标准地址查询/导出与安装地址查询的 ES 适配缺口，并完成 `standalone` 本地前后端联调与性能 smoke 验证。

**Architecture:** 继续沿用现有 `AddressSearchMaintenanceService` 与 ES Gateway 体系，标准地址列表/导出和安装地址列表优先走 ES，数据库仅承担事实源与按页批量补齐职责。`standalone` 配置负责直连本地 `ftth_cloud_address`、Redis、Elasticsearch、Kibana，前端复用现有地址模块页面与 ES 运维页完成联调。

**Tech Stack:** Spring Boot, MyBatis-Plus, Easy-ES, Redis, Elasticsearch 7.17, Vue2, Vite, Vitest, JUnit 5, Mockito, Docker Compose

---

## 文件结构

- 后端查询与导出链路
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressQueryService.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/InstallationAddressServiceImpl.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchExportService.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java`
- 后端测试与 standalone 配置
  - Modify: `ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml`
  - Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/StandardAddressSearchGatewayTest.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/InstallationAddressSearchGatewayTest.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java`
  - Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/StandaloneProfileSmokeTest.java`
- 前端联调入口
  - Modify: `ruoyi-address-ui/src/api/address.js`
  - Modify: `ruoyi-address-ui/src/router/index.js`
  - Modify: `ruoyi-address-ui/src/App.vue`
  - Modify/Create: `ruoyi-address-ui/src/views/SearchOpsConsole.vue`
  - Modify: `ruoyi-address-ui/src/api/address.test.js`
  - Modify: `ruoyi-address-ui/src/router/index.test.js`
  - Modify: `ruoyi-address-ui/src/views/SearchOpsConsole.test.js`

### Task 1: 先锁定后端 ES 查询与导出缺口

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/StandardAddressSearchGatewayTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/InstallationAddressSearchGatewayTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchExportService.java`

- [ ] **Step 1: 先写失败测试覆盖 ES 查询口径**

```java
@Test
void shouldUseAssociationStatusAndStableOrderWhenQueryInstallationPage() {
    InstallationAddressBo bo = new InstallationAddressBo();
    bo.setAssociationStatus("BOUND");

    gateway.queryPage(bo, new PageQuery(20, 1));

    verify(installationAddressSearchEsMapper).pageQuery(argThat(wrapper -> wrapper != null), eq(1), eq(20));
}

@Test
void shouldClosePitWhenStandardExportCompletes() {
    when(standardAddressSearchGateway.openExportPointInTime()).thenReturn("pit-1");
    when(standardAddressSearchGateway.queryExportBatch(any(), any(), eq("pit-1"), isNull(), eq(200)))
        .thenReturn(SearchAfterBatch.of(List.of(new StandardAddressVo()), "pit-1", List.of(), true));

    exportService.writeRows(new StandardAddressBo(), 200, rows -> {});

    verify(standardAddressSearchGateway).closeExportPointInTime("pit-1");
}
```

- [ ] **Step 2: 运行失败测试，确认当前缺口真实存在**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandardAddressSearchGatewayTest,InstallationAddressSearchGatewayTest,StandardAddressControllerExportTest test`

Expected: FAIL，表现为安装地址 ES 筛选/排序、导出关闭 PIT、导出字段口径或相关断言未满足。

- [ ] **Step 3: 实现最小后端收口代码**

```java
// EasyEsInstallationAddressSearchGateway
wrapper.eq(StringUtils.isNotBlank(queryBo.getAssociationStatus()),
    InstallationAddressSearchDocument::getAssociationStatus, queryBo.getAssociationStatus());
wrapper.orderByDesc(InstallationAddressSearchDocument::getCreateDate,
    InstallationAddressSearchDocument::getSetAddrId);

// StandardAddressSearchExportService
try {
    // PIT + search_after 批次遍历
} finally {
    standardAddressSearchGateway.closeExportPointInTime(pitId);
}
```

- [ ] **Step 4: 让测试转绿**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandardAddressSearchGatewayTest,InstallationAddressSearchGatewayTest,StandardAddressControllerExportTest test`

Expected: PASS

### Task 2: 对齐服务层批量补齐与 ES 开关行为

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressQueryService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/InstallationAddressServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressQueryServiceTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/InstallationAddressServiceImplTest.java`

- [ ] **Step 1: 先写失败测试锁定批量补齐与开关语义**

```java
@Test
void shouldEnrichStandardRowsAfterEsPageQuery() {
    when(addressSearchProperties.getStandard().getReadEnabled()).thenReturn(Boolean.TRUE);
    when(standardAddressSearchGateway.queryPage(any(), any(), any()))
        .thenReturn(new TableDataInfo<>(List.of(new StandardAddressVo()), 1));

    service.queryPageList(new StandardAddressBo(), new PageQuery(10, 1));

    verify(standardAddressTagRelMapper, atLeastOnce()).selectList(any());
}
```

- [ ] **Step 2: 运行失败测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandardAddressQueryServiceTest,InstallationAddressServiceImplTest test`

Expected: FAIL，提示 ES 分支未完整复用数据库分支的补齐或排序逻辑。

- [ ] **Step 3: 实现最小修正**

```java
if (Boolean.TRUE.equals(addressSearchProperties.getStandard().getReadEnabled())) {
    TableDataInfo<StandardAddressVo> pageResult = standardAddressSearchGateway.queryPage(queryBo, segmTypes, pageQuery);
    enrichAddrSegmRows(pageResult.getRows());
    applyWritableFlags(pageResult.getRows());
    return pageResult;
}
```

- [ ] **Step 4: 回归验证**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandardAddressQueryServiceTest,InstallationAddressServiceImplTest test`

Expected: PASS

### Task 3: 打开 standalone 本地联调配置并补 smoke

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/StandaloneProfileSmokeTest.java`

- [ ] **Step 1: 先写 standalone 绑定失败测试**

```java
assertThat(properties.getAddress()).isEqualTo("127.0.0.1:9200");
assertThat(addressSearchProperties.getKibanaUrl()).isEqualTo("http://127.0.0.1:5601");
assertThat(addressSearchProperties.getStandard().getReadEnabled()).isTrue();
assertThat(addressSearchProperties.getInstallation().getReadEnabled()).isTrue();
```

- [ ] **Step 2: 运行失败测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandaloneProfileSmokeTest test`

Expected: FAIL，提示 `standalone` 尚未完全打开本地 ES 读配置或绑定值不符。

- [ ] **Step 3: 最小化修改配置**

```yaml
easy-es:
  enable: true
  compatible: true
  address: 127.0.0.1:9200
  schema: http

address:
  search:
    enabled: true
    kibana-url: http://127.0.0.1:5601
    standard:
      read-enabled: true
    installation:
      read-enabled: true
```

- [ ] **Step 4: 回归验证**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandaloneProfileSmokeTest test`

Expected: PASS

### Task 4: 收口前端联调入口与运维页

**Files:**
- Modify: `ruoyi-address-ui/src/api/address.js`
- Modify: `ruoyi-address-ui/src/router/index.js`
- Modify: `ruoyi-address-ui/src/App.vue`
- Modify/Create: `ruoyi-address-ui/src/views/SearchOpsConsole.vue`
- Modify: `ruoyi-address-ui/src/api/address.test.js`
- Modify: `ruoyi-address-ui/src/router/index.test.js`
- Modify: `ruoyi-address-ui/src/views/SearchOpsConsole.test.js`

- [ ] **Step 1: 先写失败测试锁定前端入口**

```javascript
it('registers search ops route', () => {
  const searchOpsRoute = router.options.routes.find(route => route.path === '/search/ops');
  expect(searchOpsRoute).toBeTruthy();
});

it('calls rebuild installation api', async () => {
  addressApi.createInstallationRebuildTask({ confirmationCode: 'REBUILD_INSTALLATION' });
  expect(mockRequest.post).toHaveBeenCalledWith(
    '/address/search/tasks/rebuild/installation',
    { confirmationCode: 'REBUILD_INSTALLATION' }
  );
});
```

- [ ] **Step 2: 运行前端失败测试**

Run: `cd ruoyi-address-ui && npm test -- SearchOpsConsole api/address router/index`

Expected: FAIL，提示路由、导航或运维页交互未完全对齐。

- [ ] **Step 3: 实现最小前端收口**

```javascript
// router/index.js
{ path: '/search/ops', component: SearchOpsConsole }
```

```vue
<el-menu-item index="/search/ops">ES 运维</el-menu-item>
```

- [ ] **Step 4: 回归前端测试**

Run: `cd ruoyi-address-ui && npm test -- SearchOpsConsole api/address router/index`

Expected: PASS

### Task 5: 本地启动并完成联调 smoke

**Files:**
- Verify runtime config: `script/docker/docker-compose.yml`
- Runtime only: Docker / Maven / npm processes

- [ ] **Step 1: 启动依赖容器**

Run: `docker compose -f script/docker/docker-compose.yml up -d redis elasticsearch kibana`

Expected: `redis`、`elasticsearch`、`kibana` 均为 running

- [ ] **Step 2: 启动后端 standalone**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests spring-boot:run -Dspring-boot.run.profiles=standalone`

Expected: `ruoyi-address` 启动成功，日志中可见 `standalone` profile、生效的本地 ES/Redis 地址

- [ ] **Step 3: 启动前端**

Run: `cd ruoyi-address-ui && npm run dev -- --host 0.0.0.0`

Expected: Vite dev server 启动成功，可访问本地页面

- [ ] **Step 4: 做联调与性能 smoke**

Run:

```bash
curl -s -o /tmp/std-list.json -w "%{time_total}\n" \
  -X POST "http://127.0.0.1:9218/address/standard/list" \
  -d "pageNum=1&pageSize=20&segmName=测试"

curl -s -o /tmp/inst-list.json -w "%{time_total}\n" \
  -X POST "http://127.0.0.1:9218/address/installation/list" \
  -d "pageNum=1&pageSize=20&setAddrName=测试"
```

Expected: 关键查询可返回结果，耗时稳定在目标范围内；页面侧可完成标准地址查询、安装地址查询、导出触发与 ES 运维页访问。
