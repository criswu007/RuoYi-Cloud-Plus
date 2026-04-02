package org.dromara.address.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressSelectionRoomBo;
import org.dromara.address.domain.vo.StandardAddressSelectionResultVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.service.IStandardAddressSelectionService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 选址平台核心服务实现。
 * 目的：为选址场景提供楼栋级查询与房间地址创建能力。
 * 关键约束：默认限制到楼栋级，房间号在楼栋内唯一。
 * 副作用：创建房间地址会新增标准地址与安装地址。
 */
@RequiredArgsConstructor
@Service
@DS("address")
public class StandardAddressSelectionServiceImpl implements IStandardAddressSelectionService {

    private static final int DEFAULT_LEVEL_MAX = 10;
    private static final int DEFAULT_LIMIT = 50;
    private static final String ACTIVE_STATUS = "2140900";

    private final StandardAddressQueryService queryService;

    @Override
    /**
     * 执行选址平台模糊查询。
     *
     * @param keyword 关键字（名称或完整名称）
     * @param levelMax 最大业务级别（为空使用默认值 10）
     * @param limit 最大返回条数（为空使用默认值 50）
     * @return 符合条件的标准地址列表
     *
     * 关键约束：仅返回状态正常、业务级别不高于 levelMax 的地址。
     */
    public List<StandardAddressVo> searchStandardAddresses(String keyword, Integer levelMax, Integer limit) {
        int maxLevel = levelMax == null ? DEFAULT_LEVEL_MAX : levelMax;
        int pageSize = limit == null ? DEFAULT_LIMIT : limit;
        Map<String, StandardAddressVo> result = new LinkedHashMap<>();
        for (int addrLevel = 1; addrLevel <= Math.min(maxLevel, 2); addrLevel++) {
            collectRegionMatches(result, keyword, addrLevel, pageSize);
            if (result.size() >= pageSize) {
                return sortResult(result, pageSize);
            }
        }
        collectAddrSegmMatches(result, keyword, maxLevel, pageSize);
        return sortResult(result, pageSize);
    }

    private List<StandardAddressVo> sortResult(Map<String, StandardAddressVo> result, int pageSize) {
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return result.values().stream()
            .sorted(Comparator.comparing(StandardAddressVo::getAddrLevel, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(StandardAddressVo::getSegmId, Comparator.nullsLast(String::compareTo)))
            .limit(pageSize)
            .toList();
    }

    @Override
    /**
     * 在楼栋级地址下创建房间标准地址并生成安装地址。
     *
     * @param bo 房间创建参数
     * @return 新创建的标准地址与安装地址ID
     *
     * 关键约束：父标准地址必须存在且为楼栋级；同一楼栋下房间号唯一。
     * 异常：父标准地址不存在、层级不满足或房间号重复时抛出业务异常。
     * 副作用：新增标准地址与安装地址，产生事务写入。
     */
    public StandardAddressSelectionResultVo createRoomStandardAddress(StandardAddressSelectionRoomBo bo) {
        throw new ServiceException("选址房间创建接口仍基于旧 Long 主键语义，尚未迁移到 segmId 体系，暂未实现");
    }

    private void collectRegionMatches(Map<String, StandardAddressVo> result, String keyword, int addrLevel, int limit) {
        int remainingLimit = limit - result.size();
        if (remainingLimit <= 0) {
            return;
        }
        putResult(result, queryService.searchRegionCandidates(keyword, addrLevel, remainingLimit), limit);
    }

    private void collectAddrSegmMatches(Map<String, StandardAddressVo> result, String keyword, int maxLevel, int limit) {
        int remainingLimit = limit - result.size();
        if (remainingLimit <= 0) {
            return;
        }
        putResult(result, queryService.searchAddrSegmCandidates(keyword, maxLevel, ACTIVE_STATUS, remainingLimit), limit);
    }

    private void putResult(Map<String, StandardAddressVo> result, List<StandardAddressVo> list, int totalLimit) {
        for (StandardAddressVo item : list) {
            if (item == null || StringUtils.isBlank(item.getSegmId())) {
                continue;
            }
            result.putIfAbsent(item.getSegmId(), item);
            if (result.size() >= totalLimit) {
                return;
            }
        }
    }
}
