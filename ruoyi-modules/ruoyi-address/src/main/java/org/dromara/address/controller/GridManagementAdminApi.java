package org.dromara.address.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.dromara.address.domain.bo.GridManagementBo;
import org.dromara.address.domain.vo.GridManagementVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 网格管理模块接口基线。
 * 目的：在现有工程分层下固化网格组织、网格主资源、网格地址、客户与经理能力边界。
 * 入参/出参：统一复用仓库已有 `R`、`TableDataInfo`、`PageQuery`，并使用网格模块 BO/VO。
 * 关键约束：该接口只定义合同，不直接提供运行时实现；历史迁移与双向增量同步仅在接口语义中固化。
 * 异常与副作用：具体业务校验、导入回滚和同步副作用由后续实现层负责。
 */
@Validated
public interface GridManagementAdminApi {

    /**
     * 目的：查询网格组织树。
     * 入参：无。
     * 出参：组织树列表。
     * 关键约束：树结构需完整表达父子关系。
     * 异常与副作用：查询失败时返回业务异常，无写入副作用。
     */
    @PostMapping("/grid/org/tree")
    R<List<GridManagementVo.GridOrgTreeVo>> queryGridOrgTree();

    /**
     * 目的：查询网格组织详情。
     * 入参：组织主键。
     * 出参：组织详情。
     * 关键约束：主键不能为空。
     * 异常与副作用：组织不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/grid/org/{id}")
    R<GridManagementVo.GridOrgVo> getGridOrgInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：新增网格组织。
     * 入参：组织业务对象。
     * 出参：统一操作结果。
     * 关键约束：同级名称唯一，层级合法。
     * 异常与副作用：成功后会新增组织数据。
     */
    @PostMapping("/grid/org")
    R<Void> addGridOrg(@Valid @RequestBody GridManagementBo.GridOrgBo bo);

    /**
     * 目的：修改网格组织。
     * 入参：组织业务对象。
     * 出参：统一操作结果。
     * 关键约束：调整父级时需避免形成环形层级。
     * 异常与副作用：成功后会更新组织数据。
     */
    @PostMapping("/grid/org/update")
    R<Void> editGridOrg(@Valid @RequestBody GridManagementBo.GridOrgBo bo);

    /**
     * 目的：删除网格组织。
     * 入参：组织主键。
     * 出参：统一操作结果。
     * 关键约束：存在子组织或资源关联时需校验。
     * 异常与副作用：成功后会删除组织数据。
     */
    @PostMapping("/grid/org/remove/{id}")
    R<Void> removeGridOrg(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：导入网格组织调整数据。
     * 入参：导入文件与是否允许更新。
     * 出参：导入结果描述。
     * 关键约束：需支持历史迁移与重复导入校验。
     * 异常与副作用：成功后会写入组织数据并记录导入结果。
     */
    @PostMapping("/grid/org/importData")
    R<String> importGridOrgData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception;

    /**
     * 目的：导出网格组织数据。
     * 入参：组织筛选条件与响应流。
     * 出参：Excel 文件流。
     * 关键约束：导出字段需覆盖组织层级表达。
     * 异常与副作用：成功后会向响应流写出文件内容。
     */
    @PostMapping("/grid/org/export")
    void exportGridOrgData(GridManagementBo.GridOrgBo bo, HttpServletResponse response);

    /**
     * 目的：分页查询网格主资源。
     * 入参：网格筛选条件与分页参数。
     * 出参：网格分页列表。
     * 关键约束：网格编码应作为核心唯一键使用。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/grid/info/list")
    TableDataInfo<GridManagementVo.GridVo> listGrids(GridManagementBo.GridBo bo, PageQuery pageQuery);

    /**
     * 目的：查询网格详情。
     * 入参：网格主键。
     * 出参：网格详情。
     * 关键约束：需返回组织与经理关联信息。
     * 异常与副作用：网格不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/grid/info/{id}")
    R<GridManagementVo.GridVo> getGridInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：新增网格。
     * 入参：网格业务对象。
     * 出参：统一操作结果。
     * 关键约束：网格编码唯一，组织和经理归属合法。
     * 异常与副作用：成功后会新增网格数据。
     */
    @PostMapping("/grid/info")
    R<Void> addGrid(@Valid @RequestBody GridManagementBo.GridBo bo);

    /**
     * 目的：修改网格。
     * 入参：网格业务对象。
     * 出参：统一操作结果。
     * 关键约束：需兼顾历史数据迁移与双向增量同步能力。
     * 异常与副作用：成功后会更新网格数据并触发必要同步。
     */
    @PostMapping("/grid/info/update")
    R<Void> editGrid(@Valid @RequestBody GridManagementBo.GridBo bo);

    /**
     * 目的：删除网格。
     * 入参：网格主键集合。
     * 出参：统一操作结果。
     * 关键约束：地址、客户、经理等关联关系需先校验或迁移。
     * 异常与副作用：成功后会删除网格数据。
     */
    @PostMapping("/grid/info/remove/{ids}")
    R<Void> removeGrid(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids);

    /**
     * 目的：导出网格数据。
     * 入参：网格筛选条件与响应流。
     * 出参：Excel 文件流。
     * 关键约束：导出字段与列表字段保持一致。
     * 异常与副作用：成功后会向响应流写出文件内容。
     */
    @PostMapping("/grid/info/export")
    void exportGridData(GridManagementBo.GridBo bo, HttpServletResponse response);

    /**
     * 目的：分页查询网格地址关联关系。
     * 入参：关联筛选条件与分页参数。
     * 出参：地址关联网格分页列表。
     * 关键约束：地址口径需与标准地址模块主数据一致。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/grid/address/list")
    TableDataInfo<GridManagementVo.GridAddressRelationVo> listGridAddressRelations(GridManagementBo.GridAddressRelationBo bo, PageQuery pageQuery);

    /**
     * 目的：批量绑定地址到网格。
     * 入参：网格地址关联业务对象。
     * 出参：统一操作结果。
     * 关键约束：需明确已有归属覆盖策略和客户自动归属策略。
     * 异常与副作用：成功后会更新地址归属并触发关联客户迁移。
     */
    @PostMapping("/grid/address/bind")
    R<Void> bindGridAddress(@Valid @RequestBody GridManagementBo.GridAddressRelationBo bo);

    /**
     * 目的：导入网格地址关联数据。
     * 入参：导入文件与是否允许更新。
     * 出参：导入结果描述。
     * 关键约束：需支持历史迁移与双向增量同步基线。
     * 异常与副作用：成功后会写入地址归属关系并记录导入结果。
     */
    @PostMapping("/grid/address/importData")
    R<String> importGridAddressData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception;

    /**
     * 目的：导出网格地址导入失败数据。
     * 入参：导入记录主键与响应流。
     * 出参：失败明细文件流。
     * 关键约束：失败明细字段应与导入模板保持一致。
     * 异常与副作用：成功后会向响应流写出失败明细文件。
     */
    @PostMapping("/grid/address/exportFail/{recordId}")
    void exportGridAddressFailData(@NotNull(message = "导入记录不能为空") @PathVariable Long recordId, HttpServletResponse response);

    /**
     * 目的：分页查询网格客户。
     * 入参：客户筛选条件与分页参数。
     * 出参：网格客户分页列表。
     * 关键约束：客户与地址归属关系需保持一致。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/grid/customer/list")
    TableDataInfo<GridManagementVo.GridCustomerVo> listGridCustomers(GridManagementBo.GridCustomerBo bo, PageQuery pageQuery);

    /**
     * 目的：批量绑定客户到网格。
     * 入参：客户业务对象。
     * 出参：统一操作结果。
     * 关键约束：重复绑定需幂等，跨组织绑定需校验。
     * 异常与副作用：成功后会更新客户网格归属。
     */
    @PostMapping("/grid/customer/bind")
    R<Void> bindGridCustomer(@Valid @RequestBody GridManagementBo.GridCustomerBo bo);

    /**
     * 目的：分页查询网格经理。
     * 入参：经理筛选条件与分页参数。
     * 出参：网格经理分页列表。
     * 关键约束：经理归属组织和网格绑定关系要可追溯。
     * 异常与副作用：查询条件非法时返回业务异常，无写入副作用。
     */
    @PostMapping("/grid/manager/list")
    TableDataInfo<GridManagementVo.GridManagerVo> listGridManagers(GridManagementBo.GridManagerBo bo, PageQuery pageQuery);

    /**
     * 目的：查询网格经理详情。
     * 入参：经理主键。
     * 出参：网格经理详情。
     * 关键约束：详情需返回关联网格集合。
     * 异常与副作用：经理不存在时返回业务异常，无写入副作用。
     */
    @PostMapping("/grid/manager/{id}")
    R<GridManagementVo.GridManagerVo> getGridManagerInfo(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：新增网格经理。
     * 入参：网格经理业务对象。
     * 出参：统一操作结果。
     * 关键约束：组织归属与手机号等基础信息由实现层校验。
     * 异常与副作用：成功后会新增网格经理及其关联关系。
     */
    @PostMapping("/grid/manager")
    R<Void> addGridManager(@Valid @RequestBody GridManagementBo.GridManagerBo bo);

    /**
     * 目的：修改网格经理。
     * 入参：网格经理业务对象。
     * 出参：统一操作结果。
     * 关键约束：跨组织调整和重绑定网格需统一校验。
     * 异常与副作用：成功后会更新网格经理及其关联关系。
     */
    @PostMapping("/grid/manager/update")
    R<Void> editGridManager(@Valid @RequestBody GridManagementBo.GridManagerBo bo);

    /**
     * 目的：删除网格经理。
     * 入参：经理主键。
     * 出参：统一操作结果。
     * 关键约束：删除前需处理其关联网格。
     * 异常与副作用：成功后会删除网格经理及其关联关系。
     */
    @PostMapping("/grid/manager/remove/{id}")
    R<Void> removeGridManager(@NotNull(message = "主键不能为空") @PathVariable Long id);

    /**
     * 目的：给网格经理绑定或重绑网格。
     * 入参：经理主键与网格经理业务对象。
     * 出参：统一操作结果。
     * 关键约束：重复提交需幂等，跨组织绑定需校验。
     * 异常与副作用：成功后会更新经理与网格关联关系。
     */
    @PostMapping("/grid/manager/bindGrid/{id}")
    R<Void> bindGridManagerGrids(@NotNull(message = "主键不能为空") @PathVariable Long id,
                                 @Valid @RequestBody GridManagementBo.GridManagerBo bo);

    /**
     * 目的：导出网格经理数据。
     * 入参：经理筛选条件与响应流。
     * 出参：Excel 文件流。
     * 关键约束：需覆盖组织、电话和关联网格信息。
     * 异常与副作用：成功后会向响应流写出文件内容。
     */
    @PostMapping("/grid/manager/export")
    void exportGridManagerData(GridManagementBo.GridManagerBo bo, HttpServletResponse response);
}
