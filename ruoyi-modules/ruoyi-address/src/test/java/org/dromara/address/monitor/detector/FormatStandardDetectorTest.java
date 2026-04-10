package org.dromara.address.monitor.detector;

import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.enums.MonitorRuleTemplateEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class FormatStandardDetectorTest {

    private final FormatStandardDetector detector = new FormatStandardDetector();

    @Test
    void shouldHitWhenStandNameIsBlank() {
        MonitorHitResult result = detector.detect(buildRule(), buildContext("1001", "   ", "320106"));

        assertNotNull(result);
        assertTrue(result.isHit());
        assertTrue(result.getDetailJson().contains("EMPTY_NAME"));
        assertEquals("HIGH", result.getSeverity());
    }

    @Test
    void shouldHitWhenStandNameIsPureDigits() {
        MonitorHitResult result = detector.detect(buildRule(), buildContext("1002", "123456", "320106"));

        assertNotNull(result);
        assertTrue(result.isHit());
        assertTrue(result.getDetailJson().contains("PURE_DIGITS"));
        assertTrue(result.getDedupKey().contains("1002"));
    }

    private StandardAddressMonitorRule buildRule() {
        StandardAddressMonitorRule rule = new StandardAddressMonitorRule();
        rule.setId(11L);
        rule.setName("格式规范巡检");
        rule.setRuleTemplate(MonitorRuleTemplateEnum.FORMAT_STANDARD.getCode());
        rule.setSeverity("HIGH");
        return rule;
    }

    private MonitorDetectContext buildContext(String standardAddressId, String standName, String regionId) {
        MonitorDetectContext context = new MonitorDetectContext();
        context.setTaskId(100L);
        context.setTaskName("鼓楼区格式巡检");
        context.setStandardAddressId(Long.valueOf(standardAddressId));
        context.setStandName(standName);
        context.setRegionId(regionId);
        return context;
    }
}
