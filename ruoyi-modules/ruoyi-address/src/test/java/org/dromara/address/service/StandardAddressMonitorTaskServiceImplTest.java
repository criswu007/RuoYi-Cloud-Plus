package org.dromara.address.service;

import org.dromara.address.domain.bo.StandardAddressAdminBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.service.impl.MonitorTaskSchedulerServiceImpl;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class StandardAddressMonitorTaskServiceImplTest {

    @Test
    void shouldExposeMonitorTaskCrudAndLifecycleContract() throws Exception {
        StandardAddressAdminBo.MonitorTaskBo bo = new StandardAddressAdminBo.MonitorTaskBo();
        bo.setTaskName("全量格式巡检");
        bo.setMonitorScope("REGION");
        bo.setRegionIds(List.of("320106"));
        bo.setRelatedRuleIds(List.of(11L, 12L));
        bo.setSnailJobTaskId(9001L);

        StandardAddressAdminVo.MonitorTaskVo vo = new StandardAddressAdminVo.MonitorTaskVo();
        Date now = new Date();
        vo.setLastExecuteTime(now);
        vo.setLastSuccessTime(now);
        vo.setLastFailureReason("调度超时");
        vo.setRegionIds(List.of("320106"));
        vo.setSnailJobTaskId(9001L);

        assertEquals(List.of("320106"), bo.getRegionIds());
        assertEquals(9001L, bo.getSnailJobTaskId());
        assertEquals(now, vo.getLastExecuteTime());
        assertEquals(now, vo.getLastSuccessTime());
        assertEquals("调度超时", vo.getLastFailureReason());

        assertEquals(TableDataInfo.class,
            IStandardAddressMonitorTaskService.class
                .getMethod("queryPageList", StandardAddressAdminBo.MonitorTaskBo.class, PageQuery.class)
                .getReturnType());
        assertEquals(StandardAddressAdminVo.MonitorTaskVo.class,
            IStandardAddressMonitorTaskService.class.getMethod("queryById", Long.class).getReturnType());
        assertEquals(Boolean.class,
            IStandardAddressMonitorTaskService.class
                .getMethod("insertByBo", StandardAddressAdminBo.MonitorTaskBo.class)
                .getReturnType());
        assertEquals(Boolean.class,
            IStandardAddressMonitorTaskService.class
                .getMethod("updateByBo", StandardAddressAdminBo.MonitorTaskBo.class)
                .getReturnType());

        Method rerunMethod = IStandardAddressMonitorTaskService.class.getMethod("rerunById", Long.class);
        Method pauseMethod = IStandardAddressMonitorTaskService.class.getMethod("pauseById", Long.class);
        Method terminateMethod = IStandardAddressMonitorTaskService.class.getMethod("terminateById", Long.class);
        Method runLogMethod = IStandardAddressMonitorTaskService.class.getMethod("queryRunLogPageList", Long.class, PageQuery.class);

        assertEquals(Boolean.class, rerunMethod.getReturnType());
        assertEquals(Boolean.class, pauseMethod.getReturnType());
        assertEquals(Boolean.class, terminateMethod.getReturnType());
        assertEquals(TableDataInfo.class, runLogMethod.getReturnType());
    }

    @Test
    void shouldExposeSchedulerAndRunLogContracts() throws Exception {
        Class<?> schedulerInterface = Class.forName("org.dromara.address.service.MonitorTaskSchedulerService");
        Class<?> runLogEntity = Class.forName("org.dromara.address.domain.StandardAddressMonitorTaskRunLog");

        Method registerMethod = schedulerInterface.getMethod("registerOrRefreshTask", StandardAddressAdminBo.MonitorTaskBo.class);
        Method pauseMethod = schedulerInterface.getMethod("pauseTask", Long.class);
        Method removeMethod = schedulerInterface.getMethod("removeTask", Long.class);

        assertEquals(Long.class, registerMethod.getReturnType());
        assertEquals(Boolean.class, pauseMethod.getReturnType());
        assertEquals(Boolean.class, removeMethod.getReturnType());
        assertEquals("MonitorTaskSchedulerServiceImpl", MonitorTaskSchedulerServiceImpl.class.getSimpleName());
        assertEquals("StandardAddressMonitorTaskRunLog", runLogEntity.getSimpleName());
    }
}
