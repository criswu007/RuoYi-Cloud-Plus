package org.dromara.address.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddrSetSegm;
import org.dromara.address.domain.bo.InstallationAddressBo;
import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.esmapper.InstallationAddressSearchEsMapper;
import org.dromara.address.search.document.InstallationAddressSearchDocument;
import org.dromara.address.search.model.AddressSearchIndexRuntimeInfo;
import org.dromara.address.search.service.impl.EasyEsInstallationAddressSearchGateway;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.easyes.common.enums.EsQueryTypeEnum;
import org.dromara.easyes.core.biz.BaseSortParam;
import org.dromara.easyes.core.biz.EsPageInfo;
import org.dromara.easyes.core.biz.Param;
import org.dromara.easyes.core.conditions.select.LambdaEsQueryWrapper;
import org.dromara.easyes.core.kernel.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class InstallationAddressSearchGatewayTest {

    @Mock
    private InstallationAddressSearchEsMapper installationAddressSearchEsMapper;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    private AddressSearchProperties addressSearchProperties;

    private EasyEsInstallationAddressSearchGateway gateway;

    @BeforeEach
    void setUp() {
        addressSearchProperties = new AddressSearchProperties();
        addressSearchProperties.getInstallation().setAlias("address_installation_search");
        gateway = new EasyEsInstallationAddressSearchGateway(installationAddressSearchEsMapper, addressSearchProperties, elasticsearchClient);
    }

    @Test
    void queryPageShouldUseConfiguredIndexAndBuildPhraseFiltersAndDefaultOrder() throws Exception {
        InstallationAddressBo bo = new InstallationAddressBo();
        bo.setSetAddrName("机房");
        bo.setSetType("ROOM");
        bo.setSegmId("SEG001");
        bo.setRegionId("320106");
        bo.setOrgId("ORG001");
        bo.setAssociationStatus("BOUND");
        PageQuery pageQuery = new PageQuery(20, 2);

        InstallationAddressSearchDocument document = new InstallationAddressSearchDocument();
        document.setSetAddrId("SET001");
        document.setSegmId("SEG001");
        document.setSetAddrName("机房1排1列");
        document.setSetAddrNo("JF-001");
        document.setSetType("ROOM");
        document.setSegmType("180010");
        document.setRegionId("320106");
        document.setOrgId("ORG001");
        document.setAssociationStatus("BOUND");
        EsPageInfo<InstallationAddressSearchDocument> pageInfo = new EsPageInfo<>(List.of(document));
        pageInfo.setTotal(8L);
        when(installationAddressSearchEsMapper.pageQuery(any(), eq(2), eq(20))).thenReturn(pageInfo);

        TableDataInfo<InstallationAddressVo> result = gateway.queryPage(bo, pageQuery);

        ArgumentCaptor<LambdaEsQueryWrapper<InstallationAddressSearchDocument>> wrapperCaptor = ArgumentCaptor.forClass(LambdaEsQueryWrapper.class);
        verify(installationAddressSearchEsMapper).setCurrentActiveIndex("address_installation_search");
        verify(installationAddressSearchEsMapper).pageQuery(wrapperCaptor.capture(), eq(2), eq(20));
        assertEquals(8L, result.getTotal());
        assertEquals("SET001", result.getRows().get(0).getSetAddrId());
        assertEquals("SEG001", result.getRows().get(0).getSegmId());
        assertMultiMatchQueryParam(wrapperCaptor.getValue(), "setAddrName", "机房");
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERM, "setType", "ROOM");
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERM, "segmId", "SEG001");
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERM, "regionId", "320106");
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERM, "orgId", "ORG001");
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERM, "associationStatus", "BOUND");
        assertOrder(wrapperCaptor.getValue(), "createDate", "desc");
        assertOrder(wrapperCaptor.getValue(), "setAddrId", "desc");
    }

    @Test
    void bulkIndexShouldWriteIntoPreparedPhysicalIndex() {
        AddrSetSegm entity = new AddrSetSegm();
        entity.setSetAddrId("SET002");
        entity.setSegmId("SEG002");
        entity.setDeleteState("0");
        entity.setCreateDate(new Date());
        when(installationAddressSearchEsMapper.insertBatch(eq("address_installation_search_v2"), any())).thenReturn(1);

        gateway.bulkIndex("address_installation_search_v2", List.of(entity));

        verify(installationAddressSearchEsMapper).setCurrentActiveIndex("address_installation_search_v2");
        verify(installationAddressSearchEsMapper).insertBatch(eq("address_installation_search_v2"), any());
    }

    @Test
    void getRuntimeInfoShouldFallbackToConcreteIndexWhenAliasMissing() throws Exception {
        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        BooleanResponse aliasMissingResponse = mock(BooleanResponse.class);
        BooleanResponse indexExistsResponse = mock(BooleanResponse.class);
        CountResponse countResponse = mock(CountResponse.class);
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(indicesClient.existsAlias(any(java.util.function.Function.class))).thenReturn(aliasMissingResponse);
        when(aliasMissingResponse.value()).thenReturn(false);
        when(indicesClient.exists(any(java.util.function.Function.class))).thenReturn(indexExistsResponse);
        when(indexExistsResponse.value()).thenReturn(true);
        when(elasticsearchClient.count(any(java.util.function.Function.class))).thenReturn(countResponse);
        when(countResponse.count()).thenReturn(2500L);

        AddressSearchIndexRuntimeInfo runtimeInfo = gateway.getRuntimeInfo();

        assertEquals("address_installation_search", runtimeInfo.alias());
        assertEquals("address_installation_search", runtimeInfo.physicalIndexName());
        assertEquals(2500L, runtimeInfo.documentCount());
    }

    private static void assertQueryParam(Wrapper<?> wrapper, EsQueryTypeEnum expectedType, String expectedColumn, Object expectedValue) throws Exception {
        List<Param> params = readField(wrapper, "paramQueue");
        assertTrue(params.stream().anyMatch(param -> expectedType == param.getQueryTypeEnum()
            && expectedColumn.equals(param.getColumn())
            && expectedValue.equals(param.getVal())));
    }

    private static void assertOrder(Wrapper<?> wrapper, String expectedSort, String expectedOrder) throws Exception {
        List<BaseSortParam> baseSortParams = readField(wrapper, "baseSortParams");
        assertTrue(baseSortParams.stream().anyMatch(param -> expectedSort.equals(param.getSortField())
            && expectedOrder.equalsIgnoreCase(param.getSortOrder().jsonValue())));
    }

    private static void assertMultiMatchQueryParam(Wrapper<?> wrapper, String expectedColumn, Object expectedValue) throws Exception {
        List<Param> params = readField(wrapper, "paramQueue");
        assertTrue(params.stream().anyMatch(param -> EsQueryTypeEnum.MULTI_MATCH == param.getQueryTypeEnum()
            && param.getColumns() != null
            && java.util.Arrays.asList(param.getColumns()).contains(expectedColumn)
            && expectedValue.equals(param.getVal())));
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, String fieldName) throws Exception {
        Field field = Wrapper.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(target);
        assertNotNull(value, () -> fieldName + " 不应为空");
        return (T) value;
    }
}
