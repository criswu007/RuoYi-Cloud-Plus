package org.dromara.address.search.model;

/**
 * 地址搜索索引运行态信息。
 *
 * @param alias 索引别名
 * @param physicalIndexName 当前物理索引名
 * @param documentCount 当前文档数
 */
public record AddressSearchIndexRuntimeInfo(
    String alias,
    String physicalIndexName,
    long documentCount
) {
}
