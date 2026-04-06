package org.dromara.address.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ClosePointInTimeRequest;
import co.elastic.clients.elasticsearch.core.ClosePointInTimeResponse;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeRequest;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.util.ObjectBuilder;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.dromara.address.config.AddressSearchProperties;
import org.dromara.address.domain.AddrSegm;
import org.dromara.address.domain.bo.StandardAddressBo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.esmapper.StandardAddressSearchEsMapper;
import org.dromara.address.search.document.StandardAddressSearchDocument;
import org.dromara.address.search.model.AddressSearchIndexRuntimeInfo;
import org.dromara.address.search.service.impl.EasyEsStandardAddressSearchGateway;
import org.dromara.address.service.impl.StandardAddressDictionaryService;
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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StandardAddressSearchGatewayTest {

    @Mock
    private StandardAddressSearchEsMapper standardAddressSearchEsMapper;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private StandardAddressDictionaryService dictionaryService;

    private AddressSearchProperties addressSearchProperties;

    private EasyEsStandardAddressSearchGateway gateway;

    @BeforeEach
    void setUp() {
        addressSearchProperties = new AddressSearchProperties();
        addressSearchProperties.getStandard().setAlias("address_standard_search");
        gateway = new EasyEsStandardAddressSearchGateway(standardAddressSearchEsMapper, addressSearchProperties, elasticsearchClient, dictionaryService);
    }

    @Test
    void searchCandidatesShouldOnlyUseStandNameFullNamePhraseMatch() throws Exception {
        StandardAddressSearchDocument document = new StandardAddressSearchDocument();
        document.setSegmId("SEG001");
        document.setParentSegmId("P001");
        document.setSegmName("鼓楼区中央路");
        document.setStandName("江苏省南京市鼓楼区中央路");
        document.setSegmNo("GLQZYL");
        document.setStandNo("JSNJGLQZYL");
        document.setSegmType("180010");
        document.setRegionId("320106");
        document.setAddrLevel(7);
        document.setStatus("2140900");
        when(standardAddressSearchEsMapper.pageQuery(any(), eq(1), eq(10))).thenReturn(new EsPageInfo<>(List.of(document), 1));

        List<StandardAddressVo> result = gateway.searchCandidates("鼓楼", List.of("180010"), "2140900", "320106", 10);

        ArgumentCaptor<LambdaEsQueryWrapper<StandardAddressSearchDocument>> wrapperCaptor = ArgumentCaptor.forClass(LambdaEsQueryWrapper.class);
        verify(standardAddressSearchEsMapper).setCurrentActiveIndex("address_standard_search");
        verify(standardAddressSearchEsMapper).pageQuery(wrapperCaptor.capture(), eq(1), eq(10));
        assertEquals(List.of("SEG001"), result.stream().map(StandardAddressVo::getSegmId).toList());
        assertMultiMatchQueryParam(wrapperCaptor.getValue(), "standName", "鼓楼");
        assertNoQueryParam(wrapperCaptor.getValue(), "segmName");
        assertNoQueryParam(wrapperCaptor.getValue(), "segmNo");
        assertNoQueryParam(wrapperCaptor.getValue(), "standNo");
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERMS, "segmType", List.of("180010"));
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERM, "status", "2140900");
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERM, "regionId", "320106");
        assertOrder(wrapperCaptor.getValue(), "addrLevel", "asc");
        assertOrder(wrapperCaptor.getValue(), "segmId", "asc");
    }

    @Test
    void queryPageShouldOnlyUseStandNameFullNamePhraseMatch() throws Exception {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setSegmName("中央路");
        bo.setStandName("江苏省南京市鼓楼区中央路");
        bo.setRegionId("320106");
        bo.setStatus("2140900");
        PageQuery pageQuery = new PageQuery(20, 2);
        pageQuery.setOrderByColumn("createDate");
        pageQuery.setIsAsc("desc");

        StandardAddressSearchDocument document = new StandardAddressSearchDocument();
        document.setSegmId("SEG002");
        document.setParentSegmId("P002");
        document.setSegmName("中央路100号");
        document.setStandName("江苏省南京市鼓楼区中央路100号");
        document.setSegmNo("ZYL100H");
        document.setStandNo("JSNJGLQZYL100H");
        document.setSegmType("180010");
        document.setRegionId("320106");
        document.setDistrictId("320106");
        document.setServiceRegionId("SR001");
        document.setAddrLevel(7);
        document.setStatus("2140900");
        EsPageInfo<StandardAddressSearchDocument> pageInfo = new EsPageInfo<>(List.of(document));
        pageInfo.setTotal(12L);
        when(standardAddressSearchEsMapper.pageQuery(any(), eq(2), eq(20))).thenReturn(pageInfo);

        TableDataInfo<StandardAddressVo> result = gateway.queryPage(bo, List.of("180010"), pageQuery);

        ArgumentCaptor<LambdaEsQueryWrapper<StandardAddressSearchDocument>> wrapperCaptor = ArgumentCaptor.forClass(LambdaEsQueryWrapper.class);
        verify(standardAddressSearchEsMapper).setCurrentActiveIndex("address_standard_search");
        verify(standardAddressSearchEsMapper).pageQuery(wrapperCaptor.capture(), eq(2), eq(20));
        assertEquals(12L, result.getTotal());
        assertEquals("SEG002", result.getRows().get(0).getSegmId());
        assertEquals("江苏省南京市鼓楼区中央路100号", result.getRows().get(0).getStandName());
        assertMultiMatchQueryParam(wrapperCaptor.getValue(), "standName", "江苏省南京市鼓楼区中央路");
        assertNoQueryParam(wrapperCaptor.getValue(), "segmName");
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERMS, "segmType", List.of("180010"));
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERM, "regionId", "320106");
        assertQueryParam(wrapperCaptor.getValue(), EsQueryTypeEnum.TERM, "status", "2140900");
        assertOrder(wrapperCaptor.getValue(), "createDate", "desc");
    }

    @Test
    void queryPageShouldUseReverseTailWindowWhenRequestedPageExceedsHeadWindow() throws Exception {
        StandardAddressBo bo = new StandardAddressBo();
        bo.setRegionId("320106");
        PageQuery pageQuery = new PageQuery(10, 1002);

        StandardAddressSearchDocument newerDocument = new StandardAddressSearchDocument();
        newerDocument.setSegmId("SEG002");
        newerDocument.setStandName("江苏省南京市鼓楼区中央路2号");
        StandardAddressSearchDocument olderDocument = new StandardAddressSearchDocument();
        olderDocument.setSegmId("SEG001");
        olderDocument.setStandName("江苏省南京市鼓楼区中央路1号");

        SearchResponse<StandardAddressSearchDocument> countResponse = SearchResponse.of(response -> response
            .took(1)
            .timedOut(false)
            .shards(shards -> shards.total(1).successful(1).failed(0))
            .hits(hits -> hits
                .total(total -> total.value(10020L).relation(TotalHitsRelation.Eq))
                .hits(List.of())));
        SearchResponse<StandardAddressSearchDocument> tailResponse = SearchResponse.of(response -> response
            .took(1)
            .timedOut(false)
            .shards(shards -> shards.total(1).successful(1).failed(0))
            .hits(hits -> hits
                .total(total -> total.value(10020L).relation(TotalHitsRelation.Eq))
                .hits(List.of(
                    Hit.of(hit -> hit.index("address_standard_search").id("SEG002").source(newerDocument)),
                    Hit.of(hit -> hit.index("address_standard_search").id("SEG001").source(olderDocument))
                ))));
        when(standardAddressSearchEsMapper.getSearchBuilder(any())).thenAnswer(invocation -> new SearchRequest.Builder());
        when(elasticsearchClient.openPointInTime(anyOpenPitFunction())).thenReturn(OpenPointInTimeResponse.of(response -> response.id("pit-1")));
        when(elasticsearchClient.closePointInTime(anyClosePitFunction())).thenReturn(ClosePointInTimeResponse.of(response -> response.succeeded(true).numFreed(1)));
        when(elasticsearchClient.search(any(SearchRequest.class), eq(StandardAddressSearchDocument.class)))
            .thenReturn(countResponse)
            .thenReturn(tailResponse);

        TableDataInfo<StandardAddressVo> result = gateway.queryPage(bo, List.of("180010"), pageQuery);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(elasticsearchClient, times(2)).search(requestCaptor.capture(), eq(StandardAddressSearchDocument.class));
        List<SearchRequest> requests = requestCaptor.getAllValues();
        assertEquals(0, requests.get(0).size());
        assertEquals(0, requests.get(1).from());
        assertEquals(10, requests.get(1).size());
        assertEquals(10020L, result.getTotal());
        assertEquals(List.of("SEG001", "SEG002"), result.getRows().stream().map(StandardAddressVo::getSegmId).toList());
    }

    @Test
    void bulkIndexShouldWriteIntoPreparedPhysicalIndex() {
        AddrSegm entity = new AddrSegm();
        entity.setSegmId("SEG003");
        entity.setSegmType("180010");
        entity.setDeleteState("0");
        entity.setCreateDate(new Date());
        when(standardAddressSearchEsMapper.insertBatch(eq("address_standard_search_v2"), any())).thenReturn(1);

        gateway.bulkIndex("address_standard_search_v2", List.of(entity));

        verify(standardAddressSearchEsMapper).setCurrentActiveIndex("address_standard_search_v2");
        verify(standardAddressSearchEsMapper).insertBatch(eq("address_standard_search_v2"), any());
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
        when(countResponse.count()).thenReturn(2191L);

        AddressSearchIndexRuntimeInfo runtimeInfo = gateway.getRuntimeInfo();

        assertEquals("address_standard_search", runtimeInfo.alias());
        assertEquals("address_standard_search", runtimeInfo.physicalIndexName());
        assertEquals(2191L, runtimeInfo.documentCount());
    }

    private static void assertQueryParam(Wrapper<?> wrapper, EsQueryTypeEnum expectedType, String expectedColumn, Object expectedValue) throws Exception {
        List<Param> params = readField(wrapper, "paramQueue");
        assertTrue(params.stream().anyMatch(param -> expectedType == param.getQueryTypeEnum()
            && expectedColumn.equals(param.getColumn())
            && valueEquals(expectedValue, param.getVal())));
    }

    private static void assertOrder(Wrapper<?> wrapper, String expectedSort, String expectedOrder) throws Exception {
        List<BaseSortParam> baseSortParams = readField(wrapper, "baseSortParams");
        assertTrue(baseSortParams.stream().anyMatch(param -> expectedSort.equals(param.getSortField())
            && expectedOrder.equalsIgnoreCase(param.getSortOrder().jsonValue())));
    }

    private static void assertNoQueryParam(Wrapper<?> wrapper, String unexpectedColumn) throws Exception {
        List<Param> params = readField(wrapper, "paramQueue");
        assertTrue(params.stream().noneMatch(param -> unexpectedColumn.equals(param.getColumn())
                || (param.getColumns() != null && java.util.Arrays.asList(param.getColumns()).contains(unexpectedColumn))),
            () -> "不应包含查询字段: " + unexpectedColumn);
    }

    private static void assertMultiMatchQueryParam(Wrapper<?> wrapper, String expectedColumn, Object expectedValue) throws Exception {
        List<Param> params = readField(wrapper, "paramQueue");
        assertTrue(params.stream().anyMatch(param -> EsQueryTypeEnum.MULTI_MATCH == param.getQueryTypeEnum()
            && param.getColumns() != null
            && java.util.Arrays.asList(param.getColumns()).contains(expectedColumn)
            && valueEquals(expectedValue, param.getVal())));
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, String fieldName) throws Exception {
        Field field = Wrapper.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(target);
        assertNotNull(value, () -> fieldName + " 不应为空");
        return (T) value;
    }

    private static boolean valueEquals(Object expectedValue, Object actualValue) {
        if (expectedValue instanceof Collection<?> expectedCollection && actualValue instanceof Collection<?> actualCollection) {
            return expectedCollection.size() == actualCollection.size() && actualCollection.containsAll(expectedCollection);
        }
        return expectedValue == null ? actualValue == null : expectedValue.equals(actualValue);
    }

    @SuppressWarnings("unchecked")
    private static Function<OpenPointInTimeRequest.Builder, ObjectBuilder<OpenPointInTimeRequest>> anyOpenPitFunction() {
        return (Function<OpenPointInTimeRequest.Builder, ObjectBuilder<OpenPointInTimeRequest>>) any(Function.class);
    }

    @SuppressWarnings("unchecked")
    private static Function<ClosePointInTimeRequest.Builder, ObjectBuilder<ClosePointInTimeRequest>> anyClosePitFunction() {
        return (Function<ClosePointInTimeRequest.Builder, ObjectBuilder<ClosePointInTimeRequest>>) any(Function.class);
    }
}
