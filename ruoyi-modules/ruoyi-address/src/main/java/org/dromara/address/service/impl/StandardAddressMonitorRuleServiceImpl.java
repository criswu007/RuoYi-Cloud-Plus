package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.domain.bo.StandardAddressMonitorRuleBo;
import org.dromara.address.domain.vo.StandardAddressMonitorRuleVo;
import org.dromara.address.mapper.StandardAddressMonitorRuleMapper;
import org.dromara.address.service.IStandardAddressMonitorRuleService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 地址监控规则服务实现。
 * 目的：提供监控规则的增删改查能力。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class StandardAddressMonitorRuleServiceImpl implements IStandardAddressMonitorRuleService {

    private final StandardAddressMonitorRuleMapper baseMapper;

    @Override
    /**
     * {@inheritDoc}
     */
    public StandardAddressMonitorRuleVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public TableDataInfo<StandardAddressMonitorRuleVo> queryPageList(StandardAddressMonitorRuleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StandardAddressMonitorRule> lqw = buildQueryWrapper(bo);
        Page<StandardAddressMonitorRuleVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public List<StandardAddressMonitorRuleVo> queryList(StandardAddressMonitorRuleBo bo) {
        LambdaQueryWrapper<StandardAddressMonitorRule> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<StandardAddressMonitorRule> buildQueryWrapper(StandardAddressMonitorRuleBo bo) {
        LambdaQueryWrapper<StandardAddressMonitorRule> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), StandardAddressMonitorRule::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getRuleType()), StandardAddressMonitorRule::getRuleType, bo.getRuleType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), StandardAddressMonitorRule::getStatus, bo.getStatus());
        return lqw;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean insertByBo(StandardAddressMonitorRuleBo bo) {
        StandardAddressMonitorRule add = MapstructUtils.convert(bo, StandardAddressMonitorRule.class);
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
    public Boolean updateByBo(StandardAddressMonitorRuleBo bo) {
        StandardAddressMonitorRule update = MapstructUtils.convert(bo, StandardAddressMonitorRule.class);
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
