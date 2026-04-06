package org.dromara.address.search.model;

import co.elastic.clients.elasticsearch._types.FieldValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Search After 导出批次结果。
 * 目的：统一承载 ES 导出单批结果、下一次查询游标与 PIT 上下文，供流式导出服务按批推进。
 * 入参/出参：作为网关返回值输出当前批次数据、下一批 `search_after` 值与最新 `pitId`。
 * 关键约束：当 `finished=false` 时，调用方必须继续使用 `nextPitId + nextSearchAfter` 拉取后续批次，避免导出漏数或重复。
 * 异常与副作用：纯数据载体，无数据库写入副作用。
 */
@Getter
@AllArgsConstructor(staticName = "of")
public class SearchAfterBatch<T> {

    private final List<T> rows;
    private final String nextPitId;
    private final List<FieldValue> nextSearchAfter;
    private final boolean finished;
}
