package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressManagementStationBo;
import org.dromara.address.domain.vo.StandardAddressManagementStationVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;

/**
 * 管理站管理服务接口。
 * 目的：提供管理站管理页所需的列表、详情和增删改能力。
 */
public interface IStandardAddressManagementStationService {

    /**
     * 查询管理站详情。
     *
     * @param id 管理站主键
     * @return 管理站详情
     *
     * 关键约束：按主键唯一命中，详情不做跨表聚合。
     */
    StandardAddressManagementStationVo queryById(Long id);

    /**
     * 分页查询管理站列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     *
     * 关键约束：列表查询仅按交互字段检索，分页由 MyBatis-Plus 下推执行。
     */
    TableDataInfo<StandardAddressManagementStationVo> queryPageList(StandardAddressManagementStationBo bo, PageQuery pageQuery);

    /**
     * 新增管理站。
     *
     * @param bo 新增参数
     * @return 是否成功
     *
     * 关键约束：管理站名称不能为空。
     * 副作用：写入管理站表。
     */
    Boolean insertByBo(StandardAddressManagementStationBo bo);

    /**
     * 修改管理站。
     *
     * @param bo 修改参数
     * @return 是否成功
     *
     * 关键约束：主键不能为空，管理站名称不能为空。
     * 副作用：更新管理站表。
     */
    Boolean updateByBo(StandardAddressManagementStationBo bo);

    /**
     * 删除管理站。
     *
     * @param ids 主键集合
     * @param isValid 是否校验业务逻辑（当前阶段预留）
     * @return 是否成功
     *
     * 副作用：逻辑删除管理站记录。
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
