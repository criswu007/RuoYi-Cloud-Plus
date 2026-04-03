package org.dromara.address.support;

import org.dromara.address.domain.StandardAddressOperationLog;
import org.dromara.address.mapper.StandardAddressOperationLogMapper;
import org.dromara.common.satoken.utils.LoginHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressOperationLogRecorderTest {

    @Mock
    private StandardAddressOperationLogMapper operationLogMapper;

    @InjectMocks
    private StandardAddressOperationLogRecorder recorder;

    @Test
    void shouldPersistLocalOperationLogWithCurrentLoginContext() {
        when(operationLogMapper.insert(any(StandardAddressOperationLog.class))).thenReturn(1);

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getTenantId).thenReturn("000001");
            loginHelper.when(LoginHelper::getUsername).thenReturn("tester");

            recorder.record("segm-1", "UPDATE", "江苏省南京市鼓楼区中央路1号101室", "修改标准地址成功");
        }

        ArgumentCaptor<StandardAddressOperationLog> captor = ArgumentCaptor.forClass(StandardAddressOperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        StandardAddressOperationLog log = captor.getValue();
        assertNotNull(log.getId());
        assertEquals("segm-1", log.getStandardAddressId());
        assertEquals("UPDATE", log.getOperationType());
        assertEquals("tester", log.getOperator());
        assertEquals("000001", log.getTenantId());
        assertEquals("0", log.getDelFlag());
        assertEquals("操作对象：江苏省南京市鼓楼区中央路1号101室；修改标准地址成功", log.getDetails());
        assertNotNull(log.getOperateTime());
    }
}
