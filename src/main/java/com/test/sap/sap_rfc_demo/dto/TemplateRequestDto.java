package com.test.sap.sap_rfc_demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 템플릿 생성 요청 DTO
 * 웹 폼에서 사업자 번호를 입력받을 때 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRequestDto {
    
    private String businessNo;      // 사업자번호 (숫자 10자리: 1234567890)
    
    /**
     * 사업자번호 그대로 반환 (이미 하이픈 없는 형식)
     * 데이터베이스 조회 시 사용
     */
    public String getBusinessNoWithoutHyphen() {
        return businessNo;
    }
    
    /**
     * 하이픈이 없는 사업자번호를 하이픈 포함 형식으로 변환
     * 표시 목적으로 사용
     */
    public static String formatBusinessNo(String businessNoWithoutHyphen) {
        if (businessNoWithoutHyphen == null || businessNoWithoutHyphen.length() != 10) {
            return businessNoWithoutHyphen;
        }
        
        return businessNoWithoutHyphen.substring(0, 3) + "-" + 
               businessNoWithoutHyphen.substring(3, 5) + "-" + 
               businessNoWithoutHyphen.substring(5);
    }
    
    /**
     * 사업자번호 형식 검증 (숫자 10자리)
     */
    public boolean isValidFormat() {
        if (businessNo == null) return false;
        return businessNo.matches("\\d{10}");
    }
    
    /**
     * 사업자번호를 하이픈 포함 형식으로 포맷팅하여 반환
     */
    public String getFormattedBusinessNo() {
        return formatBusinessNo(businessNo);
    }
} 