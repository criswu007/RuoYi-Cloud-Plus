package org.dromara.address.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.mapper.AddrSetSegmMapper;
import org.dromara.address.service.impl.InstallationAddressServiceImpl;
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
class InstallationAddressServiceImplTest {

    @Mock
    private AddrSetSegmMapper addrSetSegmMapper;

    @Mock
    private IStandardAddressService standardAddressService;

    @InjectMocks
    private InstallationAddressServiceImpl installationAddressService;

    @Test
    void shouldBatchFillStandardAddressNamesInsteadOfPerRowLookup() {
        InstallationAddressVo first = new InstallationAddressVo();
        first.setSetAddrId("SET001");
        first.setSegmId("000000000000000000000101");

        InstallationAddressVo second = new InstallationAddressVo();
        second.setSetAddrId("SET002");
        second.setSegmId("000000000000000000000102");

        Page<InstallationAddressVo> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(first, second));

        when(addrSetSegmMapper.selectInstallationPage(any(Page.class), any(InstallationAddressBo.class))).thenReturn(page);
        when(standardAddressService.listStandardAddressStandNameMapBySegmIds(
            List.of("000000000000000000000101", "000000000000000000000102")
        )).thenReturn(Map.of(
            "000000000000000000000101", "南京市鼓楼区中央路1号",
            "000000000000000000000102", "南京市鼓楼区中央路2号"
        ));

        TableDataInfo<InstallationAddressVo> result = installationAddressService.queryPageList(new InstallationAddressBo(), new PageQuery(10, 1));

        assertEquals("南京市鼓楼区中央路1号", result.getRows().get(0).getStandName());
        assertEquals("南京市鼓楼区中央路2号", result.getRows().get(1).getStandName());
        verify(standardAddressService).listStandardAddressStandNameMapBySegmIds(
            List.of("000000000000000000000101", "000000000000000000000102")
        );
        verify(standardAddressService, never()).getStandardAddressBySegmId(any());
    }
}
