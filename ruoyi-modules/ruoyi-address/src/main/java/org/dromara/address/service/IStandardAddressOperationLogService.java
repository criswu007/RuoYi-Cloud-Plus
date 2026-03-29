package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressOperationLogBo;
import org.dromara.address.domain.vo.StandardAddressOperationLogVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

public interface IStandardAddressOperationLogService {

    /**
     * 查询操作日志详情。
     *
     * @param id 主键ID
     * @return 详情
     */
    StandardAddressOperationLogVo queryById(Long id);

    /**
     * 分页查询操作日志。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<StandardAddressOperationLogVo> queryPageList(StandardAddressOperationLogBo bo, PageQuery pageQuery);

    /**
     * 查询操作日志列表（不分页）。
     *
     * @param bo 查询条件
     * @return 列表
     */
    List<StandardAddressOperationLogVo> queryList(StandardAddressOperationLogBo bo);
}
