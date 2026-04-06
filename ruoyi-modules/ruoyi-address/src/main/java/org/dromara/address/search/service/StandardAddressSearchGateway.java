package org.dromara.address.search.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.AddressSearchRepairTask;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.search.document.StandardAddressSearchDocument;
import org.dromara.address.search.model.AddressSearchIndexRuntimeInfo;
import org.dromara.address.search.model.SearchAfterBatch;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

/**
 * 标准地址 ES 读链路网关。
 * <p>
 * 目的：统一封装标准地址在 ES 中的候选检索与分页列表查询，供查询服务按开关切换读链路。
 * 关键约束：仅负责 ES 查询与文档映射，不承担数据库补数、可写标记处理或业务字段兜底。
 * 异常与副作用：会访问 ES 索引；不直接写数据库。
 * </p>
 */
public interface StandardAddressSearchGateway {

    /**
     * 目的：探测当前标准地址 ES 读写端点是否可达。
     * 入参：无。
     * 出参：`true` 表示探测成功。
     * 关键约束：仅作为运维概览健康探针，不承担写链路兜底。
     * 异常与副作用：会访问 ES；实现方应自行吞掉探测异常并返回 `false`。
     */
    boolean ping();

    /**
     * 目的：获取标准地址索引运行态信息。
     * 入参：无。
     * 出参：包含 alias、当前物理索引名和文档数的运行态对象。
     * 关键约束：统计口径基于当前 alias 指向；不存在 alias 时允许回退展示 alias 本身。
     * 异常与副作用：会访问 ES 查询 alias 和 count 信息。
     */
    AddressSearchIndexRuntimeInfo getRuntimeInfo();

    /**
     * 目的：为标准地址全量重建准备新的物理索引。
     * 入参：无。
     * 出参：新建成功的物理索引名。
     * 关键约束：创建的新索引不能提前绑定读别名，避免重建期间误切换线上流量。
     * 异常与副作用：会访问 ES 创建索引，不写数据库。
     */
    String prepareRebuildIndex();

    /**
     * 目的：向指定标准地址物理索引批量写入重建数据。
     * 入参：目标物理索引名与标准地址实体批次。
     * 出参：`true` 表示当前批写入成功。
     * 关键约束：仅用于全量重建，不走在线写链路的 `refresh=wait_for` 模板；调用方负责分页游标推进。
     * 异常与副作用：会批量写 ES，不写数据库；失败时允许抛出异常。
     *
     * @param indexName 目标物理索引名
     * @param entities 标准地址实体批次
     * @return 当前批是否写入成功
     */
    boolean bulkIndex(String indexName, List<AddrSegm> entities);

    /**
     * 目的：把标准地址读别名切换到新的物理索引。
     * 入参：新物理索引名。
     * 出参：无。
     * 关键约束：切换必须在当前重建索引 refresh 完成后执行，并移除旧物理索引上的同名别名。
     * 异常与副作用：会更新 ES 别名，不写数据库。
     *
     * @param newIndexName 新物理索引名
     */
    void switchAlias(String newIndexName);

    /**
     * 目的：打开标准地址导出 PIT 上下文。
     * 入参：无。
     * 出参：导出批次查询使用的 `pitId`。
     * 关键约束：返回值必须用于同一次导出链路的后续 `search_after` 查询，直到 finally 中关闭。
     * 异常与副作用：会访问 ES 打开 PIT，不写数据库。
     */
    String openExportPointInTime();

    /**
     * 目的：基于 `PIT + search_after` 拉取标准地址导出批次。
     * 入参：后台查询条件、地址类型集合、当前 `pitId`、上一批游标与批次大小。
     * 出参：当前批次数据、下一批游标与最新 `pitId`。
     * 关键约束：过滤条件必须与后台列表保持一致，排序需补 `segmId asc` 以保证跨批次稳定性。
     * 异常与副作用：会访问 ES 检索索引，不写数据库。
     */
    SearchAfterBatch<StandardAddressVo> queryExportBatch(StandardAddressBo bo, List<String> segmTypes, String pitId, List<FieldValue> searchAfter, int batchSize);

    /**
     * 目的：关闭标准地址导出 PIT 上下文。
     * 入参：导出链路当前持有的 `pitId`。
     * 出参：无。
     * 关键约束：空 `pitId` 直接忽略；导出结束后必须调用，避免 PIT 资源泄漏。
     * 异常与副作用：会访问 ES 关闭 PIT，不写数据库。
     */
    void closeExportPointInTime(String pitId);

    /**
     * 目的：按关键字查询标准地址候选集合。
     * 入参：关键字、可选地址类型集合、状态、区域与返回条数上限。
     * 出参：候选标准地址列表，顺序以 ES 排序结果为准。
     * 关键约束：需覆盖名称 match 与编码 prefix 检索，并保持稳定排序。
     * 异常与副作用：会访问 ES 检索索引，不写数据库。
     *
     * @param keyword 关键字
     * @param segmTypes 地址类型集合
     * @param status 状态
     * @param regionId 区域 ID
     * @param limit 返回条数上限
     * @return 标准地址候选列表
     */
    List<StandardAddressVo> searchCandidates(String keyword, List<String> segmTypes, String status, String regionId, int limit);

    /**
     * 目的：按后台列表条件分页查询标准地址。
     * 入参：后台查询条件、可选地址类型集合与分页参数。
     * 出参：标准地址分页结果。
     * 关键约束：需把筛选、排序与分页都收敛在 ES 查询侧完成。
     * 异常与副作用：会访问 ES 检索索引，不写数据库。
     *
     * @param bo 后台查询条件
     * @param segmTypes 地址类型集合
     * @param pageQuery 分页参数
     * @return 标准地址分页结果
     */
    TableDataInfo<StandardAddressVo> queryPage(StandardAddressBo bo, List<String> segmTypes, PageQuery pageQuery);

    /**
     * 目的：向标准地址索引写入或覆盖单条文档。
     * 入参：标准地址搜索文档。
     * 出参：`true` 表示 ES 写入成功，`false` 表示执行完成但结果失败。
     * 关键约束：调用方依赖该方法支撑“先 ES、后 DB”的立即一致模板，写入需应用统一 refresh 策略。
     * 异常与副作用：会写 ES，不写数据库；失败时允许抛出异常。
     *
     * @param document 标准地址搜索文档
     * @return ES 写入是否成功
     */
    boolean upsert(StandardAddressSearchDocument document);

    /**
     * 目的：按主键集合删除标准地址索引文档。
     * 入参：标准地址主键集合。
     * 出参：`true` 表示 ES 删除成功，`false` 表示执行完成但结果失败。
     * 关键约束：用于新增补偿和删除主流程，空集合应直接视为成功。
     * 异常与副作用：会删除 ES 文档，不写数据库；失败时允许抛出异常。
     *
     * @param segmIds 标准地址主键集合
     * @return ES 删除是否成功
     */
    boolean deleteByIds(List<String> segmIds);

    /**
     * 目的：使用旧快照恢复标准地址索引文档。
     * 入参：标准地址旧快照文档。
     * 出参：`true` 表示恢复成功，`false` 表示执行完成但结果失败。
     * 关键约束：用于数据库失败后的同步补偿，恢复内容必须以旧快照为准。
     * 异常与副作用：会写 ES，不写数据库；失败时允许抛出异常。
     *
     * @param document 标准地址旧快照文档
     * @return ES 恢复是否成功
     */
    boolean restore(StandardAddressSearchDocument document);

    /**
     * 目的：执行单条标准地址 repair 任务。
     * 入参：repair 任务实体。
     * 出参：`true` 表示 repair 成功。
     * 关键约束：当前仅支持按主键删除文档或按快照恢复文档，两类动作都必须落到标准地址索引别名。
     * 异常与副作用：会访问 ES 执行修复，不写数据库；失败时允许抛出异常。
     *
     * @param task repair 任务
     * @return repair 是否成功
     */
    boolean repair(AddressSearchRepairTask task);
}
