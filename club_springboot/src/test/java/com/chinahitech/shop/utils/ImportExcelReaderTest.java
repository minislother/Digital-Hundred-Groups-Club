package com.chinahitech.shop.utils;
import com.chinahitech.shop.bean.User;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Excel 流式导入读取测试")
class ImportExcelReaderTest {
 @TempDir Path dir;
 @Test @DisplayName("读取多工作表 - xlsx 与 xls 均识别表头并跳过空行")
 void testRead_MultipleSheetsAndFormats() throws Exception {
  for(String ext:List.of("xlsx","xls")){
   // Given - 两个工作表，表头前含说明行。
   Path file=dir.resolve("users."+ext);
   try(Workbook book=ext.equals("xlsx")?new XSSFWorkbook():new HSSFWorkbook()){
    for(int i=0;i<2;i++){
     var sheet=book.createSheet();sheet.createRow(0).createCell(0).setCellValue("导入说明");
     var header=sheet.createRow(1);header.createCell(0).setCellValue("姓名");header.createCell(1).setCellValue("学号");header.createCell(2).setCellValue("密码");
     var row=sheet.createRow(3);row.createCell(0).setCellValue("张三");row.createCell(1).setCellValue("001"+i);row.createCell(2).setCellValue("pwd");
    }
    try(var out=Files.newOutputStream(file)){book.write(out);}
   }
   // When - 逐行输出数据。
   List<User> users=new ArrayList<>();new ImportExcelReader().read(file,users::add);
   // Then - 保留文本账号和工作表顺序。
   assertEquals(2,users.size());assertEquals("0010",users.get(0).getUserId());assertEquals("0011",users.get(1).getUserId());
  }
 }
 @Test @DisplayName("读取异常 - 消费回调失败立即中止")
 void testRead_CallbackFailureStops() throws Exception {
  // Given - 有效工作簿。
  Path file=dir.resolve("users.xlsx");
  try(var book=new XSSFWorkbook()){
   var sheet=book.createSheet();var h=sheet.createRow(0);h.createCell(0).setCellValue("学号");h.createCell(1).setCellValue("姓名");h.createCell(2).setCellValue("密码");sheet.createRow(1).createCell(0).setCellValue("s1");
   try(var out=Files.newOutputStream(file)){book.write(out);}
  }
  // When & Then - 不吞掉后台事务异常。
  assertThrows(RuntimeException.class,()->new ImportExcelReader().read(file,u->{throw new IllegalStateException("stop");}));
 }
}
