package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * `spc_station` 实体字段映射兼容性测试。
 * 目的：约束管理站名称字段始终映射线上真实列 `china_name`，避免默认查询误访问不存在的 `station_name`。
 * 入参/出参：无显式入参，测试通过表示实体字段映射满足线上库兼容约束。
 * 关键约束：凡通过 MyBatis-Plus 通用查询读取管理站名称时，必须依赖显式字段映射而不是默认驼峰转下划线规则。
 * 异常与副作用：断言失败会直接标记测试失败；无数据库写入副作用。
 */
@Tag("dev")
class SpcStationMappingTest {

    /**
     * 目的：验证 `stationName` 字段映射到线上 `spc_station.china_name`。
     * 入参：无。
     * 出参：无。
     * 关键约束：必须显式声明 `@TableField("china_name")`，否则 `selectByIds` 一类通用查询会错误访问 `station_name`。
     * 异常与副作用：若注解缺失或列名错误，测试直接失败；无其他副作用。
     *
     * @throws NoSuchFieldException 当实体字段名变更但测试未同步时抛出，提示更新映射约束。
     */
    @Test
    void stationNameShouldMapToChinaNameColumn() throws NoSuchFieldException {
        Field stationNameField = SpcStation.class.getDeclaredField("stationName");
        TableField annotation = stationNameField.getAnnotation(TableField.class);

        assertNotNull(annotation, "stationName 必须显式声明线上物理列映射");
        assertEquals("china_name", annotation.value(), "stationName 必须映射到 spc_station.china_name");
    }
}
