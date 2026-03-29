package org.dromara.address.dubbo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.address.api.RemoteStandardAddressService;
import org.dromara.address.api.domain.RemoteStandardAddressVo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.service.IStandardAddressService;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 远程地址服务实现（Dubbo）。
 * 目的：为外部系统提供地址查询与校验能力。
 * 关键约束：查询仅基于标准地址，未做层级限制。
 */
@DubboService
@Service
@RequiredArgsConstructor
@DS("address")
public class RemoteStandardAddressServiceImpl implements RemoteStandardAddressService {

    private final IStandardAddressService addressStandardService;

    @Override
    /**
     * 根据ID获取标准地址信息。
     *
     * @param id 标准地址ID
     * @return 远程地址VO，找不到返回 null
     *
     * 异常：无
     */
    public RemoteStandardAddressVo getStandardAddressById(Long id) {
        if (ObjectUtil.isNull(id)) {
            return null;
        }
        StandardAddressVo addressStandardVo = addressStandardService.getStandardAddressBySegmId(String.valueOf(id));
        if (addressStandardVo == null) {
            return null;
        }
        return toRemoteVo(addressStandardVo);
    }

    @Override
    /**
     * 关键字搜索地址。
     *
     * @param keyword 关键字
     * @param limit 最大返回数量
     * @return 匹配结果列表
     *
     * 关键约束：默认返回 10 条；匹配名称与完整名称。
     */
    public List<RemoteStandardAddressVo> searchStandardAddresses(String keyword, Integer limit) {
        if (StringUtils.isBlank(keyword)) {
            return Collections.emptyList();
        }
        int pageSize = limit != null ? limit : 10;
        Map<String, RemoteStandardAddressVo> result = new LinkedHashMap<>();
        collectSearchResult(result, buildRegionSearchBo(keyword, 1), pageSize);
        collectSearchResult(result, buildRegionSearchBo(keyword, 2), pageSize);
        collectSearchResult(result, buildAddrSegmSearchBo(keyword), pageSize);
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(result.values()).subList(0, Math.min(pageSize, result.size()));
    }

    @Override
    /**
     * 校验标准地址ID是否存在。
     *
     * @param id 标准地址ID
     * @return 是否存在
     */
    public Boolean validateStandardAddress(Long id) {
        if (ObjectUtil.isNull(id)) {
            return false;
        }
        StandardAddressVo vo = addressStandardService.getStandardAddressBySegmId(String.valueOf(id));
        return vo != null;
    }

    @Override
    /**
     * 获取完整地址名称。
     *
     * @param id 标准地址ID
     * @return 完整地址名称，不存在返回 null
     */
    public String getStandardAddressFullName(Long id) {
        if (ObjectUtil.isNull(id)) {
            return null;
        }
        StandardAddressVo vo = addressStandardService.getStandardAddressBySegmId(String.valueOf(id));
        return vo != null ? vo.getStandName() : null;
    }

    private void collectSearchResult(Map<String, RemoteStandardAddressVo> result, StandardAddressBo bo, int limit) {
        List<StandardAddressVo> list = addressStandardService.queryStandardAddressList(bo);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        for (StandardAddressVo item : list) {
            if (item == null || StringUtils.isBlank(item.getSegmId()) || result.size() >= limit) {
                continue;
            }
            result.putIfAbsent(item.getSegmId(), toRemoteVo(item));
        }
    }

    private StandardAddressBo buildRegionSearchBo(String keyword, Integer levelId) {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setLevelId(levelId);
        bo.setSegmName(keyword);
        return bo;
    }

    private StandardAddressBo buildAddrSegmSearchBo(String keyword) {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setStandName(keyword);
        return bo;
    }

    private RemoteStandardAddressVo toRemoteVo(StandardAddressVo vo) {
        RemoteStandardAddressVo remote = BeanUtil.toBean(vo, RemoteStandardAddressVo.class);
        remote.setId(parseLegacyId(vo.getSegmId()));
        remote.setName(vo.getSegmName());
        remote.setFullName(vo.getStandName());
        remote.setLevel(vo.getLevelId());
        remote.setCode(vo.getSegmNo());
        return remote;
    }

    private Long parseLegacyId(String segmId) {
        if (StringUtils.isBlank(segmId)) {
            return null;
        }
        try {
            return Long.valueOf(segmId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
