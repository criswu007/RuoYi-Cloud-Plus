package org.dromara.address.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.StandardAddressTagRel;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.address.mapper.SpcStationMapper;
import org.dromara.address.mapper.StandardAddressTagMapper;
import org.dromara.address.mapper.StandardAddressTagRelMapper;
import org.dromara.address.support.AddressRegionContext;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 标准地址查询服务。
 * 目的：按已确认的层级规则分流 `spc_region` 与 `ADDR_SEGM` 查询，并装配统一的标准地址视图对象。
 * 入参/出参：输入标准地址查询条件或 `segmId`，输出统一的 `StandardAddressVo` / `TableDataInfo`。
 * 关键约束：`1/2` 级业务地址只查询 `spc_region` 并标记为只读，其余层级查询 `ADDR_SEGM`；数据库真实 `levelId` 与业务 `addrLevel` 必须分离。
 * 异常与副作用：只读服务，无数据库写入副作用。
 */
@Service
@RequiredArgsConstructor
public class StandardAddressQueryService {

    private final AddrSegmMapper addrSegmMapper;
    private final SpcRegionMapper spcRegionMapper;
    private final StandardAddressDictionaryService dictionaryService;
    private final AddressRegionContext addressRegionContext;
    private final SpcStationMapper spcStationMapper;
    private final StandardAddressTagRelMapper standardAddressTagRelMapper;
    private final StandardAddressTagMapper standardAddressTagMapper;

    /**
     * 目的：分页查询标准地址列表。
     * 入参：标准地址查询条件与分页参数。
     * 出参：统一分页结果。
     * 关键约束：`1/2` 级查询命中 `spc_region`，其他层级查询命中 `ADDR_SEGM`；分页必须依赖数据库查询完成，严禁先查全集再代码切页。
     * 异常与副作用：无写入副作用。
     */
    public TableDataInfo<StandardAddressVo> queryPageList(StandardAddressBo bo, PageQuery pageQuery) {
        StandardAddressBo queryBo = normalizeQueryBo(bo);
        Integer addrLevel = resolveReadonlyRegionAddrLevel(queryBo);
        if (isRegionLevel(addrLevel)) {
            Page<StandardAddressVo> page = buildPage(pageQuery, false);
            Page<StandardAddressVo> result = spcRegionMapper.selectStandardAddressPage(page, queryBo);
            if (result == null) {
                result = page;
            }
            enrichRegionRows(result.getRecords());
            applyRegionFlags(result.getRecords(), addrLevel);
            return TableDataInfo.build(result);
        }
        List<String> segmTypes = resolveAddrSegmTypes(queryBo);
        Page<StandardAddressVo> page = buildPage(pageQuery, true);
        Page<StandardAddressVo> result = addrSegmMapper.selectStandardAddressPage(page, queryBo, CollUtil.isEmpty(segmTypes) ? null : segmTypes);
        if (result == null) {
            result = page;
        }
        enrichAddrSegmRows(result.getRecords());
        applyWritableFlags(result.getRecords());
        return TableDataInfo.build(result);
    }

    /**
     * 目的：查询标准地址列表。
     * 入参：标准地址查询条件。
     * 出参：统一标准地址视图列表。
     * 关键约束：结果中的只读标记必须与数据源规则保持一致。
     * 异常与副作用：无写入副作用。
     */
    public List<StandardAddressVo> queryList(StandardAddressBo bo) {
        StandardAddressBo queryBo = normalizeQueryBo(bo);
        Integer addrLevel = resolveReadonlyRegionAddrLevel(queryBo);
        if (isRegionLevel(addrLevel)) {
            List<StandardAddressVo> rows = spcRegionMapper.selectStandardAddressList(queryBo);
            enrichRegionRows(rows);
            return applyRegionFlags(rows, addrLevel);
        }
        List<String> segmTypes = resolveAddrSegmTypes(queryBo);
        List<StandardAddressVo> rows = addrSegmMapper.selectStandardAddressList(queryBo, CollUtil.isEmpty(segmTypes) ? null : segmTypes);
        enrichAddrSegmRows(rows);
        return applyWritableFlags(rows);
    }

    /**
     * 目的：根据 `segmId` 查询标准地址详情。
     * 入参：标准地址主键与可选层级。
     * 出参：统一标准地址视图；未命中时返回 `null`。
     * 关键约束：显式传入 `1/2` 级业务地址时只查询 `spc_region`；未传层级时优先查 `ADDR_SEGM`，再回退 `spc_region`。
     * 异常与副作用：无写入副作用。
     */
    public StandardAddressVo getBySegmId(String segmId, Integer addrLevel) {
        if (isRegionLevel(addrLevel)) {
            StandardAddressVo region = spcRegionMapper.selectStandardAddressByRegionId(segmId);
            enrichRegionRows(region == null ? Collections.emptyList() : Collections.singletonList(region));
            return toReadOnlyRegionVo(region, addrLevel);
        }
        StandardAddressVo vo = addrSegmMapper.selectStandardAddressBySegmId(segmId);
        if (vo != null) {
            enrichAddrSegmDetail(vo);
            return applyWritableFlags(Collections.singletonList(vo)).get(0);
        }
        StandardAddressVo region = spcRegionMapper.selectStandardAddressByRegionId(segmId);
        if (region == null) {
            return null;
        }
        enrichRegionRows(Collections.singletonList(region));
        return toReadOnlyRegionVo(region, region.getAddrLevel());
    }

    /**
     * 目的：查询区域级标准地址候选。
     * 入参：关键字、区域业务级别与候选条数上限。
     * 出参：只读区域级标准地址候选集合。
     * 关键约束：结果条数必须依赖 MyBatis-Plus 分页插件在数据库侧收敛，避免搜索接口全量扫描。
     * 异常与副作用：无写入副作用。
     */
    public List<StandardAddressVo> searchRegionCandidates(String keyword, Integer addrLevel, int limit) {
        if (limit <= 0 || !isRegionLevel(addrLevel)) {
            return Collections.emptyList();
        }
        Page<StandardAddressVo> page = buildLimitPage(limit);
        Page<StandardAddressVo> result = spcRegionMapper.selectSearchCandidatePage(page, keyword, addrLevel);
        List<StandardAddressVo> rows = result == null ? Collections.emptyList() : result.getRecords();
        enrichRegionRows(rows);
        return applyRegionFlags(rows, addrLevel);
    }

    /**
     * 目的：查询 `ADDR_SEGM` 标准地址候选。
     * 入参：关键字、最大业务级别、状态与候选条数上限。
     * 出参：可写标准地址候选集合。
     * 关键约束：结果条数必须依赖 MyBatis-Plus 分页插件在数据库侧收敛，层级过滤需先在字典服务中解析为 `segmType` 集合。
     * 异常与副作用：无写入副作用。
     */
    public List<StandardAddressVo> searchAddrSegmCandidates(String keyword, Integer addrLevelMax, String status, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        List<String> segmTypes = dictionaryService.resolveSegmTypesAtOrBelowAddrLevel(addrLevelMax);
        Page<StandardAddressVo> page = buildLimitPage(limit);
        Page<StandardAddressVo> result = addrSegmMapper.selectSearchCandidatePage(page, keyword, CollUtil.isEmpty(segmTypes) ? null : segmTypes, status);
        List<StandardAddressVo> rows = result == null ? Collections.emptyList() : result.getRecords();
        enrichAddrSegmRows(rows);
        sortAddrSegmCandidateRows(rows);
        return applyWritableFlags(rows);
    }

    /**
     * 目的：批量查询标准地址名称映射。
     * 入参：标准地址字符串主键集合。
     * 出参：`segmId -> standName` 映射。
     * 关键约束：必须优先命中 `ADDR_SEGM`，未命中的键再回退 `spc_region`，用于列表回填和跨实体批量补数。
     * 异常与副作用：无写入副作用。
     */
    public Map<String, String> listStandardAddressStandNameMapBySegmIds(Collection<String> segmIds) {
        if (segmIds == null || segmIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> normalizedSegmIds = segmIds.stream()
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (normalizedSegmIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        spcRegionMapper.selectByIds(normalizedSegmIds).forEach(region -> result.put(region.getRegionId(), region.getRegionName()));
        addrSegmMapper.selectByIds(normalizedSegmIds).forEach(addrSegm -> result.put(addrSegm.getSegmId(), addrSegm.getStandName()));
        return result;
    }

    private boolean isRegionLevel(Integer addrLevel) {
        return addrLevel != null && (addrLevel == 1 || addrLevel == 2);
    }

    private StandardAddressBo normalizeQueryBo(StandardAddressBo bo) {
        StandardAddressBo queryBo = bo == null ? new StandardAddressBo() : bo;
        queryBo.setRegionId(addressRegionContext.resolveRegionId(queryBo.getRegionId()));
        return queryBo;
    }

    private Integer resolveReadonlyRegionAddrLevel(StandardAddressBo bo) {
        if (bo == null) {
            return null;
        }
        Integer regionAddrLevel = dictionaryService.resolveReadonlyRegionAddrLevel(bo.getSegmType());
        if (regionAddrLevel != null) {
            return regionAddrLevel;
        }
        Integer addrLevel = bo.getAddrLevel();
        return isRegionLevel(addrLevel) ? addrLevel : null;
    }

    private List<String> resolveAddrSegmTypes(StandardAddressBo bo) {
        if (bo == null) {
            return Collections.emptyList();
        }
        if (StringUtils.isNotBlank(bo.getSegmType())) {
            return List.of(bo.getSegmType());
        }
        return dictionaryService.resolveSegmTypesByAddrLevel(bo.getAddrLevel());
    }

    private void enrichAddrSegmRows(List<StandardAddressVo> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        fillParentStandNames(rows);
        fillLevelFields(rows);
    }

    /**
     * 目的：补齐标准地址详情页与编辑弹窗依赖的扩展展示字段。
     * 入参：单条 `ADDR_SEGM` 标准地址详情。
     * 出参：无，直接在原对象上补齐父级名称、层级、管理站展示名称与标签展示名称。
     * 关键约束：`stationName/installStationName/busStationName/tagNames` 均为展示衍生字段，必须走批量/映射式查询补齐，不能误当物理列直接读取。
     * 异常与副作用：无写入副作用。
     */
    private void enrichAddrSegmDetail(StandardAddressVo vo) {
        if (vo == null) {
            return;
        }
        List<StandardAddressVo> rows = Collections.singletonList(vo);
        enrichAddrSegmRows(rows);
        fillStationNames(rows);
        fillTagNames(rows);
    }

    private void enrichRegionRows(List<StandardAddressVo> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        fillParentStandNames(rows);
        rows.forEach(row -> {
            if (StringUtils.isBlank(row.getSegmType())) {
                row.setSegmType(resolveRegionSegmType(row));
            }
            if (StringUtils.isBlank(row.getRegionId())) {
                row.setRegionId(row.getSegmId());
            }
        });
        fillLevelFields(rows);
    }

    private void fillParentStandNames(List<StandardAddressVo> rows) {
        List<String> parentSegmIds = rows.stream()
            .filter(Objects::nonNull)
            .filter(row -> StringUtils.isBlank(row.getParentStandName()))
            .map(StandardAddressVo::getParentSegmId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (parentSegmIds.isEmpty()) {
            return;
        }
        Map<String, String> parentNameMap = listStandardAddressStandNameMapBySegmIds(parentSegmIds);
        rows.forEach(row -> {
            if (row != null && StringUtils.isBlank(row.getParentStandName()) && StringUtils.isNotBlank(row.getParentSegmId())) {
                row.setParentStandName(parentNameMap.get(row.getParentSegmId()));
            }
        });
    }

    private void fillLevelFields(List<StandardAddressVo> rows) {
        List<String> segmTypes = rows.stream()
            .filter(Objects::nonNull)
            .filter(row -> row.getLevelId() == null || row.getAddrLevel() == null)
            .map(StandardAddressVo::getSegmType)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (segmTypes.isEmpty()) {
            return;
        }
        Map<String, Integer> levelIdMap = dictionaryService.resolveLevelIdMap(segmTypes);
        Map<String, Integer> addrLevelMap = dictionaryService.resolveAddrLevelMap(segmTypes);
        rows.forEach(row -> {
            if (row == null || StringUtils.isBlank(row.getSegmType())) {
                return;
            }
            if (row.getLevelId() == null) {
                row.setLevelId(levelIdMap.get(row.getSegmType()));
            }
            if (row.getAddrLevel() == null) {
                row.setAddrLevel(addrLevelMap.get(row.getSegmType()));
            }
        });
    }

    private void fillStationNames(List<StandardAddressVo> rows) {
        List<String> stationIds = rows.stream()
            .filter(Objects::nonNull)
            .flatMap(row -> java.util.stream.Stream.of(row.getStationId(), row.getInstallStationId(), row.getBusStationId()))
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (stationIds.isEmpty()) {
            return;
        }
        Map<String, String> stationNameMap = spcStationMapper.selectByIds(stationIds).stream()
            .filter(Objects::nonNull)
            .filter(item -> StringUtils.isNotBlank(item.getStationId()))
            .collect(LinkedHashMap::new,
                (result, item) -> result.put(item.getStationId(), item.getStationName()),
                LinkedHashMap::putAll);
        rows.forEach(row -> {
            if (row == null) {
                return;
            }
            if (StringUtils.isBlank(row.getStationName()) && StringUtils.isNotBlank(row.getStationId())) {
                row.setStationName(stationNameMap.get(row.getStationId()));
            }
            if (StringUtils.isBlank(row.getInstallStationName()) && StringUtils.isNotBlank(row.getInstallStationId())) {
                row.setInstallStationName(stationNameMap.get(row.getInstallStationId()));
            }
            if (StringUtils.isBlank(row.getBusStationName()) && StringUtils.isNotBlank(row.getBusStationId())) {
                row.setBusStationName(stationNameMap.get(row.getBusStationId()));
            }
        });
    }

    private void fillTagNames(List<StandardAddressVo> rows) {
        List<String> segmIds = rows.stream()
            .filter(Objects::nonNull)
            .map(StandardAddressVo::getSegmId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (segmIds.isEmpty()) {
            return;
        }
        List<StandardAddressTagRel> relList = standardAddressTagRelMapper.selectList(
            Wrappers.<StandardAddressTagRel>lambdaQuery().in(StandardAddressTagRel::getStandardAddressId, segmIds)
        );
        if (CollUtil.isEmpty(relList)) {
            return;
        }
        List<Long> tagIds = relList.stream()
            .map(StandardAddressTagRel::getTagId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (tagIds.isEmpty()) {
            return;
        }
        Map<Long, String> tagNameMap = standardAddressTagMapper.selectBatchIds(tagIds).stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getId() != null && StringUtils.isNotBlank(item.getName()))
            .collect(LinkedHashMap::new,
                (result, item) -> result.put(item.getId(), item.getName()),
                LinkedHashMap::putAll);
        Map<String, List<String>> standardAddressTagNameMap = new LinkedHashMap<>();
        relList.forEach(rel -> {
            if (rel == null || StringUtils.isBlank(rel.getStandardAddressId()) || rel.getTagId() == null) {
                return;
            }
            String tagName = tagNameMap.get(rel.getTagId());
            if (StringUtils.isBlank(tagName)) {
                return;
            }
            standardAddressTagNameMap.computeIfAbsent(rel.getStandardAddressId(), key -> new java.util.ArrayList<>()).add(tagName);
        });
        rows.forEach(row -> {
            if (row == null || StringUtils.isBlank(row.getSegmId())) {
                return;
            }
            List<String> tagNames = standardAddressTagNameMap.get(row.getSegmId());
            if (CollUtil.isEmpty(tagNames)) {
                return;
            }
            row.setTagNames(new java.util.ArrayList<>(new LinkedHashSet<>(tagNames)));
        });
    }

    private String resolveRegionSegmType(StandardAddressVo row) {
        if (row == null || row.getGradeId() == null) {
            return null;
        }
        if (row.getGradeId() == 2000002) {
            return PROVINCE_ADDR_TYPE;
        }
        if (row.getGradeId() == 2000004) {
            return CITY_ADDR_TYPE;
        }
        return null;
    }

    private void sortAddrSegmCandidateRows(List<StandardAddressVo> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        rows.sort(Comparator.comparing(StandardAddressVo::getAddrLevel, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(StandardAddressVo::getSegmId, Comparator.nullsLast(String::compareTo)));
    }

    private List<StandardAddressVo> applyRegionFlags(List<StandardAddressVo> rows, Integer addrLevel) {
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }
        rows.forEach(row -> {
            row.setAddrLevel(addrLevel != null ? addrLevel : row.getAddrLevel());
            row.setReadOnlyFlag(Boolean.TRUE);
            row.setCanEdit(Boolean.FALSE);
            row.setCanDelete(Boolean.FALSE);
            row.setCanMerge(Boolean.FALSE);
            row.setCanSplit(Boolean.FALSE);
        });
        return rows;
    }

    private List<StandardAddressVo> applyWritableFlags(List<StandardAddressVo> rows) {
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }
        rows.forEach(row -> {
            row.setReadOnlyFlag(Boolean.FALSE);
            row.setCanEdit(Boolean.TRUE);
            row.setCanDelete(Boolean.TRUE);
            row.setCanMerge(Boolean.TRUE);
            row.setCanSplit(Boolean.TRUE);
        });
        return rows;
    }

    private StandardAddressVo toReadOnlyRegionVo(StandardAddressVo region, Integer addrLevel) {
        if (region == null) {
            return null;
        }
        region.setAddrLevel(addrLevel != null ? addrLevel : region.getAddrLevel());
        region.setReadOnlyFlag(Boolean.TRUE);
        region.setCanEdit(Boolean.FALSE);
        region.setCanDelete(Boolean.FALSE);
        region.setCanMerge(Boolean.FALSE);
        region.setCanSplit(Boolean.FALSE);
        return region;
    }

    /**
     * 目的：构建标准地址分页对象，并按查询场景决定是否保留前端传入的排序条件。
     * 入参：分页请求对象、是否保留排序参数。
     * 出参：可直接传给 MyBatis-Plus 的分页对象。
     * 关键约束：一二级 `spc_region` 查询不允许继续附带统一排序参数，其余 `ADDR_SEGM` 查询保持现有排序能力。
     * 异常与副作用：无写入副作用；仅做内存态分页参数收口。
     */
    private Page<StandardAddressVo> buildPage(PageQuery pageQuery, boolean keepOrderBy) {
        if (pageQuery == null) {
            return new PageQuery().build();
        }
        if (keepOrderBy) {
            return pageQuery.build();
        }
        PageQuery sanitizedPageQuery = new PageQuery();
        sanitizedPageQuery.setPageNum(pageQuery.getPageNum());
        sanitizedPageQuery.setPageSize(pageQuery.getPageSize());
        return sanitizedPageQuery.build();
    }

    private Page<StandardAddressVo> buildLimitPage(int limit) {
        return new Page<>(1, limit, false);
    }

    private static final String PROVINCE_ADDR_TYPE = "180000";
    private static final String CITY_ADDR_TYPE = "180001";
}
