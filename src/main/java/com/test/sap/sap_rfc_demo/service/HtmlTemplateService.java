package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.dto.BusinessTemplateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * HTML 템플릿 생성 서비스
 * 사업자별 HTML 청구서 템플릿 생성 (조건부 렌더링 포함)
 */
@Service
@Slf4j
public class HtmlTemplateService {
    
    @Autowired
    private FileManagementService fileManagementService;
    
    /**
     * HTML 템플릿 생성 (조건부 렌더링 포함)
     */
    public void generateHtmlTemplate(BusinessTemplateDto business) {
        try {
            log.info("HTML 템플릿 생성 시작 - 사업자번호: {}", business.getBusinessNo());
            
            // 1. 사업자별 디렉토리 생성
            fileManagementService.createBusinessDirectory(business.getBusinessNo());
            
            // 2. 기본 템플릿 파일 읽기
            String baseTemplatePath = fileManagementService.getBaseHtmlTemplatePath();
            Path sourceFile = Paths.get(baseTemplatePath);
            
            if (!Files.exists(sourceFile)) {
                throw new RuntimeException("기본 HTML 템플릿 파일을 찾을 수 없습니다: " + baseTemplatePath);
            }
            
            // 3. 템플릿 내용 읽기
            String templateContent = Files.readString(sourceFile, StandardCharsets.UTF_8);
            
            // 4. 조건부 렌더링 적용
            String processedContent = applyConditionalRendering(templateContent, business);
            
            // 5. 사업자 정보 바인딩
            String finalContent = bindBusinessData(processedContent, business);
            
            // 6. 대상 파일 경로
            Path businessDir = fileManagementService.getBusinessDirectoryPath(business.getBusinessNo());
            String fileName = business.getBusinessNo() + ".html";
            Path targetFile = businessDir.resolve(fileName);
            
            // 7. 처리된 내용을 파일로 저장
            Files.writeString(targetFile, finalContent, StandardCharsets.UTF_8);
            
            log.info("HTML 템플릿 생성 완료 - 사업자번호: {}, 경로: {}", business.getBusinessNo(), targetFile);
            
        } catch (IOException e) {
            log.error("HTML 템플릿 생성 실패 - 사업자번호: {}", business.getBusinessNo(), e);
            throw new RuntimeException("HTML 템플릿 생성 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * _yn 플래그에 따른 조건부 렌더링 적용
     */
    private String applyConditionalRendering(String templateContent, BusinessTemplateDto business) {
        String result = templateContent;
        
        log.info("조건부 렌더링 적용 시작 - 사업자번호: {}", business.getBusinessNo());
        
        // 섹션 표시 여부에 따른 조건부 제거
        result = removeIfNotVisible(result, "stamp", business.isStampVisible());
        result = removeIfNotVisible(result, "cs_guide", business.isCsGuideVisible());
        result = removeIfNotVisible(result, "pay_info", business.isPayInfoVisible());
        result = removeIfNotVisible(result, "pay_guide", business.isPayGuideVisible());
        result = removeIfNotVisible(result, "prepay", business.isPrepayVisible());
        result = removeIfNotVisible(result, "invoice_note", business.isInvoiceNoteVisible());
        
        // 청구 항목 표시 여부에 따른 조건부 제거
        result = removeIfNotVisible(result, "rental", business.isRentalVisible());
        result = removeIfNotVisible(result, "membership", business.isMembershipVisible());
        result = removeIfNotVisible(result, "ovd_int", business.isOvdIntVisible());
        result = removeIfNotVisible(result, "as_fee", business.isAsFeeVisible());
        result = removeIfNotVisible(result, "consumable_fee", business.isConsumableFeeVisible());
        result = removeIfNotVisible(result, "penalty_fee", business.isPenaltyFeeVisible());
        
        // 청구 상세 테이블 열 표시 여부에 따른 조건부 제거
        result = removeTableColumnIfNotVisible(result, "order_no", business.isOrderNoVisible());
        result = removeTableColumnIfNotVisible(result, "prod_group", business.isProdGroupVisible());
        result = removeTableColumnIfNotVisible(result, "goods_nm", business.isGoodsNmVisible());
        result = removeTableColumnIfNotVisible(result, "contract_date", business.isContractDateVisible());
        result = removeTableColumnIfNotVisible(result, "use_duty_month", business.isUseDutyMonthVisible());
        result = removeTableColumnIfNotVisible(result, "contract_period", business.isContractPeriodVisible());
        result = removeTableColumnIfNotVisible(result, "fix_supply_value", business.isFixSupplyValueVisible());
        result = removeTableColumnIfNotVisible(result, "fix_vat", business.isFixVatVisible());
        result = removeTableColumnIfNotVisible(result, "fix_bill_amt", business.isFixBillAmtVisible());
        result = removeTableColumnIfNotVisible(result, "supply_value", business.isSupplyValueVisible());
        result = removeTableColumnIfNotVisible(result, "vat", business.isVatVisible());
        result = removeTableColumnIfNotVisible(result, "bill_amt", business.isBillAmtVisible());
        result = removeTableColumnIfNotVisible(result, "install_addr", business.isInstallAddrVisible());
        result = removeTableColumnIfNotVisible(result, "goods_sn", business.isGoodsSnVisible());
        result = removeTableColumnIfNotVisible(result, "dept_nm", business.isDeptNmVisible());
        result = removeTableColumnIfNotVisible(result, "dept_tel_no", business.isDeptTelNoVisible());
        result = removeTableColumnIfNotVisible(result, "note", business.isNoteVisible());
        
        log.info("조건부 렌더링 적용 완료 - 사업자번호: {}", business.getBusinessNo());
        return result;
    }
    
    /**
     * 특정 섹션을 조건부로 제거
     */
    private String removeIfNotVisible(String content, String sectionName, boolean isVisible) {
        if (isVisible) {
            log.debug("섹션 유지: {} (표시: {})", sectionName, isVisible);
            return content; // 표시해야 하므로 그대로 반환
        }
        
        log.info("섹션 제거: {} (표시: {})", sectionName, isVisible);
        
        // 다양한 패턴으로 섹션 마커를 찾아서 제거
        String[] patterns = {
            // HTML 주석 기반 마커 (대소문자 구분 없음)
            "<!--\\s*" + sectionName.toUpperCase() + "_START\\s*-->.*?<!--\\s*" + sectionName.toUpperCase() + "_END\\s*-->",
            "<!--\\s*" + sectionName.toLowerCase() + "_start\\s*-->.*?<!--\\s*" + sectionName.toLowerCase() + "_end\\s*-->",
            
            // CSS 클래스 기반 마커
            "<[^>]*class=['\"][^'\"]*" + sectionName + "[^'\"]*['\"][^>]*>.*?</[^>]+>",
            
            // ID 기반 마커
            "<[^>]*id=['\"][^'\"]*" + sectionName + "[^'\"]*['\"][^>]*>.*?</[^>]+>",
            
            // 데이터 속성 기반 마커
            "<[^>]*data-section=['\"]" + sectionName + "['\"][^>]*>.*?</[^>]+>"
        };
        
        String originalContent = content;
        for (String pattern : patterns) {
            content = content.replaceAll("(?s)" + pattern, "");
        }
        
        if (!originalContent.equals(content)) {
            log.info("섹션 제거 완료: {}", sectionName);
        } else {
            log.warn("섹션 마커를 찾을 수 없음: {}", sectionName);
        }
        
        return content;
    }
    
    /**
     * 테이블 열을 조건부로 제거
     */
    private String removeTableColumnIfNotVisible(String content, String columnName, boolean isVisible) {
        if (isVisible) {
            log.debug("테이블 열 유지: {} (표시: {})", columnName, isVisible);
            return content; // 표시해야 하므로 그대로 반환
        }
        
        log.info("테이블 열 제거: {} (표시: {})", columnName, isVisible);
        
        String originalContent = content;
        
        // 테이블 헤더의 해당 열 제거
        String headerPattern = "<th[^>]*class=['\"][^'\"]*" + columnName + "[^'\"]*['\"][^>]*>.*?</th>";
        content = content.replaceAll("(?s)" + headerPattern, "");
        
        // 테이블 데이터의 해당 열 제거
        String dataPattern = "<td[^>]*class=['\"][^'\"]*" + columnName + "[^'\"]*['\"][^>]*>.*?</td>";
        content = content.replaceAll("(?s)" + dataPattern, "");
        
        if (!originalContent.equals(content)) {
            log.info("테이블 열 제거 완료: {}", columnName);
        } else {
            log.warn("테이블 열 마커를 찾을 수 없음: {}", columnName);
        }
        
        return content;
    }
    
    /**
     * HTML 템플릿에 사업자 데이터 바인딩
     */
    private String bindBusinessData(String templateContent, BusinessTemplateDto business) {
        log.info("사업자 데이터 바인딩 시작 - 사업자번호: {}", business.getBusinessNo());
        
        String result = templateContent;
        
        // 사업자 기본 정보 바인딩
        result = result.replace("{{businessNo}}", business.getBusinessNo() != null ? business.getBusinessNo() : "");
        result = result.replace("{{businessNm}}", business.getBusinessNm() != null ? business.getBusinessNm() : "");
        result = result.replace("{{ceoNm}}", business.getCeoNm() != null ? business.getCeoNm() : "");
        result = result.replace("{{businessAddr}}", business.getBusinessAddr() != null ? business.getBusinessAddr() : "");
        result = result.replace("{{businessType}}", business.getBusinessType() != null ? business.getBusinessType() : "");
        result = result.replace("{{businessCategory}}", business.getBusinessCategory() != null ? business.getBusinessCategory() : "");
        
        // 조건부 텍스트 바인딩
        if (business.isCsGuideVisible() && business.getCsGuide() != null) {
            result = result.replace("{{csGuide}}", business.getCsGuide());
        }
        
        if (business.isPayGuideVisible() && business.getPayGuide() != null) {
            result = result.replace("{{payGuide}}", business.getPayGuide());
        }
        
        log.info("사업자 데이터 바인딩 완료 - 사업자번호: {}", business.getBusinessNo());
        return result;
    }
} 