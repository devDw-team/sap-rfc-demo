package com.test.sap.sap_rfc_demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTML 템플릿 처리 서비스
 * umsmail-guide.md Step 4-2의 메일 콘텐츠 준비 기능 구현
 */
@Service
@Slf4j
public class TemplateService {

    private static final String MAIL_TEMPLATE_PATH = "static/mail/webmail.html";

    /**
     * HTML 메일 템플릿을 읽고 변수를 치환하여 최종 HTML 콘텐츠 생성
     * 
     * @param custNm 고객명
     * @param recpYear 청구년도
     * @param recpMonth 청구월
     * @return 변수가 치환된 HTML 콘텐츠
     * @throws IOException 템플릿 파일 읽기 실패 시
     */
    public String generateMailContent(String custNm, String recpYear, Integer recpMonth) throws IOException {
        log.debug("메일 템플릿 생성 시작 - 고객명: {}, 청구년월: {}년 {}월", custNm, recpYear, recpMonth);
        
        // 템플릿 파일 읽기
        String templateContent = readTemplateFile();
        
        // 변수 치환
        String processedContent = replaceTemplateVariables(templateContent, custNm, recpYear, recpMonth);
        
        log.debug("메일 템플릿 생성 완료 - 고객명: {}", custNm);
        return processedContent;
    }

    /**
     * 템플릿 파일을 읽어서 문자열로 반환
     */
    private String readTemplateFile() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource(MAIL_TEMPLATE_PATH);
            try (InputStream inputStream = resource.getInputStream()) {
                return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("메일 템플릿 파일 읽기 실패: {}", MAIL_TEMPLATE_PATH, e);
            throw new IOException("메일 템플릿 파일을 읽을 수 없습니다: " + MAIL_TEMPLATE_PATH, e);
        }
    }

    /**
     * 템플릿 내 변수들을 실제 값으로 치환
     * 
     * ${CUST_NM} -> 고객명
     * ${C_RECP_YEAR} -> 청구년도
     * ${C_RECP_MONTH} -> 청구월 (한자리 숫자일 경우 0 제거)
     */
    private String replaceTemplateVariables(String template, String custNm, String recpYear, Integer recpMonth) {
        if (template == null) {
            return "";
        }

        // 기본값 설정
        String safeCustomerName = custNm != null ? custNm : "고객";
        String safeRecpYear = recpYear != null ? recpYear : "2024";
        String safeRecpMonth = recpMonth != null ? String.valueOf(recpMonth) : "1";

        log.debug("템플릿 변수 치환 - 고객명: {}, 년도: {}, 월: {}", safeCustomerName, safeRecpYear, safeRecpMonth);

        return template
                .replace("${CUST_NM}", safeCustomerName)
                .replace("${C_RECP_YEAR}", safeRecpYear)
                .replace("${C_RECP_MONTH}", safeRecpMonth);
    }

    /**
     * Map을 이용한 변수 치환 (확장 가능)
     */
    public String generateMailContentWithVariables(Map<String, Object> variables) throws IOException {
        String templateContent = readTemplateFile();
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            templateContent = templateContent.replace(placeholder, value);
        }
        
        return templateContent;
    }

    /**
     * 템플릿 변수 검증
     */
    public boolean validateTemplateVariables(String custNm, String recpYear, Integer recpMonth) {
        if (custNm == null || custNm.trim().isEmpty()) {
            log.warn("고객명이 비어있습니다");
            return false;
        }
        
        if (recpYear == null || recpYear.trim().isEmpty()) {
            log.warn("청구년도가 비어있습니다");
            return false;
        }
        
        if (recpMonth == null || recpMonth < 1 || recpMonth > 12) {
            log.warn("청구월이 유효하지 않습니다: {}", recpMonth);
            return false;
        }
        
        return true;
    }
} 