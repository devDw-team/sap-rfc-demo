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
    private CellStyle moneyStyle;
    private CellStyle boldMoneyStyle;
    private CellStyle centerBottomBorderStyle;
    private CellStyle moneyBottomBorderStyle;
    private CellStyle totalAmtBottomBorderStyle;
    
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
            this.moneyStyle = createMoneyStyle(workbook, false);
            this.boldMoneyStyle = createMoneyStyle(workbook, true);
            this.centerBottomBorderStyle = createCenterAlignedBottomBorderStyle(workbook);
            this.moneyBottomBorderStyle = createMoneyBottomBorderStyle(workbook);
            this.totalAmtBottomBorderStyle = createTotalAmtBottomBorderStyle(workbook, false);
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
            
            // 파일명에 사용할 사업자명 추출 및 파일명 안전화
            String rawCustNm = String.valueOf(customerInfo.get("custNm"));
            String safeCustNm = rawCustNm.replaceAll("[\\\\/:*?\"<>|]", "_").trim(); // 파일명에 부적합한 문자 제거
            String fileName = String.format("%s %s년 %s월 대금정구서.xlsx", safeCustNm, year, month);
            FileOutputStream fileOut = new FileOutputStream(DOWNLOAD_PATH + fileName);
            
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
                
                // 컬럼 0: 하단 테두리 없는 center 스타일 적용
                setCellValueWithCustomStyle(row, 0, "", this.centerStyle);
                setCellValueWithCustomStyle(row, 1, String.valueOf(billInfo.get("orderNo")), this.centerBottomBorderStyle);
                setCellValueWithCustomStyle(row, 2, String.valueOf(billInfo.get("vtext")), this.centerBottomBorderStyle);
                setCellValueWithCustomStyle(row, 3, String.valueOf(billInfo.get("goodsTx")), this.centerBottomBorderStyle);
                setCellValueWithCustomStyle(row, 4, String.valueOf(billInfo.get("instDt")), this.centerBottomBorderStyle);
                setCellValueWithCustomStyle(row, 5, String.valueOf(billInfo.get("useDutyMonth")), this.centerBottomBorderStyle);
                setCellValueWithCustomStyle(row, 6, "", this.centerBottomBorderStyle); // 약정기간
                setCellValueWithCustomStyle(row, 7, "", this.centerBottomBorderStyle); // 사용월
                setCellValueWithCustomStyle(row, 8, String.valueOf(billInfo.get("recpTp")), this.centerBottomBorderStyle);
                
                // 금액 데이터 입력 및 합계 계산
                double fixSupplyValue = parseDoubleValue(billInfo.get("fixSupplyValue"));
                double fixVat = parseDoubleValue(billInfo.get("fixVat"));
                double fixBillAmt = parseDoubleValue(billInfo.get("fixBillAmt"));
                double supplyValue = parseDoubleValue(billInfo.get("supplyValue"));
                double vat = parseDoubleValue(billInfo.get("vat"));
                double billAmt = parseDoubleValue(billInfo.get("billAmt"));
                double rowTotAmt = fixSupplyValue+fixVat+fixBillAmt+supplyValue+vat+billAmt;

                setMoneyCellValueWithCustomStyle(row, 9, fixSupplyValue, this.moneyBottomBorderStyle);
                setMoneyCellValueWithCustomStyle(row, 10, fixVat, this.moneyBottomBorderStyle);
                setMoneyCellValueWithCustomStyle(row, 11, fixBillAmt, this.moneyBottomBorderStyle);
                setMoneyCellValueWithCustomStyle(row, 12, supplyValue, this.moneyBottomBorderStyle);
                setMoneyCellValueWithCustomStyle(row, 13, vat, this.moneyBottomBorderStyle);
                setMoneyCellValueWithCustomStyle(row, 14, billAmt, this.moneyBottomBorderStyle);
                setMoneyCellValueWithCustomStyle(row, 15, rowTotAmt, this.moneyBottomBorderStyle);
                
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
            // 합계 행 높이 20px(약 15pt)로 고정
            row.setHeightInPoints(40f);

            // 컬럼 1: "합계" 텍스트, 하단 테두리 + 볼드
            Cell totalCell = row.createCell(1);
            totalCell.setCellValue("합계");
            CellStyle boldCenterBottomBorderStyle = workbook.createCellStyle();
            boldCenterBottomBorderStyle.cloneStyleFrom(this.centerBottomBorderStyle);
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldCenterBottomBorderStyle.setFont(boldFont);
            totalCell.setCellStyle(boldCenterBottomBorderStyle);

            // 컬럼 2~8: 하단 테두리 center 스타일
            for (int col = 2; col <= 8; col++) {
                setCellValueWithCustomStyle(row, col, "", this.centerBottomBorderStyle);
            }
            // 컬럼 9~14: 하단 테두리 money 스타일
            setMoneyCellValueWithCustomStyle(row, 9, totalFixSupplyValue, this.moneyBottomBorderStyle);
            setMoneyCellValueWithCustomStyle(row, 10, totalFixVat, this.moneyBottomBorderStyle);
            setMoneyCellValueWithCustomStyle(row, 11, totalFixBillAmt, this.moneyBottomBorderStyle);
            setMoneyCellValueWithCustomStyle(row, 12, totalSupplyValue, this.moneyBottomBorderStyle);
            setMoneyCellValueWithCustomStyle(row, 13, totalVat, this.moneyBottomBorderStyle);
            setMoneyCellValueWithCustomStyle(row, 14, totalBillAmt, this.moneyBottomBorderStyle);
            // 컬럼 15: 하단 테두리 totalAmt 스타일
            Cell totalAmtCell = row.createCell(15);
            totalAmtCell.setCellValue(totAmt);
            totalAmtCell.setCellStyle(this.totalAmtBottomBorderStyle);
            
            // 파일 저장
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
        
        // 숫자 포맷(세 자리마다 콤마) 적용
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        
        if (bold) {
            // 볼드체 폰트 설정
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            style.setFont(boldFont);
        }
        
        return style;
    }
    
    private CellStyle createMoneyStyle(Workbook workbook, boolean bold) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        if (bold) {
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

    private void setMoneyCellValueWithStyle(Row row, int column, double value, boolean bold) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(bold ? this.boldMoneyStyle : this.moneyStyle);
    }

    // 하단 테두리 포함 center 스타일
    private CellStyle createCenterAlignedBottomBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    // 하단 테두리 포함 money 스타일
    private CellStyle createMoneyBottomBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }

    // 커스텀 스타일로 셀 값 입력 (문자열)
    private void setCellValueWithCustomStyle(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        }
        cell.setCellStyle(style);
    }

    // 커스텀 스타일로 셀 값 입력 (숫자)
    private void setMoneyCellValueWithCustomStyle(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // 하단 테두리 포함 totalAmt 스타일
    private CellStyle createTotalAmtBottomBorderStyle(Workbook workbook, boolean bold) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        if (bold) {
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            style.setFont(boldFont);
        }
        return style;
    }
} 