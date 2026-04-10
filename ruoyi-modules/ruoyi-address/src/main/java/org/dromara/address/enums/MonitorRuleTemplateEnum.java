package org.dromara.address.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 非标地址监控规则模板枚举。
 * 目的：固化首版模板化规则类型，避免规则模板在前后端和任务执行链路中出现魔法值散落。
 * 入参/出参：入参为模板编码，出参可返回对应枚举、展示名称与说明。
 * 关键约束：首版仅支持固定模板，不开放自由脚本或动态表达式。
 * 异常与副作用：枚举本身无异常与副作用，未匹配编码时返回 `null`。
 */
@Getter
public enum MonitorRuleTemplateEnum {

    FORMAT_STANDARD("FORMAT_STANDARD", "格式规范性检测", "检测非法字符、连续分隔符、异常短名和纯数字等格式问题"),
    REGION_COMPLIANCE("REGION_COMPLIANCE", "行政区划合规性检测", "检测地址与区域主数据、父级链路之间的一致性"),
    ELEMENT_COMPLETENESS("ELEMENT_COMPLETENESS", "地址要素完整性检测", "检测不同地址层级下的名称、属性和父级要素是否缺失"),
    SMART_SUSPECT("SMART_SUSPECT", "智能疑似异常检测", "检测名称相似、父子重复、可疑缩写等疑似异常场景");

    private final String code;
    private final String label;
    private final String description;

    MonitorRuleTemplateEnum(String code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    /**
     * 目的：根据模板编码解析枚举值。
     * 入参：模板编码。
     * 出参：匹配到的模板枚举，未命中时返回 `null`。
     * 关键约束：编码匹配区分大小写，保持与数据库存储值一致。
     * 异常与副作用：无写入副作用。
     */
    public static MonitorRuleTemplateEnum of(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElse(null);
    }

    /**
     * 目的：返回全部模板编码列表，供校验和下拉选项构造复用。
     * 入参：无。
     * 出参：模板编码列表。
     * 关键约束：返回顺序与原型页面展示顺序保持一致。
     * 异常与副作用：无写入副作用。
     */
    public static List<String> codes() {
        return Arrays.stream(values()).map(MonitorRuleTemplateEnum::getCode).toList();
    }
}
