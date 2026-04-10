# Address Module Dependency Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `ruoyi-common-excel`、`ruoyi-common-dubbo` 中仅服务于 `ruoyi-address` 的改动收回到 `ruoyi-address` 内部，并恢复公共模块源码通用性。

**Architecture:** `Excel` 侧通过新增地址模块私有导出层承接导入模板的默认内容行高与列宽控制，再让控制器切换到该私有入口，最后回退公共 `ExcelUtil`。`Dubbo` 侧不新增 Java 封装，而是用地址模块本地 runtime 与 Nacos 配置显式声明 `dubbo.custom.*`，再回退公共 `DubboCustomProperties` 默认值。

**Tech Stack:** Java 17, Spring Boot, FastExcel, JUnit 5, Mockito, Apache POI, Maven

---

### Task 1: 先写失败测试锁定地址模板导出必须走私有导出层

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/excel/AddressTemplateExcelExporterTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java`

- [ ] **Step 1: 新增地址模板导出器测试，锁定默认内容行高、列宽与下拉框行为**

```java
package org.dromara.address.excel;

import cn.hutool.extra.spring.SpringUtil;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.common.core.service.DictService;
import org.dromara.common.excel.core.DropDownOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class AddressTemplateExcelExporterTest {

    @Test
    void shouldPreserveAnnotatedRowHeightsAndColumnWidthsWhenAutoWidthDisabled() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<DropDownOptions> options = List.of(
            new DropDownOptions(0, List.of("建筑、楼栋")),
            new DropDownOptions(1, List.of("是", "否")),
            new DropDownOptions(7, List.of("FTTH_双纤"))
        );

        try (var springUtilMock = org.mockito.Mockito.mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean(DictService.class)).thenReturn(org.mockito.Mockito.mock(DictService.class));

            AddressTemplateExcelExporter.exportTemplate(
                List.<StandardAddressImportVo>of(),
                "标准地址导入模板",
                StandardAddressImportVo.class,
                outputStream,
                options,
                false
            );
        }

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);

            assertEquals(24f, header.getHeightInPoints());
            assertEquals(22f, sheet.getDefaultRowHeightInPoints());
            assertEquals(18 * 256, sheet.getColumnWidth(0));
            assertEquals(12 * 256, sheet.getColumnWidth(1));
            assertEquals(48 * 256, sheet.getColumnWidth(2));

            Set<Integer> validatedColumns = new HashSet<>();
            for (DataValidation validation : sheet.getDataValidations()) {
                for (var region : validation.getRegions().getCellRangeAddresses()) {
                    for (int column = region.getFirstColumn(); column <= region.getLastColumn(); column++) {
                        validatedColumns.add(column);
                    }
                }
            }
            assertTrue(validatedColumns.containsAll(Set.of(0, 1, 7)));
        }
    }
}
```

- [ ] **Step 2: 修改控制器导出测试，锁定模板下载接口必须委托给地址私有导出器，而不是继续走公共 `ExcelUtil.exportExcel(List, ..., options)`**

```java
@Test
void shouldDelegateImportTemplateDownloadToAddressTemplateExcelExporter() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    List<DropDownOptions> options = List.of(
        new DropDownOptions(0, List.of("建筑、楼栋")),
        new DropDownOptions(1, List.of("是", "否"))
    );
    when(standardAddressService.listStandardAddressImportTemplateOptions()).thenReturn(options);

    try (var addressExcelMock = org.mockito.Mockito.mockStatic(AddressTemplateExcelExporter.class);
         var excelUtilMock = org.mockito.Mockito.mockStatic(ExcelUtil.class)) {
        excelUtilMock.when(() -> ExcelUtil.encodingFilename(anyString())).thenReturn("template.xlsx");

        controller.downloadStandardAddressImportTemplate(response);

        addressExcelMock.verify(() -> AddressTemplateExcelExporter.exportTemplate(
            eq(List.<StandardAddressImportVo>of()),
            eq("标准地址导入模板"),
            eq(StandardAddressImportVo.class),
            any(java.io.OutputStream.class),
            eq(options),
            eq(false)
        ));
    }
}
```

- [ ] **Step 3: 运行测试，确认它们先失败**

Run:

```bash
mvn -pl ruoyi-modules/ruoyi-address -Dtest=AddressTemplateExcelExporterTest,StandardAddressControllerExportTest test
```

Expected:

```text
FAIL
... AddressTemplateExcelExporter cannot be resolved ...
... Wanted but not invoked: AddressTemplateExcelExporter.exportTemplate ...
```

### Task 2: 实现地址私有导出层并回退公共 Excel 特化

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/excel/AddressDefaultRowHeightWriteHandler.java`
- Create: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/excel/AddressTemplateExcelExporter.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java`
- Modify: `ruoyi-common/ruoyi-common-excel/src/main/java/org/dromara/common/excel/utils/ExcelUtil.java`
- Delete: `ruoyi-common/ruoyi-common-excel/src/main/java/org/dromara/common/excel/handler/DefaultRowHeightWriteHandler.java`

- [ ] **Step 1: 新增地址模块私有默认内容行高处理器**

```java
package org.dromara.address.excel;

import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;

/**
 * 地址模板默认内容行高写入处理器。
 * 目的：将头对象上的 {@link ContentRowHeight} 同步到工作表默认内容行高，保证空模板导出仍保留地址模块约定样式。
 * 入参/出参：输入 Excel 头类型与写表回调，输出写入后的工作表默认内容行高。
 * 关键约束：仅在头类型声明了 {@link ContentRowHeight} 时生效；不修改表头行高。
 * 异常与副作用：会修改当前导出工作表默认内容行高，不产生数据库或外部系统副作用。
 */
public class AddressDefaultRowHeightWriteHandler implements SheetWriteHandler {

    private final Short defaultRowHeight;

    public AddressDefaultRowHeightWriteHandler(Class<?> headType) {
        ContentRowHeight contentRowHeight = headType.getAnnotation(ContentRowHeight.class);
        this.defaultRowHeight = contentRowHeight == null ? null : contentRowHeight.value();
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        if (defaultRowHeight == null) {
            return;
        }
        writeSheetHolder.getSheet().setDefaultRowHeightInPoints(defaultRowHeight);
    }
}
```

- [ ] **Step 2: 新增地址模板导出器，只暴露模板场景真正需要的能力**

```java
package org.dromara.address.excel;

import cn.idev.excel.FastExcel;
import cn.idev.excel.write.builder.ExcelWriterSheetBuilder;
import cn.idev.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.excel.convert.ExcelBigNumberConvert;
import org.dromara.common.excel.core.DropDownOptions;
import org.dromara.common.excel.core.ExcelDownHandler;
import org.dromara.common.excel.handler.DataWriteHandler;

import java.io.OutputStream;
import java.util.List;

/**
 * 地址模板 Excel 导出器。
 * 目的：承接标准地址模板导出的地址专属样式能力，避免继续侵入公共 Excel 工具。
 * 入参/出参：输入模板行数据、sheet 名称、头类型、输出流与下拉配置，输出 Excel 二进制内容到目标流。
 * 关键约束：当前仅服务地址模板导出；是否启用自动列宽由调用方显式控制。
 * 异常与副作用：会向输出流写入 Excel 内容，不产生数据库写入副作用。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AddressTemplateExcelExporter {

    public static <T> void exportTemplate(List<T> rows, String sheetName, Class<T> headType, OutputStream outputStream,
                                          List<DropDownOptions> options, boolean autoWidth) {
        ExcelWriterSheetBuilder builder = FastExcel.write(outputStream, headType)
            .autoCloseStream(false)
            .registerConverter(new ExcelBigNumberConvert())
            .registerWriteHandler(new AddressDefaultRowHeightWriteHandler(headType))
            .registerWriteHandler(new DataWriteHandler(headType))
            .sheet(sheetName);
        if (autoWidth) {
            builder.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
        }
        builder.registerWriteHandler(new ExcelDownHandler(options));
        builder.doWrite(rows);
    }
}
```

- [ ] **Step 3: 切换标准地址导入模板下载接口到地址私有导出器**

```java
import org.dromara.address.excel.AddressTemplateExcelExporter;

@Override
@SaCheckPermission("address:standard:import")
@PostMapping("/import/template")
public void downloadStandardAddressImportTemplate(HttpServletResponse response) throws Exception {
    FileUtils.setAttachmentResponseHeader(response, ExcelUtil.encodingFilename("标准地址导入模板"));
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
    List<DropDownOptions> options = addressStandardService.listStandardAddressImportTemplateOptions();
    AddressTemplateExcelExporter.exportTemplate(
        List.<StandardAddressImportVo>of(),
        "标准地址导入模板",
        StandardAddressImportVo.class,
        response.getOutputStream(),
        options,
        false
    );
}
```

- [ ] **Step 4: 回退公共 `ExcelUtil` 的地址特化重载与处理器注册**

```java
// 恢复这些签名到公共实现，不再保留 autoWidth 重载
public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, HttpServletResponse response, List<DropDownOptions> options) {
    try {
        resetResponse(sheetName, response);
        ServletOutputStream os = response.getOutputStream();
        exportExcel(list, sheetName, clazz, false, os, options);
    } catch (IOException e) {
        throw new RuntimeException("导出Excel异常");
    }
}

public static <T> void exportExcel(List<T> list, String sheetName, Class<T> clazz, boolean merge,
                                   OutputStream os, List<DropDownOptions> options) {
    ExcelWriterSheetBuilder builder = FastExcel.write(os, clazz)
        .autoCloseStream(false)
        .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
        .registerConverter(new ExcelBigNumberConvert())
        .registerWriteHandler(new DataWriteHandler(clazz))
        .sheet(sheetName);
    if (merge) {
        builder.registerWriteHandler(new CellMergeStrategy(list, true));
    }
    builder.registerWriteHandler(new ExcelDownHandler(options));
    builder.doWrite(list);
}

public static <T> void exportExcel(Class<T> headType, OutputStream os, List<DropDownOptions> options, Consumer<ExcelWriterWrapper<T>> consumer) {
    try (ExcelWriter writer = FastExcel.write(os, headType)
        .autoCloseStream(false)
        .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
        .registerConverter(new ExcelBigNumberConvert())
        .registerWriteHandler(new DataWriteHandler(headType))
        .registerWriteHandler(new ExcelDownHandler(options))
        .build()) {
        consumer.accept(ExcelWriterWrapper.of(writer));
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

同时删除：

```text
ruoyi-common/ruoyi-common-excel/src/main/java/org/dromara/common/excel/handler/DefaultRowHeightWriteHandler.java
```

- [ ] **Step 5: 运行测试，确认地址模块私有导出层接管后行为不回归**

Run:

```bash
mvn -pl ruoyi-modules/ruoyi-address -Dtest=AddressTemplateExcelExporterTest,StandardAddressControllerExportTest test
```

Expected:

```text
BUILD SUCCESS
Tests run: ... Failures: 0, Errors: 0
```

- [ ] **Step 6: 提交 Excel 收口改动**

```bash
git add ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/excel \
        ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/controller/StandardAddressController.java \
        ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/excel/AddressTemplateExcelExporterTest.java \
        ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java \
        ruoyi-common/ruoyi-common-excel/src/main/java/org/dromara/common/excel/utils/ExcelUtil.java \
        ruoyi-common/ruoyi-common-excel/src/main/java/org/dromara/common/excel/handler/DefaultRowHeightWriteHandler.java
git commit -m "refactor ruoyi-address: consolidate template excel customization"
```

### Task 3: 先写失败测试锁定 Dubbo 默认值必须由地址模块配置承担

**Files:**
- Create: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/config/AddressDubboConfigFilesTest.java`

- [ ] **Step 1: 新增配置文件测试，要求 standalone runtime 与地址 Nacos 配置都显式声明 `dubbo.custom.*`**

```java
package org.dromara.address.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class AddressDubboConfigFilesTest {

    @Test
    void shouldDeclareDubboCustomDefaultsInStandaloneRuntimeYaml() throws Exception {
        assertEquals("false", readYamlProperty(new ClassPathResource("runtime/address-local-runtime-standalone.yml"), "dubbo.custom.request-log"));
        assertEquals("INFO", readYamlProperty(new ClassPathResource("runtime/address-local-runtime-standalone.yml"), "dubbo.custom.log-level"));
    }

    @Test
    void shouldDeclareDubboCustomDefaultsInAddressNacosYaml() throws Exception {
        Path nacosYaml = Path.of("../../script/config/nacos/ruoyi-address.yml").normalize();
        assertTrue(Files.exists(nacosYaml), () -> "缺少地址模块 Nacos 配置文件: " + nacosYaml);
        assertEquals("false", readYamlProperty(new FileSystemResource(nacosYaml), "dubbo.custom.request-log"));
        assertEquals("INFO", readYamlProperty(new FileSystemResource(nacosYaml), "dubbo.custom.log-level"));
    }

    private String readYamlProperty(Resource resource, String propertyName) throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        CompositePropertySource composite = new CompositePropertySource(resource.getFilename());
        for (var propertySource : loader.load(resource.getFilename(), resource)) {
            composite.addPropertySource(propertySource);
        }
        Object value = composite.getProperty(propertyName);
        return value == null ? null : value.toString();
    }
}
```

- [ ] **Step 2: 运行测试，确认它先失败**

Run:

```bash
mvn -pl ruoyi-modules/ruoyi-address -Dtest=AddressDubboConfigFilesTest test
```

Expected:

```text
FAIL
... expected: <false> but was: <null> ...
... expected: <INFO> but was: <null> ...
```

### Task 4: 把 Dubbo 默认值移入地址配置并回退公共属性类

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/resources/runtime/address-local-runtime-standalone.yml`
- Modify: `script/config/nacos/ruoyi-address.yml`
- Modify: `ruoyi-common/ruoyi-common-dubbo/src/main/java/org/dromara/common/dubbo/properties/DubboCustomProperties.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/StandaloneProfileSmokeTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/config/AddressDubboConfigFilesTest.java`（如需微调模块基准路径）

- [ ] **Step 1: 在地址 standalone runtime 与 Nacos 配置中声明 `dubbo.custom.*`**

```yaml
# ruoyi-modules/ruoyi-address/src/main/resources/runtime/address-local-runtime-standalone.yml
dubbo:
  enabled: false
  custom:
    request-log: false
    log-level: INFO
  application:
    qos-enable: false
    register-mode: instance
```

```yaml
# script/config/nacos/ruoyi-address.yml
dubbo:
  custom:
    request-log: false
    log-level: INFO

spring:
  datasource:
    dynamic:
      primary: address
```

- [ ] **Step 2: 回退公共 `DubboCustomProperties` 的地址私有默认值**

```java
@Data
@RefreshScope
@ConfigurationProperties(prefix = "dubbo.custom")
public class DubboCustomProperties {

    /**
     * 是否开启请求日志记录
     */
    private Boolean requestLog;

    /**
     * 日志级别
     */
    private RequestLogEnum logLevel;
}
```

- [ ] **Step 3: 扩展 standalone 冒烟测试，确认地址模块本地启动后能从自身配置绑定到 `DubboCustomProperties`**

```java
import org.dromara.common.dubbo.properties.DubboCustomProperties;

@Autowired
private DubboCustomProperties dubboCustomProperties;

@Test
void shouldExposeLocalRedisAndEsEndpointsUnderStandaloneProfile() {
    assertEquals("127.0.0.1", redisProperties.getHost());
    assertEquals(6379, redisProperties.getPort());
    assertEquals("ruoyi123", redisProperties.getPassword());
    assertEquals(Boolean.FALSE, dubboCustomProperties.getRequestLog());
    assertEquals("INFO", dubboCustomProperties.getLogLevel().name());
    assertEquals("127.0.0.1:9200", easyEsAddress);
}
```

- [ ] **Step 4: 运行配置测试与 standalone 冒烟测试**

Run:

```bash
mvn -pl ruoyi-modules/ruoyi-address -Dtest=AddressDubboConfigFilesTest,StandaloneProfileSmokeTest test
```

Expected:

```text
BUILD SUCCESS
Tests run: ... Failures: 0, Errors: 0
```

- [ ] **Step 5: 提交 Dubbo 收口改动**

```bash
git add ruoyi-modules/ruoyi-address/src/main/resources/runtime/address-local-runtime-standalone.yml \
        script/config/nacos/ruoyi-address.yml \
        ruoyi-common/ruoyi-common-dubbo/src/main/java/org/dromara/common/dubbo/properties/DubboCustomProperties.java \
        ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/config/AddressDubboConfigFilesTest.java \
        ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/StandaloneProfileSmokeTest.java
git commit -m "refactor ruoyi-address: move dubbo defaults into module config"
```

### Task 5: 做最终回归与打包验证

**Files:**
- Modify: none

- [ ] **Step 1: 运行本次收口涉及的全部重点测试**

Run:

```bash
mvn -pl ruoyi-modules/ruoyi-address -Dtest=AddressTemplateExcelExporterTest,StandardAddressControllerExportTest,AddressDubboConfigFilesTest,StandaloneProfileSmokeTest test
```

Expected:

```text
BUILD SUCCESS
Tests run: ... Failures: 0, Errors: 0
```

- [ ] **Step 2: 从地址模块发起带依赖构建，确认公共模块回退后仍可编译打包**

Run:

```bash
mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests package
```

Expected:

```text
BUILD SUCCESS
[INFO] Reactor Summary:
[INFO] ... ruoyi-common-excel ........ SUCCESS
[INFO] ... ruoyi-common-dubbo ........ SUCCESS
[INFO] ... ruoyi-address ............. SUCCESS
```

- [ ] **Step 3: 手工检查收口目标是否达成**

Run:

```bash
git diff --name-only -- ruoyi-common/ruoyi-common-excel ruoyi-common/ruoyi-common-dubbo ruoyi-modules/ruoyi-address script/config/nacos/ruoyi-address.yml
```

Expected:

```text
仅剩本次计划涉及的地址私有导出类、控制器/测试、地址配置文件，以及公共模块回退改动
```

- [ ] **Step 4: 提交最终回归结果**

```bash
git add ruoyi-modules/ruoyi-address ruoyi-common/ruoyi-common-excel ruoyi-common/ruoyi-common-dubbo script/config/nacos/ruoyi-address.yml
git commit -m "refactor ruoyi-address: consolidate external dependency customizations"
```
