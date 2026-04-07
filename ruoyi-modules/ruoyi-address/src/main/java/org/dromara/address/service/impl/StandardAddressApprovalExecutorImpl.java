package org.dromara.address.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.StandardAddressApproval;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressSplitItemBo;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标准地址审批正式执行器实现。
 * 目的：把审批申请单中的 JSON 快照恢复成业务对象，并委托现有命令服务完成正式写入。
 * 入参/出参：输入审批申请单，内部恢复目标 BO/VO 后执行业务写入。
 * 关键约束：执行逻辑必须与当前正式写链路保持一致，禁止旁路写库。
 * 异常与副作用：会修改 `ADDR_SEGM`、导入记录、操作日志与 ES 索引。
 */
@Service
@RequiredArgsConstructor
public class StandardAddressApprovalExecutorImpl implements StandardAddressApprovalExecutor {

    private final StandardAddressCommandService commandService;
    private final StandardAddressImportService importService;

    /**
     * 目的：根据申请单操作类型执行正式地址变更。
     * 入参：审批申请单。
     * 出参：无。
     * 关键约束：`requestPayload` 和 `importBatchPayload` 必须能够恢复为对应业务对象。
     * 异常与副作用：正式执行失败会抛出业务异常，供监听器统一记录执行失败信息。
     */
    @Override
    public void execute(StandardAddressApproval approval) {
        String operationType = approval.getOperationType();
        switch (operationType) {
            case StandardAddressApprovalService.OPERATION_ADD -> commandService.addStandardAddress(parseObject(approval.getRequestPayload(), StandardAddressBo.class));
            case StandardAddressApprovalService.OPERATION_UPDATE -> commandService.updateStandardAddress(parseObject(approval.getRequestPayload(), StandardAddressBo.class));
            case StandardAddressApprovalService.OPERATION_DELETE -> {
                StandardAddressApprovalService.DeleteApprovalPayload payload = parseObject(approval.getRequestPayload(), StandardAddressApprovalService.DeleteApprovalPayload.class);
                commandService.deleteStandardAddresses(payload.getSegmIds(), true);
            }
            case StandardAddressApprovalService.OPERATION_MERGE -> {
                StandardAddressApprovalService.MergeApprovalPayload payload = parseObject(approval.getRequestPayload(), StandardAddressApprovalService.MergeApprovalPayload.class);
                commandService.mergeStandardAddresses(payload.getSourceSegmIds(), payload.getTargetSegmId());
            }
            case StandardAddressApprovalService.OPERATION_SPLIT -> {
                StandardAddressApprovalService.SplitApprovalPayload payload = parseObject(approval.getRequestPayload(), StandardAddressApprovalService.SplitApprovalPayload.class);
                commandService.splitStandardAddress(payload.getSourceSegmId(), payload.getSplitItems());
            }
            case StandardAddressApprovalService.OPERATION_IMPORT -> {
                StandardAddressApprovalService.ImportApprovalPayload payload = parseObject(approval.getRequestPayload(), StandardAddressApprovalService.ImportApprovalPayload.class);
                List<StandardAddressImportVo> rows = JsonUtils.parseArray(approval.getImportBatchPayload(), StandardAddressImportVo.class);
                importService.importStandardAddressData(rows, payload.getUpdateSupport(), payload.getOperName(), payload.getFileName());
            }
            default -> throw new ServiceException("不支持的标准地址审批操作类型：" + operationType);
        }
    }

    /**
     * 目的：恢复 JSON 快照为指定对象。
     * 入参：JSON 字符串与目标类型。
     * 出参：反序列化对象。
     * 关键约束：JSON 为空时直接判定为非法申请单。
     * 异常与副作用：解析失败时抛出业务异常，不产生额外写入副作用。
     */
    private <T> T parseObject(String json, Class<T> clazz) {
        T value = JsonUtils.parseObject(json, clazz);
        if (value == null) {
            throw new ServiceException("审批单快照缺失，无法执行正式变更");
        }
        return value;
    }
}
