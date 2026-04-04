package org.dromara.address.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 地址搜索 ES 相关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "address.search")
public class AddressSearchProperties {

    /**
     * 是否启用地址搜索相关能力。
     */
    private Boolean enabled = Boolean.FALSE;

    /**
     * ES 写入刷新策略。
     */
    private String refreshPolicy = "none";

    /**
     * 全量重建索引时单批处理数量。
     */
    private Integer rebuildBatchSize = 500;

    /**
     * PIT（Point In Time）保活时长。
     */
    private String pitKeepAlive = "1m";

    /**
     * 标准地址索引配置。
     */
    private IndexProperties standard = new IndexProperties();

    /**
     * 安装地址索引配置。
     */
    private IndexProperties installation = new IndexProperties();

    /**
     * 单个索引读写配置。
     */
    @Data
    public static class IndexProperties {

        /**
         * 索引别名。
         */
        private String alias;

        /**
         * 是否允许读。
         */
        private Boolean readEnabled = Boolean.TRUE;

        /**
         * 是否允许写。
         */
        private Boolean writeEnabled = Boolean.TRUE;
    }
}
