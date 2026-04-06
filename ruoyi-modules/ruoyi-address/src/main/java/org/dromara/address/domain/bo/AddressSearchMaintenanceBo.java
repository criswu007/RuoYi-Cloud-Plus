package org.dromara.address.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public final class AddressSearchMaintenanceBo {

    private AddressSearchMaintenanceBo() {
    }

    @Data
    public static class RebuildTaskSubmitBo {

        /**
         * 重建确认口令。
         */
        @NotBlank(message = "confirmationCode不能为空")
        private String confirmationCode;
    }

    @Data
    public static class RepairExecuteBo {

        /**
         * repair 执行确认口令。
         */
        @NotBlank(message = "confirmationCode不能为空")
        private String confirmationCode;
    }

    @Data
    public static class TaskQueryBo {

        /**
         * 任务类型。
         */
        private String taskType;

        /**
         * 任务状态。
         */
        private String status;
    }

    @Data
    public static class RepairTaskQueryBo {

        /**
         * repair 实体类型。
         */
        private String entityType;

        /**
         * repair 状态。
         */
        private String status;
    }
}
