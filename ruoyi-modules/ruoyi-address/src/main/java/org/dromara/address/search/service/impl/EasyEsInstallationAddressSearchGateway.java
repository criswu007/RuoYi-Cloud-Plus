package org.dromara.address.search.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddrSetSegm;
import org.dromara.address.domain.AddressSearchRepairTask;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.esmapper.InstallationAddressSearchEsMapper;
import org.dromara.address.search.builder.InstallationAddressSearchDocumentBuilder;
import org.dromara.address.search.document.InstallationAddressSearchDocument;
import org.dromara.address.search.model.AddressSearchIndexRuntimeInfo;
import org.dromara.address.search.service.InstallationAddressSearchGateway;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.easyes.core.biz.CreateIndexParam;
import org.dromara.easyes.core.biz.EntityInfo;
import org.dromara.easyes.core.biz.EsPageInfo;
import org.dromara.easyes.core.conditions.select.LambdaEsQueryWrapper;
import org.dromara.easyes.core.toolkit.EntityInfoHelper;
import org.dromara.easyes.core.toolkit.IndexUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Easy-ES 的安装地址搜索网关实现。
 * <p>
 * 目的：把安装地址后台分页查询统一路由到 ES，并映射回安装地址视图对象。
 * 关键约束：必须显式切换当前活跃索引别名；排序保持与数据库分页 SQL 一致。
 * 异常与副作用：会访问 ES 检索索引，不写数据库。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class EasyEsInstallationAddressSearchGateway implements InstallationAddressSearchGateway {

    private static final String REPAIR_ACTION_DELETE_DOC = "DELETE_DOC";
    private static final String REPAIR_ACTION_UPSERT_DOC = "UPSERT_DOC";

    private final InstallationAddressSearchEsMapper installationAddressSearchEsMapper;
    private final AddressSearchProperties addressSearchProperties;
    private final ElasticsearchClient elasticsearchClient;

    /**
     * 目的：探测安装地址 ES 端点是否可达。
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
     * 目的：获取安装地址索引当前 alias 指向与文档总量。
     * 入参：无。
     * 出参：安装地址索引运行态对象。
     * 关键约束：物理索引名优先取 alias 实际指向；若查询 alias 失败则抛出业务异常。
     * 异常与副作用：会访问 ES alias/count 接口，无数据库副作用。
     */
    @Override
    public AddressSearchIndexRuntimeInfo getRuntimeInfo() {
        String alias = addressSearchProperties.getInstallation().getAlias();
        String physicalIndexName = resolveCurrentPhysicalIndex(alias);
        long documentCount = countDocuments(alias);
        return new AddressSearchIndexRuntimeInfo(alias, physicalIndexName, documentCount);
    }

    /**
     * 目的：创建安装地址重建使用的新物理索引。
     * 入参：无。
     * 出参：新物理索引名。
     * 关键约束：创建时不能绑定线上读别名，避免重建中途流量提前切换。
     * 异常与副作用：会访问 ES 创建新索引，不写数据库。
     */
    @Override
    public String prepareRebuildIndex() {
        String rebuildIndex = buildRebuildIndexName(addressSearchProperties.getInstallation().getAlias());
        createPhysicalIndexWithoutAlias(rebuildIndex);
        return rebuildIndex;
    }

    /**
     * 目的：批量写入安装地址重建数据。
     * 入参：目标物理索引名与安装地址实体批次。
     * 出参：`true` 表示当前批写入成功。
     * 关键约束：写入前需把安装地址实体转换为完整搜索文档，并保留当前关联状态。
     * 异常与副作用：会批量写 ES，不写数据库。
     */
    @Override
    public boolean bulkIndex(String indexName, List<AddrSetSegm> entities) {
        List<InstallationAddressSearchDocument> documents = entities == null
            ? List.of()
            : entities.stream()
            .map(entity -> InstallationAddressSearchDocumentBuilder.fromEntity(entity, resolveAssociationStatus(entity)))
            .toList();
        if (CollUtil.isEmpty(documents)) {
            return true;
        }
        installationAddressSearchEsMapper.setCurrentActiveIndex(indexName);
        Integer affected = installationAddressSearchEsMapper.insertBatch(indexName, documents);
        return affected != null && affected >= documents.size();
    }

    /**
     * 目的：将安装地址读别名切换到新的物理索引。
     * 入参：新的物理索引名。
     * 出参：无。
     * 关键约束：切换前必须先 refresh 当前重建索引，确保别名切换后结果立即可读。
     * 异常与副作用：会访问 ES refresh 索引并更新别名，不写数据库。
     */
    @Override
    public void switchAlias(String newIndexName) {
        installationAddressSearchEsMapper.refresh(newIndexName);
        String alias = addressSearchProperties.getInstallation().getAlias();
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
            throw new IllegalStateException("切换安装地址索引别名失败", ex);
        }
    }

    /**
     * 目的：在 ES 中执行安装地址后台分页查询。
     * 入参：后台查询条件与分页参数。
     * 出参：安装地址分页结果。
     * 关键约束：名称字段需走“所有分词都命中”的同字段检索，避免分词 OR 命中其他安装地址；其余结构化字段走 term；默认排序保持 `createDate desc, setAddrId desc`。
     * 异常与副作用：会访问 ES 索引，不写数据库。
     */
    @Override
    public TableDataInfo<InstallationAddressVo> queryPage(InstallationAddressBo bo, PageQuery pageQuery) {
        InstallationAddressBo queryBo = bo == null ? new InstallationAddressBo() : bo;
        installationAddressSearchEsMapper.setCurrentActiveIndex(addressSearchProperties.getInstallation().getAlias());
        LambdaEsQueryWrapper<InstallationAddressSearchDocument> wrapper = new LambdaEsQueryWrapper<>();
        wrapper.multiMatchQuery(StringUtils.isNotBlank(queryBo.getSetAddrName()), queryBo.getSetAddrName(), Operator.And, 0, null, "setAddrName");
        wrapper.eq(StringUtils.isNotBlank(queryBo.getSetType()), InstallationAddressSearchDocument::getSetType, queryBo.getSetType());
        wrapper.eq(StringUtils.isNotBlank(queryBo.getSegmId()), InstallationAddressSearchDocument::getSegmId, queryBo.getSegmId());
        wrapper.eq(StringUtils.isNotBlank(queryBo.getRegionId()), InstallationAddressSearchDocument::getRegionId, queryBo.getRegionId());
        wrapper.eq(StringUtils.isNotBlank(queryBo.getOrgId()), InstallationAddressSearchDocument::getOrgId, queryBo.getOrgId());
        wrapper.eq(StringUtils.isNotBlank(queryBo.getAssociationStatus()), InstallationAddressSearchDocument::getAssociationStatus, queryBo.getAssociationStatus());
        wrapper.orderByDesc(InstallationAddressSearchDocument::getCreateDate, InstallationAddressSearchDocument::getSetAddrId);

        PageQuery actualPageQuery = pageQuery == null ? new PageQuery() : pageQuery;
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<?> page = actualPageQuery.build();
        EsPageInfo<InstallationAddressSearchDocument> pageInfo = installationAddressSearchEsMapper.pageQuery(wrapper, (int) page.getCurrent(), (int) page.getSize());
        if (pageInfo == null || pageInfo.getList() == null) {
            return new TableDataInfo<>(Collections.emptyList(), 0);
        }
        return new TableDataInfo<>(pageInfo.getList().stream().map(this::toVo).toList(), pageInfo.getTotal());
    }

    /**
     * 目的：向安装地址索引写入或覆盖单条文档。
     * 入参：安装地址搜索文档。
     * 出参：`true` 表示 ES 写入成功。
     * 关键约束：写入必须命中当前安装地址索引别名，并应用统一 refresh 策略保障立即一致读语义。
     * 异常与副作用：会写 ES，不写数据库；失败时抛出异常。
     */
    @Override
    public boolean upsert(InstallationAddressSearchDocument document) {
        if (document == null || StringUtils.isBlank(document.getSetAddrId())) {
            return false;
        }
        try {
            elasticsearchClient.index(request -> request
                .index(addressSearchProperties.getInstallation().getAlias())
                .id(document.getSetAddrId())
                .document(document)
                .refresh(resolveRefreshPolicy()));
            return true;
        } catch (IOException ex) {
            throw new IllegalStateException("写入安装地址 ES 文档失败", ex);
        }
    }

    /**
     * 目的：按主键集合删除安装地址索引文档。
     * 入参：安装地址主键集合。
     * 出参：`true` 表示删除完成。
     * 关键约束：空集合直接返回成功；逐条删除时统一使用配置化 refresh 策略。
     * 异常与副作用：会删除 ES 文档，不写数据库；失败时抛出异常。
     */
    @Override
    public boolean deleteByIds(List<String> setAddrIds) {
        if (CollUtil.isEmpty(setAddrIds)) {
            return true;
        }
        try {
            for (String setAddrId : setAddrIds) {
                if (StringUtils.isBlank(setAddrId)) {
                    continue;
                }
                elasticsearchClient.delete(request -> request
                    .index(addressSearchProperties.getInstallation().getAlias())
                    .id(setAddrId)
                    .refresh(resolveRefreshPolicy()));
            }
            return true;
        } catch (IOException ex) {
            throw new IllegalStateException("删除安装地址 ES 文档失败", ex);
        }
    }

    /**
     * 目的：按旧快照恢复安装地址索引文档。
     * 入参：安装地址旧快照文档。
     * 出参：`true` 表示恢复成功。
     * 关键约束：恢复逻辑与普通 upsert 一致，始终以旧快照内容为准。
     * 异常与副作用：会写 ES，不写数据库；失败时抛出异常。
     */
    @Override
    public boolean restore(InstallationAddressSearchDocument document) {
        return upsert(document);
    }

    /**
     * 目的：执行安装地址 repair 任务。
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
            default -> throw new IllegalArgumentException("不支持的安装地址repair动作: " + task.getRepairAction());
        };
    }

    private InstallationAddressVo toVo(InstallationAddressSearchDocument source) {
        InstallationAddressVo target = new InstallationAddressVo();
        target.setSetAddrId(source.getSetAddrId());
        target.setSegmId(source.getSegmId());
        target.setSetAddrName(source.getSetAddrName());
        target.setStandName(source.getStandName());
        target.setSetAddrNo(source.getSetAddrNo());
        target.setSetType(source.getSetType());
        target.setSegmType(source.getSegmType());
        target.setRegionId(source.getRegionId());
        target.setOrgId(source.getOrgId());
        target.setAssociationStatus(source.getAssociationStatus());
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
            throw new IllegalStateException("查询安装地址 alias 指向失败", ex);
        }
    }

    private long countDocuments(String alias) {
        try {
            if (!aliasExists(alias) && !indexExists(alias)) {
                return 0L;
            }
            return elasticsearchClient.count(request -> request.index(alias)).count();
        } catch (IOException ex) {
            throw new IllegalStateException("统计安装地址索引文档数失败", ex);
        }
    }

    private void createPhysicalIndexWithoutAlias(String indexName) {
        EntityInfo entityInfo = EntityInfoHelper.getEntityInfo(InstallationAddressSearchDocument.class);
        CreateIndexParam createIndexParam = IndexUtils.getCreateIndexParam(entityInfo, InstallationAddressSearchDocument.class);
        createIndexParam.setIndexName(indexName);
        createIndexParam.setAliasName(null);
        if (!IndexUtils.createIndex(elasticsearchClient, entityInfo, createIndexParam)) {
            throw new IllegalStateException("创建安装地址重建索引失败");
        }
    }

    private List<String> resolveAliasIndices(String alias) {
        try {
            if (!aliasExists(alias)) {
                return List.of();
            }
            return List.copyOf(elasticsearchClient.indices().getAlias(request -> request.name(alias)).result().keySet());
        } catch (IOException ex) {
            throw new IllegalStateException("查询安装地址索引别名失败", ex);
        }
    }

    private boolean aliasExists(String alias) throws IOException {
        return elasticsearchClient.indices().existsAlias(request -> request.name(alias)).value();
    }

    private boolean indexExists(String indexName) throws IOException {
        return elasticsearchClient.indices().exists(request -> request.index(indexName)).value();
    }

    private String resolveAssociationStatus(AddrSetSegm entity) {
        return entity != null && StringUtils.isNotBlank(entity.getSegmId()) ? "BOUND" : "UNBOUND";
    }

    private List<String> resolveRepairIds(String entityId) {
        return StringUtils.isBlank(entityId) ? List.of() : List.of(entityId);
    }

    private InstallationAddressSearchDocument parseRepairDocument(AddressSearchRepairTask task) {
        if (StringUtils.isBlank(task.getPayloadJson())) {
            throw new IllegalArgumentException("安装地址repair任务缺少payload");
        }
        return JSONUtil.toBean(task.getPayloadJson(), InstallationAddressSearchDocument.class);
    }

    private String buildRebuildIndexName(String alias) {
        return String.format("%s_v%s", alias, IdUtil.getSnowflakeNextId());
    }
}
