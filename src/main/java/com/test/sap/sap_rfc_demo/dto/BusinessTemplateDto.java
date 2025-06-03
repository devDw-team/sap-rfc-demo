package com.test.sap.sap_rfc_demo.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 사업자-청구서양식 조인 결과 DTO
 * BBIMCM_BSMG와 BBIMCM_INVT 테이블 조인 쿼리 결과를 담는 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessTemplateDto {
    
    // 사업자 정보
    private Long busiMgmtId;
    private String businessNo;              // 사업자번호 (파일명으로 사용)
    private String businessNm;              // 사업자명
    private String ceoNm;                   // 대표자명
    private String businessAddr;            // 사업자 주소
    private String businessType;            // 업태
    private String businessCategory;        // 종목
    
    // 섹션 표시 여부 플래그들
    private String stampYn;                 // 인감날인 사용여부
    private String csGuideYn;               // 고객센터안내 사용여부
    private String csGuide;                 // 고객센터안내 내용
    private String payInfoYn;               // 결제정보 사용여부
    private String payGuideYn;              // 결제안내정보 사용여부
    private String payGuide;                // 결제안내정보 내용
    private String prepayYn;                // 선납금정보 사용여부
    private String invoiceNoteYn;           // 청구서 비고 사용여부
    
    // 청구 항목 표시 여부 플래그들
    private String rentalYn;                // 렌탈료 사용여부
    private String membershipYn;            // 멤버십 사용여부
    private String ovdIntYn;               // 연체이자 사용여부
    private String asFeeYn;                // A/S 대금 사용여부
    private String consumableFeeYn;        // 소모품 교체비 사용여부
    private String penaltyFeeYn;           // 위약금 사용여부
    
    // 청구 상세 테이블 열 표시 여부 플래그들
    private String orderNoYn;              // 주문번호 사용여부
    private String prodGroupYn;            // 품목(제품군) 사용여부
    private String goodsNmYn;              // 제품명(모델명) 사용여부
    private String contractDateYn;         // 계약일(설치일) 사용여부
    private String useDutyMonthYn;         // 의무사용기간 사용여부
    private String contractPeriodYn;       // 약정기간 사용여부
    private String fixSupplyValueYn;       // 월 렌탈료(공급가액) 사용여부
    private String fixVatYn;               // 월 렌탈료(부가세) 사용여부
    private String fixBillAmtYn;           // 월 렌탈료(합계) 사용여부
    private String supplyValueYn;          // 당월 렌탈료(공급가액) 사용여부
    private String vatYn;                  // 당월 렌탈료(부가세) 사용여부
    private String billAmtYn;              // 당월 렌탈료(합계) 사용여부
    private String installAddrYn;          // 설치처 주소 사용여부
    private String goodsSnYn;              // 바코드 번호 사용여부
    private String deptNmYn;               // 관리지국명 사용여부
    private String deptTelNoYn;            // 관리지국 연락처 사용여부
    private String noteYn;                 // 비고 사용여부
    
    // ========== 조건부 표시 여부 판단 메서드들 ==========
    
    // 섹션 표시 여부
    public boolean isStampVisible() {
        return "Y".equals(stampYn);
    }
    
    public boolean isCsGuideVisible() {
        return "Y".equals(csGuideYn);
    }
    
    public boolean isPayInfoVisible() {
        return "Y".equals(payInfoYn);
    }
    
    public boolean isPayGuideVisible() {
        return "Y".equals(payGuideYn);
    }
    
    public boolean isPrepayVisible() {
        return "Y".equals(prepayYn);
    }
    
    public boolean isInvoiceNoteVisible() {
        return "Y".equals(invoiceNoteYn);
    }
    
    // 청구 항목 표시 여부
    public boolean isRentalVisible() {
        return "Y".equals(rentalYn);
    }
    
    public boolean isMembershipVisible() {
        return "Y".equals(membershipYn);
    }
    
    public boolean isOvdIntVisible() {
        return "Y".equals(ovdIntYn);
    }
    
    public boolean isAsFeeVisible() {
        return "Y".equals(asFeeYn);
    }
    
    public boolean isConsumableFeeVisible() {
        return "Y".equals(consumableFeeYn);
    }
    
    public boolean isPenaltyFeeVisible() {
        return "Y".equals(penaltyFeeYn);
    }
    
    // 청구 상세 테이블 열 표시 여부
    public boolean isOrderNoVisible() {
        return "Y".equals(orderNoYn);
    }
    
    public boolean isProdGroupVisible() {
        return "Y".equals(prodGroupYn);
    }
    
    public boolean isGoodsNmVisible() {
        return "Y".equals(goodsNmYn);
    }
    
    public boolean isContractDateVisible() {
        return "Y".equals(contractDateYn);
    }
    
    public boolean isUseDutyMonthVisible() {
        return "Y".equals(useDutyMonthYn);
    }
    
    public boolean isContractPeriodVisible() {
        return "Y".equals(contractPeriodYn);
    }
    
    public boolean isFixSupplyValueVisible() {
        return "Y".equals(fixSupplyValueYn);
    }
    
    public boolean isFixVatVisible() {
        return "Y".equals(fixVatYn);
    }
    
    public boolean isFixBillAmtVisible() {
        return "Y".equals(fixBillAmtYn);
    }
    
    public boolean isSupplyValueVisible() {
        return "Y".equals(supplyValueYn);
    }
    
    public boolean isVatVisible() {
        return "Y".equals(vatYn);
    }
    
    public boolean isBillAmtVisible() {
        return "Y".equals(billAmtYn);
    }
    
    public boolean isInstallAddrVisible() {
        return "Y".equals(installAddrYn);
    }
    
    public boolean isGoodsSnVisible() {
        return "Y".equals(goodsSnYn);
    }
    
    public boolean isDeptNmVisible() {
        return "Y".equals(deptNmYn);
    }
    
    public boolean isDeptTelNoVisible() {
        return "Y".equals(deptTelNoYn);
    }
    
    public boolean isNoteVisible() {
        return "Y".equals(noteYn);
    }
    
    /**
     * 표시할 컬럼의 개수를 계산 (테이블의 colspan 계산 용도)
     */
    public int getVisibleColumnCount() {
        int count = 0;
        if (isOrderNoVisible()) count++;
        if (isProdGroupVisible()) count++;
        if (isGoodsNmVisible()) count++;
        if (isContractDateVisible()) count++;
        if (isUseDutyMonthVisible()) count++;
        if (isContractPeriodVisible()) count++;
        if (isFixSupplyValueVisible()) count++;
        if (isFixVatVisible()) count++;
        if (isFixBillAmtVisible()) count++;
        if (isSupplyValueVisible()) count++;
        if (isVatVisible()) count++;
        if (isBillAmtVisible()) count++;
        if (isInstallAddrVisible()) count++;
        if (isGoodsSnVisible()) count++;
        if (isDeptNmVisible()) count++;
        if (isDeptTelNoVisible()) count++;
        if (isNoteVisible()) count++;
        return count;
    }
} 