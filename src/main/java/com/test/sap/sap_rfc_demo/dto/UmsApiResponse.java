package com.test.sap.sap_rfc_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UMS API 응답 DTO
 * umsmail-guide.md Step 4-2에 정의된 UMS API 응답 형식을 위한 클래스
 * 
 * 응답 예시: {"code": "13", "msg": "요청성공", "key": "248395725"}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UmsApiResponse {
    
    /**
     * 발송 성공 여부 (code가 "13"인 경우 true)
     */
    private boolean success;
    
    /**
     * UMS API 응답 코드
     */
    private String code;
    
    /**
     * UMS API 응답 메시지
     */
    private String message;
    
    /**
     * UMS API 응답 키 (발송 성공 시 반환되는 고유 키)
     */
    private String key;
} 