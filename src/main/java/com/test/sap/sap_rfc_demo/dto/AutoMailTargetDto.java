package com.test.sap.sap_rfc_demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 청구서 발송 대상 조회 DTO
 * automail-guide.md Step 1에 정의된 조회 결과 매핑
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoMailTargetDto {
    
    private String stcd2;        // 사업자번호
    private String custNm;       // 고객명
    private String kunnr;        // 고객코드
    private Long zgrpno;         // 그룹번호
    private String orderNo;      // 주문번호
    private Short fxday;         // 고정일
    private String email;        // 이메일 주소1
    private String email2;       // 이메일 주소2
    private String recpYm;       // 청구년월
    private Long chkCnt;         // 관련 청구 정보 건수
    
    /**
     * 적재 조건 확인 (chk_cnt > 0)
     */
    public boolean isValidForProcessing() {
        return chkCnt != null && chkCnt > 0;
    }
    
    /**
     * ZGRPNO 조건 분기 확인
     * ZGRPNO가 0이거나 NULL인 경우 ORDER_NO 기준으로 처리
     */
    public boolean useOrderNoCondition() {
        return zgrpno == null || zgrpno == 0;
    }
    
    /**
     * ZGRPNO 조건 분기 확인
     * ZGRPNO가 유효한 값을 가지는 경우 ZGRPNO 기준으로 처리
     */
    public boolean useZgrpnoCondition() {
        return zgrpno != null && zgrpno != 0;
    }
} 