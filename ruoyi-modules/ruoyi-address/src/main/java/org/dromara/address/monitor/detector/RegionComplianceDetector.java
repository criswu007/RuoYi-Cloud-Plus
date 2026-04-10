package org.dromara.address.monitor.detector;

import cn.hutool.json.JSONUtil;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.enums.MonitorRuleTemplateEnum;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 行政区划合规性 detector。
 * 目的：承接行政区划合规性模板的基础检测逻辑，首版先兜底识别缺失 `regionId` 的地址。
 * 入参/出参：入参为规则实体与检测上下文，出参为命中结果。
 * 关键约束：首版只做字段完整性校验，不做区域名称语义比对。
 * 异常与副作用：无数据库写入副作用。
 */
@Component
public class RegionComplianceDetector implements MonitorRuleDetector {

    @Override
    public String getTemplateCode() {
        return MonitorRuleTemplateEnum.REGION_COMPLIANCE.getCode();
    }

    @Override
    /**
     * 目的：校验单条地址是否缺失基础区域信息。
     * 入参：监控规则实体与检测上下文。
     * 出参：命中结果；未命中返回 `null`。
     * 关键约束：当前仅把 `regionId` 为空视为异常命中。
     * 异常与副作用：无数据库写入副作用。
     */
    public MonitorHitResult detect(StandardAddressMonitorRule rule, MonitorDetectContext context) {
        if (rule == null || context == null || StringUtils.isNotBlank(context.getRegionId())) {
            return null;
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("issueCode", "REGION_ID_MISSING");
        detail.put("message", "标准地址缺少regionId");
        detail.put("standName", context.getStandName());

        MonitorHitResult result = new MonitorHitResult();
        result.setHit(true);
        result.setSeverity(StringUtils.defaultIfBlank(rule.getSeverity(), "MEDIUM"));
        result.setDetailJson(JSONUtil.toJsonStr(detail));
        result.setDedupKey(String.format("%s:%s:%s:REGION_ID_MISSING",
            getTemplateCode(),
            rule.getId(),
            context.getStandardAddressId()));
        return result;
    }
}
