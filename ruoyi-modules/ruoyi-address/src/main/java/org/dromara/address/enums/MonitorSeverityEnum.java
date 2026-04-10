package org.dromara.address.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 非标地址监控严重等级枚举。
 * 目的：统一规则配置、异常预警和治理页面中的严重等级口径。
 * 入参/出参：入参为严重等级编码，出参可返回对应展示名称。
 * 关键约束：首版仅支持 `HIGH/MEDIUM/LOW` 三档。
 * 异常与副作用：枚举本身无异常与副作用，未匹配编码时返回 `null`。
 */
@Getter
public enum MonitorSeverityEnum {

    HIGH("HIGH", "高"),
    MEDIUM("MEDIUM", "中"),
    LOW("LOW", "低");

    private final String code;
    private final String label;

    MonitorSeverityEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 目的：根据严重等级编码解析枚举值。
     * 入参：严重等级编码。
     * 出参：匹配到的严重等级枚举，未命中时返回 `null`。
     * 关键约束：编码匹配区分大小写。
     * 异常与副作用：无写入副作用。
     */
    public static MonitorSeverityEnum of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElse(null);
    }

    /**
     * 目的：返回全部严重等级编码列表，供参数校验复用。
     * 入参：无。
     * 出参：严重等级编码列表。
     * 关键约束：返回顺序与页面展示顺序保持一致。
     * 异常与副作用：无写入副作用。
     */
    public static List<String> codes() {
        return Arrays.stream(values()).map(MonitorSeverityEnum::getCode).toList();
    }
}
