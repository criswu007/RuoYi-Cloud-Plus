package org.dromara.address.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.util.List;

/**
 * 网格管理业务对象集合。
 */
public final class GridManagementBo {

    private GridManagementBo() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GridOrgBo extends BaseEntity {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 主键ID。
         */
        private Long id;

        /**
         * 父级组织ID。
         */
        private Long parentId;

        /**
         * 组织名称。
         */
        private String orgName;

        /**
         * 状态。
         */
        private String status;

        /**
         * 备注。
         */
        private String remark;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GridBo extends BaseEntity {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 主键ID。
         */
        private Long id;

        /**
         * 网格编码。
         */
        private String gridId;

        /**
         * 网格名称。
         */
        private String gridName;

        /**
         * 网格属性。
         */
        private String gridProperty;

        /**
         * 网格经理ID。
         */
        private Long gridManagerId;

        /**
         * 网格经理名称。
         */
        private String gridManager;

        /**
         * 归属站点。
         */
        private String belongStation;

        /**
         * 组织ID。
         */
        private Long orgId;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GridAddressRelationBo extends BaseEntity {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 关联ID。
         */
        private Long relationId;

        /**
         * 地址ID。
         */
        private Long addressId;

        /**
         * 地址名称。
         */
        private String addressName;

        /**
         * 网格ID。
         */
        private Long gridId;

        /**
         * 网格名称。
         */
        private String gridName;

        /**
         * 待绑定地址ID集合。
         */
        private List<Long> addressIds;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GridCustomerBo extends BaseEntity {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 客户ID。
         */
        private String customerId;

        /**
         * 客户名称。
         */
        private String customerName;

        /**
         * 手机号。
         */
        private String phone;

        /**
         * 地址。
         */
        private String address;

        /**
         * 网格ID。
         */
        private Long gridId;

        /**
         * 网格名称。
         */
        private String gridName;

        /**
         * 待绑定客户ID集合。
         */
        private List<String> customerIds;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GridManagerBo extends BaseEntity {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 主键ID。
         */
        private Long id;

        /**
         * 经理编码。
         */
        private String managerId;

        /**
         * 经理名称。
         */
        private String managerName;

        /**
         * 组织ID。
         */
        private Long orgId;

        /**
         * 组织名称。
         */
        private String orgName;

        /**
         * 手机号。
         */
        private String phone;

        /**
         * 网格ID。
         */
        private Long gridId;

        /**
         * 网格名称。
         */
        private String gridName;

        /**
         * 关联网格ID集合。
         */
        private List<Long> gridIds;
    }
}
