package org.dromara.address.monitor.detector;

import cn.hutool.json.JSONUtil;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.enums.MonitorRuleTemplateEnum;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 智能疑似异常 detector。
 * 目的：承接智能疑似异常模板的基础检测逻辑，首版识别异常短名和重复片段等高置信疑似异常。
 * 入参/出参：入参为规则实体与检测上下文，出参为命中结果。
 * 关键约束：本轮不接入外部智能平台，只做可解释的本地疑似规则。
 * 异常与副作用：无数据库写入副作用。
 */
@Component
public class SmartSuspectDetector implements MonitorRuleDetector {

    private static final int SUSPICIOUS_SHORT_NAME_LENGTH = 2;

    @Override
    public String getTemplateCode() {
        return MonitorRuleTemplateEnum.SMART_SUSPECT.getCode();
    }

    @Override
    /**
     * 目的：识别单条标准地址中的高置信疑似异常模式。
     * 入参：监控规则实体与检测上下文。
     * 出参：命中结果；未命中返回 `null`。
     * 关键约束：优先返回最先匹配的一类疑似问题，避免一条地址同规则生成多条告警。
     * 异常与副作用：无数据库写入副作用。
     */
    public MonitorHitResult detect(StandardAddressMonitorRule rule, MonitorDetectContext context) {
        if (rule == null || context == null) {
            return null;
        }
        String standName = StringUtils.trimToEmpty(context.getStandName());
        if (StringUtils.isBlank(standName)) {
            return null;
        }
        if (isSuspiciousShortName(standName)) {
            return buildHit(rule, context, "SUSPICIOUS_SHORT_NAME", "标准地址名称过短，疑似无业务语义");
        }
        String repeatedPhrase = resolveRepeatedPhrase(standName);
        if (StringUtils.isNotBlank(repeatedPhrase)) {
            return buildHit(rule, context, "REPEATED_PHRASE", "标准地址名称包含重复片段: " + repeatedPhrase);
        }
        return null;
    }

    /**
     * 目的：判断名称是否属于异常短名。
     * 入参：标准地址名称。
     * 出参：是否属于高置信异常短名。
     * 关键约束：仅把长度不超过 2 且不是纯数字的名称视为可疑，避免误伤正常中短地址。
     * 异常与副作用：无写入副作用。
     */
    private boolean isSuspiciousShortName(String standName) {
        return standName.length() <= SUSPICIOUS_SHORT_NAME_LENGTH && !standName.matches("\\d+");
    }

    /**
     * 目的：解析名称中的重复片段。
     * 入参：标准地址名称。
     * 出参：重复片段；未命中返回 `null`。
     * 关键约束：片段长度限定在 2 到 6 之间，避免把单字符重复或超长整串误判为疑似重复。
     * 异常与副作用：无写入副作用。
     */
    private String resolveRepeatedPhrase(String standName) {
        for (int phraseLength = 2; phraseLength <= 6 && phraseLength * 2 <= standName.length(); phraseLength++) {
            for (int index = 0; index + phraseLength * 2 <= standName.length(); index++) {
                String phrase = standName.substring(index, index + phraseLength);
                String nextPhrase = standName.substring(index + phraseLength, index + phraseLength * 2);
                if (StringUtils.equals(phrase, nextPhrase)) {
                    return phrase;
                }
            }
        }
        return null;
    }

    /**
     * 目的：构造智能疑似异常命中结果。
     * 入参：规则、检测上下文、问题编码与提示信息。
     * 出参：标准化命中结果。
     * 关键约束：去重键必须绑定规则、地址和疑似问题编码，保证重复巡检只更新同类记录。
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
        result.setSeverity(StringUtils.defaultIfBlank(rule.getSeverity(), "MEDIUM"));
        result.setDetailJson(JSONUtil.toJsonStr(detail));
        result.setDedupKey(String.format("%s:%s:%s:%s",
            getTemplateCode(),
            rule.getId(),
            context.getStandardAddressId(),
            issueCode));
        return result;
    }
}
