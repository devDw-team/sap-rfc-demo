package com.test.sap.sap_rfc_demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * 파일 관리 서비스
 * 템플릿 파일의 생성, 삭제, 경로 관리를 담당
 */
@Service
@Slf4j
public class FileManagementService {
    
    @Value("${app.file.base-path:src/main/resources/static}")
    private String basePath;
    
    /**
     * 특정 사업자의 기존 템플릿 파일들 삭제 (사업자별 디렉토리 전체 삭제)
     */
    public void deleteExistingTemplates(String businessNo) {
        try {
            // 사업자별 디렉토리 경로
            Path businessDir = Paths.get(basePath, "template", businessNo);
            
            if (Files.exists(businessDir)) {
                // 디렉토리 내 모든 파일 삭제
                Files.walk(businessDir)
                     .sorted(Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
                
                log.info("기존 템플릿 디렉토리 삭제 완료 - 사업자번호: {}, 경로: {}", businessNo, businessDir);
            }
            
        } catch (IOException e) {
            log.error("파일 삭제 실패 - 사업자번호: {}", businessNo, e);
            throw new RuntimeException("파일 삭제 실패", e);
        }
    }
    
    /**
     * 사업자별 디렉토리 생성
     */
    public void createBusinessDirectory(String businessNo) {
        try {
            Path businessDir = Paths.get(basePath, "template", businessNo);
            
            if (!Files.exists(businessDir)) {
                Files.createDirectories(businessDir);
                log.info("사업자 디렉토리 생성 완료 - 사업자번호: {}, 경로: {}", businessNo, businessDir);
            }
            
        } catch (IOException e) {
            log.error("디렉토리 생성 실패 - 사업자번호: {}", businessNo, e);
            throw new RuntimeException("디렉토리 생성 실패", e);
        }
    }
    
    /**
     * 특정 사업자의 템플릿 파일 경로 조회
     */
    public String getTemplateFilePath(String businessNo, String fileType) {
        String businessDir = basePath + "/template/" + businessNo;
        String fileName = businessNo + "." + fileType;
        return businessDir + "/" + fileName;
    }
    
    /**
     * 템플릿 파일 존재 여부 확인
     */
    public boolean isTemplateExists(String businessNo, String fileType) {
        String filePath = getTemplateFilePath(businessNo, fileType);
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * 사업자별 디렉토리 경로 반환
     */
    public Path getBusinessDirectoryPath(String businessNo) {
        return Paths.get(basePath, "template", businessNo);
    }
    
    /**
     * 기본 템플릿 파일 경로 반환
     */
    public String getBaseHtmlTemplatePath() {
        return basePath + "/html/bill-template.html";
    }
    
    /**
     * 기본 Excel 템플릿 파일 경로 반환
     */
    public String getBaseExcelTemplatePath() {
        return basePath + "/excel/billinfo_template_mark.xlsx";
    }
} 