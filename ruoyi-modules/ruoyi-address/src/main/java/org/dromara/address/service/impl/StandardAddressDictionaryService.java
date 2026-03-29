package org.dromara.address.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.address.mapper.SegmAddrTypeMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 标准地址字典服务。
 * 目的：统一封装线上 `segm_addr_type` 的层级解释能力，避免控制器和业务服务直接拼接字典查询。
 * 入参/出参：输入真实 `segmType` 或 `levelId`，输出层级或类型集合。
 * 关键约束：层级解释必须以线上 `segm_addr_type` 为唯一来源，不能回退到旧模块固定数字常量。
 * 异常与副作用：只读服务，无数据库写入副作用。
 */
@Service
@RequiredArgsConstructor
public class StandardAddressDictionaryService {

    private final SegmAddrTypeMapper segmAddrTypeMapper;

    /**
     * 目的：根据地址类型编码解析层级 ID。
     * 入参：线上 `ADDR_SEGM.segm_type` 对应的地址类型编码。
     * 出参：层级 ID，不存在时返回 `null`。
     * 关键约束：只接受线上真实 `segmType`，不做旧层级常量兜底。
     * 异常与副作用：无写入副作用。
     */
    public Integer resolveLevelId(String segmType) {
        if (segmType == null || segmType.isBlank()) {
            return null;
        }
        return segmAddrTypeMapper.selectLevelIdByAddrTypeId(segmType);
    }

    /**
     * 目的：根据层级 ID 解析候选地址类型编码集合。
     * 入参：层级 ID。
     * 出参：地址类型编码集合，未命中时返回空集合。
     * 关键约束：结果直接对应线上 `segm_addr_type.level_id`。
     * 异常与副作用：无写入副作用。
     */
    public List<String> resolveSegmTypesByLevelId(Integer levelId) {
        if (levelId == null) {
            return Collections.emptyList();
        }
        List<String> segmTypes = segmAddrTypeMapper.selectAddrTypeIdsByLevelId(levelId);
        return segmTypes == null ? Collections.emptyList() : segmTypes;
    }

    /**
     * 目的：根据当前层级解析下一个层级 ID。
     * 入参：当前层级 ID。
     * 出参：下一个层级 ID；不存在时返回 `null`。
     * 关键约束：层级顺序完全以线上 `segm_addr_type.level_id` 排序结果为准。
     * 异常与副作用：无写入副作用。
     */
    public Integer resolveNextLevelId(Integer currentLevelId) {
        if (currentLevelId == null) {
            return null;
        }
        return segmAddrTypeMapper.selectNextLevelId(currentLevelId);
    }

    /**
     * 目的：解析指定层级的默认地址类型编码。
     * 入参：层级 ID。
     * 出参：默认地址类型编码；不存在时返回 `null`。
     * 关键约束：当前按 `addr_type_id` 升序取第一条，后续若联调库提供更明确规则可替换此策略。
     * 异常与副作用：无写入副作用。
     */
    public String resolveDefaultSegmTypeByLevelId(Integer levelId) {
        if (levelId == null) {
            return null;
        }
        return segmAddrTypeMapper.selectFirstAddrTypeIdByLevelId(levelId);
    }
}
