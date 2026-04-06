package org.dromara.address.search;

import org.dromara.address.search.document.InstallationAddressSearchDocument;
import org.dromara.address.search.document.StandardAddressSearchDocument;
import org.dromara.easyes.annotation.IndexField;
import org.dromara.easyes.annotation.IndexId;
import org.dromara.easyes.annotation.IndexName;
import org.dromara.easyes.annotation.rely.FieldType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class AddressSearchDocumentMetadataTest {

    @Test
    void standardDocumentShouldDeclareIndexMetadataForSearch() throws Exception {
        IndexName indexName = StandardAddressSearchDocument.class.getAnnotation(IndexName.class);

        assertNotNull(indexName, "标准地址 document 必须声明索引元数据");
        assertEquals("address_standard_search", indexName.value());
        assertFieldHasIndexId(StandardAddressSearchDocument.class, "documentId");
        assertFieldType(StandardAddressSearchDocument.class, "segmId", FieldType.KEYWORD);
        assertTextField(StandardAddressSearchDocument.class, "segmName");
        assertTextField(StandardAddressSearchDocument.class, "standName");
        assertFieldType(StandardAddressSearchDocument.class, "addrLevel", FieldType.INTEGER);
        assertFieldType(StandardAddressSearchDocument.class, "regionId", FieldType.KEYWORD);
        assertFieldType(StandardAddressSearchDocument.class, "createDate", FieldType.DATE);
    }

    @Test
    void installationDocumentShouldDeclareIndexMetadataForSearch() throws Exception {
        IndexName indexName = InstallationAddressSearchDocument.class.getAnnotation(IndexName.class);

        assertNotNull(indexName, "安装地址 document 必须声明索引元数据");
        assertEquals("address_installation_search", indexName.value());
        assertFieldHasIndexId(InstallationAddressSearchDocument.class, "documentId");
        assertFieldType(InstallationAddressSearchDocument.class, "setAddrId", FieldType.KEYWORD);
        assertTextField(InstallationAddressSearchDocument.class, "setAddrName");
        assertTextField(InstallationAddressSearchDocument.class, "standName");
        assertFieldType(InstallationAddressSearchDocument.class, "segmType", FieldType.KEYWORD);
        assertFieldType(InstallationAddressSearchDocument.class, "regionId", FieldType.KEYWORD);
        assertFieldType(InstallationAddressSearchDocument.class, "createDate", FieldType.DATE);
    }

    private static void assertFieldHasIndexId(Class<?> documentClass, String fieldName) throws Exception {
        Field field = documentClass.getDeclaredField(fieldName);
        assertNotNull(field.getAnnotation(IndexId.class), () -> documentClass.getSimpleName() + "." + fieldName + " 必须声明 @IndexId");
    }

    private static void assertTextField(Class<?> documentClass, String fieldName) throws Exception {
        Field field = documentClass.getDeclaredField(fieldName);
        IndexField indexField = field.getAnnotation(IndexField.class);
        assertNotNull(indexField, () -> documentClass.getSimpleName() + "." + fieldName + " 必须声明 @IndexField");
        assertEquals(FieldType.TEXT, indexField.fieldType());
        assertEquals("ik_max_word", indexField.analyzer());
        assertEquals("ik_smart", indexField.searchAnalyzer());
    }

    private static void assertFieldType(Class<?> documentClass, String fieldName, FieldType expectedType) throws Exception {
        Field field = documentClass.getDeclaredField(fieldName);
        IndexField indexField = field.getAnnotation(IndexField.class);
        assertNotNull(indexField, () -> documentClass.getSimpleName() + "." + fieldName + " 必须声明 @IndexField");
        assertEquals(expectedType, indexField.fieldType());
    }
}
