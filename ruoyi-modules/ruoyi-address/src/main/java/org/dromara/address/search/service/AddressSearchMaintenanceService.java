package org.dromara.address.search.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.AddrSetSegm;
import org.dromara.address.domain.AddressSearchRepairTask;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.AddrSetSegmMapper;
import org.dromara.address.mapper.AddressSearchRepairTaskMapper;
import org.dromara.address.search.support.AddressSearchMaintenanceProgressListener;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 地址搜索维护服务。
 * <p>
 * 目的：统一承载标准地址/安装地址 ES 索引的全量重建与 repair 执行能力，作为后续运维入口的核心编排服务。
 * 入参/出参：输入重建目标或 repair 任务主键，输出通过异常表达失败；成功场景不返回业务数据。
 * 关键约束：维护能力必须复用当前“业务上立即一致”和 repair 表口径，不得引入新的读写降级语义；全量重建期间不得提前切换别名。
 * 异常与副作用：会访问 MySQL 批量扫描主事实表、访问 ES 创建索引/写入文档/切换别名，并更新 repair 任务状态。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AddressSearchMaintenanceService {

    private static final String DELETE_STATE_ACTIVE = "0";
    private static final String ENTITY_TYPE_STANDARD = "STANDARD";
    private static final String ENTITY_TYPE_INSTALLATION = "INSTALLATION";
    private static final String REPAIR_STATUS_SUCCESS = "SUCCESS";
    private static final String REPAIR_STATUS_FAILED = "FAILED";

    private final StandardAddressSearchGateway standardAddressSearchGateway;
    private final InstallationAddressSearchGateway installationAddressSearchGateway;
    private final AddressSearchRepairTaskMapper addressSearchRepairTaskMapper;
    private final AddrSegmMapper addrSegmMapper;
    private final AddrSetSegmMapper addrSetSegmMapper;
    private final AddressSearchProperties addressSearchProperties;

    /**
     * 目的：按主键升序批量重建标准地址搜索索引，并在完成后原子切换读别名。
     * 入参：无。
     * 出参：无。
     * 关键约束：只重建未删除标准地址；切换别名前必须完成全部批次写入。
     * 异常与副作用：会扫描 `ADDR_SEGM`、写 ES 新索引并切换 ES 别名。
     */
    public void rebuildStandardIndex() {
        rebuildStandardIndex(AddressSearchMaintenanceProgressListener.NO_OP);
    }

    /**
     * 目的：按主键升序批量重建标准地址搜索索引，并把执行进度通过监听器回传给任务编排层。
     * 入参：重建进度监听器。
     * 出参：无。
     * 关键约束：监听器只接收累计总量和累计处理量；切换别名前必须完成全部批次写入。
     * 异常与副作用：会扫描 `ADDR_SEGM`、写 ES 新索引、切换 ES 别名并触发监听器回调。
     *
     * @param listener 运维进度监听器
     */
    public void rebuildStandardIndex(AddressSearchMaintenanceProgressListener listener) {
        AddressSearchMaintenanceProgressListener actualListener = listener == null ? AddressSearchMaintenanceProgressListener.NO_OP : listener;
        Long totalCount = addrSegmMapper.selectCount(Wrappers.<AddrSegm>lambdaQuery()
            .eq(AddrSegm::getDeleteState, DELETE_STATE_ACTIVE));
        actualListener.onTotalResolved(totalCount == null ? 0L : totalCount);
        String rebuildIndex = standardAddressSearchGateway.prepareRebuildIndex();
        actualListener.onPhysicalIndexPrepared(rebuildIndex);
        long processedCount = 0L;
        String lastSegmId = null;
        while (true) {
            Page<AddrSegm> page = new Page<>(1, resolveRebuildBatchSize(), false);
            Page<AddrSegm> batchPage = addrSegmMapper.selectPage(page, Wrappers.<AddrSegm>lambdaQuery()
                .eq(AddrSegm::getDeleteState, DELETE_STATE_ACTIVE)
                .gt(StringUtils.isNotBlank(lastSegmId), AddrSegm::getSegmId, lastSegmId)
                .orderByAsc(AddrSegm::getSegmId));
            List<AddrSegm> records = batchPage == null || batchPage.getRecords() == null
                ? Collections.emptyList()
                : batchPage.getRecords();
            if (records.isEmpty()) {
                break;
            }
            requireSuccess(standardAddressSearchGateway.bulkIndex(rebuildIndex, records), "标准地址重建批量写入失败");
            processedCount += records.size();
            actualListener.onBatchCompleted(processedCount);
            lastSegmId = records.get(records.size() - 1).getSegmId();
        }
        standardAddressSearchGateway.switchAlias(rebuildIndex);
        actualListener.onAliasSwitched();
    }

    /**
     * 目的：按主键升序批量重建安装地址搜索索引，并在完成后原子切换读别名。
     * 入参：无。
     * 出参：无。
     * 关键约束：只重建未删除安装地址；切换别名前必须完成全部批次写入。
     * 异常与副作用：会扫描 `ADDR_SET_SEGM`、写 ES 新索引并切换 ES 别名。
     */
    public void rebuildInstallationIndex() {
        rebuildInstallationIndex(AddressSearchMaintenanceProgressListener.NO_OP);
    }

    /**
     * 目的：按主键升序批量重建安装地址搜索索引，并把执行进度通过监听器回传给任务编排层。
     * 入参：重建进度监听器。
     * 出参：无。
     * 关键约束：监听器只接收累计总量和累计处理量；切换别名前必须完成全部批次写入。
     * 异常与副作用：会扫描 `ADDR_SET_SEGM`、写 ES 新索引、切换 ES 别名并触发监听器回调。
     *
     * @param listener 运维进度监听器
     */
    public void rebuildInstallationIndex(AddressSearchMaintenanceProgressListener listener) {
        AddressSearchMaintenanceProgressListener actualListener = listener == null ? AddressSearchMaintenanceProgressListener.NO_OP : listener;
        Long totalCount = addrSetSegmMapper.selectCount(Wrappers.<AddrSetSegm>lambdaQuery()
            .eq(AddrSetSegm::getDeleteState, DELETE_STATE_ACTIVE));
        actualListener.onTotalResolved(totalCount == null ? 0L : totalCount);
        String rebuildIndex = installationAddressSearchGateway.prepareRebuildIndex();
        actualListener.onPhysicalIndexPrepared(rebuildIndex);
        long processedCount = 0L;
        String lastSetAddrId = null;
        while (true) {
            Page<AddrSetSegm> page = new Page<>(1, resolveRebuildBatchSize(), false);
            Page<AddrSetSegm> batchPage = addrSetSegmMapper.selectPage(page, Wrappers.<AddrSetSegm>lambdaQuery()
                .eq(AddrSetSegm::getDeleteState, DELETE_STATE_ACTIVE)
                .gt(StringUtils.isNotBlank(lastSetAddrId), AddrSetSegm::getSetAddrId, lastSetAddrId)
                .orderByAsc(AddrSetSegm::getSetAddrId));
            List<AddrSetSegm> records = batchPage == null || batchPage.getRecords() == null
                ? Collections.emptyList()
                : batchPage.getRecords();
            if (records.isEmpty()) {
                break;
            }
            requireSuccess(installationAddressSearchGateway.bulkIndex(rebuildIndex, records), "安装地址重建批量写入失败");
            processedCount += records.size();
            actualListener.onBatchCompleted(processedCount);
            lastSetAddrId = records.get(records.size() - 1).getSetAddrId();
        }
        installationAddressSearchGateway.switchAlias(rebuildIndex);
        actualListener.onAliasSwitched();
    }

    /**
     * 目的：按 repair 任务主键重放单条待修复任务。
     * 入参：repair 任务主键。
     * 出参：无。
     * 关键约束：只允许根据任务中的实体类型路由到对应索引网关；成功后回写 `SUCCESS`，失败后回写 `FAILED` 并递增重试次数。
     * 异常与副作用：会读取并更新 `address_search_repair_task`，并访问 ES 执行修复。
     *
     * @param taskId repair 任务主键
     */
    public void executeRepairTask(Long taskId) {
        AddressSearchRepairTask task = addressSearchRepairTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("repair任务不存在: " + taskId);
        }
        try {
            boolean repaired = switch (task.getEntityType()) {
                case ENTITY_TYPE_STANDARD -> standardAddressSearchGateway.repair(task);
                case ENTITY_TYPE_INSTALLATION -> installationAddressSearchGateway.repair(task);
                default -> throw new IllegalArgumentException("不支持的repair实体类型: " + task.getEntityType());
            };
            requireSuccess(repaired, "repair执行返回失败");
            markRepairStatus(task, REPAIR_STATUS_SUCCESS, safeRetryCount(task), new Date());
        } catch (Exception ex) {
            markRepairStatus(task, REPAIR_STATUS_FAILED, safeRetryCount(task) + 1, new Date());
            throw ex instanceof RuntimeException runtimeException ? runtimeException : new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private int resolveRebuildBatchSize() {
        Integer rebuildBatchSize = addressSearchProperties.getRebuildBatchSize();
        return rebuildBatchSize == null || rebuildBatchSize <= 0 ? 500 : rebuildBatchSize;
    }

    private int safeRetryCount(AddressSearchRepairTask task) {
        return task == null || task.getRetryCount() == null ? 0 : task.getRetryCount();
    }

    private void markRepairStatus(AddressSearchRepairTask task, String status, int retryCount, Date updateTime) {
        AddressSearchRepairTask updatedTask = new AddressSearchRepairTask();
        updatedTask.setId(task.getId());
        updatedTask.setStatus(status);
        updatedTask.setRetryCount(retryCount);
        updatedTask.setUpdatedTime(updateTime);
        addressSearchRepairTaskMapper.updateById(updatedTask);
    }

    private void requireSuccess(boolean success, String errorMessage) {
        if (!success) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
