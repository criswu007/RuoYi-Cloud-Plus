package org.dromara.address.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * `ADDR_SEGM` 实体线上库兼容性测试。
 * 目的：约束 `AddrSegm` 在对接线上 `ftth_cloud_address` 时，不再查询线上不存在的 `modify_date` 字段。
 * 入参/出参：无显式入参，测试通过表示实体字段映射满足线上库兼容约束。
 * 关键约束：若字段仅存在于 standalone 样例库而不存在于线上库，实体必须显式声明为非持久化字段。
 * 异常与副作用：断言失败会直接标记测试失败；无数据库写入副作用。
 */
public class AddrSegmCompatibilityTest {

    /**
     * 目的：验证 `modifyDate` 字段不会再参与 MyBatis-Plus 的默认查询列拼装。
     * 入参：无。
     * 出参：无。
     * 关键约束：必须通过 `@TableField(exist = false)` 显式排除该字段，避免线上库查询 `modify_date` 时报错。
     * 异常与副作用：若字段缺少注解或仍参与持久化映射，测试直接失败；无其他副作用。
     *
     * @throws NoSuchFieldException 当实体字段名变更但测试未同步时抛出，提示更新兼容性约束。
     */
    @Test
    @Tag("dev")
    void shouldExcludeModifyDateFromOnlineAddrSegmPersistenceMapping() throws NoSuchFieldException {
        Field modifyDateField = AddrSegm.class.getDeclaredField("modifyDate");
        TableField tableField = modifyDateField.getAnnotation(TableField.class);

        assertNotNull(tableField, "modifyDate 必须显式声明 TableField 映射策略");
        assertFalse(tableField.exist(), "modifyDate 不应参与线上 ADDR_SEGM 的默认持久化字段映射");
    }
}
