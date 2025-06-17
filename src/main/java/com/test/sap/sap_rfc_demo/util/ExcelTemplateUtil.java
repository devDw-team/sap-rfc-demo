package com.test.sap.sap_rfc_demo.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import java.io.*;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class ExcelTemplateUtil {
    private static final String TEMPLATE_PATH = "src/main/resources/static/excel/billinfo_template_mark.xlsx";
    private static final String DOWNLOAD_PATH = "src/main/resources/static/excel/download/";

    // 한국 공휴일 (예시 - 실제로는 매년 변경되므로 동적으로 관리 필요)
    private static final Set<LocalDate> HOLIDAYS_2025 = new HashSet<>();
    private static final Set<LocalDate> HOLIDAYS_2026 = new HashSet<>();
    
    static {
        // 2025년 주요 공휴일 예시
        HOLIDAYS_2025.add(LocalDate.of(2025, 8, 15));  // 광복절
        HOLIDAYS_2025.add(LocalDate.of(2025, 10, 3));  // 개천절
        HOLIDAYS_2025.add(LocalDate.of(2025, 10, 6));  // 추석연휴
        HOLIDAYS_2025.add(LocalDate.of(2025, 10, 7));  // 추석연휴
        HOLIDAYS_2025.add(LocalDate.of(2025, 10, 8));  // 추석연휴
        HOLIDAYS_2025.add(LocalDate.of(2025, 10, 9));  // 한글날
        HOLIDAYS_2025.add(LocalDate.of(2025, 12, 25)); // 크리스마스
        
        HOLIDAYS_2026.add(LocalDate.of(2026, 1, 1)); // 새해 첫날
        HOLIDAYS_2026.add(LocalDate.of(2026, 2, 16)); // 설날 연휴
        HOLIDAYS_2026.add(LocalDate.of(2026, 2, 17)); // 설날 
        HOLIDAYS_2026.add(LocalDate.of(2026, 2, 18)); // 설날 연휴
        HOLIDAYS_2026.add(LocalDate.of(2026, 3, 1)); // 삼일절
        HOLIDAYS_2026.add(LocalDate.of(2026, 3, 2)); // 대체공휴일(삼일절)
        HOLIDAYS_2026.add(LocalDate.of(2026, 5, 5)); // 어린이날
        HOLIDAYS_2026.add(LocalDate.of(2026, 5, 24)); // 부처님 오신날
        HOLIDAYS_2026.add(LocalDate.of(2026, 5, 25)); // 대체공휴일(부처님 오신날)
        HOLIDAYS_2026.add(LocalDate.of(2026, 6, 6)); // 현충일
        HOLIDAYS_2026.add(LocalDate.of(2026, 8, 15));  // 광복절
        HOLIDAYS_2026.add(LocalDate.of(2026, 8, 17));  // 대체공휴일(광복절)
        HOLIDAYS_2026.add(LocalDate.of(2026, 9, 24));  // 추석 연휴
        HOLIDAYS_2026.add(LocalDate.of(2026, 9, 25));  // 추석 
        HOLIDAYS_2026.add(LocalDate.of(2026, 9, 26));  // 추석 연휴
        HOLIDAYS_2026.add(LocalDate.of(2026, 10, 3));  // 개천절
        HOLIDAYS_2026.add(LocalDate.of(2026, 10, 5));  // 대체공휴일(개천절)
        HOLIDAYS_2026.add(LocalDate.of(2026, 10, 9));  // 한글날
        HOLIDAYS_2026.add(LocalDate.of(2026, 12, 25));  // 크리스마스
    }

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
        return generateExcelFromTemplate(data, TEMPLATE_PATH);
    }

    /**
     * 템플릿 기반 엑셀 생성 (사업자별 템플릿 파일 경로 지정 가능)
     * @param data Map (customer, bills, bill_summary, bill_type_summary, remarks)
     * @param templatePath 사용할 템플릿 파일 경로
     * @return 생성된 파일 경로 (성공 시), 실패 시 null
     */
    @SuppressWarnings("unchecked")
    public String generateExcelFromTemplate(Map<String, Object> data, String templatePath) {
        // 원본 보안 설정 저장
        double originalMinInflateRatio = ZipSecureFile.getMinInflateRatio();
        
        try {
            System.out.println("[ExcelTemplateUtil] Excel 파일 생성 시작");
            System.out.println("[ExcelTemplateUtil] 템플릿 파일 경로: " + templatePath);
            
            if (data == null || !data.containsKey("customer") || !data.containsKey("bills")) {
                System.err.println("[ExcelTemplateUtil] 필수 데이터가 없습니다.");
                System.err.println("[ExcelTemplateUtil] data null: " + (data == null));
                if (data != null) {
                    System.err.println("[ExcelTemplateUtil] customer 존재: " + data.containsKey("customer"));
                    System.err.println("[ExcelTemplateUtil] bills 존재: " + data.containsKey("bills"));
                }
                return null;
            }
            
            // 템플릿 파일 존재 여부 확인
            File templateFile = new File(templatePath);
            if (!templateFile.exists()) {
                System.err.println("[ExcelTemplateUtil] 템플릿 파일이 존재하지 않습니다: " + templatePath);
                return null;
            }
            System.out.println("[ExcelTemplateUtil] 템플릿 파일 확인 완료");
            
            // Excel 파일 처리를 위해 임시로 보안 임계값 조정
            // 기존 0.005에서 0.004로 더 낮춤 (극도로 압축된 템플릿 파일 지원)
            ZipSecureFile.setMinInflateRatio(0.004);
            System.out.println("[ExcelTemplateUtil] POI 보안 설정 조정 완료 (임계값: 0.004)");
            
            // 날짜 정보
            LocalDate now = LocalDate.now();
            String year = String.valueOf(now.getYear());
            String month = String.format("%02d", now.getMonthValue());
            System.out.println("[ExcelTemplateUtil] 날짜 정보 - 년: " + year + ", 월: " + month);

            // 템플릿 파일 로드 (매개변수로 받은 경로 사용)
            System.out.println("[ExcelTemplateUtil] 템플릿 파일 로드 시작");
            FileInputStream template = new FileInputStream(templatePath);
            Workbook workbook = new XSSFWorkbook(template);
            System.out.println("[ExcelTemplateUtil] Workbook 생성 완료");
            
            this.centerStyle = createCenterAlignedStyle(workbook);
            this.moneyStyle = createMoneyStyle(workbook, false);
            this.boldCenterStyle = createBoldCenterStyle(workbook);
            this.moneyBottomBorderStyle = createMoneyBottomBorderStyle(workbook);
            this.centerBottomBorderStyle = createCenterAlignedBottomBorderStyle(workbook);
            this.boldCenterBottomBorderStyle = createBoldCenterBottomBorderStyle(workbook);
            this.boldMoneyBottomBorderStyle = createBoldMoneyBottomBorderStyle(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("[ExcelTemplateUtil] 스타일 생성 및 시트 로드 완료");

            // 1. customer, bill_summary, remarks 등 단일 row 플레이스홀더 치환
            Map<String, Object> customer = (Map<String, Object>) data.get("customer");
            Map<String, Object> billSummary = (Map<String, Object>) data.getOrDefault("bill_summary", null);
            Map<String, Object> remarks = (Map<String, Object>) data.getOrDefault("remarks", null);
            
            System.out.println("[ExcelTemplateUtil] 고객 정보 처리 시작");
            if (customer != null) {
                System.out.println("[ExcelTemplateUtil] 고객명: " + customer.get("CUST_NM"));
                System.out.println("[ExcelTemplateUtil] 사업자번호: " + customer.get("STCD2"));
            } else {
                System.err.println("[ExcelTemplateUtil] customer 정보가 null입니다.");
            }
            
            // customer 정보 치환
            if (customer != null) {
                replacePlaceholder(sheet, "{CUST_NM}", String.valueOf(customer.get("CUST_NM")));
                replacePlaceholder(sheet, "{STCD2}", String.valueOf(customer.get("STCD2")));
                replacePlaceholder(sheet, "{J_1KFREPRE}", String.valueOf(customer.get("J_1KFREPRE")));
                replacePlaceholder(sheet, "{J_1KFTBUS}", String.valueOf(customer.get("J_1KFTBUS")));
                replacePlaceholder(sheet, "{J_1KFTIND}", String.valueOf(customer.get("J_1KFTIND")));
                replacePlaceholder(sheet, "{INVOICE_NOTE}", String.valueOf(customer.get("INVOICE_NOTE")));
                System.out.println("[ExcelTemplateUtil] 고객 정보 치환 완료");
            }

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
                //if (selKunCnt < 1) {
                //    LocalDate today = LocalDate.now();
                //    // 청구년월(C_RECP_YM): 오늘날짜 기준 년월(YYYYMM)
                //    cRecpYm = String.format("%d%02d", today.getYear(), today.getMonthValue());
                    
                    // 납부기한(C_DUE_DATE): 오늘날짜 기준 해당 월의 영업일 기준 말일 날짜(YYYY-MM-DD)
                //    LocalDate lastBusinessDay = getLastBusinessDay(today.getYear(), today.getMonthValue());
                //    cDueDate = lastBusinessDay.toString(); // YYYY-MM-DD 형식
                //} else {
                    // 기존 프로세스: billSummary에서 값 그대로 추출
                    cRecpYm = String.valueOf(billSummary.get("C_RECP_YM"));
                    cDueDate = String.valueOf(billSummary.get("C_DUE_DATE"));
                //}
                
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
                String formattedDueDate;
                if (cDueDate != null && cDueDate.matches("\\d{8}")) {
                    // YYYYMMDD -> YYYY.MM.DD
                    formattedDueDate = cDueDate.substring(0, 4) + "." + cDueDate.substring(4, 6) + "." + cDueDate.substring(6, 8);
                } else if (cDueDate != null && cDueDate.length() == 10 && cDueDate.contains("-")) {
                    // YYYY-MM-DD -> YYYY.MM.DD
                    formattedDueDate = cDueDate.replace("-", ".");
                } else {
                    formattedDueDate = cDueDate;
                }
                replacePlaceholder(sheet, "{C_DUE_DATE}", formattedDueDate);
                
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
            System.out.println("[ExcelTemplateUtil] 파일 저장 경로: " + outPath);
            
            // 디렉토리 생성 확인
            File downloadDir = new File(DOWNLOAD_PATH);
            if (!downloadDir.exists()) {
                boolean created = downloadDir.mkdirs();
                System.out.println("[ExcelTemplateUtil] 다운로드 디렉토리 생성: " + created);
            }
            
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
                setCellValueWithStyle(row, 5, formatDateString(String.valueOf(bill.getOrDefault("USE_DUTY_DATE", ""))), centerBottomBorderStyle);
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
                setCellValueWithStyle(row, 30, String.valueOf(bill.getOrDefault("REQ_VALUE1", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 31, String.valueOf(bill.getOrDefault("REQ_VALUE2", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 32, String.valueOf(bill.getOrDefault("REQ_VALUE3", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 33, String.valueOf(bill.getOrDefault("REQ_VALUE4", "")), centerBottomBorderStyle);
                setCellValueWithStyle(row, 34, String.valueOf(bill.getOrDefault("REQ_VALUE5", "")), centerBottomBorderStyle);
                
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

            // 파일 저장
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            template.close();
            
            System.out.println("[ExcelTemplateUtil] Excel 파일 생성 완료: " + outPath);
            return outPath;
            
        } catch (FileNotFoundException e) {
            System.err.println("[ExcelTemplateUtil] 파일을 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println("[ExcelTemplateUtil] IO 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("[ExcelTemplateUtil] Excel 생성 중 예상치 못한 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            // 보안 설정 원복
            ZipSecureFile.setMinInflateRatio(originalMinInflateRatio);
            System.out.println("[ExcelTemplateUtil] POI 보안 설정 원복 완료");
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
        if (date != null) {
            // YYYYMMDD 형식 (8자리 숫자) -> YYYY.MM.DD 변환
            if (date.matches("\\d{8}")) {
                return date.substring(0, 4) + "." + date.substring(4, 6) + "." + date.substring(6, 8);
            }
            // YYYY-MM-DD 형식 (10자리, 하이픈 포함) -> YYYY.MM.DD 변환
            else if (date.length() == 10 && date.contains("-")) {
                return date.replace("-", ".");
            }
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
    
    /**
     * 지정된 년월의 마지막 영업일을 반환
     * @param year 년도
     * @param month 월
     * @return 해당 월의 마지막 영업일
     */
    private static LocalDate getLastBusinessDay(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
        
        // 마지막 날부터 역순으로 검사하여 영업일 찾기
        while (!isBusinessDay(lastDayOfMonth)) {
            lastDayOfMonth = lastDayOfMonth.minusDays(1);
        }
        
        return lastDayOfMonth;
    }
    
    /**
     * 주어진 날짜가 영업일인지 확인
     * @param date 확인할 날짜
     * @return 영업일 여부
     */
    private static boolean isBusinessDay(LocalDate date) {
        // 주말 체크
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || 
            date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        
        // 공휴일 체크 (해당 년도의 공휴일 set을 사용)
        Set<LocalDate> holidays = getHolidaysForYear(date.getYear());
        return !holidays.contains(date);
    }
    
    /**
     * 특정 년도의 공휴일 목록 반환
     * @param year 년도
     * @return 공휴일 Set
     */
    private static Set<LocalDate> getHolidaysForYear(int year) {
        // 실제 구현에서는 년도별로 공휴일을 관리하거나
        // 외부 API/DB에서 가져오는 것을 권장
        if (year == 2025) {
            return HOLIDAYS_2025;
        }
        if (year == 2026) {
            return HOLIDAYS_2026;
        }
        // 다른 년도의 경우 빈 Set 반환 (주말만 제외)
        return new HashSet<>();
    }
} 