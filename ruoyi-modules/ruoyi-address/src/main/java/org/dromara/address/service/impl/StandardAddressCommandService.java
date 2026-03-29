package org.dromara.address.service.impl;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.AddrSetSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 标准地址命令服务。
 * 目的：负责标准地址新增、修改、删除、批量下级预览与批量新增等写链路能力，统一落到线上标准地址主表模型。
 * 入参/出参：输入 canonical `segmId/segmType/standName` 语义的 BO，输出写操作结果或批量预览集合。
 * 关键约束：写操作只作用于 `ADDR_SEGM`；一二级地址本身不可写；删除优先校验子节点，再校验安装地址关联确认。
 * 异常与副作用：会写入 `ADDR_SEGM.delete_state`、新增或更新 `ADDR_SEGM` 数据，并在批量新增时批量生成新 `segmId`。
 */
@Service
@RequiredArgsConstructor
public class StandardAddressCommandService {

    private static final String ACTIVE_STATUS = "2140900";
    private static final String NOT_DELETED = "0";

    private final AddrSegmMapper addrSegmMapper;
    private final AddrSetSegmMapper addrSetSegmMapper;
    private final SpcRegionMapper spcRegionMapper;
    private final StandardAddressDictionaryService dictionaryService;
    private final StandardAddressNameService nameService;
    private final StandardAddressIdGenerator idGenerator;

    /**
     * 目的：新增标准地址。
     * 入参：标准地址业务对象。
     * 出参：新增是否成功。
     * 关键约束：新增对象层级不得为一二级，且必须落到 `ADDR_SEGM`。
     * 异常与副作用：会写入 `ADDR_SEGM` 并回填 `segmId/standName/standNo`。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addStandardAddress(StandardAddressBo bo) {
        Integer requestedLevelId = bo.getLevelId();
        if (requestedLevelId == null) {
            requestedLevelId = dictionaryService.resolveLevelId(bo.getSegmType());
        }
        if (requestedLevelId != null) {
            validateWritableLevel(requestedLevelId, "新增");
        }
        AddrSegm parent = requireParent(bo.getParentSegmId(), "新增");
        Integer levelId = resolveWritableLevelId(bo, null, parent, "新增");
        AddrSegm entity = buildEntity(bo, null, parent, levelId);
        boolean success = addrSegmMapper.insert(entity) > 0;
        if (success) {
            bo.setSegmId(entity.getSegmId());
            bo.setStandName(entity.getStandName());
            bo.setStandNo(entity.getStandNo());
            bo.setSegmNo(entity.getSegmNo());
        }
        return success;
    }

    /**
     * 目的：修改标准地址。
     * 入参：标准地址业务对象。
     * 出参：修改是否成功。
     * 关键约束：修改对象层级不得为一二级；名称或父级变更后需级联刷新子节点全称与简拼。
     * 异常与副作用：会更新 `ADDR_SEGM`，并递归刷新所有直接/间接子节点的 `stand_name/stand_no`。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStandardAddress(StandardAddressBo bo) {
        if (StringUtils.isBlank(bo.getSegmId())) {
            throw new ServiceException("标准地址ID不能为空");
        }
        AddrSegm existing = addrSegmMapper.selectById(bo.getSegmId());
        if (existing == null || !"0".equals(StringUtils.blankToDefault(existing.getDeleteState(), NOT_DELETED))) {
            throw new ServiceException("标准地址不存在");
        }
        String parentSegmId = StringUtils.defaultIfBlank(bo.getParentSegmId(), existing.getParentSegmId());
        AddrSegm parent = requireParent(parentSegmId, "修改");
        Integer levelId = resolveWritableLevelId(bo, existing, parent, "修改");
        AddrSegm update = buildEntity(bo, existing, parent, levelId);
        boolean success = addrSegmMapper.updateById(update) > 0;
        if (success) {
            refreshChildrenStandInfo(update);
        }
        return success;
    }

    /**
     * 目的：删除标准地址。
     * 入参：标准地址主键集合与是否确认删除安装地址关联。
     * 出参：删除是否成功。
     * 关键约束：删除优先校验下级地址，再校验安装地址关联；仅做逻辑删除。
     * 异常与副作用：会把 `ADDR_SEGM.delete_state` 更新为已删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteStandardAddresses(Collection<String> segmIds, boolean confirm) {
        if (CollUtil.isEmpty(segmIds)) {
            throw new ServiceException("标准地址ID不能为空");
        }
        List<AddrSegm> current = addrSegmMapper.selectBatchIds(segmIds);
        if (CollUtil.isEmpty(current)) {
            throw new ServiceException("标准地址不存在");
        }
        current.forEach(item -> validateWritableLevel(dictionaryService.resolveLevelId(item.getSegmType()), "删除"));
        Long childCount = addrSegmMapper.countChildren(segmIds);
        if (childCount != null && childCount > 0) {
            throw new ServiceException("删除失败：存在下级标准地址");
        }
        Long installCount = addrSetSegmMapper.countBySegmIds(segmIds);
        if (installCount != null && installCount > 0 && !confirm) {
            throw new ServiceException("删除失败：存在关联安装地址，请确认后重试");
        }
        return addrSegmMapper.logicalDeleteBySegmIds(segmIds) > 0;
    }

    /**
     * 目的：预览批量新增下级标准地址。
     * 入参：批量新增参数。
     * 出参：预览结果列表。
     * 关键约束：只做预演不落库，父级必须存在。
     * 异常与副作用：无写入副作用。
     */
    public List<StandardAddressAdminVo.BatchPreviewVo> previewChildren(StandardAddressBatchAddBo bo) {
        validateBatchRange(bo);
        AddrSegm parent = requireParent(bo.getParentSegmId(), "批量预览");
        Integer childLevelId = resolveChildLevelId(parent, false);
        String childSegmType = childLevelId == null ? null : dictionaryService.resolveDefaultSegmTypeByLevelId(childLevelId);
        List<StandardAddressAdminVo.BatchPreviewVo> result = new ArrayList<>();
        for (int current = bo.getStartNum(); current <= bo.getEndNum(); current++) {
            String segmName = buildChildSegmName(bo, current);
            String standName = buildStandName(parent, segmName);
            StandardAddressAdminVo.BatchPreviewVo item = new StandardAddressAdminVo.BatchPreviewVo();
            item.setSegmName(segmName);
            item.setStandName(standName);
            item.setSegmNo(nameService.buildSegmNo(segmName));
            item.setStandNo(nameService.buildStandNo(standName));
            item.setLevelId(childLevelId);
            item.setSegmType(childSegmType);
            result.add(item);
        }
        return result;
    }

    /**
     * 目的：批量新增下级标准地址。
     * 入参：批量新增参数。
     * 出参：新增是否成功。
     * 关键约束：父级必须存在，子节点层级由字典顺序推导，新增对象本身不能落到一二级。
     * 异常与副作用：会批量写入 `ADDR_SEGM`。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchAddChildren(StandardAddressBatchAddBo bo) {
        List<StandardAddressAdminVo.BatchPreviewVo> preview = previewChildren(bo);
        AddrSegm parent = requireParent(bo.getParentSegmId(), "批量新增");
        Integer childLevelId = resolveChildLevelId(parent, true);
        validateWritableLevel(childLevelId, "批量新增");
        String childSegmType = dictionaryService.resolveDefaultSegmTypeByLevelId(childLevelId);
        if (StringUtils.isBlank(childSegmType)) {
            throw new ServiceException("批量新增失败：无法解析下级地址类型");
        }
        int affected = 0;
        for (StandardAddressAdminVo.BatchPreviewVo item : preview) {
            AddrSegm entity = new AddrSegm();
            entity.setSegmId(idGenerator.nextSegmId());
            entity.setParentSegmId(parent.getSegmId());
            entity.setSegmName(item.getSegmName());
            entity.setStandName(item.getStandName());
            entity.setSegmNo(item.getSegmNo());
            entity.setStandNo(item.getStandNo());
            entity.setSegmType(childSegmType);
            entity.setRegionId(parent.getRegionId());
            entity.setDistrictId(parent.getDistrictId());
            entity.setServiceRegionId(parent.getServiceRegionId());
            entity.setStatus(ACTIVE_STATUS);
            entity.setDeleteState(NOT_DELETED);
            affected += addrSegmMapper.insert(entity);
        }
        return affected == preview.size();
    }

    private AddrSegm buildEntity(StandardAddressBo bo, AddrSegm existing, AddrSegm parent, Integer levelId) {
        String segmName = StringUtils.defaultIfBlank(bo.getSegmName(), existing == null ? null : existing.getSegmName());
        if (StringUtils.isBlank(segmName)) {
            throw new ServiceException("当级名称不能为空");
        }
        String segmId = existing == null ? StringUtils.defaultIfBlank(bo.getSegmId(), idGenerator.nextSegmId()) : existing.getSegmId();
        String segmType = StringUtils.defaultIfBlank(bo.getSegmType(), existing == null ? dictionaryService.resolveDefaultSegmTypeByLevelId(levelId) : existing.getSegmType());
        if (StringUtils.isBlank(segmType)) {
            throw new ServiceException("地址类型不能为空");
        }
        String standName = buildStandName(parent, segmName);

        AddrSegm entity = new AddrSegm();
        entity.setSegmId(segmId);
        entity.setParentSegmId(parent.getSegmId());
        entity.setSegmName(segmName);
        entity.setStandName(standName);
        entity.setSegmNo(nameService.buildSegmNo(segmName));
        entity.setStandNo(nameService.buildStandNo(standName));
        entity.setSegmType(segmType);
        entity.setRegionId(StringUtils.defaultIfBlank(bo.getRegionId(), parent.getRegionId()));
        entity.setDistrictId(StringUtils.defaultIfBlank(bo.getDistrictId(), parent.getDistrictId()));
        entity.setServiceRegionId(StringUtils.defaultIfBlank(bo.getServiceRegionId(), parent.getServiceRegionId()));
        entity.setStatus(StringUtils.defaultIfBlank(bo.getStatus(), existing == null ? ACTIVE_STATUS : existing.getStatus()));
        entity.setNotes(StringUtils.defaultIfBlank(bo.getNotes(), existing == null ? null : existing.getNotes()));
        entity.setStationId(StringUtils.defaultIfBlank(bo.getStationId(), existing == null ? null : existing.getStationId()));
        entity.setInstallstationId(StringUtils.defaultIfBlank(bo.getInstallStationId(), existing == null ? null : existing.getInstallstationId()));
        entity.setBusstationId(StringUtils.defaultIfBlank(bo.getBusStationId(), existing == null ? null : existing.getBusstationId()));
        entity.setDeleteState(existing == null ? NOT_DELETED : StringUtils.blankToDefault(existing.getDeleteState(), NOT_DELETED));
        return entity;
    }

    private Integer resolveWritableLevelId(StandardAddressBo bo, AddrSegm existing, AddrSegm parent, String action) {
        Integer levelId = bo.getLevelId();
        if (levelId == null) {
            String segmType = StringUtils.defaultIfBlank(bo.getSegmType(), existing == null ? null : existing.getSegmType());
            levelId = dictionaryService.resolveLevelId(segmType);
        }
        if (levelId == null && existing == null && parent != null) {
            levelId = resolveChildLevelId(parent, true);
        }
        if (levelId == null) {
            throw new ServiceException(action + "失败：无法解析地址层级");
        }
        validateWritableLevel(levelId, action);
        return levelId;
    }

    private void validateWritableLevel(Integer levelId, String action) {
        if (levelId != null && (levelId == 1 || levelId == 2)) {
            throw new ServiceException(action + "失败：一二级标准地址为只读基础数据");
        }
    }

    private AddrSegm requireParent(String parentSegmId, String action) {
        if (StringUtils.isBlank(parentSegmId)) {
            throw new ServiceException(action + "失败：父级地址不能为空");
        }
        AddrSegm parent = addrSegmMapper.selectById(parentSegmId);
        if (parent != null && "0".equals(StringUtils.blankToDefault(parent.getDeleteState(), NOT_DELETED))) {
            return parent;
        }
        SpcRegion region = spcRegionMapper.selectById(parentSegmId);
        if (region == null) {
            throw new ServiceException(action + "失败：父级地址不存在");
        }
        AddrSegm regionParent = new AddrSegm();
        regionParent.setSegmId(region.getRegionId());
        regionParent.setParentSegmId(region.getSuperRegionId());
        regionParent.setSegmName(region.getRegionName());
        regionParent.setStandName(region.getRegionName());
        regionParent.setSegmNo(region.getRegionNo());
        regionParent.setRegionId(region.getRegionId());
        return regionParent;
    }

    private Integer resolveChildLevelId(AddrSegm parent, boolean required) {
        Integer parentLevelId = resolveParentLevelId(parent);
        Integer childLevelId = dictionaryService.resolveNextLevelId(parentLevelId);
        if (childLevelId == null && required) {
            throw new ServiceException("批量新增失败：父级地址已是末级，无法继续新增下级");
        }
        return childLevelId;
    }

    private Integer resolveParentLevelId(AddrSegm parent) {
        Integer parentLevelId = dictionaryService.resolveLevelId(parent.getSegmType());
        if (parentLevelId != null) {
            return parentLevelId;
        }
        if (StringUtils.isNotBlank(parent.getRegionId()) && StringUtils.equals(parent.getRegionId(), parent.getSegmId())) {
            return StringUtils.isBlank(parent.getParentSegmId()) ? 1 : 2;
        }
        return null;
    }

    private void validateBatchRange(StandardAddressBatchAddBo bo) {
        if (bo.getStartNum() == null || bo.getEndNum() == null) {
            throw new ServiceException("批量预览失败：编号范围不能为空");
        }
        if (bo.getEndNum() < bo.getStartNum()) {
            throw new ServiceException("批量预览失败：结束编号不能小于起始编号");
        }
    }

    private String buildChildSegmName(StandardAddressBatchAddBo bo, int current) {
        return bo.getPrefix() + current + StringUtils.blankToDefault(bo.getSuffix(), "");
    }

    private String buildStandName(AddrSegm parent, String segmName) {
        String parentStandName = StringUtils.defaultIfBlank(parent.getStandName(), parent.getSegmName());
        return parentStandName + segmName;
    }

    private void refreshChildrenStandInfo(AddrSegm parent) {
        List<AddrSegm> children = addrSegmMapper.selectChildrenByParentSegmId(parent.getSegmId());
        if (CollUtil.isEmpty(children)) {
            return;
        }
        for (AddrSegm child : children) {
            child.setStandName(buildStandName(parent, child.getSegmName()));
            child.setStandNo(nameService.buildStandNo(child.getStandName()));
            addrSegmMapper.updateById(child);
            refreshChildrenStandInfo(child);
        }
    }
}
