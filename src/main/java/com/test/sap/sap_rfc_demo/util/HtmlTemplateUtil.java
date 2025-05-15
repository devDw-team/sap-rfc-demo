package com.test.sap.sap_rfc_demo.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HtmlTemplateUtil {
    /**
     * HTML 템플릿 파일을 읽어와서 ${필드명} 플레이스홀더를 데이터로 치환 후 저장
     * @param templatePath 템플릿 파일 경로
     * @param outputDir 저장 디렉토리
     * @param fileName 저장 파일명
     * @param data 치환할 데이터(Map<String, Object>)
     * @return 생성된 파일의 상대 경로
     */
    public static String generateHtml(String templatePath, String outputDir, String fileName, Map<String, Object> data) throws IOException {
        // 0. 오늘 날짜를 yyyy.MM.dd 형식으로 구해서, SEND_DATE 키가 없으면 자동으로 Map에 추가
        if (!data.containsKey("SEND_DATE")) {
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            data.put("SEND_DATE", today.format(formatter));
        }
        // 0-2. C_RECP_YM(예: 202501) 값이 있으면 연도/월 분리해서 Map에 자동 추가
        if (data.containsKey("C_RECP_YM")) {
            Object ymObj = data.get("C_RECP_YM");
            if (ymObj != null) {
                String ym = ymObj.toString();
                if (ym.length() == 6 && ym.matches("\\d{6}")) {
                    // 앞 4자리: 연도, 뒤 2자리: 월
                    String year = ym.substring(0, 4);
                    String month = ym.substring(4, 6);
                    data.put("C_RECP_YEAR", year);
                    data.put("C_RECP_MONTH", month);
                }
            }
        }
        // 0-3. TOTAL_AMOUNT 값이 있으면 3자리마다 콤마가 들어간 문자열을 TOTAL_AMOUNT_COMMA로 Map에 자동 추가
        if (data.containsKey("TOTAL_AMOUNT")) {
            Object amtObj = data.get("TOTAL_AMOUNT");
            if (amtObj != null) {
                try {
                    long amt = Long.parseLong(amtObj.toString().replaceAll(",", ""));
                    data.put("TOTAL_AMOUNT_COMMA", String.format("%,d", amt));
                } catch (Exception e) {
                    data.put("TOTAL_AMOUNT_COMMA", amtObj.toString());
                }
            }
        }
        // 0-4. 금액 관련 필드 자동 콤마 포맷 (Map 1-depth)
        String[] moneyKeys = {"TOTAL_AMOUNT", "SUMMARY_AMOUNT", "PRE_AMT", "REMAIN_AMT", "SUPPLY_VALUE", "VAT", "CELL_TOTAL", "taxableAmountTotal", "taxAmountTotal", "totAmount"};
        for (String key : moneyKeys) {
            if (data.containsKey(key)) {
                Object amtObj = data.get(key);
                if (amtObj != null) {
                    try {
                        long amt = Long.parseLong(amtObj.toString().replaceAll(",", ""));
                        data.put(key + "_COMMA", String.format("%,d", amt));
                    } catch (Exception e) {
                        data.put(key + "_COMMA", amtObj.toString());
                    }
                }
            }
        }
        // 0-5. bills, bill_type_summary 등 List<Map> 내부 금액 필드도 자동 콤마 포맷
        String[] listKeys = {"bills", "bill_type_summary"};
        for (String listKey : listKeys) {
            if (data.containsKey(listKey) && data.get(listKey) instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> list = (java.util.List<Map<String, Object>>) data.get(listKey);
                for (Map<String, Object> row : list) {
                    for (String key : moneyKeys) {
                        if (row.containsKey(key)) {
                            Object amtObj = row.get(key);
                            if (amtObj != null) {
                                try {
                                    long amt = Long.parseLong(amtObj.toString().replaceAll(",", ""));
                                    row.put(key + "_COMMA", String.format("%,d", amt));
                                } catch (Exception e) {
                                    row.put(key + "_COMMA", amtObj.toString());
                                }
                            }
                        }
                    }
                }
            }
        }
        // 0-6. C_DUE_DATE 값이 yyyy-mm-dd 형식이면 yyyy.mm.dd로 변환해서 C_DUE_DATE_DOT에 추가
        if (data.containsKey("C_DUE_DATE")) {
            Object dueObj = data.get("C_DUE_DATE");
            if (dueObj != null) {
                String due = dueObj.toString();
                // yyyy-mm-dd 형식인지 체크
                if (due.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    // LocalDate로 파싱 후 yyyy.MM.dd로 포맷
                    try {
                        java.time.LocalDate date = java.time.LocalDate.parse(due);
                        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd");
                        data.put("C_DUE_DATE_DOT", date.format(fmt));
                    } catch (Exception e) {
                        // 파싱 실패 시 단순 치환
                        data.put("C_DUE_DATE_DOT", due.replaceAll("-", "."));
                    }
                } else {
                    // 형식이 다르면 원본 그대로
                    data.put("C_DUE_DATE_DOT", due);
                }
            }
        }
        // 0-7. FILE_NAME 키가 없으면 fileName 파라미터에서 확장자 없는 부분을 FILE_NAME으로 Map에 추가
        if (!data.containsKey("FILE_NAME")) {
            String onlyName = fileName;
            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx > 0) {
                onlyName = fileName.substring(0, dotIdx);
            }
            data.put("FILE_NAME", onlyName);
        }
        // 0-8. bill_type_summary의 SUMMARY_CNT, SUMMARY_AMOUNT 총합을 totCnt, totAmt로 Map에 추가
        if (data.containsKey("bill_type_summary") && data.get("bill_type_summary") instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> billTypes = (java.util.List<Map<String, Object>>) data.get("bill_type_summary");
            long totCnt = 0L;
            long totAmt = 0L;
            for (Map<String, Object> billType : billTypes) {
                // SUMMARY_CNT 합계
                if (billType.containsKey("SUMMARY_CNT")) {
                    try {
                        totCnt += Long.parseLong(billType.get("SUMMARY_CNT").toString().replaceAll(",", ""));
                    } catch (Exception e) { /* 무시 */ }
                }
                // SUMMARY_AMOUNT 합계
                if (billType.containsKey("SUMMARY_AMOUNT")) {
                    try {
                        totAmt += Long.parseLong(billType.get("SUMMARY_AMOUNT").toString().replaceAll(",", ""));
                    } catch (Exception e) { /* 무시 */ }
                }
            }
            data.put("totCnt", totCnt);
            data.put("totAmt", totAmt);
            // totAmt_COMMA도 자동 추가
            data.put("totAmt_COMMA", String.format("%,d", totAmt));
        }
        // 0-9. bills의 SUPPLY_VALUE, VAT 총합 및 합계 계산 (taxableAmountTotal, taxAmountTotal, totAmount)
        if (data.containsKey("bills") && data.get("bills") instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> bills = (java.util.List<Map<String, Object>>) data.get("bills");
            long taxableAmountTotal = 0L;
            long taxAmountTotal = 0L;
            for (Map<String, Object> bill : bills) {
                // SUPPLY_VALUE 합계
                if (bill.containsKey("SUPPLY_VALUE")) {
                    try {
                        taxableAmountTotal += Long.parseLong(bill.get("SUPPLY_VALUE").toString().replaceAll(",", ""));
                    } catch (Exception e) { /* 무시 */ }
                }
                // VAT 합계
                if (bill.containsKey("VAT")) {
                    try {
                        taxAmountTotal += Long.parseLong(bill.get("VAT").toString().replaceAll(",", ""));
                    } catch (Exception e) { /* 무시 */ }
                }
            }
            long totAmount = taxableAmountTotal + taxAmountTotal;
            data.put("taxableAmountTotal", taxableAmountTotal);
            data.put("taxAmountTotal", taxAmountTotal);
            data.put("totAmount", totAmount);
            // 콤마 포맷도 추가
            data.put("taxableAmountTotal_COMMA", String.format("%,d", taxableAmountTotal));
            data.put("taxAmountTotal_COMMA", String.format("%,d", taxAmountTotal));
            data.put("totAmount_COMMA", String.format("%,d", totAmount));
        }
        // 0-10. bills row 개수에 따라 전체 보기 버튼 노출 여부 결정
        if (data.containsKey("bills") && data.get("bills") instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> bills = (java.util.List<Map<String, Object>>) data.get("bills");
            data.put("SHOW_DETAIL_MORE_BTN", bills.size() >= 11); // 11개 이상이면 true, 아니면 false
        }
        // 1. 템플릿 파일 읽기
        String html = Files.readString(new File(templatePath).toPath(), StandardCharsets.UTF_8);

        // 1.5. bills 반복 구간 치환
        if (data.containsKey("bills") && data.get("bills") instanceof java.util.List) {
            Pattern loopPattern = Pattern.compile("<!--BILLS_LOOP_START-->([\\s\\S]*?)<!--BILLS_LOOP_END-->");
            Matcher loopMatcher = loopPattern.matcher(html);
            if (loopMatcher.find()) {
                String rowTemplate = loopMatcher.group(1);
                StringBuilder rows = new StringBuilder();
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> bills = (java.util.List<Map<String, Object>>) data.get("bills");
                for (Map<String, Object> bill : bills) {
                    String row = rowTemplate;
                    for (Map.Entry<String, Object> entry : bill.entrySet()) {
                        row = row.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue().toString() : "");
                    }
                    // CELL_TOTAL 계산
                    try {
                        long supply = bill.get("SUPPLY_VALUE") != null ? Long.parseLong(bill.get("SUPPLY_VALUE").toString()) : 0L;
                        long vat = bill.get("VAT") != null ? Long.parseLong(bill.get("VAT").toString()) : 0L;
                        row = row.replace("${CELL_TOTAL}", String.format("%,d", supply + vat));
                    } catch (Exception e) {
                        row = row.replace("${CELL_TOTAL}", "");
                    }
                    rows.append(row);
                }
                // 반복 구간 전체를 rows로 치환
                html = html.replace(loopMatcher.group(0), rows.toString());
            }
        }

        // 1.6. bill_type_summary 반복 구간 치환
        if (data.containsKey("bill_type_summary") && data.get("bill_type_summary") instanceof java.util.List) {
            Pattern loopPattern = Pattern.compile("<!--BILL_TYPE_LOOP_START-->([\\s\\S]*?)<!--BILL_TYPE_LOOP_END-->");
            Matcher loopMatcher = loopPattern.matcher(html);
            if (loopMatcher.find()) {
                String rowTemplate = loopMatcher.group(1);
                StringBuilder rows = new StringBuilder();
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> billTypes = (java.util.List<Map<String, Object>>) data.get("bill_type_summary");
                for (Map<String, Object> billType : billTypes) {
                    String row = rowTemplate;
                    for (Map.Entry<String, Object> entry : billType.entrySet()) {
                        row = row.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue().toString() : "");
                    }
                    rows.append(row);
                }
                html = html.replace(loopMatcher.group(0), rows.toString());
            }
        }

        // 2. ${필드명} 치환 (1-depth만, Map<String, Object>에서만)
        Pattern pattern = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}");
        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = findValue(data, key);
            matcher.appendReplacement(sb, value != null ? Matcher.quoteReplacement(value.toString()) : "");
        }
        matcher.appendTail(sb);
        String replaced = sb.toString();

        // 3. 저장 디렉토리 생성
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();
        String outPath = outputDir + File.separator + fileName + ".html";
        Files.writeString(new File(outPath).toPath(), replaced, StandardCharsets.UTF_8);
        return outPath;
    }

    // Map<String, Object>의 1-depth에서 key를 찾아 반환
    private static Object findValue(Map<String, Object> data, String key) {
        for (Object v : data.values()) {
            if (v instanceof Map) {
                Map<?,?> m = (Map<?,?>)v;
                if (m.containsKey(key)) return m.get(key);
            }
            // 리스트(예: bills 등)는 무시
        }
        // 최상위에서 바로 찾는 경우
        if (data.containsKey(key)) return data.get(key);
        return null;
    }
} 