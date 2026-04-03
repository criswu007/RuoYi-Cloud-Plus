package org.dromara.address.service;

import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.bo.StandardAddressBatchAddBo;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.bo.StandardAddressSplitItemBo;
import org.dromara.address.domain.vo.StandardAddressAdminVo;
import org.dromara.address.mapper.AddrSegmMapper;
import org.dromara.address.mapper.AddrSetSegmMapper;
import org.dromara.address.support.StandardAddressOperationLogRecorder;
import org.dromara.address.service.impl.StandardAddressCommandService;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
import org.dromara.address.service.impl.StandardAddressIdGenerator;
import org.dromara.address.service.impl.StandardAddressNameService;
import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanWrapperImpl;

import java.util.concurrent.atomic.AtomicReference;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressCommandServiceTest {

    @Mock
    private AddrSegmMapper addrSegmMapper;

    @Mock
    private AddrSetSegmMapper addrSetSegmMapper;

    @Mock
    private StandardAddressDictionaryService dictionaryService;

    @Mock
    private StandardAddressNameService nameService;

    @Mock
    private StandardAddressIdGenerator idGenerator;

    @Mock
    private StandardAddressOperationLogRecorder operationLogRecorder;

    @InjectMocks
    private StandardAddressCommandService commandService;

    @Test
    void shouldRejectWriteForLevel1AndLevel2() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setSegmType("180000");
        when(dictionaryService.resolveAddrLevel("180000")).thenReturn(1);

        assertThrows(ServiceException.class, () -> commandService.addStandardAddress(bo));
    }

    @Test
    void shouldRequireConfirmWhenInstallationAddressExists() {
        when(addrSegmMapper.countChildren(List.of("segm-1"))).thenReturn(0L);
        when(addrSetSegmMapper.countBySegmIds(List.of("segm-1"))).thenReturn(2L);
        AddrSegm current = new AddrSegm();
        current.setSegmId("segm-1");
        current.setSegmType("180007");
        when(addrSegmMapper.selectBatchIds(List.of("segm-1"))).thenReturn(List.of(current));
        when(dictionaryService.resolveAddrLevel("180007")).thenReturn(16);

        assertThrows(ServiceException.class, () -> commandService.deleteStandardAddresses(List.of("segm-1"), false));
    }

    @Test
    void shouldPreviewBatchChildrenByParentAndRange() {
        StandardAddressBatchAddBo bo = new StandardAddressBatchAddBo();
        bo.setParentSegmId("segm-parent");
        bo.setPrefix("A");
        bo.setStartNum(1);
        bo.setEndNum(3);

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("segm-parent");
        parent.setStandName("江苏省南京市");
        when(addrSegmMapper.selectById("segm-parent")).thenReturn(parent);
        when(nameService.buildSegmNo("A1")).thenReturn("A1");
        when(nameService.buildStandNo("江苏省南京市A1")).thenReturn("JSSNJSA1");

        List<StandardAddressAdminVo.BatchPreviewVo> preview = commandService.previewChildren(bo);

        assertEquals(3, preview.size());
        assertEquals("A1", preview.get(0).getSegmName());
        assertEquals("江苏省南京市A1", preview.get(0).getStandName());
    }

    @Test
    void shouldMapCanonicalExtensionFieldsWhenAddingStandardAddress() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setParentSegmId("segm-parent");
        bo.setSegmName("101室");
        bo.setSegmType("180007");

        BeanWrapperImpl boWrapper = new BeanWrapperImpl(bo);
        boWrapper.setPropertyValue("singleProjectCode", "GC-2026-001");
        boWrapper.setPropertyValue("supportingFeeCommunityFlag", "Y");
        boWrapper.setPropertyValue("isCity", "Y");
        boWrapper.setPropertyValue("addrInTypeFtth", 2140760);
        boWrapper.setPropertyValue("ftthPonType", 2141301);
        boWrapper.setPropertyValue("addrInTypeLan", 2140784);
        boWrapper.setPropertyValue("areaType", 2140511);
        boWrapper.setPropertyValue("placeType", 2140800);
        boWrapper.setPropertyValue("coverNum", 128);

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("segm-parent");
        parent.setStandName("江苏省南京市");
        parent.setRegionId("320100");
        parent.setDistrictId("320106");
        parent.setServiceRegionId("320106001001");

        when(addrSegmMapper.selectById("segm-parent")).thenReturn(parent);
        when(dictionaryService.resolveAddrLevel("180007")).thenReturn(16);
        when(dictionaryService.resolveSegmTypesByAddrLevel(16)).thenReturn(List.of("180007"));
        when(idGenerator.nextSegmId()).thenReturn("segm-new");
        when(nameService.buildSegmNo("101室")).thenReturn("101S");
        when(nameService.buildStandNo("江苏省南京市101室")).thenReturn("JSSNJS101S");

        AtomicReference<AddrSegm> captured = new AtomicReference<>();
        when(addrSegmMapper.insert(any(AddrSegm.class))).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return 1;
        });

        Boolean result = commandService.addStandardAddress(bo);

        assertTrue(Boolean.TRUE.equals(result));
        BeanWrapperImpl entityWrapper = new BeanWrapperImpl(captured.get());
        assertEquals("GC-2026-001", entityWrapper.getPropertyValue("singleProjectCode"));
        assertEquals("Y", entityWrapper.getPropertyValue("supportingFeeCommunityFlag"));
        assertEquals("Y", entityWrapper.getPropertyValue("isCity"));
        assertEquals(2140760, entityWrapper.getPropertyValue("addrInTypeFtth"));
        assertEquals(2141301, entityWrapper.getPropertyValue("ftthPonType"));
        assertEquals(2140784, entityWrapper.getPropertyValue("addrInTypeLan"));
        assertEquals(2140511, entityWrapper.getPropertyValue("areaType"));
        assertEquals(2140800, entityWrapper.getPropertyValue("placeType"));
        assertEquals(128, entityWrapper.getPropertyValue("coverNum"));
    }

    @Test
    void shouldAllowAddWhenSelectedLevelIsGreaterThanParentLevel() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setParentSegmId("segm-parent");
        bo.setSegmName("紫峰大厦");
        bo.setAddrLevel(9);

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("segm-parent");
        parent.setStandName("江苏省南京市鼓楼区中山路");
        parent.setSegmType("180004");
        parent.setRegionId("320100");

        when(addrSegmMapper.selectById("segm-parent")).thenReturn(parent);
        when(dictionaryService.resolveAddrLevel("180004")).thenReturn(7);
        when(dictionaryService.resolveSegmTypesByAddrLevel(9)).thenReturn(List.of("180005"));
        when(dictionaryService.resolveDefaultSegmTypeByAddrLevel(9)).thenReturn("180005");
        when(idGenerator.nextSegmId()).thenReturn("segm-new");
        when(nameService.buildSegmNo("紫峰大厦")).thenReturn("ZFDS");
        when(nameService.buildStandNo("江苏省南京市鼓楼区中山路紫峰大厦")).thenReturn("JSSNJSGLQZSLZFDS");
        when(addrSegmMapper.insert(any(AddrSegm.class))).thenReturn(1);

        Boolean result = commandService.addStandardAddress(bo);

        assertTrue(Boolean.TRUE.equals(result));
        verify(operationLogRecorder).record(
            "segm-new",
            "INSERT",
            "江苏省南京市鼓楼区中山路紫峰大厦",
            "新增标准地址成功"
        );
    }

    @Test
    void shouldRejectAddWhenSelectedLevelIsNotGreaterThanParentLevel() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setParentSegmId("segm-parent");
        bo.setSegmName("101室");
        bo.setAddrLevel(7);

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("segm-parent");
        parent.setStandName("江苏省南京市");
        parent.setSegmType("180004");

        when(addrSegmMapper.selectById("segm-parent")).thenReturn(parent);
        when(dictionaryService.resolveSegmTypesByAddrLevel(7)).thenReturn(List.of("180004"));
        when(dictionaryService.resolveAddrLevel("180004")).thenReturn(7);

        ServiceException exception = assertThrows(ServiceException.class, () -> commandService.addStandardAddress(bo));

        assertEquals("新增失败：当前地址级别必须高于父级地址级别", exception.getMessage());
    }

    @Test
    void shouldRejectAddWhenLevelIdDoesNotMatchSegmType() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setParentSegmId("segm-parent");
        bo.setSegmName("101室");
        bo.setAddrLevel(15);
        bo.setSegmType("180007");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("segm-parent");
        parent.setStandName("江苏省南京市");

        when(addrSegmMapper.selectById("segm-parent")).thenReturn(parent);
        when(dictionaryService.resolveAddrLevel("180007")).thenReturn(16);
        when(dictionaryService.resolveSegmTypesByAddrLevel(15)).thenReturn(List.of("180010"));

        ServiceException exception = assertThrows(ServiceException.class, () -> commandService.addStandardAddress(bo));

        assertEquals("新增失败：地址层级与地址类型不一致", exception.getMessage());
    }

    @Test
    void shouldRejectUpdateWhenLevelIdDoesNotMatchSegmType() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setSegmId("segm-1");
        bo.setParentSegmId("segm-parent");
        bo.setSegmName("101室");
        bo.setAddrLevel(16);
        bo.setSegmType("180010");

        AddrSegm existing = new AddrSegm();
        existing.setSegmId("segm-1");
        existing.setParentSegmId("segm-parent");
        existing.setSegmName("1001室");
        existing.setStandName("江苏省南京市1001室");
        existing.setSegmType("180007");
        existing.setDeleteState("0");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("segm-parent");
        parent.setStandName("江苏省南京市");

        when(addrSegmMapper.selectById("segm-1")).thenReturn(existing);
        when(addrSegmMapper.selectById("segm-parent")).thenReturn(parent);
        when(dictionaryService.resolveAddrLevel("180010")).thenReturn(15);
        when(dictionaryService.resolveSegmTypesByAddrLevel(16)).thenReturn(List.of("180007"));

        ServiceException exception = assertThrows(ServiceException.class, () -> commandService.updateStandardAddress(bo));

        assertEquals("修改失败：地址层级与地址类型不一致", exception.getMessage());
    }

    @Test
    void shouldRecordUpdateOperationLogWhenUpdateStandardAddressSucceeds() {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setSegmId("segm-1");
        bo.setParentSegmId("segm-parent");
        bo.setSegmName("102室");
        bo.setAddrLevel(16);
        bo.setSegmType("180007");

        AddrSegm existing = new AddrSegm();
        existing.setSegmId("segm-1");
        existing.setParentSegmId("segm-parent");
        existing.setSegmName("101室");
        existing.setStandName("江苏省南京市鼓楼区中央路1号101室");
        existing.setSegmType("180007");
        existing.setDeleteState("0");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("segm-parent");
        parent.setStandName("江苏省南京市鼓楼区中央路1号");
        parent.setSegmType("180005");

        when(addrSegmMapper.selectById("segm-1")).thenReturn(existing);
        when(addrSegmMapper.selectById("segm-parent")).thenReturn(parent);
        when(dictionaryService.resolveAddrLevel("180007")).thenReturn(16);
        when(dictionaryService.resolveSegmTypesByAddrLevel(16)).thenReturn(List.of("180007"));
        when(nameService.buildSegmNo("102室")).thenReturn("102S");
        when(nameService.buildStandNo("江苏省南京市鼓楼区中央路1号102室")).thenReturn("JSSNJSGLQZYL1H102S");
        when(addrSegmMapper.updateById(any(AddrSegm.class))).thenReturn(1);
        when(addrSegmMapper.selectChildrenByParentSegmId("segm-1")).thenReturn(List.of());

        Boolean result = commandService.updateStandardAddress(bo);

        assertTrue(Boolean.TRUE.equals(result));
        verify(operationLogRecorder).record(
            "segm-1",
            "UPDATE",
            "江苏省南京市鼓楼区中央路1号102室",
            "修改标准地址成功"
        );
    }

    @Test
    void shouldRecordDeleteOperationLogWhenLogicalDeleteSucceeds() {
        AddrSegm current = new AddrSegm();
        current.setSegmId("segm-1");
        current.setSegmType("180007");
        current.setStandName("江苏省南京市鼓楼区中央路1号101室");
        current.setDeleteState("0");

        when(addrSegmMapper.selectBatchIds(List.of("segm-1"))).thenReturn(List.of(current));
        when(dictionaryService.resolveAddrLevel("180007")).thenReturn(16);
        when(addrSegmMapper.countChildren(List.of("segm-1"))).thenReturn(0L);
        when(addrSetSegmMapper.countBySegmIds(List.of("segm-1"))).thenReturn(0L);
        when(addrSegmMapper.logicalDeleteBySegmIds(List.of("segm-1"))).thenReturn(1);

        Boolean result = commandService.deleteStandardAddresses(List.of("segm-1"), true);

        assertTrue(Boolean.TRUE.equals(result));
        verify(operationLogRecorder).record(
            "segm-1",
            "DELETE",
            "江苏省南京市鼓楼区中央路1号101室",
            "删除标准地址成功"
        );
    }

    @Test
    void shouldRejectMergeWhenTargetIncludedInSource() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> commandService.mergeStandardAddresses(List.of("segm-1", "segm-2"), "segm-1"));

        assertEquals("合并失败：目标地址不能包含在待合并地址中", exception.getMessage());
    }

    @Test
    void shouldMoveChildrenAndInstallationRelationsWhenMerge() {
        AddrSegm source = new AddrSegm();
        source.setSegmId("segm-source");
        source.setSegmType("180007");
        source.setStandName("江苏省南京市鼓楼区1号");
        source.setDeleteState("0");

        AddrSegm target = new AddrSegm();
        target.setSegmId("segm-target");
        target.setSegmType("180005");
        target.setStandName("江苏省南京市鼓楼区中央路");
        target.setDeleteState("0");

        when(addrSegmMapper.selectBatchIds(List.of("segm-source", "segm-target"))).thenReturn(List.of(source, target));
        when(dictionaryService.resolveAddrLevel("180007")).thenReturn(16);
        when(dictionaryService.resolveAddrLevel("180005")).thenReturn(9);
        when(addrSegmMapper.moveChildrenToTarget(List.of("segm-source"), "segm-target")).thenReturn(2);
        when(addrSetSegmMapper.rebindSegmIds(List.of("segm-source"), "segm-target")).thenReturn(3);
        when(addrSegmMapper.logicalDeleteBySegmIds(List.of("segm-source"))).thenReturn(1);

        Boolean result = commandService.mergeStandardAddresses(List.of("segm-source"), "segm-target");

        assertTrue(Boolean.TRUE.equals(result));
        verify(addrSegmMapper).moveChildrenToTarget(List.of("segm-source"), "segm-target");
        verify(addrSetSegmMapper).rebindSegmIds(List.of("segm-source"), "segm-target");
        verify(addrSegmMapper).logicalDeleteBySegmIds(List.of("segm-source"));
        verify(operationLogRecorder).record(
            eq("segm-target"),
            eq("MERGE"),
            eq("江苏省南京市鼓楼区中央路"),
            contains("合并来源地址成功")
        );
        verify(operationLogRecorder).record(
            eq("segm-source"),
            eq("MERGE"),
            eq("江苏省南京市鼓楼区1号"),
            contains("已合并到")
        );
    }

    @Test
    void shouldSplitAddressUsingInheritedSourceAttributesAndRecycleSource() {
        AddrSegm source = new AddrSegm();
        source.setSegmId("segm-source");
        source.setParentSegmId("segm-parent");
        source.setSegmType("180007");
        source.setSegmName("原101室");
        source.setStandName("江苏省南京市鼓楼区中央路1号101室");
        source.setRegionId("320100");
        source.setDistrictId("320106");
        source.setServiceRegionId("320106001001");
        source.setStationId("station-1");
        source.setInstallstationId("install-1");
        source.setBusstationId("bus-1");
        source.setStatus("2140900");
        source.setDeleteState("0");

        AddrSegm parent = new AddrSegm();
        parent.setSegmId("segm-parent");
        parent.setStandName("江苏省南京市鼓楼区中央路1号");
        parent.setSegmType("180005");

        StandardAddressSplitItemBo splitItem = new StandardAddressSplitItemBo();
        splitItem.setSegmName("102室");

        when(addrSegmMapper.selectById("segm-source")).thenReturn(source);
        when(addrSegmMapper.selectById("segm-parent")).thenReturn(parent);
        when(dictionaryService.resolveAddrLevel("180007")).thenReturn(16);
        when(idGenerator.nextSegmId()).thenReturn("segm-new");
        when(nameService.buildSegmNo("102室")).thenReturn("102S");
        when(nameService.buildStandNo("江苏省南京市鼓楼区中央路1号102室")).thenReturn("JSSNJSGLQZYL1H102S");
        when(addrSegmMapper.insert(any(AddrSegm.class))).thenReturn(1);
        when(addrSegmMapper.logicalDeleteBySegmIds(List.of("segm-source"))).thenReturn(1);

        Boolean result = commandService.splitStandardAddress("segm-source", List.of(splitItem));

        assertTrue(Boolean.TRUE.equals(result));
        verify(addrSegmMapper).insert(any(AddrSegm.class));
        verify(addrSegmMapper).logicalDeleteBySegmIds(List.of("segm-source"));
        verify(nameService).buildStandNo(eq("江苏省南京市鼓楼区中央路1号102室"));
        verify(operationLogRecorder).record(
            eq("segm-source"),
            eq("SPLIT"),
            eq("江苏省南京市鼓楼区中央路1号101室"),
            contains("已拆分为")
        );
        verify(operationLogRecorder).record(
            eq("segm-new"),
            eq("SPLIT"),
            eq("江苏省南京市鼓楼区中央路1号102室"),
            contains("拆分创建")
        );
    }
}
