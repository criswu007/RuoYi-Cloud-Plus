package org.dromara.address.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.PubRestriction;
import org.dromara.address.domain.SegmAddrType;
import org.dromara.address.domain.SpcStation;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.mapper.PubRestrictionMapper;
import org.dromara.address.mapper.SegmAddrTypeMapper;
import org.dromara.address.mapper.SpcStationMapper;
import org.dromara.address.support.AddressRegionContext;
import org.dromara.common.excel.core.DropDownOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 标准地址字典服务。
 * 目的：统一封装线上 `segm_addr_type` 的层级解释能力，避免控制器和业务服务直接拼接字典查询。
 * 入参/出参：输入真实 `segmType`、数据库 `levelId` 或业务 `addrLevel`，输出层级或类型集合。
 * 关键约束：数据库真实层级与前端业务级次必须统一由本服务做映射，不能再混用两个语义。
 * 异常与副作用：只读服务，无数据库写入副作用。
 */
@Service
@RequiredArgsConstructor
public class StandardAddressDictionaryService {

    private static final int DEFAULT_STATION_OPTION_LIMIT = 20;
    private static final int MAX_STATION_OPTION_LIMIT = 50;
    private static final String PROVINCE_ADDR_TYPE_ID = "180000";
    private static final String CITY_ADDR_TYPE_ID = "180001";
    private static final String KEYWORD_ADDR_IN_TYPE_FTTH = "ADDR_IN_TYPE_FTTH";
    private static final String KEYWORD_FTTH_PON_TYPE = "FTTH_PON_TYPE";
    private static final String KEYWORD_ADDR_IN_TYPE_LAN = "ADDR_IN_TYPE_LAN";
    private static final String KEYWORD_AREA_TYPE = "AREA_TYPE";
    private static final String KEYWORD_ADDR_PLACE_TYPE = "ADDR_PLACE_TYPE";
    private static final String KEYWORD_ADDR_UNIT_TYPE = "ADDR_UNIT_TYPE";

    private final SegmAddrTypeMapper segmAddrTypeMapper;
    private final PubRestrictionMapper pubRestrictionMapper;
    private final SpcStationMapper spcStationMapper;
    private final AddressRegionContext addressRegionContext;

    /**
     * 目的：根据地址类型编码解析数据库真实层级 ID。
     * 入参：线上 `ADDR_SEGM.segm_type` 对应的地址类型编码。
     * 出参：数据库真实层级 ID，不存在时返回 `null`。
     * 关键约束：只接受线上真实 `segmType`，不做旧层级常量兜底。
     * 异常与副作用：无写入副作用。
     */
    public Integer resolveLevelId(String segmType) {
        if (segmType == null || segmType.isBlank()) {
            return null;
        }
        return listSegmAddrTypes().stream()
            .filter(item -> segmType.equals(item.getAddrTypeId()))
            .map(SegmAddrType::getLevelId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * 目的：根据地址类型编码解析地址业务级别（1-19）。
     * 入参：线上 `ADDR_SEGM.segm_type` 对应的地址类型编码。
     * 出参：地址业务级别；不存在时返回 `null`。
     * 关键约束：业务级别按真实 `level_id` 升序做稠密排序得出，不能直接把 `level_id` 当 1-19 级使用。
     * 异常与副作用：无写入副作用。
     */
    public Integer resolveAddrLevel(String segmType) {
        Integer levelId = resolveLevelId(segmType);
        if (levelId == null) {
            return null;
        }
        return buildAddrLevelMap().get(levelId);
    }

    /**
     * 目的：根据地址类型编码识别一二级只读区域投影层级。
     * 入参：线上 `ADDR_SEGM.segm_type` 对应的地址类型编码。
     * 出参：若为省/市区域投影则返回对应业务级别；否则返回 `null`。
     * 关键约束：一二级投影识别必须基于明确的 `addr_type_id`，不能再依赖数据库 `level_id=1/2` 推断。
     * 异常与副作用：无写入副作用。
     */
    public Integer resolveReadonlyRegionAddrLevel(String segmType) {
        if (PROVINCE_ADDR_TYPE_ID.equals(segmType)) {
            Integer addrLevel = resolveAddrLevel(segmType);
            return addrLevel != null ? addrLevel : 1;
        }
        if (CITY_ADDR_TYPE_ID.equals(segmType)) {
            Integer addrLevel = resolveAddrLevel(segmType);
            return addrLevel != null ? addrLevel : 2;
        }
        return null;
    }

    /**
     * 目的：根据地址业务级别解析候选地址类型编码集合。
     * 入参：地址业务级别。
     * 出参：地址类型编码集合，未命中时返回空集合。
     * 关键约束：先将业务级别映射到真实 `level_id`，再解析对应地址类型。
     * 异常与副作用：无写入副作用。
     */
    public List<String> resolveSegmTypesByAddrLevel(Integer addrLevel) {
        Integer levelId = resolveLevelIdByAddrLevel(addrLevel);
        if (levelId == null) {
            return Collections.emptyList();
        }
        return listSegmAddrTypes().stream()
            .filter(item -> levelId.equals(item.getLevelId()))
            .map(SegmAddrType::getAddrTypeId)
            .filter(Objects::nonNull)
            .sorted()
            .toList();
    }

    /**
     * 目的：根据当前地址业务级别解析下一个业务级别。
     * 入参：当前地址业务级别。
     * 出参：下一个地址业务级别；不存在时返回 `null`。
     * 关键约束：业务级别顺序完全由真实 `level_id` 升序映射得出。
     * 异常与副作用：无写入副作用。
     */
    public Integer resolveNextAddrLevel(Integer currentAddrLevel) {
        if (currentAddrLevel == null) {
            return null;
        }
        return buildAddrLevelMap().values().stream()
            .filter(level -> level > currentAddrLevel)
            .sorted()
            .findFirst()
            .orElse(null);
    }

    /**
     * 目的：解析指定业务级别的默认地址类型编码。
     * 入参：地址业务级别。
     * 出参：默认地址类型编码；不存在时返回 `null`。
     * 关键约束：当前按 `addr_type_id` 升序取第一条，后续若联调库提供更明确规则可替换此策略。
     * 异常与副作用：无写入副作用。
     */
    public String resolveDefaultSegmTypeByAddrLevel(Integer addrLevel) {
        if (addrLevel == null) {
            return null;
        }
        return resolveSegmTypesByAddrLevel(addrLevel).stream().findFirst().orElse(null);
    }

    /**
     * 目的：批量解析地址类型到层级的映射关系。
     * 入参：地址类型编码集合。
     * 出参：`addrTypeId -> 数据库真实 levelId` 映射；未命中的类型不会出现在结果中。
     * 关键约束：必须一次性完成批量映射，避免列表结果逐条回查 `segm_addr_type`。
     * 异常与副作用：无写入副作用。
     */
    public Map<String, Integer> resolveLevelIdMap(Collection<String> segmTypes) {
        if (segmTypes == null || segmTypes.isEmpty()) {
            return Collections.emptyMap();
        }
        return listSegmAddrTypes().stream()
            .filter(item -> item.getAddrTypeId() != null && segmTypes.contains(item.getAddrTypeId()))
            .filter(item -> item.getLevelId() != null)
            .collect(LinkedHashMap::new,
                (result, item) -> result.put(item.getAddrTypeId(), item.getLevelId()),
                LinkedHashMap::putAll);
    }

    /**
     * 目的：批量解析地址类型到业务级别的映射关系。
     * 入参：地址类型编码集合。
     * 出参：`addrTypeId -> addrLevel` 映射；未命中的类型不会出现在结果中。
     * 关键约束：业务级别必须通过真实 `level_id` 稠密映射得到。
     * 异常与副作用：无写入副作用。
     */
    public Map<String, Integer> resolveAddrLevelMap(Collection<String> segmTypes) {
        if (segmTypes == null || segmTypes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Integer> addrLevelMap = buildAddrLevelMap();
        return listSegmAddrTypes().stream()
            .filter(item -> item.getAddrTypeId() != null && segmTypes.contains(item.getAddrTypeId()))
            .filter(item -> item.getLevelId() != null && addrLevelMap.containsKey(item.getLevelId()))
            .collect(LinkedHashMap::new,
                (result, item) -> result.put(item.getAddrTypeId(), addrLevelMap.get(item.getLevelId())),
                LinkedHashMap::putAll);
    }

    /**
     * 目的：解析不高于指定业务级别的全部地址类型编码。
     * 入参：最大业务级别。
     * 出参：满足 `addrLevel <= maxAddrLevel` 的地址类型编码集合；未传层级时返回全部类型。
     * 关键约束：候选搜索场景必须先在字典侧完成层级裁剪，再下推到大表主查询条件中，避免大表关联层级字典。
     * 异常与副作用：无写入副作用。
     */
    public List<String> resolveSegmTypesAtOrBelowAddrLevel(Integer maxAddrLevel) {
        Map<Integer, Integer> addrLevelMap = buildAddrLevelMap();
        return listSegmAddrTypes().stream()
            .filter(item -> item.getAddrTypeId() != null)
            .filter(item -> maxAddrLevel == null
                || (item.getLevelId() != null
                && addrLevelMap.containsKey(item.getLevelId())
                && addrLevelMap.get(item.getLevelId()) <= maxAddrLevel))
            .map(SegmAddrType::getAddrTypeId)
            .sorted()
            .toList();
    }

    /**
     * 目的：查询标准地址级别下拉选项。
     * 入参：无。
     * 出参：按层级与类型编码升序排序的级别选项集合。
     * 关键约束：返回结果必须完全来自线上 `segm_addr_type`，用于前端标准地址级别筛选和展示，不允许再落回硬编码常量。
     * 异常与副作用：无写入副作用。
     */
    public List<StandardAddressAdminVo.LevelOptionVo> listLevelOptions() {
        List<SegmAddrType> rows = listSegmAddrTypes();
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, Integer> addrLevelMap = buildAddrLevelMap(rows);
        return rows.stream()
            .sorted(Comparator.comparing(SegmAddrType::getLevelId, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SegmAddrType::getAddrTypeId, Comparator.nullsLast(String::compareTo)))
            .map(row -> toLevelOption(row, addrLevelMap))
            .toList();
    }

    /**
     * 目的：查询标准地址编辑页固定字典分组。
     * 入参：无。
     * 出参：标准地址编辑页字典集合。
     * 关键约束：返回 key 必须与前端表单字段一一对应，且字典值直接取 `pub_restriction.serial_no`。
     * 异常与副作用：无写入副作用。
     */
    public StandardAddressAdminVo.FormOptionsVo listFormOptions() {
        StandardAddressAdminVo.FormOptionsVo result = initFormOptions();
        List<PubRestriction> rows = pubRestrictionMapper.selectStandardAddressFormRestrictions();
        if (rows == null || rows.isEmpty()) {
            return result;
        }
        for (PubRestriction row : rows) {
            StandardAddressAdminVo.RestrictionOptionVo option = toRestrictionOption(row);
            switch (row.getKeyword()) {
                case "ADDR_SEGM_STATUS" -> result.getStatusOptions().add(option);
                case "ADDR_IN_TYPE_FTTH" -> result.getAddrInTypeFtthOptions().add(option);
                case "FTTH_PON_TYPE" -> result.getFtthPonTypeOptions().add(option);
                case "ADDR_IN_TYPE_LAN" -> result.getAddrInTypeLanOptions().add(option);
                case "AREA_TYPE" -> result.getAreaTypeOptions().add(option);
                case "ADDR_PLACE_TYPE" -> result.getPlaceTypeOptions().add(option);
                default -> {
                }
            }
        }
        return result;
    }

    /**
     * 目的：查询标准地址编辑页管理站候选。
     * 入参：管理站类型、区域、关键字和返回上限。
     * 出参：管理站候选列表。
     * 关键约束：`manageType` 不能为空；候选需直接来源于 `spc_station`。
     * 异常与副作用：无写入副作用。
     */
    public List<StandardAddressAdminVo.StationOptionVo> listStationOptions(StandardAddressAdminBo.StationOptionQueryBo bo) {
        if (bo == null || bo.getManageType() == null || bo.getManageType().isBlank()) {
            return Collections.emptyList();
        }
        String regionId = addressRegionContext.resolveRegionId(bo.getRegionId());
        List<SpcStation> rows = spcStationMapper.selectStationOptions(
            regionId,
            bo.getManageType(),
            bo.getKeyword()
        );
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        int limit = normalizeStationOptionLimit(bo.getLimit());
        return rows.stream()
            .limit(limit)
            .map(this::toStationOption)
            .toList();
    }

    /**
     * 目的：按地址类型中文名称解析真实地址类型编码。
     * 入参：模板或界面展示的地址类型中文名称。
     * 出参：命中的 `addr_type_id`；未命中返回 `null`。
     * 关键约束：必须直接基于线上 `segm_addr_type.name` 匹配，不允许落回硬编码级别映射。
     * 异常与副作用：无写入副作用。
     */
    public String resolveSegmTypeByName(String segmTypeName) {
        if (segmTypeName == null || segmTypeName.isBlank()) {
            return null;
        }
        String normalized = segmTypeName.trim();
        return listSegmAddrTypes().stream()
            .filter(item -> normalized.equals(item.getAddrTypeName()))
            .map(SegmAddrType::getAddrTypeId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * 目的：按字典关键字和中文标签解析真实字典值。
     * 入参：字典关键字与中文标签。
     * 出参：命中的 `serial_no`；未命中返回 `null`。
     * 关键约束：仅匹配标准地址表单相关字典，不对无关键字做兜底。
     * 异常与副作用：无写入副作用。
     */
    public String resolveRestrictionValue(String keyword, String label) {
        if (keyword == null || keyword.isBlank() || label == null || label.isBlank()) {
            return null;
        }
        String normalizedKeyword = keyword.trim();
        String normalizedLabel = label.trim();
        String value = listFormRestrictions().stream()
            .filter(item -> normalizedKeyword.equals(item.getKeyword()))
            .filter(item -> normalizedLabel.equals(item.getDescChina()))
            .map(PubRestriction::getSerialNo)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        if (value == null && KEYWORD_ADDR_PLACE_TYPE.equals(normalizedKeyword)) {
            return listFormRestrictions().stream()
                .filter(item -> KEYWORD_ADDR_UNIT_TYPE.equals(item.getKeyword()))
                .filter(item -> normalizedLabel.equals(item.getDescChina()))
                .map(PubRestriction::getSerialNo)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        }
        return value;
    }

    /**
     * 目的：按区域、管理站类型和名称关键字匹配单个管理站。
     * 入参：区域 ID、管理站类型和模板中的管理站文本。
     * 出参：命中的首条管理站 ID；未命中返回 `null`。
     * 关键约束：必须优先按 `region_id + manage_type` 收敛范围；命中多条时取排序后的第一条。
     * 异常与副作用：无写入副作用。
     */
    public String matchStationId(String regionId, String manageType, String keyword) {
        if (manageType == null || manageType.isBlank() || keyword == null || keyword.isBlank()) {
            return null;
        }
        List<SpcStation> rows = spcStationMapper.selectStationOptions(regionId, manageType, keyword.trim());
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return rows.get(0).getStationId();
    }

    /**
     * 目的：构建标准地址导入模板下拉选项。
     * 入参：无。
     * 出参：模板下拉选项集合。
     * 关键约束：下拉值需与当前导入解析规则完全一致，避免模板与导入口径漂移。
     * 异常与副作用：无写入副作用。
     */
    public List<DropDownOptions> listImportTemplateOptions() {
        List<String> segmTypeNames = listSegmAddrTypes().stream()
            .sorted(Comparator.comparing(SegmAddrType::getLevelId, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SegmAddrType::getAddrTypeId, Comparator.nullsLast(String::compareTo)))
            .map(SegmAddrType::getAddrTypeName)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<String, List<String>> restrictionMap = buildRestrictionLabelMap();
        List<DropDownOptions> result = new ArrayList<>();
        result.add(new DropDownOptions(0, segmTypeNames));
        result.add(new DropDownOptions(1, List.of("是", "否")));
        result.add(new DropDownOptions(7, restrictionMap.getOrDefault(KEYWORD_ADDR_IN_TYPE_FTTH, Collections.emptyList())));
        result.add(new DropDownOptions(8, restrictionMap.getOrDefault(KEYWORD_FTTH_PON_TYPE, Collections.emptyList())));
        result.add(new DropDownOptions(9, restrictionMap.getOrDefault(KEYWORD_AREA_TYPE, Collections.emptyList())));
        result.add(new DropDownOptions(10, mergeRestrictionLabels(restrictionMap, KEYWORD_ADDR_PLACE_TYPE, KEYWORD_ADDR_UNIT_TYPE)));
        result.add(new DropDownOptions(11, List.of("是", "否")));
        return result;
    }

    private StandardAddressAdminVo.LevelOptionVo toLevelOption(SegmAddrType row, Map<Integer, Integer> addrLevelMap) {
        StandardAddressAdminVo.LevelOptionVo option = new StandardAddressAdminVo.LevelOptionVo();
        option.setAddrTypeId(row.getAddrTypeId());
        option.setName(row.getAddrTypeName());
        option.setAddrLevel(row.getLevelId() == null ? null : addrLevelMap.get(row.getLevelId()));
        option.setLevelId(row.getLevelId());
        return option;
    }

    private StandardAddressAdminVo.FormOptionsVo initFormOptions() {
        StandardAddressAdminVo.FormOptionsVo result = new StandardAddressAdminVo.FormOptionsVo();
        result.setStatusOptions(new ArrayList<>());
        result.setAddrInTypeFtthOptions(new ArrayList<>());
        result.setFtthPonTypeOptions(new ArrayList<>());
        result.setAddrInTypeLanOptions(new ArrayList<>());
        result.setAreaTypeOptions(new ArrayList<>());
        result.setPlaceTypeOptions(new ArrayList<>());
        return result;
    }

    private StandardAddressAdminVo.RestrictionOptionVo toRestrictionOption(PubRestriction row) {
        StandardAddressAdminVo.RestrictionOptionVo option = new StandardAddressAdminVo.RestrictionOptionVo();
        option.setValue(row.getSerialNo());
        option.setLabel(row.getDescChina());
        return option;
    }

    private StandardAddressAdminVo.StationOptionVo toStationOption(SpcStation row) {
        StandardAddressAdminVo.StationOptionVo option = new StandardAddressAdminVo.StationOptionVo();
        option.setStationId(row.getStationId());
        option.setStationName(row.getStationName());
        option.setRegionId(row.getRegionId());
        option.setManageType(row.getManageType());
        return option;
    }

    private List<PubRestriction> listFormRestrictions() {
        List<PubRestriction> rows = pubRestrictionMapper.selectStandardAddressFormRestrictions();
        return rows == null ? Collections.emptyList() : rows;
    }

    private Map<String, List<String>> buildRestrictionLabelMap() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (PubRestriction row : listFormRestrictions()) {
            if (row.getKeyword() == null || row.getKeyword().isBlank() || row.getDescChina() == null || row.getDescChina().isBlank()) {
                continue;
            }
            result.computeIfAbsent(row.getKeyword(), key -> new ArrayList<>()).add(row.getDescChina());
        }
        result.replaceAll((key, value) -> value.stream().distinct().toList());
        return result;
    }

    private List<String> mergeRestrictionLabels(Map<String, List<String>> restrictionMap, String... keywords) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        for (String keyword : keywords) {
            for (String label : restrictionMap.getOrDefault(keyword, Collections.emptyList())) {
                ordered.putIfAbsent(label, label);
            }
        }
        return new ArrayList<>(ordered.values());
    }

    private int normalizeStationOptionLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_STATION_OPTION_LIMIT;
        }
        return Math.min(limit, MAX_STATION_OPTION_LIMIT);
    }

    private List<SegmAddrType> listSegmAddrTypes() {
        List<SegmAddrType> rows = segmAddrTypeMapper.selectList(null);
        return rows == null ? Collections.emptyList() : rows;
    }

    private Integer resolveLevelIdByAddrLevel(Integer addrLevel) {
        if (addrLevel == null) {
            return null;
        }
        return buildAddrLevelMap().entrySet().stream()
            .filter(entry -> addrLevel.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    private Map<Integer, Integer> buildAddrLevelMap() {
        return buildAddrLevelMap(listSegmAddrTypes());
    }

    private Map<Integer, Integer> buildAddrLevelMap(List<SegmAddrType> rows) {
        List<Integer> orderedLevelIds = rows.stream()
            .map(SegmAddrType::getLevelId)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
        if (orderedLevelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < orderedLevelIds.size(); index++) {
            result.put(orderedLevelIds.get(index), index + 1);
        }
        return result;
    }
}
