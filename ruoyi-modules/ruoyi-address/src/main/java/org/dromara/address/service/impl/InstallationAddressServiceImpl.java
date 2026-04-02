package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.InstallationAddress;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.mapper.InstallationAddressMapper;
import org.dromara.address.service.IInstallationAddressService;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 安装地址服务实现。
 * 目的：提供安装地址的查询与维护能力。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class InstallationAddressServiceImpl implements IInstallationAddressService {

    private final InstallationAddressMapper baseMapper;
    private final IStandardAddressService standardAddressService;

    @Override
    /**
     * {@inheritDoc}
     */
    public InstallationAddressVo queryById(Long id) {
        InstallationAddressVo vo = baseMapper.selectVoById(id);
        if (vo != null) {
            fillStandardAddressInfo(List.of(vo));
        }
        return vo;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public TableDataInfo<InstallationAddressVo> queryPageList(InstallationAddressBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<InstallationAddress> lqw = buildQueryWrapper(bo);
        Page<InstallationAddressVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillStandardAddressInfo(result.getRecords());
        return TableDataInfo.build(result);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public List<InstallationAddressVo> queryList(InstallationAddressBo bo) {
        LambdaQueryWrapper<InstallationAddress> lqw = buildQueryWrapper(bo);
        List<InstallationAddressVo> list = baseMapper.selectVoList(lqw);
        fillStandardAddressInfo(list);
        return list;
    }

    private LambdaQueryWrapper<InstallationAddress> buildQueryWrapper(InstallationAddressBo bo) {
        LambdaQueryWrapper<InstallationAddress> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getStandardAddressId() != null, InstallationAddress::getStandardAddressId, bo.getStandardAddressId());
        lqw.like(StringUtils.isNotBlank(bo.getInstallName()), InstallationAddress::getInstallName, bo.getInstallName());
        lqw.eq(StringUtils.isNotBlank(bo.getResourceId()), InstallationAddress::getResourceId, bo.getResourceId());
        lqw.eq(StringUtils.isNotBlank(bo.getResourceType()), InstallationAddress::getResourceType, bo.getResourceType());
        if (bo.getParams() != null && bo.getParams().get("beginTime") != null && bo.getParams().get("endTime") != null) {
            lqw.between(true, InstallationAddress::getCreateTime, bo.getParams().get("beginTime"), bo.getParams().get("endTime"));
        }
        return lqw;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean insertByBo(InstallationAddressBo bo) {
        InstallationAddress add = MapstructUtils.convert(bo, InstallationAddress.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean updateByBo(InstallationAddressBo bo) {
        InstallationAddress update = MapstructUtils.convert(bo, InstallationAddress.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    private void fillStandardAddressInfo(List<InstallationAddressVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<String> standardAddressSegmIds = list.stream()
            .map(InstallationAddressVo::getStandardAddressId)
            .filter(id -> id != null && id > 0)
            .map(String::valueOf)
            .distinct()
            .toList();
        if (standardAddressSegmIds.isEmpty()) {
            for (InstallationAddressVo vo : list) {
                vo.setHasStandardAddress(false);
            }
            return;
        }
        Map<String, String> fullNameMap = standardAddressService.listStandardAddressStandNameMapBySegmIds(standardAddressSegmIds);
        for (InstallationAddressVo vo : list) {
            String fullName = vo.getStandardAddressId() == null ? null : fullNameMap.get(String.valueOf(vo.getStandardAddressId()));
            vo.setHasStandardAddress(StringUtils.isNotBlank(fullName));
            vo.setStandardAddressFullName(fullName);
        }
    }
}
