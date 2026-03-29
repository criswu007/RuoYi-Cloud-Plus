package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressAttribute;
import org.dromara.address.domain.bo.StandardAddressAttributeBo;
import org.dromara.address.domain.vo.StandardAddressAttributeVo;
import org.dromara.address.mapper.StandardAddressAttributeMapper;
import org.dromara.address.service.IStandardAddressAttributeService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 地址属性扩展服务实现。
 * 目的：提供标准地址属性扩展的查询与维护能力。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class StandardAddressAttributeServiceImpl implements IStandardAddressAttributeService {

    private final StandardAddressAttributeMapper baseMapper;

    @Override
    /**
     * {@inheritDoc}
     */
    public StandardAddressAttributeVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public TableDataInfo<StandardAddressAttributeVo> queryPageList(StandardAddressAttributeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StandardAddressAttribute> lqw = buildQueryWrapper(bo);
        Page<StandardAddressAttributeVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public List<StandardAddressAttributeVo> queryList(StandardAddressAttributeBo bo) {
        LambdaQueryWrapper<StandardAddressAttribute> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<StandardAddressAttribute> buildQueryWrapper(StandardAddressAttributeBo bo) {
        LambdaQueryWrapper<StandardAddressAttribute> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getStandardAddressId() != null, StandardAddressAttribute::getStandardAddressId, bo.getStandardAddressId());
        lqw.eq(StringUtils.isNotBlank(bo.getAttrKey()), StandardAddressAttribute::getAttrKey, bo.getAttrKey());
        lqw.like(StringUtils.isNotBlank(bo.getAttrValue()), StandardAddressAttribute::getAttrValue, bo.getAttrValue());
        return lqw;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean insertByBo(StandardAddressAttributeBo bo) {
        StandardAddressAttribute add = MapstructUtils.convert(bo, StandardAddressAttribute.class);
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
    public Boolean updateByBo(StandardAddressAttributeBo bo) {
        StandardAddressAttribute update = MapstructUtils.convert(bo, StandardAddressAttribute.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
