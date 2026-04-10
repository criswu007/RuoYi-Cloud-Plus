package org.dromara.address.service;

import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.domain.bo.StandardAddressMonitorRuleBo;
import org.dromara.address.domain.vo.StandardAddressMonitorRuleVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class StandardAddressMonitorRuleServiceImplTest {

    @Test
    void shouldUseTemplateRuleFieldsAcrossContracts() throws Exception {
        StandardAddressMonitorRuleBo bo = new StandardAddressMonitorRuleBo();
        bo.setRuleCode("FORMAT_STANDARD_001");
        bo.setRuleTemplate("FORMAT_STANDARD");
        bo.setSeverity("HIGH");
        bo.setPriority(10);
        bo.setDedupHours(24);
        bo.setConfigJson("{\"maxNameLength\":64}");

        StandardAddressMonitorRuleVo vo = new StandardAddressMonitorRuleVo();
        vo.setRuleCode("FORMAT_STANDARD_001");
        vo.setRuleTemplate("FORMAT_STANDARD");
        vo.setSeverity("HIGH");
        vo.setPriority(10);
        vo.setDedupHours(24);
        vo.setConfigJson("{\"maxNameLength\":64}");

        StandardAddressMonitorRule entity = new StandardAddressMonitorRule();
        entity.setRuleCode("FORMAT_STANDARD_001");
        entity.setRuleTemplate("FORMAT_STANDARD");
        entity.setSeverity("HIGH");
        entity.setPriority(10);
        entity.setDedupHours(24);
        entity.setConfigJson("{\"maxNameLength\":64}");

        assertEquals("FORMAT_STANDARD_001", bo.getRuleCode());
        assertEquals("FORMAT_STANDARD", vo.getRuleTemplate());
        assertEquals("HIGH", entity.getSeverity());
        assertEquals(10, entity.getPriority());
        assertEquals(24, entity.getDedupHours());
        assertEquals("{\"maxNameLength\":64}", vo.getConfigJson());
    }

    @Test
    void shouldExposeEnableAndDisableOperationsOnRuleService() throws Exception {
        Method enableMethod = IStandardAddressMonitorRuleService.class.getMethod("enableByIds", List.class);
        Method disableMethod = IStandardAddressMonitorRuleService.class.getMethod("disableByIds", List.class);

        assertEquals(Boolean.class, enableMethod.getReturnType());
        assertEquals(Boolean.class, disableMethod.getReturnType());
    }
}
