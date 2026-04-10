package org.dromara.address.monitor.detector;

import cn.hutool.json.JSONUtil;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.enums.MonitorRuleTemplateEnum;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 格式规范性 detector。
 * 目的：承接格式规范性模板的具体检测逻辑，识别空名称、异常短名、连续分隔符与纯数字地址。
 * 入参/出参：入参为规则实体与检测上下文，出参为命中结果。
 * 关键约束：当前阶段只实现基础格式规则，复杂词法校验后续再扩展；detector 只返回结果，不直接写库。
 * 异常与副作用：无数据库写入副作用。
 */
@Component
public class FormatStandardDetector implements MonitorRuleDetector {

    private static final int DEFAULT_MIN_NAME_LENGTH = 2;

    @Override
    public String getTemplateCode() {
        return MonitorRuleTemplateEnum.FORMAT_STANDARD.getCode();
    }

    @Override
    /**
     * 目的：按格式规范模板检测单条标准地址名称。
     * 入参：监控规则实体与检测上下文。
     * 出参：命中结果；未命中返回 `null`。
     * 关键约束：命中原因只返回一条最先匹配的基础异常，避免同一轮扫描对同一规则重复生成多条记录。
     * 异常与副作用：无数据库写入副作用。
     */
    public MonitorHitResult detect(StandardAddressMonitorRule rule, MonitorDetectContext context) {
        if (rule == null || context == null) {
            return null;
        }
        String standName = StringUtils.trimToEmpty(context.getStandName());
        if (StringUtils.isBlank(standName)) {
            return buildHit(rule, context, "EMPTY_NAME", "标准地址名称为空");
        }
        if (standName.length() < DEFAULT_MIN_NAME_LENGTH) {
            return buildHit(rule, context, "NAME_TOO_SHORT", "标准地址名称长度过短");
        }
        if (standName.matches(".*\\s{2,}.*")) {
            return buildHit(rule, context, "CONSECUTIVE_WHITESPACE", "标准地址名称包含连续空白字符");
        }
        if (standName.matches(".*([,，.。/\\\\\\-_#])\\1+.*")) {
            return buildHit(rule, context, "CONSECUTIVE_SEPARATOR", "标准地址名称包含连续分隔符");
        }
        if (standName.matches("\\d+")) {
            return buildHit(rule, context, "PURE_DIGITS", "标准地址名称不能为纯数字");
        }
        return null;
    }

    /**
     * 目的：构造格式规范性异常命中结果。
     * 入参：规则、检测上下文、命中编码与提示信息。
     * 出参：标准化后的命中结果。
     * 关键约束：去重键必须稳定绑定规则、地址和命中编码，避免同类异常重复入库。
     * 异常与副作用：无数据库写入副作用。
     */
    private MonitorHitResult buildHit(StandardAddressMonitorRule rule, MonitorDetectContext context, String issueCode, String message) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("issueCode", issueCode);
        detail.put("message", message);
        detail.put("standName", context.getStandName());
        detail.put("ruleTemplate", getTemplateCode());

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
