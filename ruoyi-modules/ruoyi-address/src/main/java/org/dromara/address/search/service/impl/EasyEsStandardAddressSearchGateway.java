package org.dromara.address.search.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.AddressSearchRepairTask;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.esmapper.StandardAddressSearchEsMapper;
import org.dromara.address.search.builder.StandardAddressSearchDocumentBuilder;
import org.dromara.address.search.document.StandardAddressSearchDocument;
import org.dromara.address.search.model.AddressSearchIndexRuntimeInfo;
import org.dromara.address.search.model.SearchAfterBatch;
import org.dromara.address.search.service.StandardAddressSearchGateway;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.easyes.core.biz.EntityInfo;
import org.dromara.easyes.core.biz.CreateIndexParam;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.easyes.core.biz.EsPageInfo;
import org.dromara.easyes.core.conditions.select.LambdaEsQueryWrapper;
import org.dromara.easyes.core.toolkit.EntityInfoHelper;
import org.dromara.easyes.core.toolkit.IndexUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 Easy-ES 的标准地址搜索网关实现。
 * <p>
 * 目的：把标准地址候选查询与后台分页查询统一路由到 ES，并映射回标准地址视图对象。
 * 关键约束：必须显式切换当前活跃索引别名，避免 mapper 落到默认索引名；仅返回 Task 2 所需核心字段。
 * 异常与副作用：会访问 ES 检索索引，不写数据库。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class EasyEsStandardAddressSearchGateway implements StandardAddressSearchGateway {

    private static final String REPAIR_ACTION_DELETE_DOC = "DELETE_DOC";
    private static final String REPAIR_ACTION_UPSERT_DOC = "UPSERT_DOC";
    private static final int ES_MAX_RESULT_WINDOW = 10000;
    private static final String ES_DEFAULT_SORT_FIELD = "_shard_doc";
    private static final String ES_TIE_BREAKER_FIELD = "segmId";

    private final StandardAddressSearchEsMapper standardAddressSearchEsMapper;
    private final AddressSearchProperties addressSearchProperties;
    private final ElasticsearchClient elasticsearchClient;
    private final StandardAddressDictionaryService dictionaryService;

    /**
     * 目的：探测标准地址 ES 端点是否可达。
     * 入参：无。
     * 出参：`true` 表示探测成功。
     * 关键约束：探测失败时不向外抛出异常，避免运维概览页整体失败。
     * 异常与副作用：会访问 ES ping 接口，无数据库副作用。
     */
    @Override
    public boolean ping() {
        try {
            return Boolean.TRUE.equals(elasticsearchClient.ping().value());
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 目的：获取标准地址索引当前 alias 指向与文档总量。
     * 入参：无。
     * 出参：标准地址索引运行态对象。
     * 关键约束：物理索引名优先取 alias 实际指向；若查询 alias 失败则抛出业务异常。
     * 异常与副作用：会访问 ES alias/count 接口，无数据库副作用。
     */
    @Override
    public AddressSearchIndexRuntimeInfo getRuntimeInfo() {
        String alias = addressSearchProperties.getStandard().getAlias();
        String physicalIndexName = resolveCurrentPhysicalIndex(alias);
        long documentCount = countDocuments(alias);
        return new AddressSearchIndexRuntimeInfo(alias, physicalIndexName, documentCount);
    }

    /**
     * 目的：创建标准地址重建使用的新物理索引。
     * 入参：无。
     * 出参：新物理索引名。
     * 关键约束：创建时不能绑定线上读别名，避免重建中途流量提前切换。
     * 异常与副作用：会访问 ES 创建新索引，不写数据库。
     */
    @Override
    public String prepareRebuildIndex() {
        String rebuildIndex = buildRebuildIndexName(addressSearchProperties.getStandard().getAlias());
        createPhysicalIndexWithoutAlias(rebuildIndex);
        return rebuildIndex;
    }

    /**
     * 目的：批量写入标准地址重建数据。
     * 入参：目标物理索引名与标准地址实体批次。
     * 出参：`true` 表示当前批写入成功。
     * 关键约束：写入前需把标准地址实体转换为完整搜索文档，并补齐 `addrLevel`。
     * 异常与副作用：会批量写 ES，不写数据库。
     */
    @Override
    public boolean bulkIndex(String indexName, List<AddrSegm> entities) {
        List<StandardAddressSearchDocument> documents = toRebuildDocuments(entities);
        if (CollUtil.isEmpty(documents)) {
            return true;
        }
        standardAddressSearchEsMapper.setCurrentActiveIndex(indexName);
        Integer affected = standardAddressSearchEsMapper.insertBatch(indexName, documents);
        return affected != null && affected >= documents.size();
    }

    /**
     * 目的：将标准地址读别名切换到新的物理索引。
     * 入参：新的物理索引名。
     * 出参：无。
     * 关键约束：切换前必须先 refresh 当前重建索引，确保别名切换后结果立即可读。
     * 异常与副作用：会访问 ES refresh 索引并更新别名，不写数据库。
     */
    @Override
    public void switchAlias(String newIndexName) {
        standardAddressSearchEsMapper.refresh(newIndexName);
        String alias = addressSearchProperties.getStandard().getAlias();
        List<String> currentIndices = resolveAliasIndices(alias);
        boolean aliasOnNewIndex = currentIndices.contains(newIndexName);
        List<Action> actions = new ArrayList<>();
        for (String currentIndex : currentIndices) {
            if (StringUtils.equals(currentIndex, newIndexName)) {
                continue;
            }
            actions.add(new Action.Builder()
                .remove(remove -> remove.index(currentIndex).alias(alias))
                .build());
        }
        if (!aliasOnNewIndex) {
            actions.add(new Action.Builder()
                .add(add -> add.index(newIndexName).alias(alias).isWriteIndex(true))
                .build());
        }
        if (actions.isEmpty()) {
            return;
        }
        try {
            elasticsearchClient.indices().updateAliases(request -> request.actions(actions));
        } catch (IOException ex) {
            throw new IllegalStateException("切换标准地址索引别名失败", ex);
        }
    }

    /**
     * 目的：打开标准地址导出使用的 PIT 上下文。
     * 入参：无。
     * 出参：可用于后续 `search_after` 查询的 `pitId`。
     * 关键约束：必须使用当前激活的标准地址索引别名打开 PIT，并沿用统一的保活时长。
     * 异常与副作用：会访问 ES 打开 PIT，不写数据库。
     */
    @Override
    public String openExportPointInTime() {
        standardAddressSearchEsMapper.setCurrentActiveIndex(addressSearchProperties.getStandard().getAlias());
        try {
            return elasticsearchClient.openPointInTime(request -> request
                    .index(addressSearchProperties.getStandard().getAlias())
                    .keepAlive(time -> time.time(addressSearchProperties.getPitKeepAlive())))
                .id();
        } catch (IOException ex) {
            throw new IllegalStateException("打开标准地址导出 PIT 失败", ex);
        }
    }

    /**
     * 目的：基于 `PIT + search_after` 拉取标准地址导出批次。
     * 入参：后台查询条件、地址类型集合、当前 `pitId`、上一批游标与批次大小。
     * 出参：当前导出批次。
     * 关键约束：导出排序固定为 `segmId asc`；查询条件需与后台列表筛选口径保持一致；使用 PIT 时不得继续附带索引参数。
     * 异常与副作用：会访问 ES，不写数据库。
     */
    @Override
    public SearchAfterBatch<StandardAddressVo> queryExportBatch(StandardAddressBo bo, List<String> segmTypes, String pitId, List<FieldValue> searchAfter, int batchSize) {
        standardAddressSearchEsMapper.setCurrentActiveIndex(addressSearchProperties.getStandard().getAlias());
        LambdaEsQueryWrapper<StandardAddressSearchDocument> wrapper = buildQueryWrapper(bo, segmTypes);
        wrapper.orderByAsc(StandardAddressSearchDocument::getSegmId);
        SearchRequest.Builder searchBuilder = standardAddressSearchEsMapper.getSearchBuilder(wrapper);
        searchBuilder.index(List.of());
        searchBuilder.size(batchSize);
        searchBuilder.pit(pointInTime -> pointInTime
            .id(pitId)
            .keepAlive(time -> time.time(addressSearchProperties.getPitKeepAlive())));
        if (CollUtil.isNotEmpty(searchAfter)) {
            searchBuilder.searchAfter(searchAfter);
        }
        try {
            SearchResponse<StandardAddressSearchDocument> response = elasticsearchClient.search(searchBuilder.build(), StandardAddressSearchDocument.class);
            List<Hit<StandardAddressSearchDocument>> hits = response.hits().hits();
            List<StandardAddressVo> rows = hits.stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(this::toVo)
                .toList();
            List<FieldValue> nextSearchAfter = hits.isEmpty() ? searchAfter : hits.get(hits.size() - 1).sort();
            boolean finished = hits.size() < batchSize;
            String nextPitId = StringUtils.isNotBlank(response.pitId()) ? response.pitId() : pitId;
            return SearchAfterBatch.of(rows, nextPitId, nextSearchAfter, finished);
        } catch (IOException ex) {
            throw new IllegalStateException("查询标准地址导出批次失败", ex);
        }
    }

    /**
     * 目的：关闭标准地址导出 PIT 上下文。
     * 入参：当前导出链路持有的 `pitId`。
     * 出参：无。
     * 关键约束：空 `pitId` 直接返回，避免无意义远程调用。
     * 异常与副作用：会访问 ES 关闭 PIT，不写数据库。
     */
    @Override
    public void closeExportPointInTime(String pitId) {
        if (StringUtils.isBlank(pitId)) {
            return;
        }
        try {
            elasticsearchClient.closePointInTime(request -> request.id(pitId));
        } catch (IOException ex) {
            throw new IllegalStateException("关闭标准地址导出 PIT 失败", ex);
        }
    }

    /**
     * 目的：在 ES 中执行标准地址候选检索。
     * 入参：关键字、地址类型集合、状态、区域与结果上限。
     * 出参：标准地址候选列表。
     * 关键约束：候选关键字仅允许在 `standName` 全称字段内以“所有分词都命中”的方式检索，避免分词 OR 命中其他小区名称，并按 `addrLevel asc, segmId asc` 保持稳定排序。
     * 异常与副作用：会访问 ES 索引，不写数据库。
     */
    @Override
    public List<StandardAddressVo> searchCandidates(String keyword, List<String> segmTypes, String status, String regionId, int limit) {
        standardAddressSearchEsMapper.setCurrentActiveIndex(addressSearchProperties.getStandard().getAlias());
        LambdaEsQueryWrapper<StandardAddressSearchDocument> wrapper = new LambdaEsQueryWrapper<>();
        wrapper.multiMatchQuery(StringUtils.isNotBlank(keyword), keyword, Operator.And, 0, null, "standName");
        wrapper.in(CollUtil.isNotEmpty(segmTypes), StandardAddressSearchDocument::getSegmType, segmTypes);
        wrapper.eq(StringUtils.isNotBlank(status), StandardAddressSearchDocument::getStatus, status);
        wrapper.eq(StringUtils.isNotBlank(regionId), StandardAddressSearchDocument::getRegionId, regionId);
        wrapper.orderByAsc(StandardAddressSearchDocument::getAddrLevel, StandardAddressSearchDocument::getSegmId);
        EsPageInfo<StandardAddressSearchDocument> pageInfo = standardAddressSearchEsMapper.pageQuery(wrapper, 1, limit);
        return pageInfo.getList().stream()
            .map(this::toVo)
            .toList();
    }

    /**
     * 目的：在 ES 中执行标准地址后台分页查询。
     * 入参：后台查询条件、地址类型集合与分页参数。
     * 出参：标准地址分页结果。
     * 关键约束：分页、筛选、排序都在 ES 侧完成，返回结果仅包含后续数据库补数所需核心字段。
     * 异常与副作用：会访问 ES 索引，不写数据库。
     */
    @Override
    public TableDataInfo<StandardAddressVo> queryPage(StandardAddressBo bo, List<String> segmTypes, PageQuery pageQuery) {
        standardAddressSearchEsMapper.setCurrentActiveIndex(addressSearchProperties.getStandard().getAlias());
        PageQuery actualPageQuery = pageQuery == null ? new PageQuery() : pageQuery;
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<?> page = actualPageQuery.build();
        long current = Math.max(1L, page.getCurrent());
        int size = Math.max(1, Math.toIntExact(page.getSize()));
        long from = (current - 1) * size;
        if (from + size <= ES_MAX_RESULT_WINDOW) {
            LambdaEsQueryWrapper<StandardAddressSearchDocument> wrapper = buildQueryWrapper(bo, segmTypes);
            applyPageOrder(wrapper, pageQuery);
            EsPageInfo<StandardAddressSearchDocument> pageInfo = standardAddressSearchEsMapper.pageQuery(wrapper, (int) current, size);
            return new TableDataInfo<>(pageInfo.getList().stream().map(this::toVo).toList(), pageInfo.getTotal());
        }
        long total = countPageHits(bo, segmTypes);
        if (from >= total) {
            return new TableDataInfo<>(Collections.emptyList(), total);
        }
        if (total - from > ES_MAX_RESULT_WINDOW) {
            throw new UnsupportedOperationException("标准地址分页超出 ES 头尾窗口");
        }
        return queryTailWindowPage(bo, segmTypes, pageQuery, from, size, total);
    }

    /**
     * 目的：向标准地址索引写入或覆盖单条文档。
     * 入参：标准地址搜索文档。
     * 出参：`true` 表示 ES 写入成功。
     * 关键约束：写入必须命中当前标准地址索引别名，并应用统一 refresh 策略保障立即一致读语义。
     * 异常与副作用：会写 ES，不写数据库；失败时抛出异常。
     */
    @Override
    public boolean upsert(StandardAddressSearchDocument document) {
        if (document == null || StringUtils.isBlank(document.getSegmId())) {
            return false;
        }
        try {
            elasticsearchClient.index(request -> request
                .index(addressSearchProperties.getStandard().getAlias())
                .id(document.getSegmId())
                .document(document)
                .refresh(resolveRefreshPolicy()));
            return true;
        } catch (IOException ex) {
            throw new IllegalStateException("写入标准地址 ES 文档失败", ex);
        }
    }

    /**
     * 目的：按主键集合删除标准地址索引文档。
     * 入参：标准地址主键集合。
     * 出参：`true` 表示删除完成。
     * 关键约束：空集合直接返回成功；逐条删除时统一使用配置化 refresh 策略。
     * 异常与副作用：会删除 ES 文档，不写数据库；失败时抛出异常。
     */
    @Override
    public boolean deleteByIds(List<String> segmIds) {
        if (CollUtil.isEmpty(segmIds)) {
            return true;
        }
        try {
            for (String segmId : segmIds) {
                if (StringUtils.isBlank(segmId)) {
                    continue;
                }
                elasticsearchClient.delete(request -> request
                    .index(addressSearchProperties.getStandard().getAlias())
                    .id(segmId)
                    .refresh(resolveRefreshPolicy()));
            }
            return true;
        } catch (IOException ex) {
            throw new IllegalStateException("删除标准地址 ES 文档失败", ex);
        }
    }

    /**
     * 目的：按旧快照恢复标准地址索引文档。
     * 入参：标准地址旧快照文档。
     * 出参：`true` 表示恢复成功。
     * 关键约束：恢复逻辑与普通 upsert 一致，始终以旧快照内容为准。
     * 异常与副作用：会写 ES，不写数据库；失败时抛出异常。
     */
    @Override
    public boolean restore(StandardAddressSearchDocument document) {
        return upsert(document);
    }

    /**
     * 目的：执行标准地址 repair 任务。
     * 入参：repair 任务实体。
     * 出参：`true` 表示 repair 成功。
     * 关键约束：当前仅支持删除文档与按快照恢复文档两类 repair 动作。
     * 异常与副作用：会访问 ES 执行修复，不写数据库。
     */
    @Override
    public boolean repair(AddressSearchRepairTask task) {
        if (task == null) {
            return false;
        }
        return switch (task.getRepairAction()) {
            case REPAIR_ACTION_DELETE_DOC -> deleteByIds(resolveRepairIds(task.getEntityId()));
            case REPAIR_ACTION_UPSERT_DOC -> restore(parseRepairDocument(task));
            default -> throw new IllegalArgumentException("不支持的标准地址repair动作: " + task.getRepairAction());
        };
    }

    /**
     * 目的：构建标准地址列表查询在 ES 侧复用的过滤条件。
     * 入参：后台查询条件与地址类型集合。
     * 出参：可直接复用的 ES Lambda 查询包装器。
     * 关键约束：名称模糊查询仅允许在 `standName` 全称字段内以“所有分词都命中”的方式检索，禁止同时对 `segmName` 等简称字段做并行模糊匹配。
     * 异常与副作用：仅构建内存对象，无外部副作用。
     *
     * @param bo 后台查询条件
     * @param segmTypes 地址类型集合
     * @return ES 查询包装器
     */
    private LambdaEsQueryWrapper<StandardAddressSearchDocument> buildQueryWrapper(StandardAddressBo bo, List<String> segmTypes) {
        StandardAddressBo queryBo = bo == null ? new StandardAddressBo() : bo;
        LambdaEsQueryWrapper<StandardAddressSearchDocument> wrapper = new LambdaEsQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(queryBo.getSegmId()), StandardAddressSearchDocument::getSegmId, queryBo.getSegmId());
        wrapper.eq(StringUtils.isNotBlank(queryBo.getParentSegmId()), StandardAddressSearchDocument::getParentSegmId, queryBo.getParentSegmId());
        wrapper.multiMatchQuery(StringUtils.isNotBlank(queryBo.getStandName()), queryBo.getStandName(), Operator.And, 0, null, "standName");
        wrapper.eq(StringUtils.isNotBlank(queryBo.getRegionId()), StandardAddressSearchDocument::getRegionId, queryBo.getRegionId());
        wrapper.eq(StringUtils.isNotBlank(queryBo.getStatus()), StandardAddressSearchDocument::getStatus, queryBo.getStatus());
        wrapper.in(CollUtil.isNotEmpty(segmTypes), StandardAddressSearchDocument::getSegmType, segmTypes);
        return wrapper;
    }

    /**
     * 目的：统计当前标准地址分页条件在 ES 中的命中总量。
     * 入参：后台查询条件与地址类型集合。
     * 出参：命中总条数。
     * 关键约束：统计必须复用与列表一致的过滤条件，但不附带分页和排序；仅在深分页判定阶段调用，避免首页请求额外增加一次 count。
     * 异常与副作用：会访问 ES 检索接口，不写数据库；统计失败时抛出业务异常。
     *
     * @param bo 后台查询条件
     * @param segmTypes 地址类型集合
     * @return 命中总条数
     */
    private long countPageHits(StandardAddressBo bo, List<String> segmTypes) {
        SearchRequest.Builder searchBuilder = standardAddressSearchEsMapper.getSearchBuilder(buildQueryWrapper(bo, segmTypes));
        searchBuilder.index(List.of(addressSearchProperties.getStandard().getAlias()));
        searchBuilder.from(0);
        searchBuilder.size(0);
        searchBuilder.trackTotalHits(track -> track.enabled(true));
        try {
            SearchResponse<StandardAddressSearchDocument> response = elasticsearchClient.search(searchBuilder.build(), StandardAddressSearchDocument.class);
            if (response.hits() == null || response.hits().total() == null) {
                return 0L;
            }
            return response.hits().total().value();
        } catch (IOException ex) {
            throw new IllegalStateException("统计标准地址 ES 分页总量失败", ex);
        }
    }

    /**
     * 目的：在命中尾部窗口时，使用反向排序从 ES 拉取标准地址尾页数据。
     * 入参：后台查询条件、地址类型集合、分页参数、正向偏移量、页大小与总条数。
     * 出参：符合前端正向分页语义的标准地址分页结果。
     * 关键约束：仅当 `total - from <= 10000` 时允许使用；查询时需反转排序方向并在返回前恢复正向顺序，确保尾页结果与常规正向排序保持一致。
     * 异常与副作用：会访问 ES 检索接口，不写数据库；中间窗口请求不应调用本方法。
     *
     * @param bo 后台查询条件
     * @param segmTypes 地址类型集合
     * @param pageQuery 前端分页参数
     * @param from 正向分页偏移量
     * @param size 页大小
     * @param total 命中总条数
     * @return 标准地址尾页结果
     */
    private TableDataInfo<StandardAddressVo> queryTailWindowPage(StandardAddressBo bo, List<String> segmTypes, PageQuery pageQuery, long from, int size, long total) {
        long reverseFrom = Math.max(0L, total - from - size);
        String pitId = openPagePointInTime();
        String currentPitId = pitId;
        SearchRequest.Builder searchBuilder = standardAddressSearchEsMapper.getSearchBuilder(buildQueryWrapper(bo, segmTypes));
        searchBuilder.index(List.of());
        searchBuilder.from(Math.toIntExact(reverseFrom));
        searchBuilder.size(size);
        searchBuilder.trackTotalHits(track -> track.enabled(true));
        searchBuilder.pit(pointInTime -> pointInTime
            .id(pitId)
            .keepAlive(time -> time.time(addressSearchProperties.getPitKeepAlive())));
        applySearchSort(searchBuilder, pageQuery, true);
        try {
            SearchResponse<StandardAddressSearchDocument> response = elasticsearchClient.search(searchBuilder.build(), StandardAddressSearchDocument.class);
            if (StringUtils.isNotBlank(response.pitId())) {
                currentPitId = response.pitId();
            }
            List<StandardAddressVo> rows = new ArrayList<>(response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(this::toVo)
                .toList());
            Collections.reverse(rows);
            long remaining = total - from;
            if (remaining < rows.size()) {
                rows = new ArrayList<>(rows.subList(rows.size() - Math.toIntExact(remaining), rows.size()));
            }
            return new TableDataInfo<>(rows, total);
        } catch (IOException ex) {
            throw new IllegalStateException("查询标准地址 ES 尾页失败", ex);
        } finally {
            closePagePointInTime(currentPitId);
        }
    }

    private String openPagePointInTime() {
        try {
            return elasticsearchClient.openPointInTime(request -> request
                    .index(addressSearchProperties.getStandard().getAlias())
                    .keepAlive(time -> time.time(addressSearchProperties.getPitKeepAlive())))
                .id();
        } catch (IOException ex) {
            throw new IllegalStateException("打开标准地址分页 PIT 失败", ex);
        }
    }

    private void closePagePointInTime(String pitId) {
        if (StringUtils.isBlank(pitId)) {
            return;
        }
        try {
            elasticsearchClient.closePointInTime(request -> request.id(pitId));
        } catch (IOException ex) {
            throw new IllegalStateException("关闭标准地址分页 PIT 失败", ex);
        }
    }

    private void applyPageOrder(LambdaEsQueryWrapper<StandardAddressSearchDocument> wrapper, PageQuery pageQuery) {
        if (pageQuery == null) {
            return;
        }
        if (StringUtils.isBlank(pageQuery.getOrderByColumn()) || StringUtils.isBlank(pageQuery.getIsAsc())) {
            return;
        }
        String[] orderByArr = pageQuery.getOrderByColumn().split(",");
        String normalizedIsAsc = StringUtils.replaceEach(
            pageQuery.getIsAsc(),
            new String[]{"ascending", "descending"},
            new String[]{"asc", "desc"}
        );
        String[] isAscArr = normalizedIsAsc.split(",");
        if (isAscArr.length != 1 && isAscArr.length != orderByArr.length) {
            return;
        }
        for (int i = 0; i < orderByArr.length; i++) {
            String orderByStr = orderByArr[i] == null ? null : orderByArr[i].trim();
            if (StringUtils.isBlank(orderByStr)) {
                continue;
            }
            String isAscStr = isAscArr.length == 1 ? isAscArr[0] : isAscArr[i];
            if (isAscStr == null) {
                continue;
            }
            String direction = isAscStr.trim().toLowerCase();
            if ("asc".equals(direction)) {
                wrapper.orderByAsc(orderByStr);
            } else if ("desc".equals(direction)) {
                wrapper.orderByDesc(orderByStr);
            }
        }
    }

    /**
     * 目的：把前端分页排序参数转换为 Elasticsearch 原生排序，并按需反转方向以支持尾页查询。
     * 入参：ES 查询构造器、前端分页参数与是否反转排序方向。
     * 出参：无，直接在查询构造器上追加排序配置。
     * 关键约束：未显式传排序时统一使用 `_shard_doc` 稳定分页；显式排序时必须追加 `segmId` 作为稳定 tie-breaker，避免同值排序下跨页抖动。
     * 异常与副作用：仅修改当前 ES 查询构造器，不访问外部资源。
     *
     * @param searchBuilder ES 查询构造器
     * @param pageQuery 前端分页参数
     * @param reverseOrder 是否反转排序方向
     */
    private void applySearchSort(SearchRequest.Builder searchBuilder, PageQuery pageQuery, boolean reverseOrder) {
        List<PageSortItem> sortItems = resolvePageSortItems(pageQuery);
        if (CollUtil.isEmpty(sortItems)) {
            searchBuilder.sort(sort -> sort.field(field -> field
                .field(ES_DEFAULT_SORT_FIELD)
                .order(resolveSortOrder(true, reverseOrder))));
            return;
        }
        sortItems.forEach(sortItem -> searchBuilder.sort(sort -> sort.field(field -> field
            .field(sortItem.field())
            .order(resolveSortOrder(sortItem.ascending(), reverseOrder)))));
    }

    private List<PageSortItem> resolvePageSortItems(PageQuery pageQuery) {
        if (pageQuery == null || StringUtils.isBlank(pageQuery.getOrderByColumn()) || StringUtils.isBlank(pageQuery.getIsAsc())) {
            return Collections.emptyList();
        }
        String[] orderByArr = pageQuery.getOrderByColumn().split(",");
        String normalizedIsAsc = StringUtils.replaceEach(
            pageQuery.getIsAsc(),
            new String[]{"ascending", "descending"},
            new String[]{"asc", "desc"}
        );
        String[] isAscArr = normalizedIsAsc.split(",");
        if (isAscArr.length != 1 && isAscArr.length != orderByArr.length) {
            return Collections.emptyList();
        }
        List<PageSortItem> sortItems = new ArrayList<>();
        for (int i = 0; i < orderByArr.length; i++) {
            String orderByStr = orderByArr[i] == null ? null : orderByArr[i].trim();
            if (StringUtils.isBlank(orderByStr)) {
                continue;
            }
            String isAscStr = isAscArr.length == 1 ? isAscArr[0] : isAscArr[i];
            if (isAscStr == null) {
                continue;
            }
            String direction = isAscStr.trim().toLowerCase();
            if ("asc".equals(direction) || "desc".equals(direction)) {
                sortItems.add(new PageSortItem(orderByStr, "asc".equals(direction)));
            }
        }
        boolean containsTieBreaker = sortItems.stream().anyMatch(sortItem -> StringUtils.equals(sortItem.field(), ES_TIE_BREAKER_FIELD));
        if (!containsTieBreaker) {
            sortItems.add(new PageSortItem(ES_TIE_BREAKER_FIELD, true));
        }
        return sortItems;
    }

    private SortOrder resolveSortOrder(boolean ascending, boolean reverseOrder) {
        boolean actualAscending = reverseOrder ? !ascending : ascending;
        return actualAscending ? SortOrder.Asc : SortOrder.Desc;
    }

    private record PageSortItem(String field, boolean ascending) {
    }

    private StandardAddressVo toVo(StandardAddressSearchDocument source) {
        StandardAddressVo target = new StandardAddressVo();
        target.setSegmId(source.getSegmId());
        target.setParentSegmId(source.getParentSegmId());
        target.setSegmName(source.getSegmName());
        target.setStandName(source.getStandName());
        target.setSegmNo(source.getSegmNo());
        target.setStandNo(source.getStandNo());
        target.setRegionId(source.getRegionId());
        target.setAddrLevel(source.getAddrLevel());
        target.setSegmType(source.getSegmType());
        target.setDistrictId(source.getDistrictId());
        target.setServiceRegionId(source.getServiceRegionId());
        target.setStatus(source.getStatus());
        target.setCreateDate(source.getCreateDate());
        return target;
    }

    private Refresh resolveRefreshPolicy() {
        String refreshPolicy = StringUtils.defaultIfBlank(addressSearchProperties.getRefreshPolicy(), Refresh.WaitFor.jsonValue());
        return switch (refreshPolicy.toLowerCase()) {
            case "true" -> Refresh.True;
            case "false" -> Refresh.False;
            default -> Refresh.WaitFor;
        };
    }

    private String resolveCurrentPhysicalIndex(String alias) {
        try {
            if (!aliasExists(alias)) {
                return alias;
            }
            return elasticsearchClient.indices()
                .getAlias(request -> request.name(alias))
                .result()
                .keySet()
                .stream()
                .findFirst()
                .orElse(alias);
        } catch (IOException ex) {
            throw new IllegalStateException("查询标准地址 alias 指向失败", ex);
        }
    }

    private long countDocuments(String alias) {
        try {
            if (!aliasExists(alias) && !indexExists(alias)) {
                return 0L;
            }
            return elasticsearchClient.count(request -> request.index(alias)).count();
        } catch (IOException ex) {
            throw new IllegalStateException("统计标准地址索引文档数失败", ex);
        }
    }

    private void createPhysicalIndexWithoutAlias(String indexName) {
        EntityInfo entityInfo = EntityInfoHelper.getEntityInfo(StandardAddressSearchDocument.class);
        CreateIndexParam createIndexParam = IndexUtils.getCreateIndexParam(entityInfo, StandardAddressSearchDocument.class);
        createIndexParam.setIndexName(indexName);
        createIndexParam.setAliasName(null);
        if (!IndexUtils.createIndex(elasticsearchClient, entityInfo, createIndexParam)) {
            throw new IllegalStateException("创建标准地址重建索引失败");
        }
    }

    private List<StandardAddressSearchDocument> toRebuildDocuments(List<AddrSegm> entities) {
        if (CollUtil.isEmpty(entities)) {
            return List.of();
        }
        List<String> segmTypes = entities.stream()
            .map(AddrSegm::getSegmType)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        Map<String, Integer> addrLevelMap = dictionaryService.resolveAddrLevelMap(segmTypes);
        return entities.stream()
            .map(entity -> StandardAddressSearchDocumentBuilder.fromEntity(entity, addrLevelMap.get(entity.getSegmType())))
            .filter(Objects::nonNull)
            .toList();
    }

    private List<String> resolveAliasIndices(String alias) {
        try {
            if (!aliasExists(alias)) {
                return List.of();
            }
            return List.copyOf(elasticsearchClient.indices().getAlias(request -> request.name(alias)).result().keySet());
        } catch (IOException ex) {
            throw new IllegalStateException("查询标准地址索引别名失败", ex);
        }
    }

    private boolean aliasExists(String alias) throws IOException {
        return elasticsearchClient.indices().existsAlias(request -> request.name(alias)).value();
    }

    private boolean indexExists(String indexName) throws IOException {
        return elasticsearchClient.indices().exists(request -> request.index(indexName)).value();
    }

    private List<String> resolveRepairIds(String entityId) {
        return StringUtils.isBlank(entityId) ? List.of() : List.of(entityId);
    }

    private StandardAddressSearchDocument parseRepairDocument(AddressSearchRepairTask task) {
        if (StringUtils.isBlank(task.getPayloadJson())) {
            throw new IllegalArgumentException("标准地址repair任务缺少payload");
        }
        return JSONUtil.toBean(task.getPayloadJson(), StandardAddressSearchDocument.class);
    }

    private String buildRebuildIndexName(String alias) {
        return String.format("%s_v%s", alias, IdUtil.getSnowflakeNextId());
    }
}
