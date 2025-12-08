package com.kalisco.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 엑셀 내보내기 서비스
 */
@Service
@Slf4j
public class ExcelExportService {
    
    /**
     * 메뉴별매출 데이터를 엑셀 파일로 변환
     * @param dataList 크롤링된 데이터 리스트
     * @param headers 헤더 리스트
     * @return 엑셀 파일 바이트 배열
     */
    public byte[] exportToExcel(List<Map<String, String>> dataList, List<String> headers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("메뉴별매출");
            
            // 스타일 생성
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            
            // 헤더 행 생성
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }
            
            // 데이터 행 생성
            int rowNum = 1;
            for (Map<String, String> rowData : dataList) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.createCell(i);
                    String header = headers.get(i);
                    String value = rowData.getOrDefault(header, "");
                    cell.setCellValue(value);
                    cell.setCellStyle(dataStyle);
                }
            }
            
            // 컬럼 너비 자동 조정
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                // 최소 너비 설정
                int columnWidth = sheet.getColumnWidth(i);
                if (columnWidth < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }
            
            // 바이트 배열로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
