package org.dromara.address.dubbo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.address.api.RemoteStandardAddressService;
import org.dromara.address.api.domain.RemoteStandardAddressVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.service.impl.StandardAddressQueryService;
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

    private final StandardAddressQueryService queryService;

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
        StandardAddressVo addressStandardVo = queryService.getBySegmId(String.valueOf(id), null);
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
        collectSearchResult(result, queryService.searchRegionCandidates(keyword, 1, pageSize), pageSize);
        collectSearchResult(result, queryService.searchRegionCandidates(keyword, 2, pageSize - result.size()), pageSize);
        collectSearchResult(result, queryService.searchAddrSegmCandidates(keyword, null, null, pageSize - result.size()), pageSize);
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(result.values());
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
        StandardAddressVo vo = queryService.getBySegmId(String.valueOf(id), null);
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
        StandardAddressVo vo = queryService.getBySegmId(String.valueOf(id), null);
        return vo != null ? vo.getStandName() : null;
    }

    private void collectSearchResult(Map<String, RemoteStandardAddressVo> result, List<StandardAddressVo> list, int limit) {
        if (limit <= 0) {
            return;
        }
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

    private RemoteStandardAddressVo toRemoteVo(StandardAddressVo vo) {
        RemoteStandardAddressVo remote = BeanUtil.toBean(vo, RemoteStandardAddressVo.class);
        remote.setId(parseLegacyId(vo.getSegmId()));
        remote.setName(vo.getSegmName());
        remote.setFullName(vo.getStandName());
        remote.setLevel(vo.getAddrLevel());
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
