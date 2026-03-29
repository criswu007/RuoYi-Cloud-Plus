package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressAttributeBo;
import org.dromara.address.domain.vo.StandardAddressAttributeVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

public interface IStandardAddressAttributeService {

    /**
     * 查询地址属性详情。
     *
     * @param id 主键ID
     * @return 详情
     */
    StandardAddressAttributeVo queryById(Long id);

    /**
     * 分页查询地址属性。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<StandardAddressAttributeVo> queryPageList(StandardAddressAttributeBo bo, PageQuery pageQuery);

    /**
     * 查询地址属性列表（不分页）。
     *
     * @param bo 查询条件
     * @return 属性列表
     */
    List<StandardAddressAttributeVo> queryList(StandardAddressAttributeBo bo);

    /**
     * 新增地址属性。
     *
     * @param bo 新增参数
     * @return 是否成功
     */
    Boolean insertByBo(StandardAddressAttributeBo bo);

    /**
     * 修改地址属性。
     *
     * @param bo 修改参数
     * @return 是否成功
     */
    Boolean updateByBo(StandardAddressAttributeBo bo);

    /**
     * 删除地址属性。
     *
     * @param ids 主键集合
     * @param isValid 是否校验业务逻辑
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
