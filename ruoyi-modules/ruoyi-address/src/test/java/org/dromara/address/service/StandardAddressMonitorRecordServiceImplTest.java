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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
}
