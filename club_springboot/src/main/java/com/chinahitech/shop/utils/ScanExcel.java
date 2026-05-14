package com.chinahitech.shop.utils;

import com.chinahitech.shop.bean.User;
import com.chinahitech.shop.exception.FileTypeException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ScanExcel {

    private static final String XLSX = ".xlsx";
    private static final String XLS = ".xls";
    private static final String HEADER_STUDENT_ID = "\u5b66\u53f7";
    private static final String HEADER_NAME = "\u59d3\u540d";
    private static final String HEADER_ALT_NAME = "\u540d\u5b57";
    private static final String HEADER_PASSWORD = "\u5bc6\u7801";

    public List<User> readExcel(MultipartFile file) throws Exception {
        int res = checkFile(file);
        if (res == 0) {
            throw new FileNotFoundException("File not found");
        }
        if (res == 1) {
            return readXLSX(file);
        }
        if (res == 2) {
            return readXLS(file);
        }
        throw unsupportedFileTypeException();
    }

    public List<User> readExcel(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("File not found");
        }
        String fileName = file.getName();
        try (InputStream inputStream = new FileInputStream(file)) {
            if (fileName.endsWith(XLSX)) {
                return read(new XSSFWorkbook(inputStream));
            }
            if (fileName.endsWith(XLS)) {
                return read(new HSSFWorkbook(new POIFSFileSystem(inputStream)));
            }
        }
        throw unsupportedFileTypeException();
    }

    public int checkFile(MultipartFile file) {
        if (file == null) {
            return 0;
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return 0;
        }
        if (fileName.endsWith(XLSX)) {
            return 1;
        }
        if (fileName.endsWith(XLS)) {
            return 2;
        }
        return 3;
    }

    public List<User> readXLSX(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return read(new XSSFWorkbook(inputStream));
        }
    }

    public List<User> readXLS(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return read(new HSSFWorkbook(inputStream));
        }
    }

    public List<User> read(Workbook book) throws IOException {
        DataFormatter formatter = new DataFormatter();
        List<User> users = new ArrayList<>();
        for (int sheetIndex = 0; sheetIndex < book.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = book.getSheetAt(sheetIndex);
            int headerRowIndex = findHeaderRow(sheet, formatter);
            if (headerRowIndex < 0) {
                continue;
            }

            Row headerRow = sheet.getRow(headerRowIndex);
            Map<Integer, String> keyMap = buildHeaderMap(headerRow, formatter);
            if (!keyMap.containsValue(HEADER_STUDENT_ID)
                    || !keyMap.containsValue(HEADER_NAME)
                    || !keyMap.containsValue(HEADER_PASSWORD)) {
                continue;
            }

            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                User user = readUser(row, keyMap, formatter);
                if (user != null) {
                    users.add(user);
                }
            }
        }
        return users;
    }

    public String getValue(Cell cell, int rowNum, int index, Workbook book, boolean isKey) throws IOException {
        if (cell == null) {
            return null;
        }
        String value = new DataFormatter().formatCellValue(cell);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private int findHeaderRow(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
                String value = cellValue(row.getCell(cellIndex), formatter);
                if (value != null && value.contains(HEADER_STUDENT_ID)) {
                    return rowIndex;
                }
            }
        }
        return -1;
    }

    private Map<Integer, String> buildHeaderMap(Row headerRow, DataFormatter formatter) {
        Map<Integer, String> keyMap = new HashMap<>();
        for (int cellIndex = headerRow.getFirstCellNum(); cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            String value = cellValue(headerRow.getCell(cellIndex), formatter);
            if (value == null) {
                continue;
            }
            if (value.contains(HEADER_STUDENT_ID)) {
                keyMap.put(cellIndex, HEADER_STUDENT_ID);
            } else if (value.contains(HEADER_NAME) || value.contains(HEADER_ALT_NAME)) {
                keyMap.put(cellIndex, HEADER_NAME);
            } else if (value.contains(HEADER_PASSWORD)) {
                keyMap.put(cellIndex, HEADER_PASSWORD);
            }
        }
        return keyMap;
    }

    private User readUser(Row row, Map<Integer, String> keyMap, DataFormatter formatter) {
        if (row == null) {
            return null;
        }
        User user = new User();
        boolean hasValue = false;
        for (Map.Entry<Integer, String> entry : keyMap.entrySet()) {
            String value = cellValue(row.getCell(entry.getKey()), formatter);
            if (value == null) {
                continue;
            }
            hasValue = true;
            if (Objects.equals(entry.getValue(), HEADER_STUDENT_ID)) {
                user.setUserId(value);
            } else if (Objects.equals(entry.getValue(), HEADER_NAME)) {
                user.setUserName(value);
            } else if (Objects.equals(entry.getValue(), HEADER_PASSWORD)) {
                user.setPassword(value);
            }
        }
        return hasValue ? user : null;
    }

    private String cellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private FileTypeException unsupportedFileTypeException() {
        return new FileTypeException("\u6682\u4e0d\u652f\u6301\u8bfb\u53d6\u8be5\u6587\u4ef6\u683c\u5f0f");
    }
}
