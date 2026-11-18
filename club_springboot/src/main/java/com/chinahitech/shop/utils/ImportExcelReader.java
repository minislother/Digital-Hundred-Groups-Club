package com.chinahitech.shop.utils;

import com.chinahitech.shop.bean.User;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.*;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.util.XMLHelper;
import org.xml.sax.InputSource;
import java.nio.file.Path;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

/** XLSX 使用 SAX 逐行读取；旧 XLS 受上传大小限制，逐行输出而不累计用户列表。 */
public class ImportExcelReader {
 public void read(Path path, Consumer<User> output) throws Exception {
  if(path.toString().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
   try(OPCPackage pkg=OPCPackage.open(path.toFile(),PackageAccess.READ)) {
    XSSFReader reader=new XSSFReader(pkg);
    reader.setUseReadOnlySharedStringsTable(true);
    StylesTable styles=reader.getStylesTable();
    var strings=reader.getSharedStringsTable();
    var sheets=reader.getSheetsData();
    while(sheets.hasNext()) {
     try(InputStream sheet=sheets.next()) {
      Rows rows=new Rows(output);
      var parser=XMLHelper.newXMLReader();
      parser.setContentHandler(new XSSFSheetXMLHandler(styles,strings,rows,new DataFormatter(),false));
      parser.parse(new InputSource(sheet));
     }
    }
   }
  } else {
   try(Workbook book=WorkbookFactory.create(path.toFile(),null,true)) {
    DataFormatter formatter=new DataFormatter();
    for(Sheet sheet:book) {
     Rows rows=new Rows(output);
     for(Row row:sheet) {
      rows.startRow(row.getRowNum());
      for(Cell cell:row) rows.values.put(cell.getColumnIndex(),formatter.formatCellValue(cell));
      rows.endRow(row.getRowNum());
     }
    }
   }
  }
 }
 private static class Rows implements XSSFSheetXMLHandler.SheetContentsHandler {
  final Consumer<User> output;
  final Map<Integer,String> values=new HashMap<>();
  int account=-1,name=-1,password=-1;
  Rows(Consumer<User> output){this.output=output;}
  public void startRow(int row){values.clear();}
  public void cell(String ref,String value,XSSFComment comment){values.put(new CellReference(ref).getCol()+0,value);}
  public void endRow(int row){
   if(account<0) {
    for(var cell:values.entrySet()) {
     String v=cell.getValue();
     if(v.contains("学号")) account=cell.getKey();
     else if(v.contains("姓名")||v.contains("名字")) name=cell.getKey();
     else if(v.contains("密码")) password=cell.getKey();
    }
    if(account<0||name<0||password<0){account=-1;name=-1;password=-1;}
    return;
   }
   if(values.values().stream().allMatch(v->v==null||v.isBlank())) return;
   User user=new User();
   user.setUserId(trim(values.get(account)));user.setUserName(trim(values.get(name)));user.setPassword(trim(values.get(password)));
   output.accept(user);
  }
  private String trim(String s){return s==null?null:s.trim();}
 }
}
