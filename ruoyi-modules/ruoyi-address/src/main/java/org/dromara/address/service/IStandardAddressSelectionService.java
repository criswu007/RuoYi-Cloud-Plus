package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressSelectionRoomBo;
import org.dromara.address.domain.vo.StandardAddressSelectionResultVo;
import org.dromara.address.domain.vo.StandardAddressVo;

import java.util.List;

public interface IStandardAddressSelectionService {

    /**
     * 选址平台模糊查询接口。
     *
     * @param keyword 关键字（名称或完整名称）
     * @param levelMax 最大层级（为空使用默认值 10）
     * @param limit 最大返回条数（为空使用默认值 50）
     * @return 标准地址列表
     *
     * 关键约束：仅返回状态正常的标准地址。
     */
    List<StandardAddressVo> searchStandardAddresses(String keyword, Integer levelMax, Integer limit);

    /**
     * 楼栋级下创建房间标准地址并生成安装地址。
     *
     * @param bo 房间创建参数
     * @return 新创建的标准地址与安装地址ID
     *
     * 关键约束：父标准地址必须为楼栋级且房间号唯一。
     * 异常：父地址不存在或房间重复时抛出业务异常。
     * 副作用：新增标准地址与安装地址。
     */
    StandardAddressSelectionResultVo createRoomStandardAddress(StandardAddressSelectionRoomBo bo);
}
