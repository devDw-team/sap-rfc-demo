package com.test.sap.sap_rfc_demo.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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