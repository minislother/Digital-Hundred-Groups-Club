package com.chinahitech.shop.utils;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Excel导入列策略")
class ScanExcelTest {
    @Test
    @DisplayName("兼容名字表头、列重排与无关列")
    void testRead_AliasAndReorderedColumns_ImportsUser() throws Exception {
        // Given
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet();
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("名字");
            header.createCell(1).setCellValue("备注");
            header.createCell(2).setCellValue("密码");
            header.createCell(3).setCellValue("学号");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("张三");
            row.createCell(1).setCellValue("忽略");
            row.createCell(2).setCellValue("example-password");
            row.createCell(3).setCellValue("20240001");

            // When
            var users = new ScanExcel().read(workbook);

            // Then
            assertEquals(1, users.size());
            assertEquals("20240001", users.getFirst().getUserId());
            assertEquals("张三", users.getFirst().getUserName());
            assertEquals("example-password", users.getFirst().getPassword());
        }
    }
}
