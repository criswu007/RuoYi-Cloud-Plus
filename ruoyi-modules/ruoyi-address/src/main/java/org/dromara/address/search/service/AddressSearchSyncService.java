package org.dromara.address.search.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddressSearchRepairTask;
import org.dromara.address.domain.AddressSearchSyncLog;
import org.dromara.address.mapper.AddressSearchRepairTaskMapper;
import org.dromara.address.mapper.AddressSearchSyncLogMapper;
import org.dromara.address.search.document.InstallationAddressSearchDocument;
import org.dromara.address.search.document.StandardAddressSearchDocument;
import org.dromara.address.search.support.CheckedBooleanSupplier;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 地址搜索双写同步服务。
 * <p>
 * 目的：统一编排标准地址与安装地址“先 ES、后 MySQL”的双写模板，并在数据库失败时执行同步补偿与 repair 入队。
 * 关键约束：遵循当前评估口径中的“业务上立即一致”与“ES 不可用时数据库写入也必须失败”；补偿成功时不创建 repair 任务。
 * 异常与副作用：会访问 ES、写入同步日志与 repair 表；当 ES 或 DB 任一阶段失败时会抛出异常并中断业务返回。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AddressSearchSyncService {

    private static final String ENTITY_TYPE_STANDARD = "STANDARD";
    private static final String ENTITY_TYPE_INSTALLATION = "INSTALLATION";
    private static final String BUSINESS_TYPE_CREATE = "CREATE";
    private static final String BUSINESS_TYPE_UPDATE = "UPDATE";
    private static final String BUSINESS_TYPE_DELETE = "DELETE";
    private static final String PHASE_ES_WRITE = "ES_WRITE";
    private static final String PHASE_DB_WRITE = "DB_WRITE";
    private static final String PHASE_ES_COMPENSATE = "ES_COMPENSATE";
    private static final String REPAIR_ACTION_DELETE_DOC = "DELETE_DOC";
    private static final String REPAIR_ACTION_UPSERT_DOC = "UPSERT_DOC";
    private static final String SUCCESS_FLAG_YES = "Y";
    private static final String SUCCESS_FLAG_NO = "N";
    private static final String REPAIR_STATUS_PENDING = "PENDING";

    private final StandardAddressSearchGateway standardAddressSearchGateway;
    private final InstallationAddressSearchGateway installationAddressSearchGateway;
    private final AddressSearchSyncLogMapper addressSearchSyncLogMapper;
    private final AddressSearchRepairTaskMapper addressSearchRepairTaskMapper;
    private final AddressSearchProperties addressSearchProperties;

    /**
     * 目的：执行标准地址新增的 ES 与数据库双写。
     * 入参：新增后的标准地址文档与数据库写动作。
     * 出参：数据库写动作最终结果。
     * 关键约束：写开关关闭时仅执行数据库动作；写开关开启时必须先写 ES，再写数据库。
     * 异常与副作用：会写 ES、同步日志与 repair 表；任一阶段失败都会抛出异常。
     *
     * @param document 新增后的标准地址搜索文档
     * @param dbAction 数据库写动作
     * @return 数据库动作是否成功
     */
    public boolean syncStandardCreate(StandardAddressSearchDocument document, CheckedBooleanSupplier dbAction) {
        return syncSingleDocument(
            isStandardWriteEnabled(),
            BUSINESS_TYPE_CREATE,
            ENTITY_TYPE_STANDARD,
            document == null ? null : document.getSegmId(),
            () -> standardAddressSearchGateway.upsert(document),
            dbAction,
            () -> standardAddressSearchGateway.deleteByIds(resolveSingleIdList(document == null ? null : document.getSegmId())),
            REPAIR_ACTION_DELETE_DOC,
            document
        );
    }

    /**
     * 目的：执行标准地址修改的 ES 与数据库双写。
     * 入参：修改前文档、修改后文档与数据库写动作。
     * 出参：数据库写动作最终结果。
     * 关键约束：数据库失败时必须使用旧快照恢复 ES 文档。
     * 异常与副作用：会写 ES、同步日志与 repair 表；任一阶段失败都会抛出异常。
     *
     * @param beforeDocument 修改前标准地址文档
     * @param afterDocument 修改后标准地址文档
     * @param dbAction 数据库写动作
     * @return 数据库动作是否成功
     */
    public boolean syncStandardUpdate(StandardAddressSearchDocument beforeDocument,
                                      StandardAddressSearchDocument afterDocument,
                                      CheckedBooleanSupplier dbAction) {
        return syncSingleDocument(
            isStandardWriteEnabled(),
            BUSINESS_TYPE_UPDATE,
            ENTITY_TYPE_STANDARD,
            afterDocument == null ? null : afterDocument.getSegmId(),
            () -> standardAddressSearchGateway.upsert(afterDocument),
            dbAction,
            () -> standardAddressSearchGateway.restore(beforeDocument),
            REPAIR_ACTION_UPSERT_DOC,
            beforeDocument
        );
    }

    /**
     * 目的：执行标准地址删除的 ES 与数据库双写。
     * 入参：删除前的标准地址旧快照集合与数据库写动作。
     * 出参：数据库写动作最终结果。
     * 关键约束：数据库失败时必须按旧快照逐条恢复 ES 文档。
     * 异常与副作用：会删除或恢复 ES 文档，并写入同步日志与 repair 表。
     *
     * @param beforeDocuments 删除前标准地址文档集合
     * @param dbAction 数据库写动作
     * @return 数据库动作是否成功
     */
    public boolean syncStandardDelete(List<StandardAddressSearchDocument> beforeDocuments, CheckedBooleanSupplier dbAction) {
        return syncDeleteDocuments(
            isStandardWriteEnabled(),
            BUSINESS_TYPE_DELETE,
            ENTITY_TYPE_STANDARD,
            extractStandardIds(beforeDocuments),
            () -> standardAddressSearchGateway.deleteByIds(extractStandardIds(beforeDocuments)),
            dbAction,
            () -> restoreStandardDocuments(beforeDocuments),
            beforeDocuments
        );
    }

    /**
     * 目的：执行安装地址新增的 ES 与数据库双写。
     * 入参：新增后的安装地址文档与数据库写动作。
     * 出参：数据库写动作最终结果。
     * 关键约束：写开关关闭时仅执行数据库动作；写开关开启时必须先写 ES，再写数据库。
     * 异常与副作用：会写 ES、同步日志与 repair 表；任一阶段失败都会抛出异常。
     *
     * @param document 新增后的安装地址搜索文档
     * @param dbAction 数据库写动作
     * @return 数据库动作是否成功
     */
    public boolean syncInstallationCreate(InstallationAddressSearchDocument document, CheckedBooleanSupplier dbAction) {
        return syncSingleDocument(
            isInstallationWriteEnabled(),
            BUSINESS_TYPE_CREATE,
            ENTITY_TYPE_INSTALLATION,
            document == null ? null : document.getSetAddrId(),
            () -> installationAddressSearchGateway.upsert(document),
            dbAction,
            () -> installationAddressSearchGateway.deleteByIds(resolveSingleIdList(document == null ? null : document.getSetAddrId())),
            REPAIR_ACTION_DELETE_DOC,
            document
        );
    }

    /**
     * 目的：执行安装地址修改的 ES 与数据库双写。
     * 入参：修改前文档、修改后文档与数据库写动作。
     * 出参：数据库写动作最终结果。
     * 关键约束：数据库失败时必须使用旧快照恢复 ES 文档。
     * 异常与副作用：会写 ES、同步日志与 repair 表；任一阶段失败都会抛出异常。
     *
     * @param beforeDocument 修改前安装地址文档
     * @param afterDocument 修改后安装地址文档
     * @param dbAction 数据库写动作
     * @return 数据库动作是否成功
     */
    public boolean syncInstallationUpdate(InstallationAddressSearchDocument beforeDocument,
                                          InstallationAddressSearchDocument afterDocument,
                                          CheckedBooleanSupplier dbAction) {
        return syncSingleDocument(
            isInstallationWriteEnabled(),
            BUSINESS_TYPE_UPDATE,
            ENTITY_TYPE_INSTALLATION,
            afterDocument == null ? null : afterDocument.getSetAddrId(),
            () -> installationAddressSearchGateway.upsert(afterDocument),
            dbAction,
            () -> installationAddressSearchGateway.restore(beforeDocument),
            REPAIR_ACTION_UPSERT_DOC,
            beforeDocument
        );
    }

    /**
     * 目的：执行安装地址删除的 ES 与数据库双写。
     * 入参：删除前的安装地址旧快照集合与数据库写动作。
     * 出参：数据库写动作最终结果。
     * 关键约束：数据库失败时必须按旧快照逐条恢复 ES 文档。
     * 异常与副作用：会删除或恢复 ES 文档，并写入同步日志与 repair 表。
     *
     * @param beforeDocuments 删除前安装地址文档集合
     * @param dbAction 数据库写动作
     * @return 数据库动作是否成功
     */
    public boolean syncInstallationDelete(List<InstallationAddressSearchDocument> beforeDocuments, CheckedBooleanSupplier dbAction) {
        return syncDeleteDocuments(
            isInstallationWriteEnabled(),
            BUSINESS_TYPE_DELETE,
            ENTITY_TYPE_INSTALLATION,
            extractInstallationIds(beforeDocuments),
            () -> installationAddressSearchGateway.deleteByIds(extractInstallationIds(beforeDocuments)),
            dbAction,
            () -> restoreInstallationDocuments(beforeDocuments),
            beforeDocuments
        );
    }

    private boolean syncSingleDocument(boolean writeEnabled,
                                       String businessType,
                                       String entityType,
                                       String entityId,
                                       CheckedBooleanSupplier esAction,
                                       CheckedBooleanSupplier dbAction,
                                       CheckedBooleanSupplier compensateAction,
                                       String repairAction,
                                       Object repairPayload) {
        if (!writeEnabled) {
            return executeDbOnly(dbAction);
        }
        executeEsAction(businessType, entityType, entityId, PHASE_ES_WRITE, esAction);
        try {
            boolean dbSuccess = requireSuccess(dbAction.getAsBoolean(), "数据库操作返回失败");
            recordSyncLog(businessType, entityType, entityId, PHASE_DB_WRITE, true, null);
            return dbSuccess;
        } catch (Exception ex) {
            recordSyncLog(businessType, entityType, entityId, PHASE_DB_WRITE, false, ex.getMessage());
            boolean compensated = executeCompensation(businessType, entityType, entityId, compensateAction);
            if (!compensated) {
                enqueueRepairTask(entityType, entityId, repairAction, repairPayload);
            }
            throw propagate(ex);
        }
    }

    private boolean syncDeleteDocuments(boolean writeEnabled,
                                        String businessType,
                                        String entityType,
                                        List<String> entityIds,
                                        CheckedBooleanSupplier esDeleteAction,
                                        CheckedBooleanSupplier dbAction,
                                        CompensationSupplier compensationSupplier,
                                        Object repairPayload) {
        if (!writeEnabled) {
            return executeDbOnly(dbAction);
        }
        executeEsActionForIds(businessType, entityType, entityIds, esDeleteAction);
        try {
            boolean dbSuccess = requireSuccess(dbAction.getAsBoolean(), "数据库操作返回失败");
            entityIds.forEach(entityId -> recordSyncLog(businessType, entityType, entityId, PHASE_DB_WRITE, true, null));
            return dbSuccess;
        } catch (Exception ex) {
            entityIds.forEach(entityId -> recordSyncLog(businessType, entityType, entityId, PHASE_DB_WRITE, false, ex.getMessage()));
            List<RepairPayload> failedPayloads = compensationSupplier.compensate();
            failedPayloads.forEach(payload -> enqueueRepairTask(entityType, payload.entityId(), REPAIR_ACTION_UPSERT_DOC, payload.payload()));
            throw propagate(ex);
        }
    }

    private boolean executeDbOnly(CheckedBooleanSupplier dbAction) {
        try {
            return requireSuccess(dbAction.getAsBoolean(), "数据库操作返回失败");
        } catch (Exception ex) {
            throw propagate(ex);
        }
    }

    private void executeEsAction(String businessType,
                                 String entityType,
                                 String entityId,
                                 String phase,
                                 CheckedBooleanSupplier esAction) {
        try {
            requireSuccess(esAction.getAsBoolean(), "ES 操作返回失败");
            recordSyncLog(businessType, entityType, entityId, phase, true, null);
        } catch (Exception ex) {
            recordSyncLog(businessType, entityType, entityId, phase, false, ex.getMessage());
            throw propagate(ex);
        }
    }

    private void executeEsActionForIds(String businessType,
                                       String entityType,
                                       List<String> entityIds,
                                       CheckedBooleanSupplier esAction) {
        try {
            requireSuccess(esAction.getAsBoolean(), "ES 操作返回失败");
            entityIds.forEach(entityId -> recordSyncLog(businessType, entityType, entityId, PHASE_ES_WRITE, true, null));
        } catch (Exception ex) {
            entityIds.forEach(entityId -> recordSyncLog(businessType, entityType, entityId, PHASE_ES_WRITE, false, ex.getMessage()));
            throw propagate(ex);
        }
    }

    private boolean executeCompensation(String businessType,
                                        String entityType,
                                        String entityId,
                                        CheckedBooleanSupplier compensateAction) {
        try {
            boolean compensated = requireSuccess(compensateAction.getAsBoolean(), "ES 补偿返回失败");
            recordSyncLog(businessType, entityType, entityId, PHASE_ES_COMPENSATE, true, null);
            return compensated;
        } catch (Exception ex) {
            recordSyncLog(businessType, entityType, entityId, PHASE_ES_COMPENSATE, false, ex.getMessage());
            return false;
        }
    }

    private List<RepairPayload> restoreStandardDocuments(List<StandardAddressSearchDocument> documents) {
        List<RepairPayload> failedPayloads = new ArrayList<>();
        if (documents == null) {
            return failedPayloads;
        }
        for (StandardAddressSearchDocument document : documents) {
            String entityId = document == null ? null : document.getSegmId();
            try {
                boolean success = requireSuccess(standardAddressSearchGateway.restore(document), "ES 补偿返回失败");
                recordSyncLog(BUSINESS_TYPE_DELETE, ENTITY_TYPE_STANDARD, entityId, PHASE_ES_COMPENSATE, true, null);
                if (!success) {
                    failedPayloads.add(new RepairPayload(entityId, document));
                }
            } catch (Exception ex) {
                recordSyncLog(BUSINESS_TYPE_DELETE, ENTITY_TYPE_STANDARD, entityId, PHASE_ES_COMPENSATE, false, ex.getMessage());
                failedPayloads.add(new RepairPayload(entityId, document));
            }
        }
        return failedPayloads;
    }

    private List<RepairPayload> restoreInstallationDocuments(List<InstallationAddressSearchDocument> documents) {
        List<RepairPayload> failedPayloads = new ArrayList<>();
        if (documents == null) {
            return failedPayloads;
        }
        for (InstallationAddressSearchDocument document : documents) {
            String entityId = document == null ? null : document.getSetAddrId();
            try {
                boolean success = requireSuccess(installationAddressSearchGateway.restore(document), "ES 补偿返回失败");
                recordSyncLog(BUSINESS_TYPE_DELETE, ENTITY_TYPE_INSTALLATION, entityId, PHASE_ES_COMPENSATE, true, null);
                if (!success) {
                    failedPayloads.add(new RepairPayload(entityId, document));
                }
            } catch (Exception ex) {
                recordSyncLog(BUSINESS_TYPE_DELETE, ENTITY_TYPE_INSTALLATION, entityId, PHASE_ES_COMPENSATE, false, ex.getMessage());
                failedPayloads.add(new RepairPayload(entityId, document));
            }
        }
        return failedPayloads;
    }

    private void enqueueRepairTask(String entityType, String entityId, String repairAction, Object payload) {
        AddressSearchRepairTask repairTask = new AddressSearchRepairTask();
        repairTask.setId(IdUtil.getSnowflakeNextId());
        repairTask.setEntityType(entityType);
        repairTask.setEntityId(entityId);
        repairTask.setRepairAction(repairAction);
        repairTask.setPayloadJson(StringUtils.substring(JSONUtil.toJsonStr(payload), 0, 65535));
        repairTask.setStatus(REPAIR_STATUS_PENDING);
        repairTask.setRetryCount(0);
        Date now = new Date();
        repairTask.setCreatedTime(now);
        repairTask.setUpdatedTime(now);
        addressSearchRepairTaskMapper.insert(repairTask);
    }

    private void recordSyncLog(String businessType,
                               String entityType,
                               String entityId,
                               String phase,
                               boolean success,
                               String errorMessage) {
        AddressSearchSyncLog syncLog = new AddressSearchSyncLog();
        syncLog.setId(IdUtil.getSnowflakeNextId());
        syncLog.setBusinessType(businessType);
        syncLog.setEntityType(entityType);
        syncLog.setEntityId(entityId);
        syncLog.setPhase(phase);
        syncLog.setSuccessFlag(success ? SUCCESS_FLAG_YES : SUCCESS_FLAG_NO);
        syncLog.setErrorMessage(StringUtils.substring(errorMessage, 0, 1000));
        syncLog.setCreatedTime(new Date());
        addressSearchSyncLogMapper.insert(syncLog);
    }

    private List<String> extractStandardIds(List<StandardAddressSearchDocument> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
            .map(StandardAddressSearchDocument::getSegmId)
            .filter(StringUtils::isNotBlank)
            .toList();
    }

    private List<String> extractInstallationIds(List<InstallationAddressSearchDocument> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
            .map(InstallationAddressSearchDocument::getSetAddrId)
            .filter(StringUtils::isNotBlank)
            .toList();
    }

    private List<String> resolveSingleIdList(String entityId) {
        return StringUtils.isBlank(entityId) ? List.of() : List.of(entityId);
    }

    private boolean isStandardWriteEnabled() {
        return Boolean.TRUE.equals(addressSearchProperties.getStandard().getWriteEnabled());
    }

    private boolean isInstallationWriteEnabled() {
        return Boolean.TRUE.equals(addressSearchProperties.getInstallation().getWriteEnabled());
    }

    private boolean requireSuccess(boolean success, String errorMessage) {
        if (!success) {
            throw new IllegalStateException(errorMessage);
        }
        return true;
    }

    private RuntimeException propagate(Exception ex) {
        return ex instanceof RuntimeException runtimeException ? runtimeException : new IllegalStateException(ex.getMessage(), ex);
    }

    @FunctionalInterface
    private interface CompensationSupplier {

        List<RepairPayload> compensate();
    }

    private record RepairPayload(String entityId, Object payload) {
    }
}
