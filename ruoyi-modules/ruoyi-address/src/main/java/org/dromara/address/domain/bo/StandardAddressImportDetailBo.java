package org.dromara.address.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.address.domain.StandardAddressImportBatch;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = StandardAddressImportBatch.class, reverseConvertGenerate = false)
public class StandardAddressImportDetailBo extends BaseEntity {

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * 导入文件名
     */
    private String fileName;

    /**
     * 状态（0进行中 1成功 2失败）
     */
    private String status;

    /**
     * 当级名称关键字
     */
    private String segmName;

    /**
     * 创建者（用户ID）
     */
    private Long createBy;

    /**
     * 原始主键兼容字段
     */
    @Deprecated
    public Long getId() {
        return batchId;
    }
}
