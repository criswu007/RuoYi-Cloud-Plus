package org.dromara.address.monitor.detector;

import cn.hutool.json.JSONUtil;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.enums.MonitorRuleTemplateEnum;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 地址要素完整性 detector。
 * 目的：承接地址要素完整性模板的基础检测逻辑，首版先识别当前名称缺失等硬性缺口。
 * 入参/出参：入参为规则实体与检测上下文，出参为命中结果。
 * 关键约束：首版只实现稳定的基础完整性校验，不直接依赖额外字典或外部主数据。
 * 异常与副作用：无数据库写入副作用。
 */
@Component
public class ElementCompletenessDetector implements MonitorRuleDetector {

    @Override
    public String getTemplateCode() {
        return MonitorRuleTemplateEnum.ELEMENT_COMPLETENESS.getCode();
    }

    @Override
    /**
     * 目的：检查单条标准地址的基础要素是否缺失。
     * 入参：监控规则实体与检测上下文。
     * 出参：命中结果；未命中返回 `null`。
     * 关键约束：当前仅把 `segmName` 缺失视为要素不完整，避免与其他模板职责交叉过多。
     * 异常与副作用：无数据库写入副作用。
     */
    public MonitorHitResult detect(StandardAddressMonitorRule rule, MonitorDetectContext context) {
        if (rule == null || context == null) {
            return null;
        }
        if (StringUtils.isBlank(context.getSegmName())) {
            return buildHit(rule, context, "CURRENT_NAME_MISSING", "标准地址缺少当级名称");
        }
        return null;
    }

    /**
     * 目的：构造地址要素完整性命中结果。
     * 入参：规则、检测上下文、问题编码与提示信息。
     * 出参：标准化命中结果。
     * 关键约束：去重键需稳定绑定规则、地址与问题编码，便于窗口内去重更新。
     * 异常与副作用：无数据库写入副作用。
     */
    private MonitorHitResult buildHit(StandardAddressMonitorRule rule, MonitorDetectContext context, String issueCode, String message) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("issueCode", issueCode);
        detail.put("message", message);
        detail.put("segmName", context.getSegmName());
        detail.put("standName", context.getStandName());

        MonitorHitResult result = new MonitorHitResult();
        result.setHit(true);
        result.setSeverity(StringUtils.defaultIfBlank(rule.getSeverity(), "HIGH"));
        result.setDetailJson(JSONUtil.toJsonStr(detail));
        result.setDedupKey(String.format("%s:%s:%s:%s",
            getTemplateCode(),
            rule.getId(),
            context.getStandardAddressId(),
            issueCode));
        return result;
    }
}
