package org.dromara.address.monitor.detector;

import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.enums.MonitorRuleTemplateEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class ElementCompletenessDetectorTest {

    private final ElementCompletenessDetector detector = new ElementCompletenessDetector();

    @Test
    void shouldHitWhenSegmNameIsMissing() {
        MonitorHitResult result = detector.detect(buildRule(), buildContext("1003", "", "江苏省南京市鼓楼区中央路1号", "320106"));

        assertNotNull(result);
        assertTrue(result.isHit());
        assertEquals("HIGH", result.getSeverity());
        assertTrue(result.getDetailJson().contains("CURRENT_NAME_MISSING"));
    }

    @Test
    void shouldSkipWhenCurrentNameExists() {
        MonitorHitResult result = detector.detect(buildRule(), buildContext("1004", "中央路1号", "江苏省南京市鼓楼区中央路1号", "320106"));

        assertNull(result);
    }

    private StandardAddressMonitorRule buildRule() {
        StandardAddressMonitorRule rule = new StandardAddressMonitorRule();
        rule.setId(21L);
        rule.setName("地址要素完整性巡检");
        rule.setRuleTemplate(MonitorRuleTemplateEnum.ELEMENT_COMPLETENESS.getCode());
        rule.setSeverity("HIGH");
        return rule;
    }

    private MonitorDetectContext buildContext(String standardAddressId, String segmName, String standName, String regionId) {
        MonitorDetectContext context = new MonitorDetectContext();
        context.setTaskId(200L);
        context.setTaskName("鼓楼区要素巡检");
        context.setStandardAddressId(Long.valueOf(standardAddressId));
        context.setSegmName(segmName);
        context.setStandName(standName);
        context.setRegionId(regionId);
        return context;
    }
}
