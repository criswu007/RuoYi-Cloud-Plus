package org.dromara.address.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressImportDetailBo;
import org.dromara.address.domain.bo.StandardAddressMergeBo;
import org.dromara.address.domain.bo.StandardAddressMonitorRecordBo;
import org.dromara.address.domain.bo.StandardAddressMonitorRuleBo;
import org.dromara.address.domain.bo.StandardAddressOperationLogBo;
import org.dromara.address.domain.bo.StandardAddressSelectionRoomBo;
import org.dromara.address.domain.bo.StandardAddressSplitBo;
import org.dromara.address.domain.bo.StandardAddressTagBindBo;
import org.dromara.address.domain.bo.StandardAddressTagBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.domain.vo.StandardAddressImportResultVo;
import org.dromara.address.domain.vo.StandardAddressImportDetailVo;
import org.dromara.address.domain.vo.StandardAddressMonitorRecordVo;
import org.dromara.address.domain.vo.StandardAddressMonitorRuleVo;
import org.dromara.address.domain.vo.StandardAddressMonitorTaskSummaryVo;
import org.dromara.address.domain.vo.StandardAddressOperationLogVo;
import org.dromara.address.domain.vo.StandardAddressSelectionResultVo;
import org.dromara.address.domain.vo.StandardAddressTagVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 标准地址管理 API 支撑基类。
 * 目的：为尚未进入本轮实现的接口提供统一占位实现，确保标准地址核心链路可先行编码与验证。
 * 入参/出参：完全遵循 {@link StandardAddressAdminApi} 合同，不改变控制器暴露的协议结构。
 * 关键约束：只有已经在子类中显式覆写的方法才视为已实现，其余方法统一抛出未实现异常。
 * 异常与副作用：默认抛出 {@link UnsupportedOperationException}，无数据写入副作用。
 */
public abstract class StandardAddressAdminApiSupport extends BaseController implements StandardAddressAdminApi {

    protected UnsupportedOperationException unsupportedOperation() {
        return new UnsupportedOperationException("该接口尚未纳入当前标准地址模块实现范围");
    }

    @Override
    public TableDataInfo<StandardAddressVo> listStandardAddresses(StandardAddressBo bo, PageQuery pageQuery) {
        throw unsupportedOperation();
    }

    @Override
    public R<List<StandardAddressAdminVo.LevelOptionVo>> listStandardAddressLevelOptions() {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressAdminVo.FormOptionsVo> listStandardAddressFormOptions() {
        throw unsupportedOperation();
    }

    @Override
    public R<List<StandardAddressAdminVo.StationOptionVo>> listStandardAddressStationOptions(StandardAddressAdminBo.StationOptionQueryBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressVo> getStandardAddressInfo(String segmId) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> addStandardAddress(StandardAddressBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> editStandardAddress(StandardAddressBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> removeStandardAddresses(String[] segmIds, boolean confirm) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> mergeStandardAddresses(StandardAddressMergeBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> splitStandardAddress(StandardAddressSplitBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<List<StandardAddressAdminVo.BatchPreviewVo>> previewStandardAddressChildren(StandardAddressBatchAddBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> batchAddStandardAddressChildren(StandardAddressBatchAddBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public void exportStandardAddresses(StandardAddressBo bo, HttpServletResponse response) {
        throw unsupportedOperation();
    }

    @Override
    public void downloadStandardAddressImportTemplate(HttpServletResponse response) throws Exception {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressImportResultVo> importStandardAddressData(MultipartFile file, boolean updateSupport) throws Exception {
        throw unsupportedOperation();
    }

    @Override
    public TableDataInfo<StandardAddressTagVo> listTags(StandardAddressTagBo bo, PageQuery pageQuery) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressTagVo> getTagInfo(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> addTag(StandardAddressTagBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> editTag(StandardAddressTagBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> removeTag(Long[] ids) {
        throw unsupportedOperation();
    }

    @Override
    public R<List<StandardAddressTagVo>> listTagsByStandardAddressId(String standardAddressId) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> bindTagsToStandardAddresses(StandardAddressTagBindBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> unbindTagsFromStandardAddresses(StandardAddressTagBindBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public TableDataInfo<StandardAddressImportDetailVo> listImportRecords(StandardAddressImportDetailBo bo, PageQuery pageQuery) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressImportDetailVo> getImportRecordInfo(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public TableDataInfo<StandardAddressOperationLogVo> listOperationLogs(StandardAddressOperationLogBo bo, PageQuery pageQuery) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressOperationLogVo> getOperationLogInfo(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public TableDataInfo<InstallationAddressVo> listInstallationAddresses(InstallationAddressBo bo, PageQuery pageQuery) {
        throw unsupportedOperation();
    }

    @Override
    public R<InstallationAddressVo> getInstallationAddressInfo(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> addInstallationAddress(InstallationAddressBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> editInstallationAddress(InstallationAddressBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> removeInstallationAddress(Long[] ids) {
        throw unsupportedOperation();
    }

    @Override
    public TableDataInfo<StandardAddressMonitorRuleVo> listMonitorRules(StandardAddressMonitorRuleBo bo, PageQuery pageQuery) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressMonitorRuleVo> getMonitorRuleInfo(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> addMonitorRule(StandardAddressMonitorRuleBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> editMonitorRule(StandardAddressMonitorRuleBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> removeMonitorRule(Long[] ids) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> enableMonitorRule(Long[] ids) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> disableMonitorRule(Long[] ids) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressMonitorTaskSummaryVo> summaryMonitorTasks() {
        throw unsupportedOperation();
    }

    @Override
    public R<Integer> executeMonitorTask() {
        throw unsupportedOperation();
    }

    @Override
    public TableDataInfo<StandardAddressAdminVo.MonitorTaskVo> listMonitorTasks(StandardAddressAdminBo.MonitorTaskBo bo, PageQuery pageQuery) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressAdminVo.MonitorTaskVo> getMonitorTaskInfo(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public TableDataInfo<StandardAddressAdminVo.MonitorTaskRunLogVo> listMonitorTaskRunLogs(Long taskId, PageQuery pageQuery) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> addMonitorTask(StandardAddressAdminBo.MonitorTaskBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> editMonitorTask(StandardAddressAdminBo.MonitorTaskBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> rerunMonitorTask(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> pauseMonitorTask(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> terminateMonitorTask(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public TableDataInfo<StandardAddressMonitorRecordVo> listMonitorRecords(StandardAddressMonitorRecordBo bo, PageQuery pageQuery) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressMonitorRecordVo> getMonitorRecordInfo(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> editMonitorRecord(StandardAddressMonitorRecordBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> removeMonitorRecord(Long[] ids) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> ignoreMonitorRecord(Long[] ids) {
        throw unsupportedOperation();
    }

    @Override
    public TableDataInfo<StandardAddressAdminVo.WorkOrderVo> listWorkOrders(StandardAddressAdminBo.WorkOrderBo bo, PageQuery pageQuery) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressAdminVo.WorkOrderVo> getWorkOrderInfo(Long id) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> createWorkOrder(StandardAddressAdminBo.WorkOrderBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> correctWorkOrder(Long id, StandardAddressAdminBo.WorkOrderBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> rejectWorkOrder(Long id, StandardAddressAdminBo.WorkOrderBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<List<StandardAddressVo>> searchStandardAddresses(String keyword, Integer levelMax, Integer limit) {
        throw unsupportedOperation();
    }

    @Override
    public R<List<StandardAddressAdminVo.SelectionResultVo>> mapSearchStandardAddresses(StandardAddressAdminBo.SelectionMapSearchBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<List<StandardAddressAdminVo.SelectionRoomVo>> listSelectionRooms(Long standardAddressId) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressAdminVo.SelectionPreviewVo> previewStandardAddress(Long standardAddressId) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressAdminVo.SelectionPreviewVo> previewInstallationAddress(StandardAddressAdminBo.SelectionPreviewBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<StandardAddressSelectionResultVo> createRoomStandardAddress(StandardAddressSelectionRoomBo bo) {
        throw unsupportedOperation();
    }

    @Override
    public R<Void> generateInstallationAddress(StandardAddressAdminBo.SelectionPreviewBo bo) {
        throw unsupportedOperation();
    }
}
