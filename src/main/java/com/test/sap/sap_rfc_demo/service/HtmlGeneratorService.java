package com.test.sap.sap_rfc_demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class HtmlGeneratorService {
    private static final String TEMPLATE_PATH = "src/main/resources/static/html/Coway-Bill-info-template.html";
    private static final String JSON_PATH = "src/main/resources/static/json/coway-bill-info.json";
    private static final String OUTPUT_DIR = "src/main/resources/static/html/download";

    public String generateHtmlFromTemplate() throws Exception {
        // 1. JSON 파일 읽기
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> data = objectMapper.readValue(new File(JSON_PATH), HashMap.class);

        // 2. Freemarker 설정
        Configuration cfg = new Configuration(new Version("2.3.32"));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setDirectoryForTemplateLoading(new File("src/main/resources/static/html"));

        // 3. 템플릿 로드
        Template template = cfg.getTemplate("Coway-Bill-info-template.html");

        // 4. 파일명 생성 (buyerInfo.companyName + 오늘 날짜 기준)
        String companyName = "회사명";
        if (data.containsKey("buyerInfo") && data.get("buyerInfo") instanceof Map) {
            Map buyerInfo = (Map) data.get("buyerInfo");
            Object nameObj = buyerInfo.get("companyName");
            if (nameObj != null && !nameObj.toString().isEmpty()) {
                companyName = nameObj.toString();
            }
        }
        LocalDate today = LocalDate.now();
        String year = today.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = today.format(DateTimeFormatter.ofPattern("MM"));
        String fileName = String.format("%s %s년 %s월 대금청구서.html", companyName, year, month);

        // 5. HTML 파일 생성
        File outDir = new File(OUTPUT_DIR);
        if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, fileName);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)) {
            template.process(data, writer);
        }
        return outFile.getAbsolutePath();
    }
} 