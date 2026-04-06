package org.dromara.address.search;

import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.search.builder.InstallationAddressSearchDocumentBuilder;
import org.dromara.address.search.builder.StandardAddressSearchDocumentBuilder;
import org.dromara.address.search.document.InstallationAddressSearchDocument;
import org.dromara.address.search.document.StandardAddressSearchDocument;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class AddressSearchDocumentBuilderTest {

    @Test
    void standardFromVoShouldMapCoreSearchFields() {
        StandardAddressVo source = new StandardAddressVo();
        source.setSegmId("1001");
        source.setParentSegmId("1000");
        source.setSegmName("世纪大道");
        source.setStandName("上海市浦东新区世纪大道");
        source.setSegmNo("SGM-1001");
        source.setStandNo("STD-1001");
        source.setSegmType("STREET");
        source.setRegionId("310000");
        source.setDistrictId("310115");
        source.setServiceRegionId("31010001");
        source.setAddrLevel(4);
        source.setStatus("0");
        Date standardCreateDate = new Date(1760000000000L);
        source.setCreateDate(standardCreateDate);

        StandardAddressSearchDocument target = StandardAddressSearchDocumentBuilder.fromVo(source);

        assertNotNull(target, "标准地址文档不应为空");
        assertEquals("1001", target.getDocumentId());
        assertEquals("1001", target.getSegmId());
        assertEquals("1000", target.getParentSegmId());
        assertEquals("世纪大道", target.getSegmName());
        assertEquals("上海市浦东新区世纪大道", target.getStandName());
        assertEquals("SGM-1001", target.getSegmNo());
        assertEquals("STD-1001", target.getStandNo());
        assertEquals("STREET", target.getSegmType());
        assertEquals("310000", target.getRegionId());
        assertEquals("310115", target.getDistrictId());
        assertEquals("31010001", target.getServiceRegionId());
        assertEquals(4, target.getAddrLevel());
        assertEquals("0", target.getStatus());
        assertEquals(standardCreateDate, target.getCreateDate());
    }

    @Test
    void installationFromVoShouldMapCoreSearchFieldsAndAssociationStatus() {
        InstallationAddressVo source = new InstallationAddressVo();
        source.setSetAddrId("2001");
        source.setSegmId("1001");
        source.setSetAddrName("A座101室");
        source.setStandName("上海市浦东新区世纪大道A座101室");
        source.setSetAddrNo("SET-2001");
        source.setSetType("HOME");
        source.setOrgId("ORG-01");
        source.setAssociationStatus("BOUND");
        Date installationCreateDate = new Date(1760000005000L);
        source.setCreateDate(installationCreateDate);
        source.setSegmType("BUILDING");
        source.setRegionId("310000");

        InstallationAddressSearchDocument target = InstallationAddressSearchDocumentBuilder.fromVo(source);

        assertNotNull(target, "安装地址文档不应为空");
        assertEquals("2001", target.getDocumentId());
        assertEquals("2001", target.getSetAddrId());
        assertEquals("1001", target.getSegmId());
        assertEquals("A座101室", target.getSetAddrName());
        assertEquals("上海市浦东新区世纪大道A座101室", target.getStandName());
        assertEquals("SET-2001", target.getSetAddrNo());
        assertEquals("HOME", target.getSetType());
        assertEquals("ORG-01", target.getOrgId());
        assertEquals("BOUND", target.getAssociationStatus());
        assertEquals("BUILDING", target.getSegmType());
        assertEquals("310000", target.getRegionId());
        assertEquals(installationCreateDate, target.getCreateDate());
    }
}
