package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressMonitorRuleBo;
import org.dromara.address.domain.vo.StandardAddressMonitorRuleVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

public interface IStandardAddressMonitorRuleService {

    /**
     * 查询监控规则详情。
     *
     * @param id 主键ID
     * @return 规则详情
     */
    StandardAddressMonitorRuleVo queryById(Long id);

    /**
     * 分页查询监控规则。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<StandardAddressMonitorRuleVo> queryPageList(StandardAddressMonitorRuleBo bo, PageQuery pageQuery);

    /**
     * 查询监控规则列表（不分页）。
     *
     * @param bo 查询条件
     * @return 规则列表
     */
    List<StandardAddressMonitorRuleVo> queryList(StandardAddressMonitorRuleBo bo);

    /**
     * 新增监控规则。
     *
     * @param bo 新增参数
     * @return 是否成功
     */
    Boolean insertByBo(StandardAddressMonitorRuleBo bo);

    /**
     * 修改监控规则。
     *
     * @param bo 修改参数
     * @return 是否成功
     */
    Boolean updateByBo(StandardAddressMonitorRuleBo bo);

    /**
     * 批量启用监控规则。
     *
     * @param ids 规则ID集合
     * @return 是否成功
     *
     * 目的：统一规则启用入口，供任务调度与管理端批量操作复用。
     * 关键约束：重复启用应保持幂等，仅更新状态字段。
     * 异常与副作用：成功后会批量更新规则状态。
     */
    Boolean enableByIds(List<Long> ids);

    /**
     * 批量禁用监控规则。
     *
     * @param ids 规则ID集合
     * @return 是否成功
     *
     * 目的：统一规则禁用入口，供任务调度与管理端批量操作复用。
     * 关键约束：禁用后规则不应再参与后续检测任务。
     * 异常与副作用：成功后会批量更新规则状态。
     */
    Boolean disableByIds(List<Long> ids);

    /**
     * 删除监控规则。
     *
     * @param ids 规则ID集合
     * @param isValid 是否校验业务逻辑
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
