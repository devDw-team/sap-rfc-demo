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
    
    // 스타일을 저장할 멤버 변수
    private CellStyle centerStyle;
    private CellStyle boldCenterStyle;
    private CellStyle totalAmtStyle;
    private CellStyle boldTotalAmtStyle;
    
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
            // 워크북 생성 후 스타일 초기화
            this.centerStyle = createCenterAlignedStyle(workbook);
            this.boldCenterStyle = createBoldCenterStyle(workbook);
            this.totalAmtStyle = createTotalAmtStyle(workbook, false);
            this.boldTotalAmtStyle = createTotalAmtStyle(workbook, true);
            Sheet sheet = workbook.getSheetAt(0);
            
            // customerInfo 데이터 매핑
            Map<String, Object> customerInfo = (Map<String, Object>) data.get("customerInfo");
            replacePlaceholderWithoutStyle(sheet, "{사업자명}", String.valueOf(customerInfo.get("custNm")));
            replacePlaceholderWithoutStyle(sheet, "{YYYY.MM.DD}", "2025.05.30");
            replacePlaceholderWithoutStyle(sheet, "{사업자번호}", String.valueOf(customerInfo.get("stcd2")));
            replacePlaceholderWithoutStyle(sheet, "{대표자}", String.valueOf(customerInfo.get("j1kfrepre")));
            replacePlaceholderWithoutStyle(sheet, "{업태}", String.valueOf(customerInfo.get("j1kftbus")));
            replacePlaceholderWithoutStyle(sheet, "{종목}", String.valueOf(customerInfo.get("j1kftind")));
            
            // 청구서 제목의 연월 변경
            replacePlaceholderWithoutStyle(sheet, "{YYYY}", year);
            replacePlaceholderWithoutStyle(sheet, "{MM}", month);
            
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
            double totAmt = 0;
            
            for (int i = 0; i < billInfoList.size(); i++) {
                Map<String, Object> billInfo = billInfoList.get(i);
                Row row = sheet.getRow(startRow + i);
                if (row == null) {
                    row = sheet.createRow(startRow + i);
                }
                
                // 데이터 입력
                setCellValueWithStyle(row, 0, "");
                setCellValueWithStyle(row, 1, String.valueOf(billInfo.get("orderNo")));
                setCellValueWithStyle(row, 2, String.valueOf(billInfo.get("vtext")));
                setCellValueWithStyle(row, 3, String.valueOf(billInfo.get("goodsTx")));
                setCellValueWithStyle(row, 4, String.valueOf(billInfo.get("instDt")));
                setCellValueWithStyle(row, 5, String.valueOf(billInfo.get("useDutyMonth")));
                setCellValueWithStyle(row, 6, ""); // 약정기간
                setCellValueWithStyle(row, 7, ""); // 사용월
                setCellValueWithStyle(row, 8, String.valueOf(billInfo.get("recpTp")));
                
                // 금액 데이터 입력 및 합계 계산
                double fixSupplyValue = parseDoubleValue(billInfo.get("fixSupplyValue"));
                double fixVat = parseDoubleValue(billInfo.get("fixVat"));
                double fixBillAmt = parseDoubleValue(billInfo.get("fixBillAmt"));
                double supplyValue = parseDoubleValue(billInfo.get("supplyValue"));
                double vat = parseDoubleValue(billInfo.get("vat"));
                double billAmt = parseDoubleValue(billInfo.get("billAmt"));
                double rowTotAmt = fixSupplyValue+fixVat+fixBillAmt+supplyValue+vat+billAmt;

                setCellValueWithStyle(row, 9, fixSupplyValue);
                setCellValueWithStyle(row, 10, fixVat);
                setCellValueWithStyle(row, 11, fixBillAmt);
                setCellValueWithStyle(row, 12, supplyValue);
                setCellValueWithStyle(row, 13, vat);
                setCellValueWithStyle(row, 14, billAmt);
                
                // 행별 총액에 배경색 적용
                Cell rowTotalCell = row.createCell(15);
                rowTotalCell.setCellValue(rowTotAmt);
                rowTotalCell.setCellStyle(this.totalAmtStyle);
                
                // 합계 누적
                totalFixSupplyValue += fixSupplyValue;
                totalFixVat += fixVat;
                totalFixBillAmt += fixBillAmt;
                totalSupplyValue += supplyValue;
                totalVat += vat;
                totalBillAmt += billAmt;
                totAmt += rowTotAmt;
            }
            
            // 합계 행 생성
            int totalRow = startRow + billInfoList.size();
            Row row = sheet.createRow(totalRow);

            // 합계 텍스트에 볼드체 적용
            Cell totalCell = row.createCell(1);
            totalCell.setCellValue("합계");
            totalCell.setCellStyle(this.boldCenterStyle);

            setCellValueWithStyle(row, 9, totalFixSupplyValue);
            setCellValueWithStyle(row, 10, totalFixVat);
            setCellValueWithStyle(row, 11, totalFixBillAmt);
            setCellValueWithStyle(row, 12, totalSupplyValue);
            setCellValueWithStyle(row, 13, totalVat);
            setCellValueWithStyle(row, 14, totalBillAmt);
            
            // 합계 행의 총액에 볼드체와 배경색 적용
            Cell totalAmtCell = row.createCell(15);
            totalAmtCell.setCellValue(totAmt);
            totalAmtCell.setCellStyle(this.boldTotalAmtStyle);
            
            // 파일 저장
            String fileName = String.format("%s-%s-bill_info_Excel.xlsx", year, month);
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
    
    private void replacePlaceholderWithoutStyle(Sheet sheet, String placeholder, String value) {
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

    private CellStyle createCenterAlignedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // 수평 가운데 정렬
        style.setAlignment(HorizontalAlignment.CENTER);
        // 수직 가운데 정렬
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // 수평 가운데 정렬
        style.setAlignment(HorizontalAlignment.CENTER);
        // 수직 가운데 정렬
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // 볼드체 폰트 생성
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        style.setFont(boldFont);
        
        return style;
    }

    private CellStyle createTotalAmtStyle(Workbook workbook, boolean bold) {
        CellStyle style = workbook.createCellStyle();
        // 수평 가운데 정렬
        style.setAlignment(HorizontalAlignment.CENTER);
        // 수직 가운데 정렬
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // 배경색 설정 (#F2F2F2)
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        if (bold) {
            // 볼드체 폰트 설정
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            style.setFont(boldFont);
        }
        
        return style;
    }
    
    private void setCellValueWithStyle(Row row, int column, Object value) {
        Cell cell = row.createCell(column);
        
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        }
        
        // 미리 생성된 스타일 적용
        cell.setCellStyle(this.centerStyle);
    }
} 