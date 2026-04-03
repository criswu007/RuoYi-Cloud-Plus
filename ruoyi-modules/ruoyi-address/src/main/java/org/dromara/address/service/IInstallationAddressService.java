package org.dromara.address.service;

import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

public interface IInstallationAddressService {

    /**
     * 查询安装地址详情。
     *
     * @param setAddrId 安装地址主键
     * @return 安装地址详情
     */
    InstallationAddressVo queryById(String setAddrId);

    /**
     * 分页查询安装地址。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<InstallationAddressVo> queryPageList(InstallationAddressBo bo, PageQuery pageQuery);

    /**
     * 查询安装地址列表（不分页）。
     *
     * @param bo 查询条件
     * @return 安装地址列表
     */
    List<InstallationAddressVo> queryList(InstallationAddressBo bo);

    /**
     * 新增安装地址。
     *
     * @param bo 新增参数
     * @return 是否成功
     */
    Boolean insertByBo(InstallationAddressBo bo);

    /**
     * 修改安装地址。
     *
     * @param bo 修改参数
     * @return 是否成功
     */
    Boolean updateByBo(InstallationAddressBo bo);

    /**
     * 删除安装地址。
     *
     * @param setAddrIds 主键集合
     * @param isValid 是否校验业务逻辑
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<String> setAddrIds, Boolean isValid);
}
