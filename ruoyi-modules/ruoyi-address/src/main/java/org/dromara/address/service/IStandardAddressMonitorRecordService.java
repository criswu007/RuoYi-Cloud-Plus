package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressMonitorRecordBo;
import org.dromara.address.domain.vo.StandardAddressMonitorRecordVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

public interface IStandardAddressMonitorRecordService {

    /**
     * 查询监控记录详情。
     *
     * @param id 主键ID
     * @return 监控记录详情
     */
    StandardAddressMonitorRecordVo queryById(Long id);

    /**
     * 分页查询监控记录。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<StandardAddressMonitorRecordVo> queryPageList(StandardAddressMonitorRecordBo bo, PageQuery pageQuery);

    /**
     * 查询监控记录列表（不分页）。
     *
     * @param bo 查询条件
     * @return 监控记录列表
     */
    List<StandardAddressMonitorRecordVo> queryList(StandardAddressMonitorRecordBo bo);

    /**
     * 修改监控记录。
     *
     * @param bo 修改参数
     * @return 是否成功
     */
    Boolean updateByBo(StandardAddressMonitorRecordBo bo);

    /**
     * 批量忽略监控记录。
     *
     * @param ids 记录ID集合
     * @return 是否成功
     *
     * 目的：为异常预警页面提供批量忽略入口。
     * 关键约束：重复忽略需保持幂等，仅更新状态与处理时间等治理字段。
     * 异常与副作用：成功后会批量更新异常记录状态。
     */
    Boolean ignoreByIds(List<Long> ids);

    /**
     * 删除监控记录。
     *
     * @param ids 记录ID集合
     * @param isValid 是否校验业务逻辑
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
