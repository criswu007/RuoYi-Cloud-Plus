package org.dromara.address.service.impl;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 标准地址查询服务。
 * 目的：按已确认的层级规则分流 `spc_region` 与 `ADDR_SEGM` 查询，并装配统一的标准地址视图对象。
 * 入参/出参：输入标准地址查询条件或 `segmId`，输出统一的 `StandardAddressVo` / `TableDataInfo`。
 * 关键约束：`1/2` 级只查询 `spc_region` 并标记为只读，其余层级查询 `ADDR_SEGM`；未传层级时默认查询 `ADDR_SEGM`。
 * 异常与副作用：只读服务，无数据库写入副作用。
 */
@Service
@RequiredArgsConstructor
public class StandardAddressQueryService {

    private final AddrSegmMapper addrSegmMapper;
    private final SpcRegionMapper spcRegionMapper;
    private final StandardAddressDictionaryService dictionaryService;

    /**
     * 目的：分页查询标准地址列表。
     * 入参：标准地址查询条件与分页参数。
     * 出参：统一分页结果。
     * 关键约束：`1/2` 级查询命中 `spc_region`，其他层级查询命中 `ADDR_SEGM`。
     * 异常与副作用：无写入副作用。
     */
    public TableDataInfo<StandardAddressVo> queryPageList(StandardAddressBo bo, PageQuery pageQuery) {
        List<StandardAddressVo> rows = queryList(bo);
        return TableDataInfo.build(rows, pageQuery.build());
    }

    /**
     * 目的：查询标准地址列表。
     * 入参：标准地址查询条件。
     * 出参：统一标准地址视图列表。
     * 关键约束：结果中的只读标记必须与数据源规则保持一致。
     * 异常与副作用：无写入副作用。
     */
    public List<StandardAddressVo> queryList(StandardAddressBo bo) {
        Integer levelId = bo == null ? null : bo.getLevelId();
        if (isRegionLevel(levelId)) {
            List<StandardAddressVo> rows = spcRegionMapper.selectStandardAddressList(bo);
            return applyRegionFlags(rows, levelId);
        }
        List<String> segmTypes = dictionaryService.resolveSegmTypesByLevelId(levelId);
        List<StandardAddressVo> rows = addrSegmMapper.selectStandardAddressList(bo, CollUtil.isEmpty(segmTypes) ? null : segmTypes);
        return applyWritableFlags(rows);
    }

    /**
     * 目的：根据 `segmId` 查询标准地址详情。
     * 入参：标准地址主键与可选层级。
     * 出参：统一标准地址视图；未命中时返回 `null`。
     * 关键约束：显式传入 `1/2` 级时只查询 `spc_region`；未传层级时优先查 `ADDR_SEGM`，再回退 `spc_region`。
     * 异常与副作用：无写入副作用。
     */
    public StandardAddressVo getBySegmId(String segmId, Integer levelId) {
        if (isRegionLevel(levelId)) {
            return toReadOnlyRegionVo(spcRegionMapper.selectById(segmId), levelId);
        }
        StandardAddressVo vo = addrSegmMapper.selectStandardAddressBySegmId(segmId);
        if (vo != null) {
            return applyWritableFlags(Collections.singletonList(vo)).get(0);
        }
        SpcRegion region = spcRegionMapper.selectById(segmId);
        if (region == null) {
            return null;
        }
        return toReadOnlyRegionVo(region, inferRegionLevel(region));
    }

    private boolean isRegionLevel(Integer levelId) {
        return levelId != null && (levelId == 1 || levelId == 2);
    }

    private List<StandardAddressVo> applyRegionFlags(List<StandardAddressVo> rows, Integer levelId) {
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }
        rows.forEach(row -> {
            row.setLevelId(levelId);
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
            if (row.getLevelId() == null && row.getSegmType() != null) {
                row.setLevelId(dictionaryService.resolveLevelId(row.getSegmType()));
            }
            row.setReadOnlyFlag(Boolean.FALSE);
            row.setCanEdit(Boolean.TRUE);
            row.setCanDelete(Boolean.TRUE);
            row.setCanMerge(Boolean.TRUE);
            row.setCanSplit(Boolean.TRUE);
        });
        return rows;
    }

    private StandardAddressVo toReadOnlyRegionVo(SpcRegion region, Integer levelId) {
        if (region == null) {
            return null;
        }
        StandardAddressVo vo = new StandardAddressVo();
        vo.setSegmId(region.getRegionId());
        vo.setParentSegmId(region.getSuperRegionId());
        vo.setSegmName(region.getRegionName());
        vo.setStandName(region.getRegionName());
        vo.setSegmNo(region.getRegionNo());
        vo.setNotes(region.getNotes());
        vo.setLevelId(levelId);
        vo.setReadOnlyFlag(Boolean.TRUE);
        vo.setCanEdit(Boolean.FALSE);
        vo.setCanDelete(Boolean.FALSE);
        vo.setCanMerge(Boolean.FALSE);
        vo.setCanSplit(Boolean.FALSE);
        return vo;
    }

    private Integer inferRegionLevel(SpcRegion region) {
        return region.getSuperRegionId() == null || region.getSuperRegionId().isBlank() ? 1 : 2;
    }
}
