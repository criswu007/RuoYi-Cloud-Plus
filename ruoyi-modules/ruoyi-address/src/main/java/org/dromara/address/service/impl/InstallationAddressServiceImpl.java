package org.dromara.address.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddrSetSegm;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.mapper.AddrSetSegmMapper;
import org.dromara.address.search.builder.InstallationAddressSearchDocumentBuilder;
import org.dromara.address.search.document.InstallationAddressSearchDocument;
import org.dromara.address.service.IInstallationAddressService;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.address.search.service.AddressSearchSyncService;
import org.dromara.address.search.service.InstallationAddressSearchGateway;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 安装地址服务实现。
 * 目的：基于线上 `ADDR_SET_SEGM` 主事实表提供安装地址查询与维护能力，并按页面交互批量回填标准地址名称。
 * 入参/出参：入参使用 {@link InstallationAddressBo}，出参统一返回 {@link InstallationAddressVo} 与分页结构。
 * 关键约束：主查询不直接关联 `ADDR_SEGM` 大表，标准地址名称统一在服务层批量补齐，避免高频链路 join 放大。
 * 异常与副作用：新增/修改/删除会写入 `ADDR_SET_SEGM`；查询无写入副作用。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class InstallationAddressServiceImpl implements IInstallationAddressService {

    private final AddrSetSegmMapper addrSetSegmMapper;
    private final IStandardAddressService standardAddressService;
    private final AddressSearchProperties addressSearchProperties;
    private final InstallationAddressSearchGateway installationAddressSearchGateway;
    private final AddressSearchSyncService addressSearchSyncService;

    /**
     * 查询安装地址详情。
     *
     * @param setAddrId 安装地址主键
     * @return 安装地址详情
     *
     * 关键约束：详情按唯一主键命中，标准地址名称通过批量回填逻辑复用，避免单独详情 join 大表。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public InstallationAddressVo queryById(String setAddrId) {
        if (StringUtils.isBlank(setAddrId)) {
            return null;
        }
        InstallationAddressVo vo = addrSetSegmMapper.selectInstallationBySetAddrId(setAddrId);
        fillStandardAddressInfo(vo == null ? Collections.emptyList() : Collections.singletonList(vo));
        return vo;
    }

    /**
     * 分页查询安装地址列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 安装地址分页结果
     *
     * 关键约束：分页必须由 MyBatis-Plus 分页插件下推数据库执行；查询语义与页面交互一致。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public TableDataInfo<InstallationAddressVo> queryPageList(InstallationAddressBo bo, PageQuery pageQuery) {
        TableDataInfo<InstallationAddressVo> pageData;
        if (Boolean.TRUE.equals(addressSearchProperties.getInstallation().getReadEnabled())) {
            pageData = installationAddressSearchGateway.queryPage(bo, pageQuery);
        } else {
            Page<InstallationAddressVo> page = addrSetSegmMapper.selectInstallationPage(pageQuery.build(), bo);
            pageData = TableDataInfo.build(page);
        }
        fillStandardAddressInfo(pageData.getRows());
        return pageData;
    }

    /**
     * 查询安装地址列表（不分页）。
     *
     * @param bo 查询条件
     * @return 安装地址列表
     *
     * 关键约束：仅用于需要全量列表的非分页交互，仍遵循主查询不关联大表规则。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public List<InstallationAddressVo> queryList(InstallationAddressBo bo) {
        List<InstallationAddressVo> list = addrSetSegmMapper.selectInstallationList(bo);
        fillStandardAddressInfo(list);
        return list;
    }

    /**
     * 新增安装地址。
     *
     * @param bo 新增参数
     * @return 是否新增成功
     *
     * 关键约束：安装地址主键按 24 位字符串生成；默认逻辑删除标识写入未删除态；
     * `deviceId` 仅保留接口兼容，不写入历史库。
     * 异常与副作用：会写入 `ADDR_SET_SEGM`。
     */
    @Override
    public Boolean insertByBo(InstallationAddressBo bo) {
        AddrSetSegm entity = buildEntity(bo, null);
        InstallationAddressSearchDocument document = InstallationAddressSearchDocumentBuilder.fromEntity(entity, resolveAssociationStatus(entity));
        return addressSearchSyncService.syncInstallationCreate(document, () -> {
            boolean success = addrSetSegmMapper.insert(entity) > 0;
            if (success) {
                bo.setSetAddrId(entity.getSetAddrId());
            }
            return success;
        });
    }

    /**
     * 修改安装地址。
     *
     * @param bo 修改参数
     * @return 是否修改成功
     *
     * 关键约束：仅更新当前页面交互涉及字段；主键不能为空；
     * `syncDate` 写入历史库字段 `synchronous_date`；
     * `deviceId` 仅保留接口兼容，不写入历史库。
     * 异常与副作用：会更新 `ADDR_SET_SEGM`。
     */
    @Override
    public Boolean updateByBo(InstallationAddressBo bo) {
        if (StringUtils.isBlank(bo.getSetAddrId())) {
            return false;
        }
        AddrSetSegm existing = addrSetSegmMapper.selectById(bo.getSetAddrId());
        if (existing == null) {
            return false;
        }
        AddrSetSegm entity = buildEntity(bo, existing);
        InstallationAddressSearchDocument beforeDocument = InstallationAddressSearchDocumentBuilder.fromEntity(existing, resolveAssociationStatus(existing));
        InstallationAddressSearchDocument afterDocument = InstallationAddressSearchDocumentBuilder.fromEntity(entity, resolveAssociationStatus(entity));
        return addressSearchSyncService.syncInstallationUpdate(beforeDocument, afterDocument,
            () -> addrSetSegmMapper.updateById(entity) > 0);
    }

    /**
     * 删除安装地址（逻辑删除）。
     *
     * @param setAddrIds 安装地址主键集合
     * @param isValid 是否校验业务逻辑（当前阶段预留）
     * @return 是否删除成功
     *
     * 关键约束：删除走逻辑删除，统一更新 `delete_state/delete_time`。
     * 异常与副作用：会批量更新 `ADDR_SET_SEGM`。
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> setAddrIds, Boolean isValid) {
        if (setAddrIds == null || setAddrIds.isEmpty()) {
            return true;
        }
        List<AddrSetSegm> existingList = addrSetSegmMapper.selectBatchIds(setAddrIds);
        List<InstallationAddressSearchDocument> beforeDocuments = existingList.stream()
            .map(entity -> InstallationAddressSearchDocumentBuilder.fromEntity(entity, resolveAssociationStatus(entity)))
            .toList();
        return addressSearchSyncService.syncInstallationDelete(beforeDocuments, () -> {
            LambdaUpdateWrapper<AddrSetSegm> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(AddrSetSegm::getSetAddrId, setAddrIds)
                .eq(AddrSetSegm::getDeleteState, "0")
                .set(AddrSetSegm::getDeleteState, "1")
                .set(AddrSetSegm::getDeleteTime, new Date());
            return addrSetSegmMapper.update(null, updateWrapper) > 0;
        });
    }

    /**
     * 批量补齐标准地址名称与关联状态。
     *
     * @param list 安装地址列表
     *
     * 关键约束：必须批量查询标准地址名称映射，禁止逐条回查标准地址详情形成 N+1。
     * 异常与副作用：无写入副作用。
     */
    private void fillStandardAddressInfo(List<InstallationAddressVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<String> segmIds = list.stream()
            .map(InstallationAddressVo::getSegmId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        Map<String, String> standNameMap = segmIds.isEmpty()
            ? Collections.emptyMap()
            : standardAddressService.listStandardAddressStandNameMapBySegmIds(segmIds);
        for (InstallationAddressVo row : list) {
            String standName = StringUtils.isBlank(row.getSegmId()) ? null : standNameMap.get(row.getSegmId());
            boolean hasStandardAddress = StringUtils.isNotBlank(standName);
            row.setHasStandardAddress(hasStandardAddress);
            row.setAssociationStatus(hasStandardAddress ? "BOUND" : "UNBOUND");
            row.setStandName(standName);
        }
    }

    /**
     * 生成安装地址主键。
     *
     * @return 24 位安装地址主键
     *
     * 关键约束：保持与标准地址主键一致的 24 位字符串格式，避免前端类型歧义。
     */
    private String nextSetAddrId() {
        return String.format("%024d", IdUtil.getSnowflakeNextId());
    }

    private AddrSetSegm buildEntity(InstallationAddressBo bo, AddrSetSegm existing) {
        AddrSetSegm entity = new AddrSetSegm();
        entity.setSetAddrId(existing == null ? StringUtils.defaultIfBlank(bo.getSetAddrId(), nextSetAddrId()) : existing.getSetAddrId());
        entity.setSetAddrName(bo.getSetAddrName());
        entity.setSetAddrNo(bo.getSetAddrNo());
        entity.setSetType(bo.getSetType());
        entity.setSegmId(bo.getSegmId());
        entity.setSegmType(bo.getSegmType());
        entity.setRegionId(bo.getRegionId());
        entity.setOrgId(bo.getOrgId());
        entity.setNotes(bo.getNotes());
        entity.setBossOp(bo.getBossOp());
        entity.setDeleteState(existing == null ? "0" : existing.getDeleteState());
        entity.setCreateDate(existing == null ? new Date() : existing.getCreateDate());
        entity.setSyncDate(existing == null ? null : new Date());
        return entity;
    }

    private String resolveAssociationStatus(AddrSetSegm entity) {
        return StringUtils.isNotBlank(entity.getSegmId()) ? "BOUND" : "UNBOUND";
    }
}
