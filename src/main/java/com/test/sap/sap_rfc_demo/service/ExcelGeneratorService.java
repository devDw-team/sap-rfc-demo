package com.test.sap.sap_rfc_demo.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExcelGeneratorService {
    
    private static final String TEMPLATE_PATH = "src/main/resources/static/excel/billinfo_template.xlsx";
    private static final String DOWNLOAD_PATH = "src/main/resources/static/excel/download/";
    
    @SuppressWarnings("unchecked")
    public boolean generateExcelFromTemplate(Map<String, Object> data) {
        try {
            if (data == null || !data.containsKey("customerInfo") || !data.containsKey("billInfo")) {
                return false;
            }
            
            // 현재 날짜 정보 가져오기
            LocalDate now = LocalDate.now();
            String year = String.valueOf(now.getYear());
            String month = String.format("%02d", now.getMonthValue());
            
            // 템플릿 파일 로드
            FileInputStream template = new FileInputStream(TEMPLATE_PATH);
            Workbook workbook = new XSSFWorkbook(template);
            Sheet sheet = workbook.getSheetAt(0);
            
            // customerInfo 데이터 매핑
            Map<String, Object> customerInfo = (Map<String, Object>) data.get("customerInfo");
            replacePlaceholder(sheet, "{사업자명}", String.valueOf(customerInfo.get("custNm")));
            replacePlaceholder(sheet, "{YYYY.MM.DD}", "2025.05.30");
            replacePlaceholder(sheet, "{사업자번호}", String.valueOf(customerInfo.get("stcd2")));
            replacePlaceholder(sheet, "{대표자}", String.valueOf(customerInfo.get("j1kfrepre")));
            replacePlaceholder(sheet, "{업태}", String.valueOf(customerInfo.get("j1kftbus")));
            replacePlaceholder(sheet, "{종목}", String.valueOf(customerInfo.get("j1kftind")));
            
            // 청구서 제목의 연월 변경
            replacePlaceholder(sheet, "{YYYY}", year);
            replacePlaceholder(sheet, "{MM}", month);
            
            // billInfo 데이터 매핑 (28행부터)
            List<Map<String, Object>> billInfoList = (List<Map<String, Object>>) data.get("billInfo");
            if (billInfoList == null || billInfoList.isEmpty()) {
                return false;
            }
            
            int startRow = 27; // 28행은 0-based로 27
            
            // 합계 계산을 위한 변수들
            double totalFixSupplyValue = 0;
            double totalFixVat = 0;
            double totalFixBillAmt = 0;
            double totalSupplyValue = 0;
            double totalVat = 0;
            double totalBillAmt = 0;
            
            for (int i = 0; i < billInfoList.size(); i++) {
                Map<String, Object> billInfo = billInfoList.get(i);
                Row row = sheet.getRow(startRow + i);
                if (row == null) {
                    row = sheet.createRow(startRow + i);
                }
                
                // 데이터 입력
                setCellValue(row, 0, String.valueOf(billInfo.get("orderNo")));
                setCellValue(row, 1, String.valueOf(billInfo.get("vtext")));
                setCellValue(row, 2, String.valueOf(billInfo.get("goodsTx")));
                setCellValue(row, 3, String.valueOf(billInfo.get("instDt")));
                setCellValue(row, 4, String.valueOf(billInfo.get("useDutyMonth")));
                setCellValue(row, 5, ""); // 약정기간
                setCellValue(row, 6, ""); // 사용월
                setCellValue(row, 7, String.valueOf(billInfo.get("recpTp")));
                
                // 금액 데이터 입력 및 합계 계산
                double fixSupplyValue = parseDoubleValue(billInfo.get("fixSupplyValue"));
                double fixVat = parseDoubleValue(billInfo.get("fixVat"));
                double fixBillAmt = parseDoubleValue(billInfo.get("fixBillAmt"));
                double supplyValue = parseDoubleValue(billInfo.get("supplyValue"));
                double vat = parseDoubleValue(billInfo.get("vat"));
                double billAmt = parseDoubleValue(billInfo.get("billAmt"));
                
                setCellValue(row, 8, fixSupplyValue);
                setCellValue(row, 9, fixVat);
                setCellValue(row, 10, fixBillAmt);
                setCellValue(row, 11, supplyValue);
                setCellValue(row, 12, vat);
                setCellValue(row, 13, billAmt);
                
                // 합계 누적
                totalFixSupplyValue += fixSupplyValue;
                totalFixVat += fixVat;
                totalFixBillAmt += fixBillAmt;
                totalSupplyValue += supplyValue;
                totalVat += vat;
                totalBillAmt += billAmt;
            }
            
            // 합계 행 생성
            int totalRow = startRow + billInfoList.size();
            Row row = sheet.createRow(totalRow);
            setCellValue(row, 0, "합계");
            setCellValue(row, 8, totalFixSupplyValue);
            setCellValue(row, 9, totalFixVat);
            setCellValue(row, 10, totalFixBillAmt);
            setCellValue(row, 11, totalSupplyValue);
            setCellValue(row, 12, totalVat);
            setCellValue(row, 13, totalBillAmt);
            
            // 파일 저장
            String fileName = String.format("%s-%s-billExcel.xlsx", year, month);
            FileOutputStream fileOut = new FileOutputStream(DOWNLOAD_PATH + fileName);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            template.close();
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private double parseDoubleValue(Object value) {
        if (value == null) return 0.0;
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private void replacePlaceholder(Sheet sheet, String placeholder, String value) {
        if (value == null) value = "";
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    String cellValue = cell.getStringCellValue();
                    if (cellValue.contains(placeholder)) {
                        cell.setCellValue(cellValue.replace(placeholder, value));
                    }
                }
            }
        }
    }
    
    private void setCellValue(Row row, int column, Object value) {
        Cell cell = row.createCell(column);
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        }
    }
} 