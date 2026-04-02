package org.dromara.address.service.impl;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.SpcRegion;
import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressSplitItemBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.AddrSetSegmMapper;
import org.dromara.address.mapper.SpcRegionMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * 关键约束：新增对象业务级别不得为一二级，且必须落到 `ADDR_SEGM`。
     * 异常与副作用：会写入 `ADDR_SEGM` 并回填 `segmId/standName/standNo`。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addStandardAddress(StandardAddressBo bo) {
        Integer requestedAddrLevel = bo.getAddrLevel();
        if (requestedAddrLevel == null) {
            requestedAddrLevel = dictionaryService.resolveAddrLevel(bo.getSegmType());
        }
        if (requestedAddrLevel != null) {
            validateWritableAddrLevel(requestedAddrLevel, "新增");
        }
        AddrSegm parent = requireParent(bo.getParentSegmId(), "新增");
        Integer addrLevel = resolveWritableAddrLevel(bo, null, parent, "新增");
        AddrSegm entity = buildEntity(bo, null, parent, addrLevel);
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
     * 关键约束：修改对象业务级别不得为一二级；名称或父级变更后需级联刷新子节点全称与简拼。
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
        Integer addrLevel = resolveWritableAddrLevel(bo, existing, parent, "修改");
        AddrSegm update = buildEntity(bo, existing, parent, addrLevel);
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
        current.forEach(item -> validateWritableAddrLevel(dictionaryService.resolveAddrLevel(item.getSegmType()), "删除"));
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
     * 目的：合并多个源标准地址到目标标准地址。
     * 入参：源地址 `segmId` 集合与目标地址 `segmId`。
     * 出参：合并是否成功。
     * 关键约束：一二级地址不可合并；目标地址层级必须高于源地址；目标地址不能出现在源集合中。
     * 异常与副作用：会迁移直接子节点与安装地址关联，并逻辑回收源地址。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean mergeStandardAddresses(List<String> sourceSegmIds, String targetSegmId) {
        if (CollUtil.isEmpty(sourceSegmIds)) {
            throw new ServiceException("合并失败：待合并地址不能为空");
        }
        if (StringUtils.isBlank(targetSegmId)) {
            throw new ServiceException("合并失败：目标地址不能为空");
        }
        List<String> normalizedSourceIds = normalizeSegmIds(sourceSegmIds);
        if (normalizedSourceIds.contains(targetSegmId)) {
            throw new ServiceException("合并失败：目标地址不能包含在待合并地址中");
        }
        Map<String, AddrSegm> addressMap = loadActiveAddressMap(buildMergeLookupIds(normalizedSourceIds, targetSegmId), "合并");
        AddrSegm target = addressMap.get(targetSegmId);
        Integer targetAddrLevel = requireWritableAddrLevel(target, "合并");
        for (String sourceSegmId : normalizedSourceIds) {
            AddrSegm source = addressMap.get(sourceSegmId);
            Integer sourceAddrLevel = requireWritableAddrLevel(source, "合并");
            if (targetAddrLevel >= sourceAddrLevel) {
                throw new ServiceException("合并失败：目标地址级别必须高于待合并地址");
            }
        }
        addrSegmMapper.moveChildrenToTarget(normalizedSourceIds, targetSegmId);
        addrSetSegmMapper.rebindSegmIds(normalizedSourceIds, targetSegmId);
        return addrSegmMapper.logicalDeleteBySegmIds(normalizedSourceIds) == normalizedSourceIds.size();
    }

    /**
     * 目的：把一个源标准地址拆分成多个同级标准地址。
     * 入参：源地址 `segmId` 与拆分项集合。
     * 出参：拆分是否成功。
     * 关键约束：一二级地址不可拆分；拆分项只能提交当级名称；新地址层级和扩展属性全部继承源地址。
     * 异常与副作用：会批量新增新地址并逻辑回收源地址。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean splitStandardAddress(String sourceSegmId, List<StandardAddressSplitItemBo> splitItems) {
        if (StringUtils.isBlank(sourceSegmId)) {
            throw new ServiceException("拆分失败：待拆分地址不能为空");
        }
        if (CollUtil.isEmpty(splitItems)) {
            throw new ServiceException("拆分失败：拆分地址项不能为空");
        }
        AddrSegm source = requireActiveAddress(sourceSegmId, "拆分");
        requireWritableAddrLevel(source, "拆分");
        AddrSegm parent = requireParent(source.getParentSegmId(), "拆分");
        validateSplitItems(splitItems);
        int inserted = 0;
        for (StandardAddressSplitItemBo splitItem : splitItems) {
            AddrSegm entity = buildSplitEntity(source, parent, splitItem.getSegmName());
            inserted += addrSegmMapper.insert(entity);
        }
        boolean deleted = addrSegmMapper.logicalDeleteBySegmIds(List.of(sourceSegmId)) == 1;
        return inserted == splitItems.size() && deleted;
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
        Integer childAddrLevel = resolveChildAddrLevel(parent, false);
        String childSegmType = childAddrLevel == null ? null : dictionaryService.resolveDefaultSegmTypeByAddrLevel(childAddrLevel);
        List<StandardAddressAdminVo.BatchPreviewVo> result = new ArrayList<>();
        for (int current = bo.getStartNum(); current <= bo.getEndNum(); current++) {
            String segmName = buildChildSegmName(bo, current);
            String standName = buildStandName(parent, segmName);
            StandardAddressAdminVo.BatchPreviewVo item = new StandardAddressAdminVo.BatchPreviewVo();
            item.setSegmName(segmName);
            item.setStandName(standName);
            item.setSegmNo(nameService.buildSegmNo(segmName));
            item.setStandNo(nameService.buildStandNo(standName));
            item.setAddrLevel(childAddrLevel);
            item.setSegmType(childSegmType);
            item.setLevelId(dictionaryService.resolveLevelId(childSegmType));
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
        Integer childAddrLevel = resolveChildAddrLevel(parent, true);
        validateWritableAddrLevel(childAddrLevel, "批量新增");
        String childSegmType = dictionaryService.resolveDefaultSegmTypeByAddrLevel(childAddrLevel);
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

    private AddrSegm buildEntity(StandardAddressBo bo, AddrSegm existing, AddrSegm parent, Integer addrLevel) {
        String segmName = StringUtils.defaultIfBlank(bo.getSegmName(), existing == null ? null : existing.getSegmName());
        if (StringUtils.isBlank(segmName)) {
            throw new ServiceException("当级名称不能为空");
        }
        String segmId = existing == null ? StringUtils.defaultIfBlank(bo.getSegmId(), idGenerator.nextSegmId()) : existing.getSegmId();
        String segmType = StringUtils.defaultIfBlank(bo.getSegmType(), existing == null ? dictionaryService.resolveDefaultSegmTypeByAddrLevel(addrLevel) : existing.getSegmType());
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
        entity.setSingleProjectCode(StringUtils.defaultIfBlank(bo.getSingleProjectCode(),
            existing == null ? null : existing.getSingleProjectCode()));
        entity.setSupportingFeeCommunityFlag(StringUtils.defaultIfBlank(bo.getSupportingFeeCommunityFlag(),
            existing == null ? null : existing.getSupportingFeeCommunityFlag()));
        entity.setIsCity(StringUtils.defaultIfBlank(bo.getIsCity(),
            existing == null ? null : existing.getIsCity()));
        entity.setNotes(StringUtils.defaultIfBlank(bo.getNotes(), existing == null ? null : existing.getNotes()));
        entity.setStationId(StringUtils.defaultIfBlank(bo.getStationId(), existing == null ? null : existing.getStationId()));
        entity.setInstallstationId(StringUtils.defaultIfBlank(bo.getInstallStationId(), existing == null ? null : existing.getInstallstationId()));
        entity.setBusstationId(StringUtils.defaultIfBlank(bo.getBusStationId(), existing == null ? null : existing.getBusstationId()));
        entity.setAddrInTypeFtth(firstNonNull(bo.getAddrInTypeFtth(), existing == null ? null : existing.getAddrInTypeFtth()));
        entity.setFtthPonType(firstNonNull(bo.getFtthPonType(), existing == null ? null : existing.getFtthPonType()));
        entity.setAddrInTypeLan(firstNonNull(bo.getAddrInTypeLan(), existing == null ? null : existing.getAddrInTypeLan()));
        entity.setAreaType(firstNonNull(bo.getAreaType(), existing == null ? null : existing.getAreaType()));
        entity.setPlaceType(firstNonNull(bo.getPlaceType(), existing == null ? null : existing.getPlaceType()));
        entity.setCoverNum(firstNonNull(bo.getCoverNum(), existing == null ? null : existing.getCoverNum()));
        entity.setDeleteState(existing == null ? NOT_DELETED : StringUtils.blankToDefault(existing.getDeleteState(), NOT_DELETED));
        return entity;
    }

    private AddrSegm buildSplitEntity(AddrSegm source, AddrSegm parent, String segmName) {
        if (StringUtils.isBlank(segmName)) {
            throw new ServiceException("拆分失败：拆分后当级名称不能为空");
        }
        String standName = buildStandName(parent, segmName);
        AddrSegm entity = new AddrSegm();
        entity.setSegmId(idGenerator.nextSegmId());
        entity.setParentSegmId(source.getParentSegmId());
        entity.setSegmType(source.getSegmType());
        entity.setSegmName(segmName);
        entity.setStandName(standName);
        entity.setSegmNo(nameService.buildSegmNo(segmName));
        entity.setStandNo(nameService.buildStandNo(standName));
        entity.setRegionId(source.getRegionId());
        entity.setDistrictId(source.getDistrictId());
        entity.setServiceRegionId(source.getServiceRegionId());
        entity.setStatus(source.getStatus());
        entity.setSingleProjectCode(source.getSingleProjectCode());
        entity.setSupportingFeeCommunityFlag(source.getSupportingFeeCommunityFlag());
        entity.setIsCity(source.getIsCity());
        entity.setNotes(source.getNotes());
        entity.setStationId(source.getStationId());
        entity.setInstallstationId(source.getInstallstationId());
        entity.setBusstationId(source.getBusstationId());
        entity.setAddrInTypeFtth(source.getAddrInTypeFtth());
        entity.setFtthPonType(source.getFtthPonType());
        entity.setAddrInTypeLan(source.getAddrInTypeLan());
        entity.setAreaType(source.getAreaType());
        entity.setPlaceType(source.getPlaceType());
        entity.setCoverNum(source.getCoverNum());
        entity.setDeleteState(NOT_DELETED);
        return entity;
    }

    private Integer resolveWritableAddrLevel(StandardAddressBo bo, AddrSegm existing, AddrSegm parent, String action) {
        String segmType = StringUtils.defaultIfBlank(bo.getSegmType(), existing == null ? null : existing.getSegmType());
        Integer addrLevel = bo.getAddrLevel();
        Integer segmTypeAddrLevel = null;
        if (StringUtils.isNotBlank(segmType)) {
            segmTypeAddrLevel = dictionaryService.resolveAddrLevel(segmType);
            if (segmTypeAddrLevel == null) {
                throw new ServiceException(action + "失败：地址类型不存在");
            }
        }
        if (addrLevel == null) {
            addrLevel = segmTypeAddrLevel;
        }
        if (addrLevel == null) {
            throw new ServiceException(action + "失败：无法解析地址层级");
        }
        validateAddrLevelExists(addrLevel, action);
        validateAddrLevelAndSegmType(addrLevel, segmTypeAddrLevel, action);
        validateWritableAddrLevel(addrLevel, action);
        validateAddrLevelHigherThanParent(addrLevel, parent, action);
        return addrLevel;
    }

    private Integer requireWritableAddrLevel(AddrSegm address, String action) {
        Integer addrLevel = dictionaryService.resolveAddrLevel(address.getSegmType());
        if (addrLevel == null) {
            throw new ServiceException(action + "失败：无法解析地址层级");
        }
        validateWritableAddrLevel(addrLevel, action);
        return addrLevel;
    }

    private void validateAddrLevelExists(Integer addrLevel, String action) {
        List<String> segmTypes = dictionaryService.resolveSegmTypesByAddrLevel(addrLevel);
        if (CollUtil.isEmpty(segmTypes)) {
            throw new ServiceException(action + "失败：地址层级不存在");
        }
    }

    private void validateAddrLevelAndSegmType(Integer addrLevel, Integer segmTypeAddrLevel, String action) {
        if (addrLevel != null && segmTypeAddrLevel != null && !addrLevel.equals(segmTypeAddrLevel)) {
            throw new ServiceException(action + "失败：地址层级与地址类型不一致");
        }
    }

    private void validateWritableAddrLevel(Integer addrLevel, String action) {
        if (addrLevel != null && (addrLevel == 1 || addrLevel == 2)) {
            throw new ServiceException(action + "失败：一二级标准地址为只读基础数据");
        }
    }

    private void validateAddrLevelHigherThanParent(Integer addrLevel, AddrSegm parent, String action) {
        Integer parentAddrLevel = resolveParentAddrLevel(parent);
        if (addrLevel == null || parentAddrLevel == null) {
            return;
        }
        if (addrLevel <= parentAddrLevel) {
            throw new ServiceException(action + "失败：当前地址级别必须高于父级地址级别");
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

    private AddrSegm requireActiveAddress(String segmId, String action) {
        AddrSegm address = addrSegmMapper.selectById(segmId);
        if (address == null || !NOT_DELETED.equals(StringUtils.blankToDefault(address.getDeleteState(), NOT_DELETED))) {
            throw new ServiceException(action + "失败：标准地址不存在");
        }
        return address;
    }

    private Map<String, AddrSegm> loadActiveAddressMap(Collection<String> segmIds, String action) {
        List<AddrSegm> addressList = addrSegmMapper.selectBatchIds(segmIds);
        if (CollUtil.isEmpty(addressList) || addressList.size() != segmIds.size()) {
            throw new ServiceException(action + "失败：标准地址不存在");
        }
        Map<String, AddrSegm> addressMap = new LinkedHashMap<>();
        for (AddrSegm address : addressList) {
            if (!NOT_DELETED.equals(StringUtils.blankToDefault(address.getDeleteState(), NOT_DELETED))) {
                throw new ServiceException(action + "失败：标准地址不存在");
            }
            addressMap.put(address.getSegmId(), address);
        }
        if (addressMap.size() != segmIds.size()) {
            throw new ServiceException(action + "失败：标准地址不存在");
        }
        return addressMap;
    }

    private List<String> normalizeSegmIds(Collection<String> segmIds) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String segmId : segmIds) {
            if (StringUtils.isNotBlank(segmId)) {
                normalized.add(segmId.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    private List<String> buildMergeLookupIds(List<String> sourceSegmIds, String targetSegmId) {
        List<String> lookupIds = new ArrayList<>(sourceSegmIds);
        lookupIds.add(targetSegmId);
        return lookupIds;
    }

    private void validateSplitItems(List<StandardAddressSplitItemBo> splitItems) {
        Set<String> segmNames = new LinkedHashSet<>();
        for (StandardAddressSplitItemBo splitItem : splitItems) {
            if (splitItem == null || StringUtils.isBlank(splitItem.getSegmName())) {
                throw new ServiceException("拆分失败：拆分后当级名称不能为空");
            }
            String segmName = splitItem.getSegmName().trim();
            if (!segmNames.add(segmName)) {
                throw new ServiceException("拆分失败：拆分后地址名称存在重复");
            }
            splitItem.setSegmName(segmName);
        }
    }

    private Integer resolveChildAddrLevel(AddrSegm parent, boolean required) {
        Integer parentAddrLevel = resolveParentAddrLevel(parent);
        Integer childAddrLevel = dictionaryService.resolveNextAddrLevel(parentAddrLevel);
        if (childAddrLevel == null && required) {
            throw new ServiceException("批量新增失败：父级地址已是末级，无法继续新增下级");
        }
        return childAddrLevel;
    }

    private Integer resolveParentAddrLevel(AddrSegm parent) {
        Integer parentAddrLevel = dictionaryService.resolveAddrLevel(parent.getSegmType());
        if (parentAddrLevel != null) {
            return parentAddrLevel;
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

    private <T> T firstNonNull(T currentValue, T fallbackValue) {
        return currentValue != null ? currentValue : fallbackValue;
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
