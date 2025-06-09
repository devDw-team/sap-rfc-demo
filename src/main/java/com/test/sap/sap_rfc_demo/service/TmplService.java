package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.dto.BusinessTemplateDto;
import com.test.sap.sap_rfc_demo.repository.BbimcmBsmgRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 템플릿 생성 메인 서비스
 * 사업자별 HTML 및 Excel 청구서 템플릿 생성 관리
 */
@Service
@Transactional
@Slf4j
public class TmplService {
    
    @Autowired
    private BbimcmBsmgRepository businessRepository;
    
    @Autowired
    private HtmlTemplateService htmlTemplateService;
    
    @Autowired
    private ExcelTemplateService excelTemplateService;
    
    @Autowired
    private FileManagementService fileManagementService;
    
    /**
     * 모든 사업자에 대한 템플릿 생성
     */
    public void generateAllTemplates() {
        log.info("모든 사업자 템플릿 생성 시작");
        List<BusinessTemplateDto> businessList = businessRepository.findAllBusinessTemplateData();
        
        for (BusinessTemplateDto business : businessList) {
            try {
                generateTemplateForBusiness(business);
                log.info("템플릿 생성 완료 - 사업자번호: {}", business.getBusinessNo());
            } catch (Exception e) {
                log.error("템플릿 생성 실패 - 사업자번호: {}", business.getBusinessNo(), e);
            }
        }
        log.info("모든 사업자 템플릿 생성 완료");
    }
    
    /**
     * 특정 사업자 관리 ID로 템플릿 생성 (데이터 변경시 호출)
     */
    public void generateTemplateForBusiness(Long busiMgmtId) {
        log.info("사업자 관리 ID로 템플릿 생성 시작 - ID: {}", busiMgmtId);
        
        Optional<BusinessTemplateDto> businessOpt = 
            businessRepository.findBusinessTemplateDataById(busiMgmtId);
            
        if (businessOpt.isPresent()) {
            BusinessTemplateDto business = businessOpt.get();
            
            // 1. 기존 파일 삭제
            fileManagementService.deleteExistingTemplates(business.getBusinessNo());
            
            // 2. 새 템플릿 생성
            generateTemplateForBusiness(business);
            
            log.info("사업자 템플릿 생성 완료 - 사업자번호: {}", business.getBusinessNo());
        } else {
            throw new IllegalArgumentException("사업자 관리 ID를 찾을 수 없습니다: " + busiMgmtId);
        }
    }
    
    /**
     * 특정 사업자 번호로 템플릿 생성 (웹 폼에서 호출)
     */
    public void generateTemplateByBusinessNo(String businessNo) {
        log.info("사업자 번호로 템플릿 생성 시작 - 사업자번호: {}", businessNo);
        
        Optional<BusinessTemplateDto> businessOpt = 
            businessRepository.findBusinessTemplateDataByBusinessNo(businessNo);
            
        if (businessOpt.isPresent()) {
            BusinessTemplateDto business = businessOpt.get();
            
            // 1. 기존 파일 삭제
            fileManagementService.deleteExistingTemplates(business.getBusinessNo());
            
            // 2. 새 템플릿 생성
            generateTemplateForBusiness(business);
            
            log.info("사업자 템플릿 생성 완료 - 사업자번호: {}", business.getBusinessNo());
        } else {
            throw new IllegalArgumentException("사업자번호를 찾을 수 없습니다: " + businessNo);
        }
    }
    
    /**
     * 사업자 번호 존재 여부 확인
     */
    public boolean isBusinessExists(String businessNo) {
        return businessRepository.existsByBusinessNo(businessNo);
    }
    
    /**
     * 전체 사업자 템플릿 데이터 조회
     */
    public List<BusinessTemplateDto> getAllBusinessTemplateData() {
        log.info("전체 사업자 템플릿 데이터 조회");
        return businessRepository.findAllBusinessTemplateData();
    }
    
    /**
     * 단일 사업자에 대한 템플릿 생성 (내부 메서드)
     */
    private void generateTemplateForBusiness(BusinessTemplateDto business) {
        try {
            log.info("템플릿 생성 처리 시작 - 사업자번호: {}", business.getBusinessNo());
            log.info("인감날인 사용여부: {} ({})", business.getStampYn(), 
                    business.isStampVisible() ? "표시" : "숨김");
            
            // HTML 템플릿 생성
            htmlTemplateService.generateHtmlTemplate(business);
            
            // Excel 템플릿 생성 (도장 이미지 조건부 처리 포함)
            excelTemplateService.generateExcelTemplate(business);
            
            log.info("템플릿 생성 처리 완료 - 사업자번호: {} (인감날인: {})", 
                    business.getBusinessNo(), business.getStampYn());
            
        } catch (Exception e) {
            log.error("템플릿 생성 실패 - 사업자번호: {}", business.getBusinessNo(), e);
            throw new RuntimeException("템플릿 생성 실패: " + e.getMessage(), e);
        }
    }
} 