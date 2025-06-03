package com.test.sap.sap_rfc_demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 청구서양식관리 엔티티
 * 테이블명: BBIMCM_INVT
 */
@Entity
@Table(name = "BBIMCM_INVT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BbimcmInvt {
    
    @Id
    @Column(name = "template_id")
    private Long templateId;                // 청구서 양식 ID (PK)
    
    @Column(name = "busi_mgmt_id", unique = true)
    private Long busiMgmtId;               // 사업자 관리 ID (UK)
    
    // 섹션 표시 여부
    @Column(name = "stamp_yn")
    private String stampYn;                 // 인감날인 사용여부
    
    @Column(name = "cs_guide_yn")
    private String csGuideYn;               // 고객센터안내 사용여부
    
    @Column(name = "cs_guide")
    private String csGuide;                 // 고객센터안내 내용
    
    @Column(name = "pay_info_yn")
    private String payInfoYn;               // 결제정보 사용여부
    
    @Column(name = "pay_guide_yn")
    private String payGuideYn;              // 결제안내정보 사용여부
    
    @Column(name = "pay_guide")
    private String payGuide;                // 결제안내정보 내용
    
    @Column(name = "prepay_yn")
    private String prepayYn;                // 선납금정보 사용여부
    
    @Column(name = "invoice_note_yn")
    private String invoiceNoteYn;           // 청구서 비고 사용여부
    
    // 청구 항목 표시 여부
    @Column(name = "rental_yn")
    private String rentalYn;                // 렌탈료 사용여부
    
    @Column(name = "membership_yn")
    private String membershipYn;            // 멤버십 사용여부
    
    @Column(name = "ovd_int_yn")
    private String ovdIntYn;               // 연체이자 사용여부
    
    @Column(name = "as_fee_yn")
    private String asFeeYn;                // A/S 대금 사용여부
    
    @Column(name = "consumable_fee_yn")
    private String consumableFeeYn;        // 소모품 교체비 사용여부
    
    @Column(name = "penalty_fee_yn")
    private String penaltyFeeYn;           // 위약금 사용여부
    
    // 청구 상세 테이블 열 표시 여부
    @Column(name = "order_no_yn")
    private String orderNoYn;              // 주문번호 사용여부
    
    @Column(name = "prod_group_yn")
    private String prodGroupYn;            // 품목(제품군) 사용여부
    
    @Column(name = "goods_nm_yn")
    private String goodsNmYn;              // 제품명(모델명) 사용여부
    
    @Column(name = "contract_date_yn")
    private String contractDateYn;         // 계약일(설치일) 사용여부
    
    @Column(name = "use_duty_month_yn")
    private String useDutyMonthYn;         // 의무사용기간 사용여부
    
    @Column(name = "contract_period_yn")
    private String contractPeriodYn;       // 약정기간 사용여부
    
    @Column(name = "fix_supply_value_yn")
    private String fixSupplyValueYn;       // 월 렌탈료(공급가액) 사용여부
    
    @Column(name = "fix_vat_yn")
    private String fixVatYn;               // 월 렌탈료(부가세) 사용여부
    
    @Column(name = "fix_bill_amt_yn")
    private String fixBillAmtYn;           // 월 렌탈료(합계) 사용여부
    
    @Column(name = "supply_value_yn")
    private String supplyValueYn;          // 당월 렌탈료(공급가액) 사용여부
    
    @Column(name = "vat_yn")
    private String vatYn;                  // 당월 렌탈료(부가세) 사용여부
    
    @Column(name = "bill_amt_yn")
    private String billAmtYn;              // 당월 렌탈료(합계) 사용여부
    
    @Column(name = "install_addr_yn")
    private String installAddrYn;          // 설치처 주소 사용여부
    
    @Column(name = "goods_sn_yn")
    private String goodsSnYn;              // 바코드 번호 사용여부
    
    @Column(name = "dept_nm_yn")
    private String deptNmYn;               // 관리지국명 사용여부
    
    @Column(name = "dept_tel_no_yn")
    private String deptTelNoYn;            // 관리지국 연락처 사용여부
    
    @Column(name = "note_yn")
    private String noteYn;                 // 비고 사용여부
    
    // 시스템 필드 (실제 테이블에 있는 경우만 매핑)
    @Column(name = "del_yn")
    private String delYn = "N";            // 삭제여부
    
    // 아래 컬럼들이 실제 테이블에 없다면 주석 처리
    /* 실제 테이블에 존재하지 않는 경우 주석 처리
    @Column(name = "regid")
    private String regid;                  // 최초생성ID
    
    @Column(name = "regdate")
    private LocalDateTime regdate;         // 최초생성시각
    
    @Column(name = "updid")
    private String updid;                  // 최종변경ID
    
    @Column(name = "upddate")
    private LocalDateTime upddate;         // 최종변경시각
    */
} 