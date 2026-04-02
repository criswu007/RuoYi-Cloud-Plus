package org.dromara.address.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.address.support.AddressRegionContext;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
            Page<StandardAddressVo> page = buildPage(pageQuery);
            Page<StandardAddressVo> result = spcRegionMapper.selectStandardAddressPage(page, queryBo);
            if (result == null) {
                result = page;
            }
            enrichRegionRows(result.getRecords());
            applyRegionFlags(result.getRecords(), addrLevel);
            return TableDataInfo.build(result);
        }
        List<String> segmTypes = resolveAddrSegmTypes(queryBo);
        Page<StandardAddressVo> page = buildPage(pageQuery);
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
            enrichAddrSegmRows(Collections.singletonList(vo));
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

    private String resolveRegionSegmType(StandardAddressVo row) {
        return StringUtils.isBlank(row.getParentSegmId()) ? PROVINCE_ADDR_TYPE : CITY_ADDR_TYPE;
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

    private Page<StandardAddressVo> buildPage(PageQuery pageQuery) {
        if (pageQuery == null) {
            return new PageQuery().build();
        }
        return pageQuery.build();
    }

    private Page<StandardAddressVo> buildLimitPage(int limit) {
        return new Page<>(1, limit, false);
    }

    private static final String PROVINCE_ADDR_TYPE = "180000";
    private static final String CITY_ADDR_TYPE = "180001";
}
