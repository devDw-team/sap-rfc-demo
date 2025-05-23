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
        // 하위 Map(1-depth)에도 금액 필드 콤마 포맷 추가
        for (Object v : data.values()) {
            if (v instanceof Map) {
                Map<String, Object> subMap = (Map<String, Object>) v;
                for (String key : moneyKeys) {
                    if (subMap.containsKey(key)) {
                        Object amtObj = subMap.get(key);
                        if (amtObj != null) {
                            try {
                                long amt = Long.parseLong(amtObj.toString().replaceAll(",", ""));
                                subMap.put(key + "_COMMA", String.format("%,d", amt));
                            } catch (Exception e) {
                                subMap.put(key + "_COMMA", amtObj.toString());
                            }
                        }
                    }
                }
            }
        }
        // 템플릿 파일 읽기 직전, data Map 로그 출력
        System.out.println("==== HtmlTemplateUtil data Map ====");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("===================================");
        // 1. 템플릿 파일 읽기
        String html = Files.readString(new File(templatePath).toPath(), StandardCharsets.UTF_8);

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
                    // 날짜/월 포맷 변환 추가
                    // 1. INST_DT_DOT
                    if (row.containsKey("INST_DT")) {
                        String instDt = String.valueOf(row.get("INST_DT"));
                        if (instDt.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            row.put("INST_DT_DOT", instDt.replace("-", "."));
                        } else {
                            row.put("INST_DT_DOT", instDt);
                        }
                    }
                    // 2. USE_MONTH_DOT
                    if (row.containsKey("USE_MONTH")) {
                        String useMonth = String.valueOf(row.get("USE_MONTH"));
                        if (useMonth.matches("\\d{6}")) {
                            row.put("USE_MONTH_DOT", useMonth.substring(0, 4) + "." + useMonth.substring(4, 6));
                        } else {
                            row.put("USE_MONTH_DOT", useMonth);
                        }
                    }
                    // 3. RECP_YM_DOT
                    if (row.containsKey("RECP_YM")) {
                        String recpYm = String.valueOf(row.get("RECP_YM"));
                        if (recpYm.matches("\\d{6}")) {
                            row.put("RECP_YM_DOT", recpYm.substring(0, 4) + "." + recpYm.substring(4, 6));
                        } else {
                            row.put("RECP_YM_DOT", recpYm);
                        }
                    }
                }
            }
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
            // row 개수 추가
            data.put("TOT_ROW_CNT", bills.size());
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
        // bill_summary에서 값 추출해서 최상위에 넣기
        if (data.containsKey("bill_summary") && data.get("bill_summary") instanceof Map) {
            Map<String, Object> billSummary = (Map<String, Object>) data.get("bill_summary");
            
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
            
            // C_SEL_KUN_CNT < 1 이면 오늘날짜 기준으로 값 설정
            if (selKunCnt < 1) {
                LocalDate today = LocalDate.now();
                // 청구년월(C_RECP_YM): 오늘날짜 기준 년월(YYYYMM)
                String recpYm = today.format(DateTimeFormatter.ofPattern("yyyyMM"));
                data.put("C_RECP_YM", recpYm);
                
                // 납부기한(C_DUE_DATE): 오늘날짜 기준 해당 월의 말일 날짜(YYYY-MM-DD)
                LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
                String dueDate = lastDayOfMonth.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                data.put("C_DUE_DATE", dueDate);
                
                // TOTAL_AMOUNT는 기존 값 유지 또는 0으로 설정
                if (billSummary.containsKey("TOTAL_AMOUNT")) {
                    data.put("TOTAL_AMOUNT", billSummary.get("TOTAL_AMOUNT"));
                } else {
                    data.put("TOTAL_AMOUNT", 0);
                }
            } else {
                // 기존 프로세스: bill_summary에서 값 그대로 추출
                if (billSummary.containsKey("C_RECP_YM")) {
                    data.put("C_RECP_YM", billSummary.get("C_RECP_YM"));
                }
                if (billSummary.containsKey("C_DUE_DATE")) {
                    data.put("C_DUE_DATE", billSummary.get("C_DUE_DATE"));
                }
                if (billSummary.containsKey("TOTAL_AMOUNT")) {
                    data.put("TOTAL_AMOUNT", billSummary.get("TOTAL_AMOUNT"));
                }
            }
        }
        // 반드시 bill_summary에서 값을 꺼낸 후 C_RECP_YM을 기준으로 파생값 생성
        if (data.containsKey("C_RECP_YM")) {
            String ym = data.get("C_RECP_YM").toString();
            if (ym.length() == 6 && ym.matches("\\d{6}")) {
                data.put("C_RECP_YEAR", ym.substring(0, 4));
                data.put("C_RECP_MONTH", ym.substring(4, 6));
            }
        }

        // C_RECP_YEAR, C_RECP_MONTH가 없으면 C_RECP_YM에서 파싱해서 보장
        if ((!data.containsKey("C_RECP_YEAR") || !data.containsKey("C_RECP_MONTH")) && data.containsKey("C_RECP_YM")) {
            String ym = data.get("C_RECP_YM").toString();
            if (ym.length() == 6 && ym.matches("\\d{6}")) {
                data.put("C_RECP_YEAR", ym.substring(0, 4));
                data.put("C_RECP_MONTH", ym.substring(4, 6));
            }
        }

        // PRE_AMT_COMMA, REMAIN_AMT_COMMA 보장
        if (!data.containsKey("PRE_AMT_COMMA") && data.containsKey("PRE_AMT")) {
            Object amtObj = data.get("PRE_AMT");
            if (amtObj != null) {
                data.put("PRE_AMT_COMMA", String.format("%,d", Long.parseLong(amtObj.toString())));
            }
        }
        if (!data.containsKey("REMAIN_AMT_COMMA") && data.containsKey("REMAIN_AMT")) {
            Object amtObj = data.get("REMAIN_AMT");
            if (amtObj != null) {
                data.put("REMAIN_AMT_COMMA", String.format("%,d", Long.parseLong(amtObj.toString())));
            }
        }


        // TOTAL_AMOUNT_COMMA 보장
        if (!data.containsKey("TOTAL_AMOUNT_COMMA") && data.containsKey("TOTAL_AMOUNT")) {
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

        // C_DUE_DATE_DOT 보장
        if (!data.containsKey("C_DUE_DATE_DOT") && data.containsKey("C_DUE_DATE")) {
            Object dueObj = data.get("C_DUE_DATE");
            if (dueObj != null) {
                String due = dueObj.toString();
                if (due.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    try {
                        java.time.LocalDate date = java.time.LocalDate.parse(due);
                        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd");
                        data.put("C_DUE_DATE_DOT", date.format(fmt));
                    } catch (Exception e) {
                        data.put("C_DUE_DATE_DOT", due.replaceAll("-", "."));
                    }
                } else {
                    data.put("C_DUE_DATE_DOT", due);
                }
            }
        }

        
        // 파일명 동적 생성
        String year = data.getOrDefault("C_RECP_YEAR", "").toString();
        String month = data.getOrDefault("C_RECP_MONTH", "").toString();
        String dynamicFileName = String.format("코웨이(주) %s년 %s월 대금청구서", year, month);
        // 0-7. FILE_NAME 키가 없으면 동적 파일명을 FILE_NAME으로 Map에 추가
        if (!data.containsKey("FILE_NAME")) {
            data.put("FILE_NAME", dynamicFileName);
        }

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
        String outPath = outputDir + File.separator + dynamicFileName + ".html";
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