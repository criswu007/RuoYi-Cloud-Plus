package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressManagementStation;
import org.dromara.address.domain.bo.StandardAddressManagementStationBo;
import org.dromara.address.domain.vo.StandardAddressManagementStationVo;
import org.dromara.address.mapper.StandardAddressManagementStationMapper;
import org.dromara.address.service.IStandardAddressManagementStationService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 管理站管理服务实现。
 * 目的：提供管理站管理页列表、详情与增删改能力。
 * 入参/出参：入参使用 {@link StandardAddressManagementStationBo}，出参统一返回 {@link StandardAddressManagementStationVo}。
 * 关键约束：列表查询仅按交互字段检索，分页由 MyBatis-Plus 分页插件下推。
 * 异常与副作用：新增/修改/删除会写入 `address_standard_management_station`，查询无写入副作用。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class StandardAddressManagementStationServiceImpl implements IStandardAddressManagementStationService {

    private final StandardAddressManagementStationMapper baseMapper;

    /**
     * 查询管理站详情。
     *
     * @param id 管理站主键
     * @return 管理站详情
     *
     * 关键约束：按主键唯一查询，不做额外关联查询。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public StandardAddressManagementStationVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询管理站列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     *
     * 关键约束：列表按名称模糊检索，禁止数据库方言分页语法。
     * 异常与副作用：无写入副作用。
     */
    @Override
    public TableDataInfo<StandardAddressManagementStationVo> queryPageList(StandardAddressManagementStationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<StandardAddressManagementStation> lqw = buildQueryWrapper(bo);
        Page<StandardAddressManagementStationVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * 新增管理站。
     *
     * @param bo 新增参数
     * @return 是否成功
     *
     * 关键约束：管理站名称不能为空。
     * 异常与副作用：写入管理站表，成功后回填主键。
     */
    @Override
    public Boolean insertByBo(StandardAddressManagementStationBo bo) {
        StandardAddressManagementStation entity = MapstructUtils.convert(bo, StandardAddressManagementStation.class);
        if (entity.getId() == null) {
            entity.setId(IdUtil.getSnowflakeNextId());
        }
        boolean success = baseMapper.insert(entity) > 0;
        if (success) {
            bo.setId(entity.getId());
        }
        return success;
    }

    /**
     * 修改管理站。
     *
     * @param bo 修改参数
     * @return 是否成功
     *
     * 关键约束：主键不能为空，且仅更新当前交互需要字段。
     * 异常与副作用：更新管理站表。
     */
    @Override
    public Boolean updateByBo(StandardAddressManagementStationBo bo) {
        StandardAddressManagementStation entity = MapstructUtils.convert(bo, StandardAddressManagementStation.class);
        return baseMapper.updateById(entity) > 0;
    }

    /**
     * 删除管理站。
     *
     * @param ids 主键集合
     * @param isValid 是否校验业务逻辑（当前阶段预留）
     * @return 是否成功
     *
     * 关键约束：当前阶段按逻辑删除处理。
     * 异常与副作用：删除管理站记录。
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    private LambdaQueryWrapper<StandardAddressManagementStation> buildQueryWrapper(StandardAddressManagementStationBo bo) {
        LambdaQueryWrapper<StandardAddressManagementStation> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), StandardAddressManagementStation::getName, bo.getName());
        return lqw;
    }
}
