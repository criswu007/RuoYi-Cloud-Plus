package org.dromara.address.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.address.domain.StandardAddressMonitorRule;
import org.dromara.address.domain.bo.StandardAddressMonitorRecordBo;
import org.dromara.address.domain.vo.StandardAddressMonitorRecordVo;
import org.dromara.address.mapper.StandardAddressMonitorRecordMapper;
import org.dromara.address.mapper.StandardAddressMonitorRuleMapper;
import org.dromara.address.service.impl.StandardAddressMonitorRecordServiceImpl;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressMonitorRecordServiceImplTest {

    @Mock
    private StandardAddressMonitorRecordMapper baseMapper;

    @Mock
    private StandardAddressMonitorRuleMapper addressMonitorRuleMapper;

    @Mock
    private IStandardAddressService standardAddressService;

    @InjectMocks
    private StandardAddressMonitorRecordServiceImpl service;

    @Test
    void shouldBatchFillStandardAddressNamesInsteadOfPerRowLookup() {
        StandardAddressMonitorRecordVo first = new StandardAddressMonitorRecordVo();
        first.setId(1L);
        first.setStandardAddressId(101L);
        first.setRuleId(11L);

        StandardAddressMonitorRecordVo second = new StandardAddressMonitorRecordVo();
        second.setId(2L);
        second.setStandardAddressId(102L);
        second.setRuleId(12L);

        Page<StandardAddressMonitorRecordVo> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(first, second));

        StandardAddressMonitorRule firstRule = new StandardAddressMonitorRule();
        firstRule.setId(11L);
        firstRule.setName("重号监控");

        StandardAddressMonitorRule secondRule = new StandardAddressMonitorRule();
        secondRule.setId(12L);
        secondRule.setName("缺失编码监控");

        when(baseMapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(addressMonitorRuleMapper.selectList(any())).thenReturn(List.of(firstRule, secondRule));
        when(standardAddressService.listStandardAddressStandNameMapBySegmIds(List.of("101", "102")))
            .thenReturn(Map.of("101", "南京市鼓楼区中央路1号", "102", "南京市鼓楼区中央路2号"));

        TableDataInfo<StandardAddressMonitorRecordVo> result =
            service.queryPageList(new StandardAddressMonitorRecordBo(), new PageQuery(10, 1));

        assertEquals("南京市鼓楼区中央路1号", result.getRows().get(0).getStandardAddressFullName());
        assertEquals("重号监控", result.getRows().get(0).getRuleName());
        assertEquals("南京市鼓楼区中央路2号", result.getRows().get(1).getStandardAddressFullName());
        assertEquals("缺失编码监控", result.getRows().get(1).getRuleName());
        verify(standardAddressService).listStandardAddressStandNameMapBySegmIds(List.of("101", "102"));
        verify(standardAddressService, never()).getStandardAddressBySegmId(any());
    }

    @Test
    void shouldPreferSnapshotNamesAndSkipLiveLookupWhenPresent() {
        StandardAddressMonitorRecordVo record = new StandardAddressMonitorRecordVo();
        record.setId(1L);
        record.setStandardAddressId(101L);
        record.setRuleId(11L);
        record.setStandNameSnapshot("江苏省南京市鼓楼区中央路1号");
        record.setRuleNameSnapshot("格式规范性检测");
        record.setTaskId(301L);
        record.setTaskRunLogId(401L);
        record.setTaskNameSnapshot("鼓楼区夜间巡检");
        record.setSeverity("HIGH");
        record.setHitDetailJson("{\"illegalChars\":1}");

        Page<StandardAddressMonitorRecordVo> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(record));
        when(baseMapper.selectVoPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        TableDataInfo<StandardAddressMonitorRecordVo> result =
            service.queryPageList(new StandardAddressMonitorRecordBo(), new PageQuery(10, 1));

        StandardAddressMonitorRecordVo actual = result.getRows().get(0);
        assertEquals("江苏省南京市鼓楼区中央路1号", actual.getStandardAddressFullName());
        assertEquals("格式规范性检测", actual.getRuleName());
        assertEquals(301L, actual.getTaskId());
        assertEquals(401L, actual.getTaskRunLogId());
        assertEquals("鼓楼区夜间巡检", actual.getTaskNameSnapshot());
        assertEquals("HIGH", actual.getSeverity());
        assertEquals("{\"illegalChars\":1}", actual.getHitDetailJson());
        verifyNoInteractions(addressMonitorRuleMapper);
        verifyNoInteractions(standardAddressService);
    }

    @Test
    void shouldExposeIgnoreOperationOnRecordService() throws Exception {
        Method ignoreMethod = IStandardAddressMonitorRecordService.class.getMethod("ignoreByIds", List.class);
        assertEquals(Boolean.class, ignoreMethod.getReturnType());
    }
}
