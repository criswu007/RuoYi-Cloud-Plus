package org.dromara.address.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.address.domain.StandardAddressImportRecord;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = StandardAddressImportRecord.class, reverseConvertGenerate = false)
public class StandardAddressImportRecordBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 导入文件名
     */
    private String fileName;

    /**
     * 状态（0进行中 1成功 2失败）
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建者（用户ID）
     */
    private Long createBy;
}
