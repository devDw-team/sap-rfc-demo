package com.test.sap.sap_rfc_demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.sap.sap_rfc_demo.util.ExcelTemplateUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 파일 생성 관련 설정
 */
@Configuration
@EnableScheduling
public class FileCreationConfig {

    /**
     * JSON 처리를 위한 ObjectMapper Bean
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Excel 템플릿 유틸리티 Bean
     */
    @Bean
    public ExcelTemplateUtil excelTemplateUtil() {
        return new ExcelTemplateUtil();
    }
} 