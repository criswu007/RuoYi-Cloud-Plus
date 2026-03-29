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
     * 删除监控规则。
     *
     * @param ids 规则ID集合
     * @param isValid 是否校验业务逻辑
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
