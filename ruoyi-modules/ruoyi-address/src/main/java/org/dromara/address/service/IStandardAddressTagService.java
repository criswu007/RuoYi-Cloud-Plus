package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressTagBo;
import org.dromara.address.domain.vo.StandardAddressTagVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 地址标签服务接口。
 * 目的：提供标签管理与标签绑定能力，服务标准地址打标场景。
 */
public interface IStandardAddressTagService {

    /**
     * 查询标签详情。
     *
     * @param id 标签ID
     * @return 标签详情
     *
     * 关键约束：主键必须存在。
     */
    StandardAddressTagVo queryById(Long id);

    /**
     * 分页查询标签列表。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     *
     * 关键约束：支持按名称、编码、颜色模糊/精确过滤。
     */
    TableDataInfo<StandardAddressTagVo> queryPageList(StandardAddressTagBo bo, PageQuery pageQuery);

    /**
     * 查询标签列表。
     *
     * @param bo 查询条件
     * @return 标签列表
     */
    List<StandardAddressTagVo> queryList(StandardAddressTagBo bo);

    /**
     * 新增标签。
     *
     * @param bo 新增参数
     * @return 是否成功
     *
     * 关键约束：标签名称在当前有效数据中唯一。
     * 异常：名称重复时抛出业务异常。
     * 副作用：写入标签表。
     */
    Boolean insertByBo(StandardAddressTagBo bo);

    /**
     * 修改标签。
     *
     * @param bo 修改参数
     * @return 是否成功
     *
     * 关键约束：标签名称在排除自身后保持唯一。
     * 异常：标签不存在或名称重复时抛出业务异常。
     * 副作用：更新标签表。
     */
    Boolean updateByBo(StandardAddressTagBo bo);

    /**
     * 删除标签。
     *
     * @param ids 标签ID集合
     * @param isValid 是否执行业务校验
     * @return 是否成功
     *
     * 关键约束：删除时需清理地址标签关联关系。
     * 副作用：逻辑删除标签并删除关联表数据。
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 查询指定地址的标签列表。
     *
     * @param standardAddressId 标准地址ID
     * @return 标签列表
     */
    List<StandardAddressTagVo> listTagsByStandardAddressId(String standardAddressId);

    /**
     * 查询指定地址集合的标签映射。
     *
     * @param standardAddressIds 标准地址ID集合
     * @return 标准地址ID到标签列表的映射
     */
    Map<String, List<StandardAddressTagVo>> mapTagsByStandardAddressIds(Collection<String> standardAddressIds);

    /**
     * 批量绑定标签。
     *
     * @param standardAddressIds 标准地址ID集合
     * @param tagIds 标签ID集合
     * @return 是否成功
     *
     * 关键约束：仅新增不存在的关联关系。
     * 异常：地址或标签不存在时抛出业务异常。
     * 副作用：写入地址标签关联表。
     */
    Boolean bindTagsToStandardAddresses(Collection<String> standardAddressIds, Collection<Long> tagIds);

    /**
     * 批量解绑标签。
     *
     * @param standardAddressIds 标准地址ID集合
     * @param tagIds 标签ID集合
     * @return 是否成功
     *
     * 副作用：删除地址标签关联表中命中的关系。
     */
    Boolean unbindTagsFromStandardAddresses(Collection<String> standardAddressIds, Collection<Long> tagIds);
}
