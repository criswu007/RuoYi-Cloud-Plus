package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

public interface IStandardAddressService {

    /**
     * 根据线上 `segmId` 查询标准地址详情。
     *
     * @param segmId 标准地址字符串主键
     * @return 标准地址详情
     */
    StandardAddressVo getStandardAddressBySegmId(String segmId);

    /**
     * 根据旧模块主键查询标准地址详情。
     *
     * @param id 旧模块数值主键
     * @return 标准地址详情
     */
    @Deprecated
    default StandardAddressVo getStandardAddressById(Long id) {
        return id == null ? null : getStandardAddressBySegmId(String.valueOf(id));
    }

    /**
     * 分页查询标准地址。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<StandardAddressVo> queryStandardAddressPageList(StandardAddressBo bo, PageQuery pageQuery);

    /**
     * 查询标准地址列表（不分页）。
     *
     * @param bo 查询条件
     * @return 标准地址列表
     */
    List<StandardAddressVo> queryStandardAddressList(StandardAddressBo bo);

    /**
     * 新增标准地址。
     *
     * @param bo 新增参数
     * @return 是否成功
     */
    Boolean addStandardAddress(StandardAddressBo bo);

    /**
     * 修改标准地址。
     *
     * @param bo 修改参数
     * @return 是否成功
     */
    Boolean updateStandardAddress(StandardAddressBo bo);

    /**
     * 删除标准地址（带业务校验）。
     *
     * @param standardAddressIds 标准地址ID集合
     * @param confirm 是否确认删除（存在安装地址需二次确认）
     * @return 是否成功
     */
    Boolean deleteStandardAddresses(Collection<String> standardAddressIds, boolean confirm);

    /**
     * 合并地址
     *
     * @param sourceStandardAddressIds 源标准地址ID集合
     * @param targetStandardAddressId  目标标准地址ID
     * @return 结果
     */
    Boolean mergeStandardAddresses(List<Long> sourceStandardAddressIds, Long targetStandardAddressId);

    /**
     * 拆分地址
     *
     * @param sourceStandardAddressId 源标准地址ID
     * @param newAddresses 新地址集合
     * @return 结果
     */
    Boolean splitStandardAddress(Long sourceStandardAddressId, List<StandardAddressBo> newAddresses);

    /**
     * 批量预览新增下级标准地址结果。
     *
     * @param bo 批量新增参数
     * @return 预览结果
     */
    List<StandardAddressAdminVo.BatchPreviewVo> previewStandardAddressChildren(StandardAddressBatchAddBo bo);

    /**
     * 批量添加子地址
     *
     * @param bo 批量添加参数
     * @return 结果
     */
    Boolean batchAddStandardAddressChildren(StandardAddressBatchAddBo bo);

    /**
     * 导入标准地址数据。
     *
     * @param list 数据列表
     * @param updateSupport 是否更新支持（预留）
     * @param operName 操作人
     * @param fileName 文件名
     * @return 导入结果描述
     */
    String importStandardAddressData(List<StandardAddressImportVo> list, Boolean updateSupport, String operName, String fileName);
}
