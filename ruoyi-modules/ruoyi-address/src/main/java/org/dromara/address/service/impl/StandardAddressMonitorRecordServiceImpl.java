package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressMonitorRecord;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.domain.bo.StandardAddressMonitorRecordBo;
import org.dromara.address.domain.vo.StandardAddressMonitorRecordVo;
import org.dromara.address.mapper.StandardAddressMonitorRuleMapper;
import org.dromara.address.mapper.StandardAddressMonitorRecordMapper;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.address.service.IStandardAddressMonitorRecordService;
import org.dromara.common.core.utils.MapstructUtils;
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
import java.util.stream.Collectors;

/**
 * 地址监控记录服务实现。
 * 目的：提供监控记录的查询、处理与删除能力。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class StandardAddressMonitorRecordServiceImpl implements IStandardAddressMonitorRecordService {

    private final StandardAddressMonitorRecordMapper baseMapper;
    private final StandardAddressMonitorRuleMapper addressMonitorRuleMapper;
    private final IStandardAddressService standardAddressService;

    @Override
    /**
     * {@inheritDoc}
     */
    public StandardAddressMonitorRecordVo queryById(Long id) {
        StandardAddressMonitorRecordVo vo = baseMapper.selectVoById(id);
        fillDisplayInfo(vo == null ? Collections.emptyList() : List.of(vo));
        return vo;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public TableDataInfo<StandardAddressMonitorRecordVo> queryPageList(StandardAddressMonitorRecordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StandardAddressMonitorRecord> lqw = buildQueryWrapper(bo);
        Page<StandardAddressMonitorRecordVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillDisplayInfo(result.getRecords());
        return TableDataInfo.build(result);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public List<StandardAddressMonitorRecordVo> queryList(StandardAddressMonitorRecordBo bo) {
        LambdaQueryWrapper<StandardAddressMonitorRecord> lqw = buildQueryWrapper(bo);
        List<StandardAddressMonitorRecordVo> list = baseMapper.selectVoList(lqw);
        fillDisplayInfo(list);
        return list;
    }

    private LambdaQueryWrapper<StandardAddressMonitorRecord> buildQueryWrapper(StandardAddressMonitorRecordBo bo) {
        LambdaQueryWrapper<StandardAddressMonitorRecord> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getStandardAddressId() != null, StandardAddressMonitorRecord::getStandardAddressId, bo.getStandardAddressId());
        lqw.eq(bo.getRuleId() != null, StandardAddressMonitorRecord::getRuleId, bo.getRuleId());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), StandardAddressMonitorRecord::getStatus, bo.getStatus());
        return lqw;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean updateByBo(StandardAddressMonitorRecordBo bo) {
        StandardAddressMonitorRecord update = MapstructUtils.convert(bo, StandardAddressMonitorRecord.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 填充异常记录的展示字段。
     *
     * @param list 监控记录视图集合
     *
     * 目的：补齐前端原型所需的地址名称、规则名称等可读信息，避免页面只展示 ID。
     * 关键约束：采用批量查询方式补充名称，避免出现 N+1 查询。
     * 副作用：会直接修改传入视图对象中的展示字段。
     */
    private void fillDisplayInfo(List<StandardAddressMonitorRecordVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        List<Long> standardAddressIds = list.stream()
            .map(StandardAddressMonitorRecordVo::getStandardAddressId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<Long> ruleIds = list.stream()
            .map(StandardAddressMonitorRecordVo::getRuleId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<Long, String> addressNameMap = buildStandardAddressNameMap(standardAddressIds);

        Map<Long, String> ruleNameMap = ruleIds.isEmpty()
            ? Collections.emptyMap()
            : addressMonitorRuleMapper.selectList(
                Wrappers.<StandardAddressMonitorRule>lambdaQuery().in(StandardAddressMonitorRule::getId, ruleIds)
            ).stream().collect(Collectors.toMap(StandardAddressMonitorRule::getId, StandardAddressMonitorRule::getName, (left, right) -> left));

        for (StandardAddressMonitorRecordVo item : list) {
            if (item == null) {
                continue;
            }
            item.setStandardAddressFullName(addressNameMap.get(item.getStandardAddressId()));
            item.setRuleName(ruleNameMap.get(item.getRuleId()));
        }
    }

    /**
     * 目的：批量构建监控记录中的标准地址名称映射。
     * 入参：标准地址 Long 主键集合。
     * 出参：`standardAddressId -> standName` 映射。
     * 关键约束：必须走批量标准地址名称查询，禁止逐条调用标准地址详情形成 N+1。
     * 异常与副作用：无写入副作用。
     */
    private Map<Long, String> buildStandardAddressNameMap(List<Long> standardAddressIds) {
        if (standardAddressIds == null || standardAddressIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> standNameMap = standardAddressService.listStandardAddressStandNameMapBySegmIds(
            standardAddressIds.stream().map(String::valueOf).toList()
        );
        Map<Long, String> result = new LinkedHashMap<>();
        for (Long standardAddressId : standardAddressIds) {
            result.put(standardAddressId, standNameMap.get(String.valueOf(standardAddressId)));
        }
        return result;
    }
}
