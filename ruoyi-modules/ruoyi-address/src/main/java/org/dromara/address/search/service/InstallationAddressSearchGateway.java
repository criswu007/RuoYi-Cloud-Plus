package org.dromara.address.search.service;

import org.dromara.address.domain.AddrSetSegm;
import org.dromara.address.domain.AddressSearchRepairTask;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.search.document.InstallationAddressSearchDocument;
import org.dromara.address.search.model.AddressSearchIndexRuntimeInfo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

/**
 * 安装地址 ES 读链路网关。
 * <p>
 * 目的：统一封装安装地址分页检索在 ES 中的查询入口，供服务层按开关切换读链路。
 * 关键约束：仅负责 ES 检索与文档映射，不承担标准地址名称补数与关联状态重算。
 * 异常与副作用：会访问 ES 索引；不直接写数据库。
 * </p>
 */
public interface InstallationAddressSearchGateway {

    /**
     * 目的：探测当前安装地址 ES 读写端点是否可达。
     * 入参：无。
     * 出参：`true` 表示探测成功。
     * 关键约束：仅作为运维概览健康探针，不承担写链路兜底。
     * 异常与副作用：会访问 ES；实现方应自行吞掉探测异常并返回 `false`。
     */
    boolean ping();

    /**
     * 目的：获取安装地址索引运行态信息。
     * 入参：无。
     * 出参：包含 alias、当前物理索引名和文档数的运行态对象。
     * 关键约束：统计口径基于当前 alias 指向；不存在 alias 时允许回退展示 alias 本身。
     * 异常与副作用：会访问 ES 查询 alias 和 count 信息。
     */
    AddressSearchIndexRuntimeInfo getRuntimeInfo();

    /**
     * 目的：为安装地址全量重建准备新的物理索引。
     * 入参：无。
     * 出参：新建成功的物理索引名。
     * 关键约束：创建的新索引不能提前绑定读别名，避免重建期间误切换线上流量。
     * 异常与副作用：会访问 ES 创建索引，不写数据库。
     */
    String prepareRebuildIndex();

    /**
     * 目的：向指定安装地址物理索引批量写入重建数据。
     * 入参：目标物理索引名与安装地址实体批次。
     * 出参：`true` 表示当前批写入成功。
     * 关键约束：仅用于全量重建，不走在线写链路的 `refresh=wait_for` 模板；调用方负责分页游标推进。
     * 异常与副作用：会批量写 ES，不写数据库；失败时允许抛出异常。
     *
     * @param indexName 目标物理索引名
     * @param entities 安装地址实体批次
     * @return 当前批是否写入成功
     */
    boolean bulkIndex(String indexName, List<AddrSetSegm> entities);

    /**
     * 目的：把安装地址读别名切换到新的物理索引。
     * 入参：新物理索引名。
     * 出参：无。
     * 关键约束：切换必须在当前重建索引 refresh 完成后执行，并移除旧物理索引上的同名别名。
     * 异常与副作用：会更新 ES 别名，不写数据库。
     *
     * @param newIndexName 新物理索引名
     */
    void switchAlias(String newIndexName);

    /**
     * 目的：按后台列表条件分页查询安装地址。
     * 入参：安装地址查询条件与分页参数。
     * 出参：安装地址分页结果。
     * 关键约束：筛选、排序与分页都需收敛在 ES 查询侧完成，保持后台列表语义稳定。
     * 异常与副作用：会访问 ES 检索索引，不写数据库。
     *
     * @param bo 安装地址查询条件
     * @param pageQuery 分页参数
     * @return 安装地址分页结果
     */
    TableDataInfo<InstallationAddressVo> queryPage(InstallationAddressBo bo, PageQuery pageQuery);

    /**
     * 目的：向安装地址索引写入或覆盖单条文档。
     * 入参：安装地址搜索文档。
     * 出参：`true` 表示 ES 写入成功，`false` 表示执行完成但结果失败。
     * 关键约束：调用方依赖该方法支撑“先 ES、后 DB”的立即一致模板，写入需应用统一 refresh 策略。
     * 异常与副作用：会写 ES，不写数据库；失败时允许抛出异常。
     *
     * @param document 安装地址搜索文档
     * @return ES 写入是否成功
     */
    boolean upsert(InstallationAddressSearchDocument document);

    /**
     * 目的：按主键集合删除安装地址索引文档。
     * 入参：安装地址主键集合。
     * 出参：`true` 表示 ES 删除成功，`false` 表示执行完成但结果失败。
     * 关键约束：用于新增补偿和删除主流程，空集合应直接视为成功。
     * 异常与副作用：会删除 ES 文档，不写数据库；失败时允许抛出异常。
     *
     * @param setAddrIds 安装地址主键集合
     * @return ES 删除是否成功
     */
    boolean deleteByIds(List<String> setAddrIds);

    /**
     * 目的：使用旧快照恢复安装地址索引文档。
     * 入参：安装地址旧快照文档。
     * 出参：`true` 表示恢复成功，`false` 表示执行完成但结果失败。
     * 关键约束：用于数据库失败后的同步补偿，恢复内容必须以旧快照为准。
     * 异常与副作用：会写 ES，不写数据库；失败时允许抛出异常。
     *
     * @param document 安装地址旧快照文档
     * @return ES 恢复是否成功
     */
    boolean restore(InstallationAddressSearchDocument document);

    /**
     * 目的：执行单条安装地址 repair 任务。
     * 入参：repair 任务实体。
     * 出参：`true` 表示 repair 成功。
     * 关键约束：当前仅支持按主键删除文档或按快照恢复文档，两类动作都必须落到安装地址索引别名。
     * 异常与副作用：会访问 ES 执行修复，不写数据库；失败时允许抛出异常。
     *
     * @param task repair 任务
     * @return repair 是否成功
     */
    boolean repair(AddressSearchRepairTask task);
}
