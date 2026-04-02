package org.dromara.address.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressTag;
import org.dromara.address.domain.StandardAddressTagRel;
import org.dromara.address.domain.bo.StandardAddressTagBo;
import org.dromara.address.domain.vo.StandardAddressTagVo;
import org.dromara.address.mapper.StandardAddressTagMapper;
import org.dromara.address.mapper.StandardAddressTagRelMapper;
import org.dromara.address.service.IStandardAddressTagService;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 地址标签服务实现。
 * 目的：统一处理标签管理、唯一性校验与地址打标关系维护。
 * 关键约束：标签名称唯一；绑定前校验地址与标签存在性；重复绑定自动忽略。
 * 副作用：写入地址标签表与地址标签关联表。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class StandardAddressTagServiceImpl implements IStandardAddressTagService {

    private final StandardAddressTagMapper baseMapper;
    private final StandardAddressTagRelMapper addressTagRelMapper;
    private final IStandardAddressService standardAddressService;

    @Override
    public StandardAddressTagVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<StandardAddressTagVo> queryPageList(StandardAddressTagBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StandardAddressTag> lqw = buildQueryWrapper(bo);
        Page<StandardAddressTagVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<StandardAddressTagVo> queryList(StandardAddressTagBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(StandardAddressTagBo bo) {
        ensureTagNameUnique(bo.getName(), null);
        StandardAddressTag add = MapstructUtils.convert(bo, StandardAddressTag.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(StandardAddressTagBo bo) {
        StandardAddressTag existing = baseMapper.selectById(bo.getId());
        if (existing == null) {
            throw new ServiceException("标签不存在");
        }
        ensureTagNameUnique(bo.getName(), bo.getId());
        StandardAddressTag update = MapstructUtils.convert(bo, StandardAddressTag.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (CollUtil.isEmpty(ids)) {
            return true;
        }
        addressTagRelMapper.delete(
            Wrappers.<StandardAddressTagRel>lambdaQuery().in(StandardAddressTagRel::getTagId, ids)
        );
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public List<StandardAddressTagVo> listTagsByStandardAddressId(Long standardAddressId) {
        return mapTagsByStandardAddressIds(Collections.singleton(standardAddressId))
            .getOrDefault(standardAddressId, Collections.emptyList());
    }

    @Override
    public Map<Long, List<StandardAddressTagVo>> mapTagsByStandardAddressIds(Collection<Long> standardAddressIds) {
        if (CollUtil.isEmpty(standardAddressIds)) {
            return Collections.emptyMap();
        }
        List<StandardAddressTagRel> relList = addressTagRelMapper.selectList(
            Wrappers.<StandardAddressTagRel>lambdaQuery().in(StandardAddressTagRel::getStandardAddressId, standardAddressIds)
        );
        if (CollUtil.isEmpty(relList)) {
            return Collections.emptyMap();
        }

        List<Long> tagIds = relList.stream()
            .map(StandardAddressTagRel::getTagId)
            .distinct()
            .toList();
        Map<Long, StandardAddressTagVo> tagMap = baseMapper.selectBatchIds(tagIds).stream()
            .map(item -> MapstructUtils.convert(item, StandardAddressTagVo.class))
            .collect(Collectors.toMap(StandardAddressTagVo::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<Long, List<StandardAddressTagVo>> result = new LinkedHashMap<>();
        for (StandardAddressTagRel rel : relList) {
            StandardAddressTagVo tag = tagMap.get(rel.getTagId());
            if (tag == null) {
                continue;
            }
            result.computeIfAbsent(rel.getStandardAddressId(), key -> new ArrayList<>()).add(tag);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean bindTagsToStandardAddresses(Collection<Long> standardAddressIds, Collection<Long> tagIds) {
        Set<Long> normalizedStandardAddressIds = normalizeIds(standardAddressIds, "标准地址");
        Set<Long> normalizedTagIds = normalizeIds(tagIds, "标签");
        validateStandardAddressIds(normalizedStandardAddressIds);
        validateTagIds(normalizedTagIds);

        List<StandardAddressTagRel> existingList = addressTagRelMapper.selectList(
            Wrappers.<StandardAddressTagRel>lambdaQuery()
                .in(StandardAddressTagRel::getStandardAddressId, normalizedStandardAddressIds)
                .in(StandardAddressTagRel::getTagId, normalizedTagIds)
        );
        Set<String> existingKeys = existingList.stream()
            .map(rel -> buildRelKey(rel.getStandardAddressId(), rel.getTagId()))
            .collect(Collectors.toSet());

        boolean changed = false;
        for (Long standardAddressId : normalizedStandardAddressIds) {
            for (Long tagId : normalizedTagIds) {
                String relKey = buildRelKey(standardAddressId, tagId);
                if (existingKeys.contains(relKey)) {
                    continue;
                }
                StandardAddressTagRel rel = new StandardAddressTagRel();
                rel.setStandardAddressId(standardAddressId);
                rel.setTagId(tagId);
                addressTagRelMapper.insert(rel);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unbindTagsFromStandardAddresses(Collection<Long> standardAddressIds, Collection<Long> tagIds) {
        Set<Long> normalizedStandardAddressIds = normalizeIds(standardAddressIds, "标准地址");
        Set<Long> normalizedTagIds = normalizeIds(tagIds, "标签");
        int deleted = addressTagRelMapper.delete(
            Wrappers.<StandardAddressTagRel>lambdaQuery()
                .in(StandardAddressTagRel::getStandardAddressId, normalizedStandardAddressIds)
                .in(StandardAddressTagRel::getTagId, normalizedTagIds)
        );
        return deleted > 0;
    }

    /**
     * 构造标签查询条件。
     *
     * @param bo 查询参数
     * @return 查询条件
     *
     * 关键约束：名称、编码模糊匹配；颜色精确匹配。
     */
    private LambdaQueryWrapper<StandardAddressTag> buildQueryWrapper(StandardAddressTagBo bo) {
        LambdaQueryWrapper<StandardAddressTag> lqw = Wrappers.lambdaQuery(StandardAddressTag.class);
        lqw.like(StringUtils.isNotBlank(bo.getName()), StandardAddressTag::getName, bo.getName());
        lqw.like(StringUtils.isNotBlank(bo.getCode()), StandardAddressTag::getCode, bo.getCode());
        lqw.eq(StringUtils.isNotBlank(bo.getColor()), StandardAddressTag::getColor, bo.getColor());
        return lqw;
    }

    /**
     * 校验标签名称唯一。
     *
     * @param name 标签名称
     * @param currentId 当前标签ID（新增时为空）
     *
     * 关键约束：忽略空白差异后按名称唯一。
     * 异常：命中重名数据时抛出业务异常。
     */
    private void ensureTagNameUnique(String name, Long currentId) {
        String normalizedName = StringUtils.trim(name);
        boolean exists = baseMapper.exists(
            Wrappers.<StandardAddressTag>lambdaQuery()
                .eq(StandardAddressTag::getName, normalizedName)
                .ne(currentId != null, StandardAddressTag::getId, currentId)
        );
        if (exists) {
            throw new ServiceException("标签名称已存在");
        }
    }

    /**
     * 规范化并校验ID集合。
     *
     * @param ids 原始ID集合
     * @param name 集合名称
     * @return 去重后的ID集合
     *
     * 异常：集合为空或包含空值时抛出业务异常。
     */
    private Set<Long> normalizeIds(Collection<Long> ids, String name) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException(name + "不能为空");
        }
        Set<Long> normalized = ids.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            throw new ServiceException(name + "不能为空");
        }
        return normalized;
    }

    /**
     * 校验标准地址是否全部存在。
     *
     * @param standardAddressIds 标准地址ID集合
     *
     * 异常：存在缺失地址时抛出业务异常。
     */
    private void validateStandardAddressIds(Collection<Long> standardAddressIds) {
        List<String> segmIds = standardAddressIds.stream()
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .toList();
        Map<String, String> standardAddressMap = standardAddressService.listStandardAddressStandNameMapBySegmIds(segmIds);
        long validCount = segmIds.stream()
            .filter(standardAddressMap::containsKey)
            .count();
        if (validCount != standardAddressIds.size()) {
            throw new ServiceException("存在无效的标准地址");
        }
    }

    /**
     * 校验标签是否全部存在。
     *
     * @param tagIds 标签ID集合
     *
     * 异常：存在缺失标签时抛出业务异常。
     */
    private void validateTagIds(Collection<Long> tagIds) {
        long count = baseMapper.selectCount(
            Wrappers.<StandardAddressTag>lambdaQuery().in(StandardAddressTag::getId, tagIds)
        );
        if (count != tagIds.size()) {
            throw new ServiceException("存在无效的标签");
        }
    }

    /**
     * 生成地址-标签关系唯一键。
     *
     * @param standardAddressId 标准地址ID
     * @param tagId 标签ID
     * @return 关系键
     */
    private String buildRelKey(Long standardAddressId, Long tagId) {
        return standardAddressId + "_" + tagId;
    }
}
