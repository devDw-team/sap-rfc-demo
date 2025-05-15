package com.test.sap.sap_rfc_demo.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ExcelTemplateUtil {
    private static final String TEMPLATE_PATH = "src/main/resources/static/excel/billinfo_template.xlsx";
    private static final String DOWNLOAD_PATH = "src/main/resources/static/excel/download/";

    // 스타일 멤버
    private CellStyle centerStyle;
    private CellStyle moneyStyle;
    private CellStyle boldCenterStyle;
    private CellStyle moneyBottomBorderStyle;
    private CellStyle centerBottomBorderStyle;

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
                replacePlaceholder(sheet, "{C_RECP_YM}", String.valueOf(billSummary.get("C_RECP_YM")));
                // {C_DUE_DATE} 변환 및 치환
                String cDueDate = String.valueOf(billSummary.get("C_DUE_DATE"));
                if (cDueDate != null && cDueDate.length() == 10) {
                    String formattedDueDate = cDueDate.replace("-", ".");
                    replacePlaceholder(sheet, "{C_DUE_DATE}", formattedDueDate);
                } else {
                    replacePlaceholder(sheet, "{C_DUE_DATE}", cDueDate != null ? cDueDate : "");
                }
                replacePlaceholder(sheet, "{TOTAL_AMOUNT}", String.valueOf(billSummary.get("TOTAL_AMOUNT")));
                // {C_RECP_YN} 치환 추가
                String cRecpYm = String.valueOf(billSummary.get("C_RECP_YM"));
                if (cRecpYm != null && cRecpYm.length() == 6) {
                    String yearPart = cRecpYm.substring(0, 4);
                    String monthPart = cRecpYm.substring(4, 6);
                    String formatted = yearPart + "년 " + monthPart + "월";
                    replacePlaceholder(sheet, "{C_RECP_YN}", formatted);
                } else {
                    replacePlaceholder(sheet, "{C_RECP_YN}", "");
                }
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
            double totalSupplyValue = 0, totalVat = 0, totalBillAmt = 0;
            for (int i = 0; i < bills.size(); i++) {
                Map<String, Object> bill = bills.get(i);
                Row row = sheet.getRow(startRow + i);
                if (row == null) row = sheet.createRow(startRow + i);
                setCellValueWithStyle(row, 1, String.valueOf(bill.getOrDefault("ORDER_NO", "")), centerStyle);
                setCellValueWithStyle(row, 2, String.valueOf(bill.getOrDefault("GOODS_CD", "")), centerStyle);
                setCellValueWithStyle(row, 3, String.valueOf(bill.getOrDefault("VTEXT", "")), centerStyle);
                setCellValueWithStyle(row, 4, String.valueOf(bill.getOrDefault("INST_DT", "")), centerStyle);
                setCellValueWithStyle(row, 5, String.valueOf(bill.getOrDefault("USE_DUTY_MONTH", "")), centerStyle);
                setCellValueWithStyle(row, 6, String.valueOf(bill.getOrDefault("OWNER_DATE", "")), centerStyle);
                setCellValueWithStyle(row, 7, String.valueOf(bill.getOrDefault("USE_MONTH", "")), centerStyle);
                setCellValueWithStyle(row, 8, String.valueOf(bill.getOrDefault("RECP_YM", "")), centerStyle);
                double fixSupply = parseDoubleValue(bill.get("FIX_SUPPLY_VALUE"));
                double fixVat = parseDoubleValue(bill.get("FIX_VAT"));
                double fixBillAmt = parseDoubleValue(bill.get("FIX_BILL_AMT"));
                double supply = parseDoubleValue(bill.get("SUPPLY_VALUE"));
                double vat = parseDoubleValue(bill.get("VAT"));
                double billAmt = parseDoubleValue(bill.get("BILL_AMT"));
                setMoneyCellValueWithStyle(row, 9, fixSupply, moneyStyle);
                setMoneyCellValueWithStyle(row, 10, fixVat, moneyStyle);
                setMoneyCellValueWithStyle(row, 11, fixBillAmt, moneyStyle);
                setMoneyCellValueWithStyle(row, 12, supply, moneyStyle);
                setMoneyCellValueWithStyle(row, 13, vat, moneyStyle);
                setMoneyCellValueWithStyle(row, 14, billAmt, moneyStyle);
                double rowTotal = fixSupply + fixVat + fixBillAmt + supply + vat + billAmt;
                setMoneyCellValueWithStyle(row, 15, rowTotal, moneyStyle);
                totalSupplyValue += supply;
                totalVat += vat;
                totalBillAmt += billAmt;
            }
            // 합계 행
            Row totalRow = sheet.getRow(startRow + bills.size());
            if (totalRow == null) totalRow = sheet.createRow(startRow + bills.size());
            setCellValueWithStyle(totalRow, 1, "합계", boldCenterStyle);
            for (int col = 1; col <= 8; col++) setCellValueWithStyle(totalRow, col, "", centerBottomBorderStyle);
            setMoneyCellValueWithStyle(totalRow, 9, 0, moneyBottomBorderStyle); // FIX_SUPPLY_VALUE 합계 필요시 수정
            setMoneyCellValueWithStyle(totalRow, 10, 0, moneyBottomBorderStyle); // FIX_VAT 합계 필요시 수정
            setMoneyCellValueWithStyle(totalRow, 11, 0, moneyBottomBorderStyle); // FIX_BILL_AMT 합계 필요시 수정
            setMoneyCellValueWithStyle(totalRow, 12, totalSupplyValue, moneyBottomBorderStyle);
            setMoneyCellValueWithStyle(totalRow, 13, totalVat, moneyBottomBorderStyle);
            setMoneyCellValueWithStyle(totalRow, 14, totalBillAmt, moneyBottomBorderStyle);
            setMoneyCellValueWithStyle(totalRow, 15, totalSupplyValue + totalVat + totalBillAmt, moneyBottomBorderStyle);

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
} 