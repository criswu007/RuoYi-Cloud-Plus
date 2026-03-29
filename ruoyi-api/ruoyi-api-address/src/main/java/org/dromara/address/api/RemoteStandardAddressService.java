package org.dromara.address.api;

import org.dromara.address.api.domain.RemoteStandardAddressVo;

import java.util.List;

/**
 * 远程地址服务接口
 *
 * @author Lion Li
 */
public interface RemoteStandardAddressService {

    /**
     * 根据ID获取标准地址详情
     *
     * @param id 标准地址ID
     * @return 标准地址详情
     */
    RemoteStandardAddressVo getStandardAddressById(Long id);

    /**
     * 搜索标准地址
     *
     * @param keyword 关键词
     * @param limit   限制数量
     * @return 标准地址列表
     */
    List<RemoteStandardAddressVo> searchStandardAddresses(String keyword, Integer limit);

    /**
     * 校验标准地址是否存在
     *
     * @param id 标准地址ID
     * @return 结果
     */
    Boolean validateStandardAddress(Long id);

    /**
     * 获取标准地址完整名称
     *
     * @param id 标准地址ID
     * @return 标准地址完整名称
     */
    String getStandardAddressFullName(Long id);
}
