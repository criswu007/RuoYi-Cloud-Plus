# Standard Address Import FTTH Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让标准地址导入模板中的“接入方式”与详情页“光纤接入方式”保持一致，只允许导入 `addrInTypeFtth` 口径。

**Architecture:** 保持 Excel 模板列结构不变，仅调整第 8 列下拉来源与导入解析规则。模板字典改为直接复用编辑页 `ADDR_IN_TYPE_FTTH`，导入解析仅写入 `addrInTypeFtth`，不再接受 `ADDR_IN_TYPE_LAN` 值。

**Tech Stack:** Java 17, Spring Boot, JUnit 5, Mockito, EasyExcel, Vue 3 已运行联调环境

---

### Task 1: 锁定模板下拉目标行为

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressDictionaryServiceTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/controller/StandardAddressControllerExportTest.java`

- [ ] **Step 1: 写失败测试，要求模板“接入方式”只包含光纤接入方式**

```java
assertEquals(List.of("FTTH_双纤"), result.get(2).getOptions());
assertFalse(result.get(2).getOptions().contains("LAN"));
```

- [ ] **Step 2: 运行定向测试确认当前实现失败**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandardAddressDictionaryServiceTest,StandardAddressControllerExportTest test`
Expected: FAIL，原因是当前模板仍包含 `LAN`

- [ ] **Step 3: 最小实现模板字典调整**

```java
result.add(new DropDownOptions(7, restrictionMap.getOrDefault(KEYWORD_ADDR_IN_TYPE_FTTH, Collections.emptyList())));
```

- [ ] **Step 4: 重新运行定向测试确认转绿**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandardAddressDictionaryServiceTest,StandardAddressControllerExportTest test`
Expected: PASS

### Task 2: 锁定导入解析只接受 FTTH 口径

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/test/java/org/dromara/address/service/StandardAddressImportServiceTest.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressImportService.java`

- [ ] **Step 1: 写失败测试，要求电缆接入方式值在导入时被拒绝**

```java
row.setAccessModeName("LAN");
assertEquals(1, result.getFailCount());
verify(commandService, never()).addStandardAddressForImport(any());
```

- [ ] **Step 2: 运行导入服务定向测试确认当前实现失败**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandardAddressImportServiceTest test`
Expected: FAIL，原因是当前实现仍会把 `LAN` 识别为 `addrInTypeLan`

- [ ] **Step 3: 最小实现导入解析收敛**

```java
String ftthValue = dictionaryService.resolveRestrictionValue(KEYWORD_ADDR_IN_TYPE_FTTH, normalized);
if (StringUtils.isNotBlank(ftthValue)) {
    bo.setAddrInTypeFtth(Integer.valueOf(ftthValue));
    bo.setAddrInTypeLan(null);
    return;
}
throw new ServiceException("导入失败：接入方式不存在");
```

- [ ] **Step 4: 重新运行导入服务定向测试确认转绿**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandardAddressImportServiceTest test`
Expected: PASS

### Task 3: 联调回归

**Files:**
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/domain/vo/StandardAddressImportVo.java`
- Modify: `ruoyi-modules/ruoyi-address/src/main/java/org/dromara/address/service/impl/StandardAddressImportService.java`

- [ ] **Step 1: 补齐必要注释，明确“接入方式”导入口径为光纤接入方式**

```java
/**
 * 接入方式文本值，导入口径与详情页“光纤接入方式”一致。
 */
```

- [ ] **Step 2: 运行组合回归测试**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=StandardAddressControllerExportTest,StandardAddressDictionaryServiceTest,StandardAddressImportServiceTest test`
Expected: PASS

- [ ] **Step 3: 打包并重启本地服务**

Run: `mvn -pl ruoyi-modules/ruoyi-address -am -DskipTests package`
Expected: BUILD SUCCESS

- [ ] **Step 4: 验证真实模板下拉与页面效果**

Run: `curl -s -o /tmp/address-template.xlsx -w 'HTTP %{http_code}\n' -X POST http://127.0.0.1:5173/address/standard/import/template`
Expected: `HTTP 200`，模板“接入方式”仅包含 FTTH 选项
