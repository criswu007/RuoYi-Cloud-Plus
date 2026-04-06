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
    private String refreshPolicy = "wait_for";

    /**
     * 全量重建索引时单批处理数量。
     */
    private Integer rebuildBatchSize = 500;

    /**
     * PIT（Point In Time）保活时长。
     */
    private String pitKeepAlive = "1m";

    /**
     * Kibana 运维入口地址。
     */
    private String kibanaUrl = "http://127.0.0.1:5601";

    /**
     * ES 运维写操作串行锁 key。
     */
    private String maintenanceLockKey = "address:search:maintenance:running";

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
        private Boolean readEnabled = Boolean.FALSE;

        /**
         * 是否允许写。
         */
        private Boolean writeEnabled = Boolean.FALSE;
    }

    public AddressSearchProperties() {
        this.standard.setAlias("address_standard_search");
        this.installation.setAlias("address_installation_search");
    }
}
