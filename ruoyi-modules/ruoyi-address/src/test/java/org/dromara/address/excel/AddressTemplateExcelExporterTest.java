package org.dromara.address.excel;

import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dromara.address.domain.vo.StandardAddressImportVo;
import org.dromara.common.core.service.DictService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.excel.core.DropDownOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class AddressTemplateExcelExporterTest {

    @Test
    void shouldPreserveAnnotatedRowHeightsAndColumnWidthsWhenAutoWidthDisabled() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<DropDownOptions> options = List.of(
            new DropDownOptions(0, List.of("建筑、楼栋")),
            new DropDownOptions(1, List.of("是", "否")),
            new DropDownOptions(7, List.of("FTTH_双纤")),
            new DropDownOptions(8, List.of("1G-PON")),
            new DropDownOptions(9, List.of("城区")),
            new DropDownOptions(10, List.of("普通住宅")),
            new DropDownOptions(11, List.of("是", "否"))
        );

        try (var springUtilMock = org.mockito.Mockito.mockStatic(SpringUtils.class)) {
            springUtilMock.when(() -> SpringUtils.getBean(DictService.class)).thenReturn(org.mockito.Mockito.mock(DictService.class));

            AddressTemplateExcelExporter.exportTemplate(
                List.<StandardAddressImportVo>of(),
                "标准地址导入模板",
                StandardAddressImportVo.class,
                outputStream,
                options,
                false
            );
        }

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);

            assertEquals(24f, header.getHeightInPoints());
            assertEquals(22f, sheet.getDefaultRowHeightInPoints());
            assertEquals(18 * 256, sheet.getColumnWidth(0));
            assertEquals(12 * 256, sheet.getColumnWidth(1));
            assertEquals(48 * 256, sheet.getColumnWidth(2));
            assertEquals(20 * 256, sheet.getColumnWidth(3));
            assertEquals(22 * 256, sheet.getColumnWidth(4));
            assertEquals(22 * 256, sheet.getColumnWidth(5));
            assertEquals(22 * 256, sheet.getColumnWidth(6));
            assertEquals(16 * 256, sheet.getColumnWidth(7));
            assertEquals(16 * 256, sheet.getColumnWidth(8));
            assertEquals(16 * 256, sheet.getColumnWidth(9));
            assertEquals(16 * 256, sheet.getColumnWidth(10));
            assertEquals(16 * 256, sheet.getColumnWidth(11));
            assertEquals(12 * 256, sheet.getColumnWidth(12));
            assertEquals(20 * 256, sheet.getColumnWidth(13));

            Set<Integer> validatedColumns = new HashSet<>();
            Map<Integer, Boolean> columnCoversRows = new HashMap<>();
            Map<Integer, DataValidation> validationForColumn = new HashMap<>();
            Map<Integer, String[]> expectedOptions = Map.ofEntries(
                Map.entry(0, new String[] { "建筑、楼栋" }),
                Map.entry(1, new String[] { "是", "否" }),
                Map.entry(7, new String[] { "FTTH_双纤" }),
                Map.entry(8, new String[] { "1G-PON" }),
                Map.entry(9, new String[] { "城区" }),
                Map.entry(10, new String[] { "普通住宅" }),
                Map.entry(11, new String[] { "是", "否" })
            );
            expectedOptions.keySet().forEach(column -> columnCoversRows.put(column, false));
            for (DataValidation validation : sheet.getDataValidations()) {
                for (var region : validation.getRegions().getCellRangeAddresses()) {
                    boolean coversRows = region.getFirstRow() > 0 && region.getLastRow() > region.getFirstRow();
                    for (int column = region.getFirstColumn(); column <= region.getLastColumn(); column++) {
                        validatedColumns.add(column);
                        if (expectedOptions.containsKey(column)) {
                            columnCoversRows.computeIfPresent(column, (key, value) -> value || coversRows);
                            validationForColumn.putIfAbsent(column, validation);
                        }
                    }
                }
            }
            assertEquals(Set.of(0, 1, 7, 8, 9, 10, 11), validatedColumns);
            expectedOptions.forEach((column, expectedValue) -> {
                DataValidation validation = validationForColumn.get(column);
                assertNotNull(validation, "Expected validation for column " + column);
                assertArrayEquals(expectedValue, validation.getValidationConstraint().getExplicitListValues());
                assertTrue(columnCoversRows.get(column), "Column " + column + " should cover rows below header");
            });
        }
    }
}
