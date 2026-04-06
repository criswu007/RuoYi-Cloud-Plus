package org.dromara.address.search.service;

import cn.hutool.core.collection.CollUtil;
import co.elastic.clients.elasticsearch._types.FieldValue;
import lombok.RequiredArgsConstructor;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.search.model.SearchAfterBatch;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.dromara.address.support.AddressRegionContext;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 标准地址导出服务。
 * 目的：按当前导出口径统一调度标准地址流式写出，`spc_region` 保持数据库分页导出，`ADDR_SEGM` 在启用 ES 读链路时走 `PIT + search_after`。
 * 入参/出参：输入标准地址查询条件、批次大小与批次消费函数；输出通过消费函数逐批回调标准地址数据。
 * 关键约束：区域级地址与 ES 读开关关闭场景必须回退数据库分页；ADDR_SEGM ES 导出必须复用统一的 `regionId/segmType` 过滤语义并在 finally 中关闭 PIT。
 * 异常与副作用：会访问 ES 与数据库并驱动导出写出回调，不直接写数据库。
 */
@Service
@RequiredArgsConstructor
public class StandardAddressSearchExportService {

    private final StandardAddressSearchGateway standardAddressSearchGateway;
    private final IStandardAddressService standardAddressService;
    private final StandardAddressDictionaryService dictionaryService;
    private final AddressRegionContext addressRegionContext;
    private final AddressSearchProperties addressSearchProperties;

    /**
     * 目的：按导出批次顺序回调标准地址数据。
     * 入参：标准地址查询条件、单批大小与批次消费函数。
     * 出参：无，导出数据通过消费函数输出。
     * 关键约束：批次大小必须大于 0；`ADDR_SEGM` ES 导出需要复用与后台列表一致的筛选条件；未显式传入 `regionId` 时需回退当前登录上下文。
     * 异常与副作用：会触发数据库或 ES 查询，并调用外部消费函数写出导出数据。
     */
    public void writeRows(StandardAddressBo bo, int batchSize, Consumer<List<StandardAddressVo>> consumer) {
        if (batchSize <= 0 || consumer == null) {
            return;
        }
        StandardAddressBo queryBo = normalizeQueryBo(bo);
        if (shouldUseDatabaseExport(queryBo)) {
            writeDatabaseRows(queryBo, batchSize, consumer);
            return;
        }
        writeSearchRows(queryBo, batchSize, consumer);
    }

    private StandardAddressBo normalizeQueryBo(StandardAddressBo bo) {
        StandardAddressBo queryBo = bo == null ? new StandardAddressBo() : bo;
        queryBo.setRegionId(addressRegionContext.resolveRegionId(queryBo.getRegionId()));
        return queryBo;
    }

    private boolean shouldUseDatabaseExport(StandardAddressBo bo) {
        if (!Boolean.TRUE.equals(addressSearchProperties.getStandard().getReadEnabled())) {
            return true;
        }
        Integer readonlyRegionAddrLevel = resolveReadonlyRegionAddrLevel(bo);
        return readonlyRegionAddrLevel != null && (readonlyRegionAddrLevel == 1 || readonlyRegionAddrLevel == 2);
    }

    private Integer resolveReadonlyRegionAddrLevel(StandardAddressBo bo) {
        if (bo == null) {
            return null;
        }
        Integer regionAddrLevel = dictionaryService.resolveReadonlyRegionAddrLevel(bo.getSegmType());
        if (regionAddrLevel != null) {
            return regionAddrLevel;
        }
        Integer addrLevel = bo.getAddrLevel();
        return addrLevel != null && (addrLevel == 1 || addrLevel == 2) ? addrLevel : null;
    }

    private List<String> resolveAddrSegmTypes(StandardAddressBo bo) {
        if (bo == null) {
            return Collections.emptyList();
        }
        if (org.dromara.common.core.utils.StringUtils.isNotBlank(bo.getSegmType())) {
            return List.of(bo.getSegmType());
        }
        return dictionaryService.resolveSegmTypesByAddrLevel(bo.getAddrLevel());
    }

    private void writeDatabaseRows(StandardAddressBo bo, int batchSize, Consumer<List<StandardAddressVo>> consumer) {
        long total = Long.MAX_VALUE;
        int pageNum = 1;
        while (((long) (pageNum - 1) * batchSize) < total) {
            TableDataInfo<StandardAddressVo> pageData = standardAddressService.queryStandardAddressPageList(bo, new PageQuery(batchSize, pageNum));
            List<StandardAddressVo> rows = pageData.getRows();
            if (CollUtil.isEmpty(rows)) {
                return;
            }
            consumer.accept(rows);
            total = pageData.getTotal();
            if (((long) pageNum * batchSize) >= total) {
                return;
            }
            pageNum++;
        }
    }

    private void writeSearchRows(StandardAddressBo bo, int batchSize, Consumer<List<StandardAddressVo>> consumer) {
        List<String> segmTypes = resolveAddrSegmTypes(bo);
        String pitId = standardAddressSearchGateway.openExportPointInTime();
        List<FieldValue> searchAfter = null;
        try {
            while (true) {
                SearchAfterBatch<StandardAddressVo> batch = standardAddressSearchGateway.queryExportBatch(
                    bo,
                    CollUtil.isEmpty(segmTypes) ? null : segmTypes,
                    pitId,
                    searchAfter,
                    batchSize
                );
                if (batch == null || CollUtil.isEmpty(batch.getRows())) {
                    return;
                }
                consumer.accept(batch.getRows());
                if (org.dromara.common.core.utils.StringUtils.isNotBlank(batch.getNextPitId())) {
                    pitId = batch.getNextPitId();
                }
                if (batch.isFinished()) {
                    return;
                }
                searchAfter = batch.getNextSearchAfter();
                if (CollUtil.isEmpty(searchAfter)) {
                    return;
                }
            }
        } finally {
            standardAddressSearchGateway.closeExportPointInTime(pitId);
        }
    }
}
