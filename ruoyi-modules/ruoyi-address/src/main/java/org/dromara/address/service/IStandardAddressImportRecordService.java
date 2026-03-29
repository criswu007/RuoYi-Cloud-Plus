package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressImportRecordBo;
import org.dromara.address.domain.vo.StandardAddressImportRecordVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

public interface IStandardAddressImportRecordService {

    /**
     * 根据ID查询导入记录。
     *
     * @param id 主键ID
     * @return 导入记录详情
     */
    StandardAddressImportRecordVo queryById(Long id);

    /**
     * 分页查询导入记录。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<StandardAddressImportRecordVo> queryPageList(StandardAddressImportRecordBo bo, PageQuery pageQuery);

    /**
     * 查询导入记录列表（不分页）。
     *
     * @param bo 查询条件
     * @return 导入记录列表
     */
    List<StandardAddressImportRecordVo> queryList(StandardAddressImportRecordBo bo);

    /**
     * 保存导入记录（独立事务）。
     *
     * @param fileName 文件名
     * @param status 状态（0进行中 1成功 2失败）
     * @param successCount 成功数量
     * @param failCount 失败数量
     * @param errorMsg 错误信息
     * @param operator 操作人
     *
     * 关键约束：独立事务保存，不受主业务回滚影响。
     */
    void saveRecord(String fileName, String status, int successCount, int failCount, String errorMsg, String operator);
}
