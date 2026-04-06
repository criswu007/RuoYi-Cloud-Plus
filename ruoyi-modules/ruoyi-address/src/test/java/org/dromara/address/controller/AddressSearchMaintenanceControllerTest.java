package org.dromara.address.controller;

import org.dromara.address.domain.bo.AddressSearchMaintenanceBo;
import org.dromara.address.domain.vo.AddressSearchMaintenanceVo;
import org.dromara.address.search.service.AddressSearchMaintenanceTaskService;
import org.dromara.common.core.domain.R;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AddressSearchMaintenanceControllerTest {

    @Mock
    private AddressSearchMaintenanceTaskService taskService;

    @InjectMocks
    private AddressSearchMaintenanceController controller;

    @Test
    void shouldSubmitStandardRebuildTask() {
        AddressSearchMaintenanceBo.RebuildTaskSubmitBo bo = new AddressSearchMaintenanceBo.RebuildTaskSubmitBo();
        bo.setConfirmationCode("REBUILD_STANDARD");
        when(taskService.submitStandardRebuild("REBUILD_STANDARD")).thenReturn(9001L);

        R<Long> response = controller.createStandardRebuildTask(bo);

        assertEquals(9001L, response.getData());
    }

    @Test
    void shouldExposeOpsOverview() {
        AddressSearchMaintenanceVo.OverviewVo overview = new AddressSearchMaintenanceVo.OverviewVo();
        overview.setEsReachable(Boolean.TRUE);
        overview.setPendingRepairCount(3L);
        when(taskService.getOverview()).thenReturn(overview);

        R<AddressSearchMaintenanceVo.OverviewVo> response = controller.getOverview();

        assertTrue(response.getData().getEsReachable());
        assertEquals(3L, response.getData().getPendingRepairCount());
    }
}
