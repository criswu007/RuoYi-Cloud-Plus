package org.dromara.address.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.address.domain.StandardAddressImportRecord;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = StandardAddressImportRecord.class)
public class StandardAddressImportRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failCount;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建者（用户ID）
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Date createTime;
}
