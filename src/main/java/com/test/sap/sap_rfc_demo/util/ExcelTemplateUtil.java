package com.test.sap.sap_rfc_demo.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ExcelTemplateUtil {
    private static final String TEMPLATE_PATH = "src/main/resources/static/excel/billinfo_template_mark.xlsx";
    private static final String DOWNLOAD_PATH = "src/main/resources/static/excel/download/";

    // 스타일 멤버
    private CellStyle centerStyle;
    private CellStyle moneyStyle;
    private CellStyle boldCenterStyle;
    private CellStyle moneyBottomBorderStyle;
    private CellStyle centerBottomBorderStyle;
    private CellStyle boldCenterBottomBorderStyle;
    private CellStyle boldMoneyBottomBorderStyle;

    /**
     * 템플릿 기반 엑셀 생성 (customer, bills, bill_summary, bill_type_summary, remarks 구조)
     * @param data Map (customer, bills, bill_summary, bill_type_summary, remarks)
     * @return 생성된 파일 경로 (성공 시), 실패 시 null
     */
    @SuppressWarnings("unchecked")
    public String generateExcelFromTemplate(Map<String, Object> data) {
        try {
            if (data == null || !data.containsKey("customer") || !data.containsKey("bills")) {
                return null;
            }
            // 날짜 정보
            LocalDate now = LocalDate.now();
            String year = String.valueOf(now.getYear());
            String month = String.format("%02d", now.getMonthValue());

            // 템플릿 파일 로드
            FileInputStream template = new FileInputStream(TEMPLATE_PATH);
            Workbook workbook = new XSSFWorkbook(template);
            this.centerStyle = createCenterAlignedStyle(workbook);
            this.moneyStyle = createMoneyStyle(workbook, false);
            this.boldCenterStyle = createBoldCenterStyle(workbook);
            this.moneyBottomBorderStyle = createMoneyBottomBorderStyle(workbook);
            this.centerBottomBorderStyle = createCenterAlignedBottomBorderStyle(workbook);
            this.boldCenterBottomBorderStyle = createBoldCenterBottomBorderStyle(workbook);
            this.boldMoneyBottomBorderStyle = createBoldMoneyBottomBorderStyle(workbook);
            Sheet sheet = workbook.getSheetAt(0);

            // 1. customer, bill_summary, remarks 등 단일 row 플레이스홀더 치환
            Map<String, Object> customer = (Map<String, Object>) data.get("customer");
            Map<String, Object> billSummary = (Map<String, Object>) data.getOrDefault("bill_summary", null);
            Map<String, Object> remarks = (Map<String, Object>) data.getOrDefault("remarks", null);
            // customer
            replacePlaceholder(sheet, "{CUST_NM}", String.valueOf(customer.get("CUST_NM")));
            replacePlaceholder(sheet, "{STCD2}", String.valueOf(customer.get("STCD2")));
            replacePlaceholder(sheet, "{J_1KFREPRE}", String.valueOf(customer.get("J_1KFREPRE")));
            replacePlaceholder(sheet, "{J_1KFTBUS}", String.valueOf(customer.get("J_1KFTBUS")));
            replacePlaceholder(sheet, "{J_1KFTIND}", String.valueOf(customer.get("J_1KFTIND")));
            // bill_summary
            if (billSummary != null) {
                // C_SEL_KUN_CNT 값 확인
                Object selKunCntObj = billSummary.get("C_SEL_KUN_CNT");
                int selKunCnt = 0;
                if (selKunCntObj != null) {
                    try {
                        selKunCnt = Integer.parseInt(selKunCntObj.toString());
                    } catch (NumberFormatException e) {
                        selKunCnt = 0;
                    }
                }
                
                String cRecpYm;
                String cDueDate;
                
                // C_SEL_KUN_CNT < 1 이면 오늘날짜 기준으로 값 설정
                if (selKunCnt < 1) {
                    LocalDate today = LocalDate.now();
                    // 청구년월(C_RECP_YM): 오늘날짜 기준 년월(YYYYMM)
                    cRecpYm = String.format("%d%02d", today.getYear(), today.getMonthValue());
                    
                    // 납부기한(C_DUE_DATE): 오늘날짜 기준 해당 월의 말일 날짜(YYYY-MM-DD)
                    LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
                    cDueDate = lastDayOfMonth.toString(); // YYYY-MM-DD 형식
                } else {
                    // 기존 프로세스: billSummary에서 값 그대로 추출
                    cRecpYm = String.valueOf(billSummary.get("C_RECP_YM"));
                    cDueDate = String.valueOf(billSummary.get("C_DUE_DATE"));
                }
                
                // {C_RECP_YM} 치환 추가
                System.out.println("cRecpYm : " + cRecpYm);
                if (cRecpYm != null && cRecpYm.length() == 6) {
                    String yearPart = cRecpYm.substring(0, 4);
                    String monthPart = cRecpYm.substring(4, 6);
                    String formatted = yearPart + "년 " + monthPart + "월";
                    System.out.println("formatted : " + formatted);
                    replacePlaceholder(sheet, "{C_RECP_YM}", formatted);
                } else {
                    replacePlaceholder(sheet, "{C_RECP_YM}", "");
                    System.out.println("cRecpYm is null or length is not 6");
                }
                
                // {C_DUE_DATE} 변환 및 치환
                if (cDueDate != null && cDueDate.length() == 10) {
                    String formattedDueDate = cDueDate.replace("-", ".");
                    replacePlaceholder(sheet, "{C_DUE_DATE}", formattedDueDate);
                } else {
                    replacePlaceholder(sheet, "{C_DUE_DATE}", cDueDate != null ? cDueDate : "");
                }
                
                replacePlaceholder(sheet, "{TOTAL_AMOUNT}", String.valueOf(billSummary.get("TOTAL_AMOUNT")));
            }
            // remarks
            if (remarks != null) {
                replacePlaceholder(sheet, "{J_JBIGO}", String.valueOf(remarks.get("J_JBIGO")));
            }
            // 날짜
            replacePlaceholder(sheet, "{YYYY}", year);
            replacePlaceholder(sheet, "{MM}", month);

            // 파일명 안전화
            // String rawCustNm = String.valueOf(customer.get("CUST_NM"));
            // String cRecpYm = billSummary != null ? String.valueOf(billSummary.get("C_RECP_YM")) : month;
            // String safeCustNm = rawCustNm.replaceAll("[\\/:*?\"<>|]", "_").trim();
            // String fileName = String.format("%s %s 대금청구서.xlsx", safeCustNm, cRecpYm);
            String fileName = "코웨이 청구 상세내역.xlsx";
            String outPath = DOWNLOAD_PATH + fileName;
            FileOutputStream fileOut = new FileOutputStream(outPath);

            // 2. bills 반복 row (28행부터)
            List<Map<String, Object>> bills = (List<Map<String, Object>>) data.get("bills");
            int startRow = 27; // 0-based
            double totalSupplyValue = 0, totalVat = 0, totalBillAmt = 0, totalFixSupplyValue = 0, totalFixVat = 0, totalFixBillAmt = 0;
            for (int i = 0; i < bills.size(); i++) {
                Map<String, Object> bill = bills.get(i);
                Row row = sheet.getRow(startRow + i);
                if (row == null) row = sheet.createRow(startRow + i);
                row.setHeightInPoints(33.75f); // 45px
                setCellValueWithStyle(row, 1, String.valueOf(bill.getOrDefault("ORDER_NO", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 2, String.valueOf(bill.getOrDefault("VTEXT", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 3, String.valueOf(bill.getOrDefault("GOODS_TX", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 4, formatDateString(String.valueOf(bill.getOrDefault("INST_DT", ""))), centerBottomBorderStyle);
                setCellValueWithStyle(row, 5, formatDateString(String.valueOf(bill.getOrDefault("USE_DUTY_MONTH", ""))), centerBottomBorderStyle);
                setCellValueWithStyle(row, 6, formatDateString(String.valueOf(bill.getOrDefault("OWNER_DATE", ""))), centerBottomBorderStyle);
                setCellValueWithStyle(row, 7, formatYearMonthString(String.valueOf(bill.getOrDefault("USE_MONTH", ""))), centerBottomBorderStyle);
                setCellValueWithStyle(row, 8, formatYearMonthString(String.valueOf(bill.getOrDefault("RECP_YM", ""))), centerBottomBorderStyle);
                // 금액 셀: 하단 테두리 스타일 적용
                double fixSupply = parseDoubleValue(bill.get("FIX_SUPPLY_VALUE"));
                double fixVat = parseDoubleValue(bill.get("FIX_VAT"));
                double fixBillAmt = parseDoubleValue(bill.get("FIX_BILL_AMT"));
                double supply = parseDoubleValue(bill.get("SUPPLY_VALUE"));
                double vat = parseDoubleValue(bill.get("VAT"));
                double billAmt = parseDoubleValue(bill.get("BILL_AMT"));
                setMoneyCellValueWithStyle(row, 9, fixSupply, moneyBottomBorderStyle);
                setMoneyCellValueWithStyle(row, 10, fixVat, moneyBottomBorderStyle);
                setMoneyCellValueWithStyle(row, 11, fixBillAmt, moneyBottomBorderStyle);
                setMoneyCellValueWithStyle(row, 12, supply, moneyBottomBorderStyle);
                setMoneyCellValueWithStyle(row, 13, vat, moneyBottomBorderStyle);
                setMoneyCellValueWithStyle(row, 14, billAmt, moneyBottomBorderStyle);
                
                // 15, 16, 17, 18, 19 컬럼 빈값 설정 (추후 숫자로 치환 예정)
                setCellValueWithStyle(row, 15, "", centerBottomBorderStyle);
                setCellValueWithStyle(row, 16, "", centerBottomBorderStyle);
                setCellValueWithStyle(row, 17, "", centerBottomBorderStyle);
                setCellValueWithStyle(row, 18, "", centerBottomBorderStyle);
                setCellValueWithStyle(row, 19, "", centerBottomBorderStyle);
                
                double rowTotal = supply + vat;
                setMoneyCellValueWithStyle(row, 20, rowTotal, moneyBottomBorderStyle);
                
                // 추가 컬럼들
                setCellValueWithStyle(row, 21, String.valueOf(bill.getOrDefault("PAY_COM_TX", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 22, String.valueOf(bill.getOrDefault("PAY_NO", "")), centerBottomBorderStyle);
                setMoneyCellValueWithStyle(row, 23, parseDoubleValue(bill.get("PRE_AMT")), moneyBottomBorderStyle);
                setMoneyCellValueWithStyle(row, 24, parseDoubleValue(bill.get("REMAIN_AMT")), moneyBottomBorderStyle);
                setCellValueWithStyle(row, 25, String.valueOf(bill.getOrDefault("PRE_MONTH", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 26, String.valueOf(bill.getOrDefault("INST_JUSO", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 27, String.valueOf(bill.getOrDefault("GOODS_SN", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 28, String.valueOf(bill.getOrDefault("DEPT_CD_TX", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 29, String.valueOf(bill.getOrDefault("DEPT_TELNR", "")), centerBottomBorderStyle);
                
                // 30, 31, 32, 33, 34 컬럼 빈값 설정
                setCellValueWithStyle(row, 30, "", centerBottomBorderStyle);
                setCellValueWithStyle(row, 31, "", centerBottomBorderStyle);
                setCellValueWithStyle(row, 32, "", centerBottomBorderStyle);
                setCellValueWithStyle(row, 33, "", centerBottomBorderStyle);
                setCellValueWithStyle(row, 34, "", centerBottomBorderStyle);
                
                setCellValueWithStyle(row, 35, String.valueOf(bill.getOrDefault("ZBIGO", "")), centerBottomBorderStyle);
                
                totalFixSupplyValue += fixSupply;
                totalFixVat += fixVat;
                totalFixBillAmt += fixBillAmt;
                totalSupplyValue += supply;
                totalVat += vat;
                totalBillAmt += billAmt;
            }
            // 합계 행
            Row totalRow = sheet.getRow(startRow + bills.size());
            if (totalRow == null) totalRow = sheet.createRow(startRow + bills.size());
            totalRow.setHeightInPoints(37.5f); // 50px
            setCellValueWithStyle(totalRow, 1, "합계", boldCenterBottomBorderStyle);
            for (int col = 2; col <= 8; col++) setCellValueWithStyle(totalRow, col, "", boldCenterBottomBorderStyle);
            setMoneyCellValueWithStyle(totalRow, 9, totalFixSupplyValue, boldMoneyBottomBorderStyle); // FIX_SUPPLY_VALUE 합계 필요시 수정
            setMoneyCellValueWithStyle(totalRow, 10, totalFixVat, boldMoneyBottomBorderStyle); // FIX_VAT 합계 필요시 수정
            setMoneyCellValueWithStyle(totalRow, 11, totalFixBillAmt, boldMoneyBottomBorderStyle); // FIX_BILL_AMT 합계 필요시 수정
            setMoneyCellValueWithStyle(totalRow, 12, totalSupplyValue, boldMoneyBottomBorderStyle);
            setMoneyCellValueWithStyle(totalRow, 13, totalVat, boldMoneyBottomBorderStyle);
            setMoneyCellValueWithStyle(totalRow, 14, totalBillAmt, boldMoneyBottomBorderStyle);
            
            // 15, 16, 17, 18, 19 컬럼 빈값 설정 (합계 행)
            setCellValueWithStyle(totalRow, 15, "", boldCenterBottomBorderStyle);
            setCellValueWithStyle(totalRow, 16, "", boldCenterBottomBorderStyle);
            setCellValueWithStyle(totalRow, 17, "", boldCenterBottomBorderStyle);
            setCellValueWithStyle(totalRow, 18, "", boldCenterBottomBorderStyle);
            setCellValueWithStyle(totalRow, 19, "", boldCenterBottomBorderStyle);
            
            setMoneyCellValueWithStyle(totalRow, 20, totalSupplyValue + totalVat , boldMoneyBottomBorderStyle);
            
            // 추가된 컬럼들에 대한 합계 행 처리
            for (int col = 21; col <= 29; col++) {
                if (col == 23 || col == 24) {
                    // PRE_AMT, REMAIN_AMT는 금액 필드이므로 0으로 설정
                    setMoneyCellValueWithStyle(totalRow, col, 0.0, boldMoneyBottomBorderStyle);
                } else {
                    // 나머지는 빈 값
                    setCellValueWithStyle(totalRow, col, "", boldCenterBottomBorderStyle);
                }
            }
            
            // 30, 31, 32, 33, 34 컬럼 빈값 설정 (합계 행)
            setCellValueWithStyle(totalRow, 30, "", boldCenterBottomBorderStyle);
            setCellValueWithStyle(totalRow, 31, "", boldCenterBottomBorderStyle);
            setCellValueWithStyle(totalRow, 32, "", boldCenterBottomBorderStyle);
            setCellValueWithStyle(totalRow, 33, "", boldCenterBottomBorderStyle);
            setCellValueWithStyle(totalRow, 34, "", boldCenterBottomBorderStyle);
            
            setCellValueWithStyle(totalRow, 35, "", boldCenterBottomBorderStyle); // ZBIGO

            // 3. bill_type_summary 반복 row (예: 50행부터, 실제 템플릿 구조에 맞게 조정)
            /*
            if (data.containsKey("bill_type_summary")) {
                List<Map<String, Object>> billTypeSummary = (List<Map<String, Object>>) data.get("bill_type_summary");
                int billTypeStartRow = 49; // 0-based, 예시
                for (int i = 0; i < billTypeSummary.size(); i++) {
                    Map<String, Object> type = billTypeSummary.get(i);
                    Row row = sheet.getRow(billTypeStartRow + i);
                    if (row == null) row = sheet.createRow(billTypeStartRow + i);
                    setCellValueWithStyle(row, 0, String.valueOf(type.getOrDefault("C_RECP_TP", "")), centerStyle);
                    setCellValueWithStyle(row, 1, String.valueOf(type.getOrDefault("C_RECP_TP_TX", "")), centerStyle);
                    setCellValueWithStyle(row, 2, String.valueOf(type.getOrDefault("SUMMARY_CNT", "")), centerStyle);
                    setMoneyCellValueWithStyle(row, 3, parseDoubleValue(type.get("SUMMARY_AMOUNT")), moneyStyle);
                }
            }
            */
            // 파일 저장
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            template.close();
            return outPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private double parseDoubleValue(Object value) {
        if (value == null) return 0.0;
        try {
            if (value instanceof Number) return ((Number) value).doubleValue();
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) { return 0.0; }
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

    private CellStyle createCenterAlignedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    private CellStyle createBoldCenterStyle(Workbook workbook) {
        CellStyle style = createCenterAlignedStyle(workbook);
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        style.setFont(boldFont);
        return style;
    }
    private CellStyle createMoneyStyle(Workbook workbook, boolean bold) {
        CellStyle style = createCenterAlignedStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        if (bold) {
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            style.setFont(boldFont);
        }
        return style;
    }
    private CellStyle createCenterAlignedBottomBorderStyle(Workbook workbook) {
        CellStyle style = createCenterAlignedStyle(workbook);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
    private CellStyle createMoneyBottomBorderStyle(Workbook workbook) {
        CellStyle style = createMoneyStyle(workbook, false);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
    private void setCellValueWithStyle(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    private void setMoneyCellValueWithStyle(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // 날짜 포맷 변환 함수 추가
    private String formatDateString(String date) {
        if (date != null && date.length() == 10 && date.contains("-")) {
            return date.replace("-", ".");
        }
        return date;
    }

    // "YYYYMM" -> "YYYY.MM" 변환 함수 추가
    private String formatYearMonthString(String yyyymm) {
        if (yyyymm != null && yyyymm.length() == 6) {
            return yyyymm.substring(0, 4) + "." + yyyymm.substring(4, 6);
        }
        return yyyymm;
    }

    // Bold + 하단 테두리 스타일 생성 함수 추가
    private CellStyle createBoldCenterBottomBorderStyle(Workbook workbook) {
        CellStyle style = createCenterAlignedBottomBorderStyle(workbook);
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        style.setFont(boldFont);
        return style;
    }
    private CellStyle createBoldMoneyBottomBorderStyle(Workbook workbook) {
        CellStyle style = createMoneyBottomBorderStyle(workbook);
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        style.setFont(boldFont);
        return style;
    }
} 