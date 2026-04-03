package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class AddrSetSegmMappingTest {

    @Test
    void syncDateShouldMapToSynchronousDateColumn() throws NoSuchFieldException {
        Field syncDateField = AddrSetSegm.class.getDeclaredField("syncDate");
        TableField annotation = syncDateField.getAnnotation(TableField.class);

        assertNotNull(annotation, "syncDate 必须显式声明字段映射");
        assertEquals("synchronous_date", annotation.value(), "syncDate 必须映射到历史库字段 synchronous_date");
    }

    @Test
    void deviceIdShouldBeNonPersistentForHistoricalSchema() throws NoSuchFieldException {
        Field deviceIdField = AddrSetSegm.class.getDeclaredField("deviceId");
        TableField annotation = deviceIdField.getAnnotation(TableField.class);

        assertNotNull(annotation, "deviceId 必须显式声明为兼容字段");
        assertFalse(annotation.exist(), "历史库无 device_id，deviceId 只能作为兼容入参字段");
    }
}
