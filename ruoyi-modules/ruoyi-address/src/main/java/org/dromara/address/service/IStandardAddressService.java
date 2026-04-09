package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressSplitItemBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.common.excel.core.DropDownOptions;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
     * 查询标准地址级别选项。
     *
     * @return 标准地址级别选项列表
     *
     * 关键约束：选项必须与线上 `segm_addr_type` 保持一致，供前端筛选和展示统一复用。
     * 异常与副作用：无写入副作用。
     */
    List<StandardAddressAdminVo.LevelOptionVo> listStandardAddressLevelOptions();

    /**
     * 查询标准地址编辑页聚合字典。
     *
     * @return 编辑页聚合字典
     *
     * 关键约束：字典值必须直接来源于线上 `pub_restriction`，按标准地址表单字段固定分组返回。
     * 异常与副作用：无写入副作用。
     */
    StandardAddressAdminVo.FormOptionsVo listStandardAddressFormOptions();

    /**
     * 查询标准地址编辑页管理站候选。
     *
     * @param bo 管理站候选查询条件
     * @return 管理站候选列表
     *
     * 关键约束：候选必须直接来源于线上 `spc_station`，并显式按 `manageType` 区分维修/安装/营业。
     * 异常与副作用：无写入副作用。
     */
    List<StandardAddressAdminVo.StationOptionVo> listStandardAddressStationOptions(StandardAddressAdminBo.StationOptionQueryBo bo);

    /**
     * 查询标准地址导入模板下拉选项。
     *
     * @return Excel 下拉选项集合
     */
    List<DropDownOptions> listStandardAddressImportTemplateOptions();

    /**
     * 查询标准地址列表（不分页）。
     *
     * @param bo 查询条件
     * @return 标准地址列表
     */
    List<StandardAddressVo> queryStandardAddressList(StandardAddressBo bo);

    /**
     * 批量查询标准地址名称映射。
     *
     * @param segmIds 标准地址主键集合
     * @return `segmId -> standName` 映射
     *
     * 关键约束：必须以批量方式补齐名称，避免列表类场景逐条回查标准地址详情导致 N+1 问题。
     * 异常与副作用：无写入副作用。
     */
    Map<String, String> listStandardAddressStandNameMapBySegmIds(Collection<String> segmIds);

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
     * @param sourceSegmIds 源标准地址 `segmId` 集合
     * @param targetSegmId  目标标准地址 `segmId`
     * @return 结果
     */
    Boolean mergeStandardAddresses(List<String> sourceSegmIds, String targetSegmId);

    /**
     * 拆分地址
     *
     * @param sourceSegmId 源标准地址 `segmId`
     * @param splitItems 拆分项集合
     * @return 结果
     */
    Boolean splitStandardAddress(String sourceSegmId, List<StandardAddressSplitItemBo> splitItems);

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
     * @return 导入结果摘要
     */
    StandardAddressImportResultVo importStandardAddressData(List<StandardAddressImportVo> list, Boolean updateSupport, String operName, String fileName);
}
