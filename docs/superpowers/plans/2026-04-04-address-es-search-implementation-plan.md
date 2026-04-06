# 地址 ES 检索方案 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `ruoyi-address` 模块落地标准地址/安装地址 ES 检索、导出、同步双写和索引修复能力，并满足当前评估口径中的一致性与响应时间目标。

**Architecture:** `spc_region` 仍保留数据库查询；`ADDR_SEGM` 和 `ADDR_SET_SEGM` 的全局模糊、排序、分页与导出结果集圈定切换到 `ES`。在线写链路通过文档构建器和同步服务执行“先 ES、后 MySQL”的双写，并落同步日志与修复任务；全量重建和 repair 通过独立维护服务执行。

**Tech Stack:** Spring Boot, MyBatis-Plus, Easy-ES 3.0.1, Elasticsearch Java Client, MySQL, JUnit 5, Mockito, ExcelUtil

---

## 文件结构

- 搜索基础配置
  - 修改：`ruoyi-modules/ruoyi-address/pom.xml`
  - 修改：`ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/AddressSearchProperties.java`
- ES 文档与 Mapper
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/document/StandardAddressSearchDocument.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/document/InstallationAddressSearchDocument.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/builder/StandardAddressSearchDocumentBuilder.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/builder/InstallationAddressSearchDocumentBuilder.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/esmapper/StandardAddressSearchEsMapper.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/esmapper/InstallationAddressSearchEsMapper.java`
- 读链路
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/model/SearchAfterBatch.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchExportService.java`
  - 修改：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressQueryService.java`
  - 修改：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/InstallationAddressServiceImpl.java`
  - 修改：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java`
- 双写与修复
  - 创建：`ruoyi-modules/ruoyi-address/sql/address/address_search_support.sql`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddressSearchSyncLog.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddressSearchRepairTask.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddressSearchSyncLogMapper.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddressSearchRepairTaskMapper.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/support/CheckedBooleanSupplier.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchSyncService.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceService.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/AddressSearchMaintenanceController.java`
  - 修改：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressCommandService.java`
  - 修改：`ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/InstallationAddressServiceImpl.java`
- 测试
  - 创建：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchDocumentBuilderTest.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/StandardAddressSearchGatewayTest.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/InstallationAddressSearchGatewayTest.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/StandardAddressSearchExportServiceTest.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchSyncServiceTest.java`
  - 创建：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchMaintenanceServiceTest.java`
  - 修改：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressQueryServiceTest.java`
  - 修改：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/InstallationAddressServiceImplTest.java`
  - 修改：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressCommandServiceTest.java`
  - 修改：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressImportServiceTest.java`
  - 修改：`ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java`

### Task 1: 建立 ES 配置、文档模型与构建器

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/pom.xml`
- Modify: `ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/AddressSearchProperties.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/document/StandardAddressSearchDocument.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/document/InstallationAddressSearchDocument.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/builder/StandardAddressSearchDocumentBuilder.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/builder/InstallationAddressSearchDocumentBuilder.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/esmapper/StandardAddressSearchEsMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/esmapper/InstallationAddressSearchEsMapper.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchDocumentBuilderTest.java`

- [ ] **Step 1: 先写文档构建器失败测试**

```java
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AddressSearchDocumentBuilderTest {

    private final StandardAddressSearchDocumentBuilder standardBuilder = new StandardAddressSearchDocumentBuilder();
    private final InstallationAddressSearchDocumentBuilder installationBuilder = new InstallationAddressSearchDocumentBuilder();

    @Test
    void shouldBuildStandardAddressSearchDocumentWithCoreSearchFields() {
        StandardAddressVo vo = new StandardAddressVo();
        vo.setSegmId("0001");
        vo.setParentSegmId("P001");
        vo.setSegmName("中央路100号");
        vo.setStandName("江苏省南京市鼓楼区中央路100号");
        vo.setSegmNo("ZYL100H");
        vo.setStandNo("JSNJGLQZYL100H");
        vo.setSegmType("180010");
        vo.setRegionId("320106");
        vo.setStatus("2140900");
        vo.setAddrLevel(7);

        StandardAddressSearchDocument document = standardBuilder.fromVo(vo);

        assertEquals("0001", document.getSegmId());
        assertEquals("江苏省南京市鼓楼区中央路100号", document.getStandName());
        assertEquals("JSNJGLQZYL100H", document.getStandNo());
        assertEquals(7, document.getAddrLevel());
        assertEquals("2140900", document.getStatus());
    }

    @Test
    void shouldBuildInstallationAddressSearchDocumentWithAssociationStatus() {
        InstallationAddressVo vo = new InstallationAddressVo();
        vo.setSetAddrId("SET001");
        vo.setSetAddrName("机房1排1列");
        vo.setSetAddrNo("JF-001");
        vo.setSetType("机房");
        vo.setSegmId("0001");
        vo.setRegionId("320106");
        vo.setOrgId("ORG001");
        vo.setAssociationStatus("BOUND");

        InstallationAddressSearchDocument document = installationBuilder.fromVo(vo);

        assertEquals("SET001", document.getSetAddrId());
        assertEquals("机房1排1列", document.getSetAddrName());
        assertEquals("BOUND", document.getAssociationStatus());
        assertEquals("0001", document.getSegmId());
    }
}
```

- [ ] **Step 2: 运行测试，确认当前实现尚不存在**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=AddressSearchDocumentBuilderTest test`

Expected: FAIL，提示 `StandardAddressSearchDocumentBuilder`、`InstallationAddressSearchDocumentBuilder` 或对应文档类不存在。

- [ ] **Step 3: 实现最小配置、文档与构建器**

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/AddressSearchProperties.java
@Data
@Configuration
@ConfigurationProperties(prefix = "address.search")
public class AddressSearchProperties {
    private boolean enabled = false;
    private String refreshPolicy = "wait_for";
    private int rebuildBatchSize = 500;
    private String pitKeepAlive = "1m";
    private final Index standard = new Index("address_standard_search", false, false);
    private final Index installation = new Index("address_installation_search", false, false);

    @Data
    @AllArgsConstructor
    public static class Index {
        private String alias;
        private boolean readEnabled;
        private boolean writeEnabled;
    }
}

// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/document/StandardAddressSearchDocument.java
@Data
@IndexName("address_standard_search")
public class StandardAddressSearchDocument {
    @IndexId
    private String segmId;
    private String parentSegmId;
    @IndexField(fieldType = FieldType.TEXT, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String segmName;
    @IndexField(fieldType = FieldType.TEXT, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String standName;
    private String segmNo;
    private String standNo;
    private String segmType;
    private Integer addrLevel;
    private String regionId;
    private String districtId;
    private String serviceRegionId;
    private String status;
    private Date createDate;
}

// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/builder/StandardAddressSearchDocumentBuilder.java
@Component
public class StandardAddressSearchDocumentBuilder {
    public StandardAddressSearchDocument fromVo(StandardAddressVo vo) {
        StandardAddressSearchDocument document = new StandardAddressSearchDocument();
        document.setSegmId(vo.getSegmId());
        document.setParentSegmId(vo.getParentSegmId());
        document.setSegmName(vo.getSegmName());
        document.setStandName(vo.getStandName());
        document.setSegmNo(vo.getSegmNo());
        document.setStandNo(vo.getStandNo());
        document.setSegmType(vo.getSegmType());
        document.setAddrLevel(vo.getAddrLevel());
        document.setRegionId(vo.getRegionId());
        document.setDistrictId(vo.getDistrictId());
        document.setServiceRegionId(vo.getServiceRegionId());
        document.setStatus(vo.getStatus());
        document.setCreateDate(vo.getCreateDate());
        return document;
    }
}
```

```xml
<!-- ruoyi-modules/ruoyi-address/pom.xml -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-elasticsearch</artifactId>
</dependency>
```

```yaml
# ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml
easy-es:
  enable: true

address:
  search:
    enabled: true
    refresh-policy: wait_for
    rebuild-batch-size: 500
    pit-keep-alive: 1m
    standard:
      alias: address_standard_search
      read-enabled: false
      write-enabled: false
    installation:
      alias: address_installation_search
      read-enabled: false
      write-enabled: false
```

- [ ] **Step 4: 重新运行测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=AddressSearchDocumentBuilderTest test`

Expected: PASS

- [ ] **Step 5: 提交基础设施骨架**

```bash
git add ruoyi-modules/ruoyi-address/pom.xml \
  ruoyi-modules/ruoyi-address/src/main/resources/application-standalone.yml \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/config/AddressSearchProperties.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/document/StandardAddressSearchDocument.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/document/InstallationAddressSearchDocument.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/builder/StandardAddressSearchDocumentBuilder.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/builder/InstallationAddressSearchDocumentBuilder.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/esmapper/StandardAddressSearchEsMapper.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/esmapper/InstallationAddressSearchEsMapper.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchDocumentBuilderTest.java
git commit -m "feat: add address search document foundation"
```

### Task 2: 接入标准地址 ES 读链路

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressQueryService.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/StandardAddressSearchGatewayTest.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressQueryServiceTest.java`

- [ ] **Step 1: 先写标准地址读链路失败测试**

```java
@Test
void shouldUseEsGatewayForAddrSegmCandidateWhenReadSwitchEnabled() {
    StandardAddressBo bo = new StandardAddressBo();
    when(dictionaryService.resolveReadonlyRegionAddrLevel(null)).thenReturn(null);
    when(dictionaryService.resolveSegmTypesAtOrBelowAddrLevel(10)).thenReturn(List.of("180010"));
    when(addressSearchProperties.getStandard().isReadEnabled()).thenReturn(true);
    when(standardAddressSearchGateway.searchCandidates("鼓楼", List.of("180010"), "2140900", null, 10))
        .thenReturn(List.of(buildAddrSegmVo("SEG001", 7)));

    List<StandardAddressVo> result = queryService.searchAddrSegmCandidates("鼓楼", 10, "2140900", 10);

    assertEquals(List.of("SEG001"), result.stream().map(StandardAddressVo::getSegmId).toList());
    verify(standardAddressSearchGateway).searchCandidates("鼓楼", List.of("180010"), "2140900", null, 10);
    verify(addrSegmMapper, never()).selectSearchCandidatePage(any(), any(), any(), any());
}

@Test
void shouldKeepSpcRegionReadsOnDatabaseEvenWhenEsSwitchEnabled() {
    StandardAddressBo bo = new StandardAddressBo();
    bo.setAddrLevel(2);
    when(dictionaryService.resolveReadonlyRegionAddrLevel(null)).thenReturn(null);
    when(spcRegionMapper.selectStandardAddressPage(any(), any())).thenReturn(new Page<>(1, 20, 0));

    queryService.queryPageList(bo, new PageQuery(20, 1));

    verify(spcRegionMapper).selectStandardAddressPage(any(), any());
    verifyNoInteractions(standardAddressSearchGateway);
}
```

- [ ] **Step 2: 运行测试，确认当前查询服务尚未切换到 ES**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressQueryServiceTest,StandardAddressSearchGatewayTest test`

Expected: FAIL，提示 `standardAddressSearchGateway` 相关断言不成立或依赖未定义。

- [ ] **Step 3: 实现标准地址 ES 查询网关并接入 `StandardAddressQueryService`**

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java
public interface StandardAddressSearchGateway {
    List<StandardAddressVo> searchCandidates(String keyword, List<String> segmTypes, String status, String regionId, int limit);
    TableDataInfo<StandardAddressVo> queryPage(StandardAddressBo bo, List<String> segmTypes, PageQuery pageQuery);
}

// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java
@Service
@RequiredArgsConstructor
public class EasyEsStandardAddressSearchGateway implements StandardAddressSearchGateway {
    private final StandardAddressSearchEsMapper standardAddressSearchEsMapper;
    private final AddressSearchProperties addressSearchProperties;

    @Override
    public List<StandardAddressVo> searchCandidates(String keyword, List<String> segmTypes, String status, String regionId, int limit) {
        standardAddressSearchEsMapper.setCurrentActiveIndex(addressSearchProperties.getStandard().getAlias());
        LambdaEsQueryWrapper<StandardAddressSearchDocument> wrapper = new LambdaEsQueryWrapper<>();
        wrapper.and(w -> w.match(StandardAddressSearchDocument::getSegmName, keyword)
            .or().match(StandardAddressSearchDocument::getStandName, keyword)
            .or().prefix(StandardAddressSearchDocument::getSegmNo, keyword)
            .or().prefix(StandardAddressSearchDocument::getStandNo, keyword));
        wrapper.in(CollUtil.isNotEmpty(segmTypes), StandardAddressSearchDocument::getSegmType, segmTypes);
        wrapper.eq(StringUtils.isNotBlank(status), StandardAddressSearchDocument::getStatus, status);
        wrapper.eq(StringUtils.isNotBlank(regionId), StandardAddressSearchDocument::getRegionId, regionId);
        wrapper.orderByAsc(StandardAddressSearchDocument::getAddrLevel, StandardAddressSearchDocument::getSegmId);
        return standardAddressSearchEsMapper.pageQuery(wrapper, 1, limit).getList().stream().map(this::toVo).toList();
    }
}
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressQueryService.java
if (addressSearchProperties.getStandard().isReadEnabled()) {
    TableDataInfo<StandardAddressVo> pageResult = standardAddressSearchGateway.queryPage(queryBo, segmTypes, pageQuery);
    enrichAddrSegmRows(pageResult.getRows());
    applyWritableFlags(pageResult.getRows());
    return pageResult;
}
```

- [ ] **Step 4: 重新运行标准地址读链路测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressQueryServiceTest,StandardAddressSearchGatewayTest test`

Expected: PASS

- [ ] **Step 5: 提交标准地址读链路切换**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressQueryService.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/StandardAddressSearchGatewayTest.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressQueryServiceTest.java
git commit -m "feat: route standard address search reads through es"
```

### Task 3: 接入安装地址 ES 读链路

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/InstallationAddressServiceImpl.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/InstallationAddressSearchGatewayTest.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/InstallationAddressServiceImplTest.java`

- [ ] **Step 1: 先写安装地址分页读链路失败测试**

```java
@Test
void shouldUseEsPageResultThenBatchFillStandardAddressNames() {
    when(addressSearchProperties.getInstallation().isReadEnabled()).thenReturn(true);

    InstallationAddressVo first = new InstallationAddressVo();
    first.setSetAddrId("SET001");
    first.setSegmId("SEG001");
    InstallationAddressVo second = new InstallationAddressVo();
    second.setSetAddrId("SET002");
    second.setSegmId("SEG002");

    TableDataInfo<InstallationAddressVo> esPage = new TableDataInfo<>(List.of(first, second), 2);
    when(installationAddressSearchGateway.queryPage(any(), any())).thenReturn(esPage);
    when(standardAddressService.listStandardAddressStandNameMapBySegmIds(List.of("SEG001", "SEG002")))
        .thenReturn(Map.of("SEG001", "标准地址1", "SEG002", "标准地址2"));

    TableDataInfo<InstallationAddressVo> result = installationAddressService.queryPageList(new InstallationAddressBo(), new PageQuery(10, 1));

    assertEquals("标准地址1", result.getRows().get(0).getStandName());
    verify(installationAddressSearchGateway).queryPage(any(), any());
    verify(addrSetSegmMapper, never()).selectInstallationPage(any(), any());
}
```

- [ ] **Step 2: 运行测试，确认当前实现仍然直查数据库**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=InstallationAddressServiceImplTest,InstallationAddressSearchGatewayTest test`

Expected: FAIL，提示 `installationAddressSearchGateway` 未被调用或相关依赖不存在。

- [ ] **Step 3: 实现安装地址 ES 查询网关并接入服务**

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java
public interface InstallationAddressSearchGateway {
    TableDataInfo<InstallationAddressVo> queryPage(InstallationAddressBo bo, PageQuery pageQuery);
}

// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/InstallationAddressServiceImpl.java
@Override
public TableDataInfo<InstallationAddressVo> queryPageList(InstallationAddressBo bo, PageQuery pageQuery) {
    TableDataInfo<InstallationAddressVo> pageData;
    if (addressSearchProperties.getInstallation().isReadEnabled()) {
        pageData = installationAddressSearchGateway.queryPage(bo, pageQuery);
    } else {
        Page<InstallationAddressVo> page = addrSetSegmMapper.selectInstallationPage(pageQuery.build(), bo);
        pageData = TableDataInfo.build(page);
    }
    fillStandardAddressInfo(pageData.getRows());
    return pageData;
}
```

- [ ] **Step 4: 重新运行安装地址读链路测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=InstallationAddressServiceImplTest,InstallationAddressSearchGatewayTest test`

Expected: PASS

- [ ] **Step 5: 提交安装地址读链路切换**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/InstallationAddressServiceImpl.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/InstallationAddressSearchGatewayTest.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/InstallationAddressServiceImplTest.java
git commit -m "feat: route installation address list reads through es"
```

### Task 4: 把标准地址导出改成 `PIT + search_after`

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/model/SearchAfterBatch.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchExportService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/StandardAddressSearchExportServiceTest.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java`

- [ ] **Step 1: 先写导出服务失败测试**

```java
@Test
void shouldStreamExportRowsBySearchAfterBatches() {
    StandardAddressVo first = new StandardAddressVo();
    first.setSegmId("SEG001");
    StandardAddressVo second = new StandardAddressVo();
    second.setSegmId("SEG002");

    SearchAfterBatch<StandardAddressVo> batch1 = SearchAfterBatch.of(List.of(first), List.of(FieldValue.of("SEG001")), false);
    SearchAfterBatch<StandardAddressVo> batch2 = SearchAfterBatch.of(List.of(second), List.of(FieldValue.of("SEG002")), true);
    when(standardAddressSearchGateway.queryExportBatch(any(), isNull(), eq(500))).thenReturn(batch1);
    when(standardAddressSearchGateway.queryExportBatch(any(), any(), eq(500))).thenReturn(batch2);

    List<String> exportedIds = new ArrayList<>();
    exportService.writeRows(new StandardAddressBo(), 500, rows -> exportedIds.addAll(rows.stream().map(StandardAddressVo::getSegmId).toList()));

    assertEquals(List.of("SEG001", "SEG002"), exportedIds);
    verify(standardAddressSearchGateway, times(2)).queryExportBatch(any(), any(), eq(500));
}
```

- [ ] **Step 2: 运行导出相关测试，确认当前控制器仍按页循环数据库分页**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressSearchExportServiceTest,StandardAddressControllerExportTest test`

Expected: FAIL，提示 `StandardAddressSearchExportService` 不存在或控制器仍调用 `queryStandardAddressPageList`。

- [ ] **Step 3: 实现导出服务并改造控制器**

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/model/SearchAfterBatch.java
@Data
@AllArgsConstructor(staticName = "of")
public class SearchAfterBatch<T> {
    private List<T> rows;
    private List<FieldValue> nextSearchAfter;
    private boolean finished;
}

// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchExportService.java
@Service
@RequiredArgsConstructor
public class StandardAddressSearchExportService {
    private final StandardAddressSearchGateway standardAddressSearchGateway;

    public void writeRows(StandardAddressBo bo, int batchSize, Consumer<List<StandardAddressVo>> consumer) {
        List<FieldValue> cursor = null;
        boolean finished = false;
        while (!finished) {
            SearchAfterBatch<StandardAddressVo> batch = standardAddressSearchGateway.queryExportBatch(bo, cursor, batchSize);
            if (batch.getRows().isEmpty()) {
                return;
            }
            consumer.accept(batch.getRows());
            cursor = batch.getNextSearchAfter();
            finished = batch.isFinished();
        }
    }
}
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java
private final StandardAddressSearchExportService standardAddressSearchExportService;

private void writeStandardAddressExportRows(StandardAddressBo bo, ExcelWriterWrapper<StandardAddressVo> writer) {
    var writeSheet = ExcelWriterWrapper.buildSheet("标准地址");
    standardAddressSearchExportService.writeRows(bo, EXPORT_BATCH_SIZE, rows -> writer.write(rows, writeSheet));
}
```

- [ ] **Step 4: 重新运行导出测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressSearchExportServiceTest,StandardAddressControllerExportTest test`

Expected: PASS

- [ ] **Step 5: 提交导出链路改造**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/model/SearchAfterBatch.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchExportService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/StandardAddressSearchExportServiceTest.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java
git commit -m "feat: stream standard address export through search after"
```

### Task 5: 实现同步日志、修复任务与双写事务模板

**Files:**
- Create: `ruoyi-modules/ruoyi-address/sql/address/address_search_support.sql`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddressSearchSyncLog.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddressSearchRepairTask.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddressSearchSyncLogMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddressSearchRepairTaskMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/support/CheckedBooleanSupplier.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchSyncService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchSyncServiceTest.java`

- [ ] **Step 1: 先写双写补偿失败测试**

```java
@Test
void shouldDeleteEsDocumentWhenDatabaseInsertFailsAfterStandardCreate() {
    StandardAddressSearchDocument document = new StandardAddressSearchDocument();
    document.setSegmId("SEG001");
    when(addressSearchProperties.getStandard().isWriteEnabled()).thenReturn(true);
    when(standardAddressSearchGateway.upsert(document)).thenReturn(true);

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> addressSearchSyncService.syncStandardCreate(document, () -> {
            throw new RuntimeException("db failed");
        }));

    assertEquals("db failed", ex.getMessage());
    verify(standardAddressSearchGateway).upsert(document);
    verify(standardAddressSearchGateway).deleteByIds(List.of("SEG001"));
    verify(addressSearchRepairTaskMapper).insert(any(AddressSearchRepairTask.class));
}
```

- [ ] **Step 2: 运行同步服务测试，确认当前尚无双写模板**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=AddressSearchSyncServiceTest test`

Expected: FAIL，提示 `AddressSearchSyncService` 或修复任务实体不存在。

- [ ] **Step 3: 实现 SQL、实体、Mapper 和双写事务模板**

```sql
-- ruoyi-modules/ruoyi-address/sql/address/address_search_support.sql
create table if not exists address_search_sync_log (
    id bigint primary key,
    business_type varchar(32) not null,
    entity_type varchar(32) not null,
    entity_id varchar(32) not null,
    phase varchar(32) not null,
    success_flag char(1) not null,
    error_message varchar(1000),
    created_time datetime not null
);

create table if not exists address_search_repair_task (
    id bigint primary key,
    entity_type varchar(32) not null,
    entity_id varchar(32) not null,
    repair_action varchar(32) not null,
    payload_json text,
    status varchar(32) not null,
    retry_count int not null,
    created_time datetime not null,
    updated_time datetime not null
);
```

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/support/CheckedBooleanSupplier.java
@FunctionalInterface
public interface CheckedBooleanSupplier {
    boolean getAsBoolean() throws Exception;
}

// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java
public interface StandardAddressSearchGateway {
    List<StandardAddressVo> searchCandidates(String keyword, List<String> segmTypes, String status, String regionId, int limit);
    TableDataInfo<StandardAddressVo> queryPage(StandardAddressBo bo, List<String> segmTypes, PageQuery pageQuery);
    SearchAfterBatch<StandardAddressVo> queryExportBatch(StandardAddressBo bo, List<FieldValue> searchAfter, int batchSize);
    boolean upsert(StandardAddressSearchDocument document);
    boolean deleteByIds(List<String> segmIds);
    boolean restore(StandardAddressSearchDocument document);
}

// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java
public interface InstallationAddressSearchGateway {
    TableDataInfo<InstallationAddressVo> queryPage(InstallationAddressBo bo, PageQuery pageQuery);
    boolean upsert(InstallationAddressSearchDocument document);
    boolean deleteByIds(List<String> setAddrIds);
    boolean restore(InstallationAddressSearchDocument document);
}

// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchSyncService.java
@Service
@RequiredArgsConstructor
public class AddressSearchSyncService {
    public boolean syncStandardCreate(StandardAddressSearchDocument document, CheckedBooleanSupplier dbAction) {
        try {
            standardAddressSearchGateway.upsert(document);
            boolean success = dbAction.getAsBoolean();
            recordSuccess("STANDARD", document.getSegmId(), "CREATE");
            return success;
        } catch (Exception ex) {
            rollbackQuietly(() -> standardAddressSearchGateway.deleteByIds(List.of(document.getSegmId())));
            recordRepair("STANDARD", document.getSegmId(), "DELETE_DOC", document);
            throw new RuntimeException(ex);
        }
    }
}
```

- [ ] **Step 4: 重新运行同步服务测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=AddressSearchSyncServiceTest test`

Expected: PASS

- [ ] **Step 5: 提交同步与修复基础设施**

```bash
git add ruoyi-modules/ruoyi-address/sql/address/address_search_support.sql \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddressSearchSyncLog.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddressSearchRepairTask.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddressSearchSyncLogMapper.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddressSearchRepairTaskMapper.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/support/CheckedBooleanSupplier.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchSyncService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchSyncServiceTest.java
git commit -m "feat: add address search sync and repair foundation"
```

### Task 6: 接入标准地址写链路双写与补偿

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressCommandService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressImportService.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressCommandServiceTest.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressImportServiceTest.java`

- [ ] **Step 1: 先写标准地址写链路失败测试**

```java
@Test
void shouldWriteEsBeforeDatabaseWhenAddingStandardAddress() {
    StandardAddressBo bo = buildAddBo();
    when(addressSearchProperties.getStandard().isWriteEnabled()).thenReturn(true);
    when(addressSearchSyncService.syncStandardCreate(any(), any())).thenAnswer(invocation -> {
        CheckedBooleanSupplier dbAction = invocation.getArgument(1);
        return dbAction.getAsBoolean();
    });
    when(addrSegmMapper.insert(any(AddrSegm.class))).thenReturn(1);

    Boolean result = commandService.addStandardAddress(bo);

    assertTrue(result);
    verify(addressSearchSyncService).syncStandardCreate(any(), any());
    verify(addrSegmMapper).insert(any(AddrSegm.class));
}

@Test
void shouldCompensateEsWhenDeletingStandardAddressDatabaseStepFails() {
    when(addressSearchProperties.getStandard().isWriteEnabled()).thenReturn(true);
    when(addrSegmMapper.selectBatchIds(List.of("SEG001"))).thenReturn(List.of(buildExistingAddr("SEG001")));
    when(addrSegmMapper.countChildren(List.of("SEG001"))).thenReturn(0L);
    when(addrSetSegmMapper.countBySegmIds(List.of("SEG001"))).thenReturn(0L);
    doThrow(new RuntimeException("db failed")).when(addrSegmMapper).logicalDeleteBySegmIds(List.of("SEG001"));

    assertThrows(RuntimeException.class, () -> commandService.deleteStandardAddresses(List.of("SEG001"), true));

    verify(addressSearchSyncService).syncStandardDelete(anyList(), any());
}
```

- [ ] **Step 2: 运行标准地址命令测试，确认当前命令服务尚未接入双写模板**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressCommandServiceTest,StandardAddressImportServiceTest test`

Expected: FAIL，提示 `addressSearchSyncService` 相关断言不成立。

- [ ] **Step 3: 在 `StandardAddressCommandService` 中接入同步模板**

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressCommandService.java
@Transactional(rollbackFor = Exception.class)
public Boolean addStandardAddress(StandardAddressBo bo) {
    Integer requestedAddrLevel = resolveWritableAddrLevel(bo, null, parent, "新增");
    AddrSegm entity = buildEntity(bo, null, parent, requestedAddrLevel);
    StandardAddressSearchDocument document = standardAddressSearchDocumentBuilder.fromEntity(entity, requestedAddrLevel);
    return addressSearchSyncService.syncStandardCreate(document, () -> {
        boolean success = addrSegmMapper.insert(entity) > 0;
        if (success) {
            operationLogRecorder.record(entity.getSegmId(), OPERATION_TYPE_INSERT, entity.getStandName(), "新增标准地址成功");
        }
        return success;
    });
}

@Transactional(rollbackFor = Exception.class)
public Boolean updateStandardAddress(StandardAddressBo bo) {
    AddrSegm existing = requireActiveAddress(bo.getSegmId(), "修改");
    AddrSegm update = buildEntity(bo, existing, parent, addrLevel);
    StandardAddressSearchDocument beforeDocument = standardAddressSearchDocumentBuilder.fromEntity(existing, dictionaryService.resolveAddrLevel(existing.getSegmType()));
    StandardAddressSearchDocument afterDocument = standardAddressSearchDocumentBuilder.fromEntity(update, addrLevel);
    return addressSearchSyncService.syncStandardUpdate(beforeDocument, afterDocument, () -> {
        boolean success = addrSegmMapper.updateById(update) > 0;
        if (success) {
            refreshChildrenStandInfo(update);
        }
        return success;
    });
}
```

导入链路只做一处修改：保留 `StandardAddressImportService` 现有流程，但新增测试证明 `addStandardAddressForImport` / `updateStandardAddressForImport` 已复用双写逻辑，不再额外分叉实现。

- [ ] **Step 4: 重新运行标准地址写链路测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressCommandServiceTest,StandardAddressImportServiceTest test`

Expected: PASS

- [ ] **Step 5: 提交标准地址双写改造**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressCommandService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressImportService.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressCommandServiceTest.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressImportServiceTest.java
git commit -m "feat: sync standard address writes to es"
```

### Task 7: 接入安装地址写链路双写与补偿

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/InstallationAddressServiceImpl.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/InstallationAddressServiceImplTest.java`

- [ ] **Step 1: 先写安装地址写链路失败测试**

```java
@Test
void shouldSyncInstallationAddressCreateThroughAddressSearchSyncService() {
    InstallationAddressBo bo = new InstallationAddressBo();
    bo.setSetAddrName("机房1排1列");
    bo.setSegmId("SEG001");
    when(addressSearchProperties.getInstallation().isWriteEnabled()).thenReturn(true);
    when(addressSearchSyncService.syncInstallationCreate(any(), any())).thenAnswer(invocation -> {
        CheckedBooleanSupplier dbAction = invocation.getArgument(1);
        return dbAction.getAsBoolean();
    });
    when(addrSetSegmMapper.insert(any(AddrSetSegm.class))).thenReturn(1);

    Boolean result = installationAddressService.insertByBo(bo);

    assertTrue(result);
    verify(addressSearchSyncService).syncInstallationCreate(any(), any());
}

@Test
void shouldCaptureOldSnapshotBeforeInstallationAddressDelete() {
    when(addressSearchProperties.getInstallation().isWriteEnabled()).thenReturn(true);
    when(addrSetSegmMapper.selectBatchIds(List.of("SET001"))).thenReturn(List.of(buildSetEntity("SET001", "SEG001")));
    when(addressSearchSyncService.syncInstallationDelete(anyList(), any())).thenReturn(true);

    installationAddressService.deleteWithValidByIds(List.of("SET001"), true);

    verify(addrSetSegmMapper).selectBatchIds(List.of("SET001"));
    verify(addressSearchSyncService).syncInstallationDelete(anyList(), any());
}
```

- [ ] **Step 2: 运行安装地址服务测试，确认当前写链路未接入双写模板**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=InstallationAddressServiceImplTest test`

Expected: FAIL，提示 `addressSearchSyncService` 未被调用。

- [ ] **Step 3: 在安装地址新增、修改、删除中接入双写模板**

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/InstallationAddressServiceImpl.java
@Override
public Boolean insertByBo(InstallationAddressBo bo) {
    AddrSetSegm entity = buildEntity(bo, null);
    InstallationAddressSearchDocument document = installationAddressSearchDocumentBuilder.fromEntity(entity, "UNBOUND");
    return addressSearchSyncService.syncInstallationCreate(document, () -> addrSetSegmMapper.insert(entity) > 0);
}

@Override
public Boolean updateByBo(InstallationAddressBo bo) {
    AddrSetSegm existing = addrSetSegmMapper.selectById(bo.getSetAddrId());
    AddrSetSegm update = buildEntity(bo, existing);
    InstallationAddressSearchDocument beforeDocument = installationAddressSearchDocumentBuilder.fromEntity(existing, resolveAssociationStatus(existing));
    InstallationAddressSearchDocument afterDocument = installationAddressSearchDocumentBuilder.fromEntity(update, resolveAssociationStatus(update));
    return addressSearchSyncService.syncInstallationUpdate(beforeDocument, afterDocument, () -> addrSetSegmMapper.updateById(update) > 0);
}
```

- [ ] **Step 4: 重新运行安装地址服务测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=InstallationAddressServiceImplTest test`

Expected: PASS

- [ ] **Step 5: 提交安装地址双写改造**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/InstallationAddressServiceImpl.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/InstallationAddressServiceImplTest.java
git commit -m "feat: sync installation address writes to es"
```

### Task 8: 实现全量重建、repair 执行与维护入口

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceService.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/AddressSearchMaintenanceController.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddrSegmMapper.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddrSetSegmMapper.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchMaintenanceServiceTest.java`

- [ ] **Step 1: 先写全量重建与 repair 失败测试**

```java
@Test
void shouldRebuildStandardIndexByBatchThenSwitchAlias() throws Exception {
    when(standardAddressSearchGateway.prepareRebuildIndex()).thenReturn("address_standard_search_v2");
    Page<AddrSegm> page1 = new Page<>(1, 500, 1);
    page1.setRecords(List.of(buildAddrSegm("SEG001")));
    Page<AddrSegm> page2 = new Page<>(1, 500, 0);
    page2.setRecords(List.of());
    when(addrSegmMapper.selectPage(any(), any())).thenReturn(page1, page2);

    maintenanceService.rebuildStandardIndex();

    verify(standardAddressSearchGateway).bulkIndex(eq("address_standard_search_v2"), anyList());
    verify(standardAddressSearchGateway).switchAlias("address_standard_search_v2");
}

@Test
void shouldReplayPendingRepairTask() {
    AddressSearchRepairTask task = buildPendingTask("STANDARD", "SEG001", "UPSERT_DOC");
    when(addressSearchRepairTaskMapper.selectById(task.getId())).thenReturn(task);

    maintenanceService.executeRepairTask(task.getId());

    verify(standardAddressSearchGateway).repair(task);
    verify(addressSearchRepairTaskMapper).updateById(argThat(updated -> "SUCCESS".equals(updated.getStatus())));
}
```

- [ ] **Step 2: 运行维护服务测试，确认当前还没有重建和 repair 能力**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=AddressSearchMaintenanceServiceTest test`

Expected: FAIL，提示 `AddressSearchMaintenanceService`、`prepareRebuildIndex` 或 `executeRepairTask` 不存在。

- [ ] **Step 3: 实现维护服务与维护控制器**

```java
// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceService.java
@Service
@RequiredArgsConstructor
public class AddressSearchMaintenanceService {
    private final StandardAddressSearchGateway standardAddressSearchGateway;
    private final InstallationAddressSearchGateway installationAddressSearchGateway;
    private final AddressSearchRepairTaskMapper addressSearchRepairTaskMapper;
    private final AddrSegmMapper addrSegmMapper;
    private final AddrSetSegmMapper addrSetSegmMapper;

    public void rebuildStandardIndex() throws Exception {
        String rebuildIndex = standardAddressSearchGateway.prepareRebuildIndex();
        String lastSegmId = null;
        while (true) {
            Page<AddrSegm> page = new Page<>(1, 500, false);
            LambdaQueryWrapper<AddrSegm> wrapper = Wrappers.<AddrSegm>lambdaQuery()
                .eq(AddrSegm::getDeleteState, "0")
                .gt(StringUtils.isNotBlank(lastSegmId), AddrSegm::getSegmId, lastSegmId)
                .orderByAsc(AddrSegm::getSegmId);
            Page<AddrSegm> batchPage = addrSegmMapper.selectPage(page, wrapper);
            if (batchPage.getRecords().isEmpty()) {
                break;
            }
            standardAddressSearchGateway.bulkIndex(rebuildIndex, batchPage.getRecords());
            lastSegmId = batchPage.getRecords().get(batchPage.getRecords().size() - 1).getSegmId();
        }
        standardAddressSearchGateway.switchAlias(rebuildIndex);
    }
}

// ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/AddressSearchMaintenanceController.java
@RestController
@RequiredArgsConstructor
@RequestMapping("/address/search")
public class AddressSearchMaintenanceController extends BaseController {
    private final AddressSearchMaintenanceService addressSearchMaintenanceService;

    @PostMapping("/rebuild/standard")
    public R<Void> rebuildStandardIndex() throws Exception {
        addressSearchMaintenanceService.rebuildStandardIndex();
        return R.ok();
    }

    @PostMapping("/repair/{taskId}")
    public R<Void> executeRepair(@PathVariable Long taskId) {
        addressSearchMaintenanceService.executeRepairTask(taskId);
        return R.ok();
    }
}
```

- [ ] **Step 4: 运行维护测试与核心回归测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=AddressSearchMaintenanceServiceTest,StandardAddressQueryServiceTest,InstallationAddressServiceImplTest,StandardAddressCommandServiceTest test`

Expected: PASS

- [ ] **Step 5: 提交维护能力与回归结果**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/AddressSearchMaintenanceService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/AddressSearchMaintenanceController.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddrSegmMapper.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddrSetSegmMapper.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/StandardAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/InstallationAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsStandardAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/search/service/impl/EasyEsInstallationAddressSearchGateway.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/search/AddressSearchMaintenanceServiceTest.java
git commit -m "feat: add address search rebuild and repair maintenance"
```

## 收尾验证

- 跑标准地址读链路回归：
  `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressQueryServiceTest,StandardAddressSelectionServiceTest test`
- 跑安装地址读写回归：
  `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=InstallationAddressServiceImplTest test`
- 跑导出回归：
  `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=StandardAddressControllerExportTest,StandardAddressSearchExportServiceTest test`
- 跑双写与 repair 回归：
  `mvn -pl ruoyi-modules/ruoyi-address -DskipTests=false -Dtest=AddressSearchSyncServiceTest,AddressSearchMaintenanceServiceTest,StandardAddressCommandServiceTest test`
- 本地启动冒烟：
  `mvn -pl ruoyi-modules/ruoyi-address -am spring-boot:run -Dspring-boot.run.profiles=standalone`

## 实施顺序约束

- Task 1 必须先完成，否则后续 ES 读写接口、别名和文档模型都无从落地。
- Task 2 与 Task 3 可以并行，但 Task 4 依赖 Task 2。
- Task 5 完成前禁止改写标准地址或安装地址在线写链路。
- Task 6 与 Task 7 可以串行推进；建议先标准地址，后安装地址。
- Task 8 必须在 Task 5 至 Task 7 完成后再做，以便 maintenance 服务直接复用真实网关与 repair 表。
