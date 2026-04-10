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
class SmartSuspectDetectorTest {

    private final SmartSuspectDetector detector = new SmartSuspectDetector();

    @Test
    void shouldHitWhenStandNameIsSuspiciouslyShort() {
        MonitorHitResult result = detector.detect(buildRule(), buildContext("1005", "东路", "东路", "320106"));

        assertNotNull(result);
        assertTrue(result.isHit());
        assertEquals("MEDIUM", result.getSeverity());
        assertTrue(result.getDetailJson().contains("SUSPICIOUS_SHORT_NAME"));
    }

    @Test
    void shouldHitWhenStandNameContainsRepeatedPhrase() {
        MonitorHitResult result = detector.detect(buildRule(), buildContext("1006", "中央路", "中央路中央路12号", "320106"));

        assertNotNull(result);
        assertTrue(result.isHit());
        assertTrue(result.getDetailJson().contains("REPEATED_PHRASE"));
    }

    @Test
    void shouldSkipWhenStandNameLooksNormal() {
        MonitorHitResult result = detector.detect(buildRule(), buildContext("1007", "中央路12号", "江苏省南京市鼓楼区中央路12号", "320106"));

        assertNull(result);
    }

    private StandardAddressMonitorRule buildRule() {
        StandardAddressMonitorRule rule = new StandardAddressMonitorRule();
        rule.setId(31L);
        rule.setName("智能疑似异常巡检");
        rule.setRuleTemplate(MonitorRuleTemplateEnum.SMART_SUSPECT.getCode());
        rule.setSeverity("MEDIUM");
        return rule;
    }

    private MonitorDetectContext buildContext(String standardAddressId, String segmName, String standName, String regionId) {
        MonitorDetectContext context = new MonitorDetectContext();
        context.setTaskId(300L);
        context.setTaskName("鼓楼区疑似异常巡检");
        context.setStandardAddressId(Long.valueOf(standardAddressId));
        context.setSegmName(segmName);
        context.setStandName(standName);
        context.setRegionId(regionId);
        return context;
    }
}
