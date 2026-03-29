# 标准地址核心主链路 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于线上 `ADDR_SEGM / spc_region / segm_addr_type / spc_station / pub_restriction` 落成标准地址核心主链路首批可运行实现，覆盖列表、详情、新增、修改、删除、批量下级地址预览、批量新增下级地址、导出。

**Architecture:** 保留 `ruoyi-address` 现有 `controller / service / mapper / domain` 主结构，引入围绕线上表的实体与 Mapper，由 `StandardAddressServiceImpl` 作为 facade 统一编排。查询按 `levelId` 分流：`1/2` 级只读投影走 `spc_region`，`3+` 级和全部写操作走 `ADDR_SEGM`；对 `ADDR_SEGM` 的层级条件一律先通过 `segm_addr_type` 解析成目标 `segm_type` 集合，再作用于 `addr_segm.segm_type`；`levelId` 只从 `segm_addr_type` 投影，不直接落库；名称、简拼、删除校验、批量预览和级联刷新集中在独立 helper/service 中，旧 `StandardAddress` 旧表模型与旧 Mapper 暂不删除，只作为未纳入首批功能的编译兼容保留。

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis-Plus + XML, RuoYi `R/TableDataInfo/PageQuery`, FastExcel, Hutool, JUnit 5, Spring Boot Test, Mockito.

---

## 范围边界

- 本计划只实现标准地址首批主链路：
  `POST /address/standard/list`
  `POST /address/standard/{segmId}`
  `POST /address/standard`
  `POST /address/standard/update`
  `POST /address/standard/remove/{segmIds}`
  `POST /address/standard/batchPreviewChild`
  `POST /address/standard/batchAddChild`
  `POST /address/standard/export`
- 本计划明确不落首批代码：
  合并、拆分、导入、导入回滚、标签、日志、非标监控、管理站独立管理、网格、legacy 外部兼容接口、BOSS/工单/企业微信/地图联调。
- 对未纳入首批但仍依赖 `StandardAddressBo/Vo/IStandardAddressService` 的内部代码，使用“兼容访问器/默认方法”保证编译通过，不反向牵引首批模型继续沿用旧泛化命名。

## 原型对齐

- 原型入口：`https://ftthtest.wetrytech.com/#/standardAddress/addressList`
- 已确认页面交互：
  列表筛选仅使用“标准地址 + 级别”
  列表列包含“标准地址、地址简拼、父级地址、当级名称、级别、创建时间”
  详情页展示扩展字段：标签、三类管理站、接入方式、接入能力、城乡属性、房屋属性、是否配套费小区、覆盖户数、工程编号
  新增/编辑弹窗字段与详情字段一致，但父级地址和拼装名称只读
  “更多操作”下包含“批量新增地址”，弹窗左侧为父级地址模糊搜索，右侧为 `prefix/start/end` 规则和实时预览
- 后端实现以需求文档和线上表结构为准，不以原型中的 mock 数据结构反推表设计。

## 执行前确认

- `spec_region` 在实现中统一按 `spc_region` 落地。
- `segmId` 生成规则文档尚未给出唯一来源，首批实现将生成逻辑封装在独立组件中，先采用“应用侧生成 24 位字符串 ID”的可替换策略；若后续联调库要求 sequence/trigger，只替换该组件。
- 删除校验中的“关联资源”首批先预留扩展点，实际强校验先覆盖“下级地址 + 安装地址”；网格、客户等关联待对应模块具备后补挂。
- `StandardAddressBo/Vo` 必须以线上 canonical 命名为主，但需要保留 deprecated 兼容访问器，避免 `选址 / Dubbo / 非首批服务` 立即失编。
- 批量新增父级搜索接口必须限制单次候选不超过 `200` 条，避免把原型 mock 的无限滚动误实现成全量模糊检索。

## 文件结构与职责

### 新增文件

- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddrSegm.java`
  线上 `ADDR_SEGM` 主实体，字段名与注释直接对齐 `segm_* / stand_* / region_id / station_id / notes`。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/SegmAddrType.java`
  地址类型与层级投影实体，负责 `addr_type_id -> level_id`。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/SpcRegion.java`
  `1/2` 级区域主数据实体。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/SpcStation.java`
  管理站旁证实体，用于详情和列表补充名称。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/PubRestriction.java`
  字典实体，负责状态、城乡属性、接入能力等文本解释。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddrSegmMapper.java`
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/SegmAddrTypeMapper.java`
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/SpcRegionMapper.java`
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/SpcStationMapper.java`
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/PubRestrictionMapper.java`
  线上表访问层。
- `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/AddrSegmMapper.xml`
- `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/SegmAddrTypeMapper.xml`
- `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/SpcRegionMapper.xml`
- `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/SpcStationMapper.xml`
- `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/PubRestrictionMapper.xml`
  自定义查询 SQL，承载列表、详情、模糊父级搜索、级联查询、批量预览依赖 SQL。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressDictionaryService.java`
  `segmType -> levelId`、状态字典、管理站字典、级别文本解释。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressNameService.java`
  `standName/standNo/segmNo` 拼装与级联刷新。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressIdGenerator.java`
  可替换的 `segmId` 生成器。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressQueryService.java`
  列表、详情、导出查询聚合。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressCommandService.java`
  新增、修改、删除、批量预览、批量新增写链路聚合。
- `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressContractTest.java`
- `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressDictionaryServiceTest.java`
- `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressNameServiceTest.java`
- `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressQueryServiceTest.java`
- `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressCommandServiceTest.java`
- `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerMappingTest.java`
  首批单元测试。

### 修改文件

- `ruoyi-modules/ruoyi-address/pom.xml`
  为模块显式补齐测试依赖。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressAdminApi.java`
  将 `Long id/ids` 改为 `String segmId/segmIds`，补齐批量预览/导出合同注释。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java`
  改成实现 `StandardAddressAdminApi` 的真正落地 Controller，全部改为 `POST`。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/IStandardAddressService.java`
  增加 string 主键语义的方法，保留 deprecated 兼容默认方法。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressServiceImpl.java`
  从旧单体实现切换为 facade。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressBo.java`
  字段统一为 `segmId/parentSegmId/segmName/segmNo/standName/standNo/segmType/levelId/regionId/districtId/serviceRegionId/status/notes/...`，保留 deprecated alias 访问器。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressBatchAddBo.java`
  使用 `parentSegmId/prefix/startNum/endNum` 并预留 `previewOnly` 等扩展字段。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressVo.java`
  统一返回模型，增加只读标记、三类管理站名称和扩展字段。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressAdminVo.java`
  将 `BatchPreviewVo` 改成 canonical 字段：`segmName/standName/segmNo/standNo/levelId/segmType`。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressSelectionServiceImpl.java`
  只做最小编译兼容调整，避免引用已废弃字段名。
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/dubbo/RemoteStandardAddressServiceImpl.java`
  只做最小编译兼容调整，保留旧 Dubbo 接口不扩需求。

### 保留不动

- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/StandardAddress.java`
- `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/StandardAddressMapper.java`
- `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/AddressStandardMapper.xml`
  首批不直接删除，避免影响选址、监控、标签等未进入本轮实现的旧代码；二阶段整体迁移时再统一清理。

### Task 1: 锁定 API 契约与测试脚手架

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/pom.xml`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressAdminApi.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/IStandardAddressService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressBo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressBatchAddBo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressVo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressAdminVo.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressContractTest.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerMappingTest.java`

- [ ] **Step 1: 写失败测试，锁定 canonical 字段与 POST 合同**

```java
@Test
void shouldUseCanonicalStandardAddressFields() {
    StandardAddressBo bo = new StandardAddressBo();
    bo.setSegmId("000102010000000011800001");
    bo.setParentSegmId("000102010000000011800000");
    bo.setSegmName("淮海路街道");
    bo.setLevelId(3);
    assertEquals("000102010000000011800001", bo.getSegmId());
}

@Test
void shouldKeepDeprecatedAliasForNonCoreCallers() {
    StandardAddressVo vo = new StandardAddressVo();
    vo.setSegmId("000102010000000011800001");
    vo.setStandName("江苏省南京市主城区白下区淮海路街道");
    assertEquals("000102010000000011800001", vo.getId());
    assertEquals("江苏省南京市主城区白下区淮海路街道", vo.getFullName());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressContractTest,StandardAddressControllerMappingTest test`

Expected: FAIL，原因包括字段不存在、接口仍是 `Long id`、Controller 仍未实现 `POST` 合同。

- [ ] **Step 3: 最小实现测试脚手架和契约模型**

```java
public interface IStandardAddressService {

    StandardAddressVo getStandardAddressBySegmId(String segmId);

    @Deprecated
    default StandardAddressVo getStandardAddressById(Long id) {
        return id == null ? null : getStandardAddressBySegmId(String.valueOf(id));
    }
}
```

- [ ] **Step 4: 重新运行测试确认通过**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressContractTest,StandardAddressControllerMappingTest test`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add ruoyi-modules/ruoyi-address/pom.xml \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressAdminApi.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/IStandardAddressService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressBo.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressBatchAddBo.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressVo.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressAdminVo.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressContractTest.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerMappingTest.java
git commit -m "test: lock standard address core contract"
```

### Task 2: 落地线上表访问层与共享规则组件

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddrSegm.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/SegmAddrType.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/SpcRegion.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/SpcStation.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/PubRestriction.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddrSegmMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/SegmAddrTypeMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/SpcRegionMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/SpcStationMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/PubRestrictionMapper.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/AddrSegmMapper.xml`
- Create: `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/SegmAddrTypeMapper.xml`
- Create: `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/SpcRegionMapper.xml`
- Create: `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/SpcStationMapper.xml`
- Create: `ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/PubRestrictionMapper.xml`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressDictionaryService.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressNameService.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressIdGenerator.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressDictionaryServiceTest.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressNameServiceTest.java`

- [ ] **Step 1: 写失败测试，先锁定 `segmType -> levelId`、简拼规则、24 位 ID 输出**

```java
@Test
void shouldResolveLevelIdBySegmType() {
    when(segmAddrTypeMapper.selectLevelIdByAddrTypeId(180003)).thenReturn(3);
    assertEquals(3, dictionaryService.resolveLevelId(180003));
}

@Test
void shouldGenerateStandNoWithUpperInitials() {
    assertEquals("JSSNJSZCQBXQHHLJD",
        nameService.buildStandNo("江苏省南京市主城区白下区淮海路街道"));
}

@Test
void shouldGenerate24CharSegmId() {
    String segmId = idGenerator.nextSegmId();
    assertEquals(24, segmId.length());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressDictionaryServiceTest,StandardAddressNameServiceTest test`

Expected: FAIL，原因包括 mapper/service/ID 生成器尚不存在。

- [ ] **Step 3: 实现线上表实体、Mapper、XML 与共享规则组件**

```java
public String buildStandNo(String standName) {
    String normalized = standName.replace('（', '(').replace('）', ')');
    return PinyinUtil.getFirstLetter(normalized, "").toUpperCase(Locale.ROOT);
}
```

```java
public String nextSegmId() {
    return String.format("%024d", IdUtil.getSnowflakeNextId());
}
```

- [ ] **Step 4: 重新运行测试确认通过**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressDictionaryServiceTest,StandardAddressNameServiceTest test`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/AddrSegm.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/SegmAddrType.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/SpcRegion.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/SpcStation.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/PubRestriction.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/AddrSegmMapper.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/SegmAddrTypeMapper.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/SpcRegionMapper.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/SpcStationMapper.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/mapper/PubRestrictionMapper.java \
  ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/AddrSegmMapper.xml \
  ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/SegmAddrTypeMapper.xml \
  ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/SpcRegionMapper.xml \
  ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/SpcStationMapper.xml \
  ruoyi-modules/ruoyi-address/src/main/resources/mapper/address/PubRestrictionMapper.xml \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressDictionaryService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressNameService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressIdGenerator.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressDictionaryServiceTest.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressNameServiceTest.java
git commit -m "feat: add online schema access for standard address"
```

### Task 3: 实现查询分流、统一投影与导出查询

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressQueryService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/IStandardAddressService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressVo.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressQueryServiceTest.java`

- [ ] **Step 1: 写失败测试，锁定查询分流与只读投影**

```java
@Test
void shouldRouteLevel1AndLevel2ToSpcRegion() {
    StandardAddressBo bo = new StandardAddressBo();
    bo.setLevelId(1);
    queryService.queryPageList(bo, new PageQuery());
    verify(spcRegionMapper).selectStandardAddressPage(any(), any());
    verify(addrSegmMapper, never()).selectStandardAddressPage(any(), any());
}

@Test
void shouldMarkSpcRegionRecordReadonly() {
    StandardAddressVo vo = queryService.getBySegmId("320100", 1);
    assertTrue(vo.getReadOnlyFlag());
    assertFalse(vo.getCanEdit());
    assertFalse(vo.getCanDelete());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressQueryServiceTest test`

Expected: FAIL，原因包括查询服务尚未存在、Mapper 方法未定义、只读字段未投影。

- [ ] **Step 3: 实现查询服务与统一 VO 装配**

```java
if (levelId != null && (levelId == 1 || levelId == 2)) {
    return spcRegionMapper.selectStandardAddressPage(bo, pageQuery);
}
List<Integer> segmTypes = dictionaryService.resolveSegmTypesByLevelId(levelId);
return addrSegmMapper.selectStandardAddressPage(bo, segmTypes, pageQuery);
```

```java
private StandardAddressVo toReadOnlyRegionVo(SpcRegion region, Integer levelId) {
    StandardAddressVo vo = new StandardAddressVo();
    vo.setSegmId(region.getRegionId());
    vo.setParentSegmId(region.getSuperRegionId());
    vo.setSegmName(region.getRegionName());
    vo.setStandName(region.getRegionName());
    vo.setLevelId(levelId);
    vo.setReadOnlyFlag(true);
    vo.setCanEdit(false);
    vo.setCanDelete(false);
    return vo;
}
```

- [ ] **Step 4: 重新运行测试确认通过**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressQueryServiceTest test`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressQueryService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressServiceImpl.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/IStandardAddressService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressVo.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressQueryServiceTest.java
git commit -m "feat: add standard address query routing"
```

### Task 4: 实现新增、修改、删除、批量预览与批量新增

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressCommandService.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressBo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressBatchAddBo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressAdminVo.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressCommandServiceTest.java`

- [ ] **Step 1: 写失败测试，锁定写链路核心规则**

```java
@Test
void shouldRejectWriteForLevel1AndLevel2() {
    when(dictionaryService.resolveLevelId(180000)).thenReturn(1);
    StandardAddressBo bo = new StandardAddressBo();
    bo.setSegmType(180000);
    assertThrows(ServiceException.class, () -> commandService.addStandardAddress(bo));
}

@Test
void shouldRequireConfirmWhenInstallationAddressExists() {
    when(addrSegmMapper.countChildren(List.of("segm-1"))).thenReturn(0L);
    when(installationAddressMapper.countBySegmIds(List.of("segm-1"))).thenReturn(2L);
    assertThrows(ServiceException.class, () -> commandService.removeStandardAddresses(List.of("segm-1"), false));
}

@Test
void shouldPreviewBatchChildrenByParentAndRange() {
    List<BatchPreviewVo> preview = commandService.previewChildren("segm-parent", "A", 1, 3);
    assertEquals(3, preview.size());
    assertEquals("A1", preview.get(0).getSegmName());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressCommandServiceTest test`

Expected: FAIL，原因包括命令服务尚不存在、删除校验未实现、批量预览未实现。

- [ ] **Step 3: 实现命令服务**

```java
public void validateWritableLevel(Integer levelId, String action) {
    if (levelId != null && (levelId == 1 || levelId == 2)) {
        throw new ServiceException(action + "失败：一二级标准地址为只读基础数据");
    }
}
```

```java
public List<BatchPreviewVo> previewChildren(StandardAddressBatchAddBo bo) {
    Integer childLevelId = dictionaryService.resolveNextLevelId(bo.getParentSegmId());
    return IntStream.rangeClosed(bo.getStartNum(), bo.getEndNum())
        .mapToObj(no -> buildPreviewItem(bo, childLevelId, no))
        .toList();
}
```

- [ ] **Step 4: 重新运行测试确认通过**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressCommandServiceTest test`

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressCommandService.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressServiceImpl.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressBo.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/bo/StandardAddressBatchAddBo.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressAdminVo.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressCommandServiceTest.java
git commit -m "feat: add standard address command flow"
```

### Task 5: 接入 Controller、导出能力与最小兼容收口

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressSelectionServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/dubbo/RemoteStandardAddressServiceImpl.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressVo.java`
- Test: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerMappingTest.java`

- [ ] **Step 1: 写失败测试，锁定 Controller 到 Service 的最终装配**

```java
@Test
void shouldExposePostOnlyEndpointsFromController() {
    RequestMapping mapping = StandardAddressController.class.getAnnotation(RequestMapping.class);
    assertArrayEquals(new String[]{"/address/standard"}, mapping.value());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressControllerMappingTest test`

Expected: FAIL，原因包括 Controller 尚未实现 `StandardAddressAdminApi` 或仍保留 `GET/PUT/DELETE`。

- [ ] **Step 3: 落地 Controller 与兼容收口**

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/address/standard")
public class StandardAddressController extends BaseController implements StandardAddressAdminApi {
    private final IStandardAddressService standardAddressService;
}
```

```java
@Deprecated
public String getFullName() {
    return this.standName;
}
```

- [ ] **Step 4: 运行模块级验证**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false test`

Expected: PASS，至少包含本计划新增测试全部通过；如旧非首批测试或示例依赖缺失，先修正编译兼容再继续。

- [ ] **Step 5: 提交本任务**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressServiceImpl.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressSelectionServiceImpl.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/dubbo/RemoteStandardAddressServiceImpl.java \
  ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressVo.java \
  ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerMappingTest.java
git commit -m "feat: wire standard address core controller"
```

## 验证清单

- 单测：
  `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressContractTest,StandardAddressControllerMappingTest test`
  `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressDictionaryServiceTest,StandardAddressNameServiceTest test`
  `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressQueryServiceTest test`
  `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dtest=StandardAddressCommandServiceTest test`
- 模块全量：
  `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false test`
- 手工联调建议：
  列表页验证“标准地址 + 级别”筛选
  详情页验证只读字段和三类管理站/扩展属性回显
  新增/修改验证 `standName/standNo` 自动刷新
  删除验证二次确认
  批量新增弹窗验证父级搜索、预览、执行批量生成
  导出验证字段顺序与列表一致

## 风险与止损

- `segmId` 生成规则仍是本计划唯一需要在编码首日二次确认的高风险项；止损方式是把生成逻辑锁在 `StandardAddressIdGenerator`，避免写死到业务方法。
- `spc_region` 是否就是联调库真实表名需要在首个 SQL 联调前再核一次；若库名不同，只改 Mapper/XML。
- 首批不删除旧 `StandardAddress` 旧表模型，目的是避免监控、标签、选址、Dubbo 等未纳入本轮需求的代码一起被迫迁移。
- 删除关联资源只先落“安装地址”强校验，网格/客户关系用扩展点占位，避免在其他模块尚未落地前把标准地址主链路卡死。

## 评审说明

- `writing-plans` 技能要求计划完成后走 reviewer subagent 复审；当前会话受工具约束，不能派 reviewer 子代理。
- 执行本计划前，改用“本地自审 + 每个任务完成后跑对应测试命令”的方式替代 reviewer loop。
