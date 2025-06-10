package com.test.sap.sap_rfc_demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 메일 데이터 JSON 구성을 위한 DTO 클래스들
 * automail-guide.md Step 2-5에 정의된 JSON 포맷 매핑
 */
public class MailDataDto {

    /**
     * 전체 메일 데이터 구조
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MailData {
        private Customer customer;
        private List<BillTypeSummary> billTypeSummary;
        private List<HtmlBill> htmlbills;
        private List<ExcelBill> excelbills;
    }

    /**
     * 고객 정보 (customer)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Customer {
        private String stcd2;           // 사업자번호
        private String custNm;          // 사업자명
        private String j1kfrepre;       // 대표자명
        private String j1kftbus;        // 업태
        private String j1kftind;        // 업종
        private String cRecpYm;         // 청구년월 (NEW)
        private String cDueDate;        // 납부기한 (NEW)
        private BigDecimal totalAmount; // 청구합계 (NEW)
        private String payComTx;        // 결제수단명
        private String payNo;           // 계좌/카드번호
        private BigDecimal preAmt;      // 선납금액
        private BigDecimal remainAmt;   // 선납잔액
        private String preMonth;        // 선납개월수
        private String invoiceNote;     // 청구서 비고 (NEW)
    }



    /**
     * 청구 유형별 요약 (bill_type_summary)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillTypeSummary {
        private String cRecpTpTx;       // 항목명 (렌탈료, 멤버십, A/S 등)
        private BigDecimal summaryCnt;  // 항목별 금액 합계
        private Long summaryAmount;     // 항목별 건수
    }

    /**
     * HTML 청구 상세 정보 (htmlbills)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HtmlBill {
        private String recpTpTx;        // 구분 (렌탈료, 멤버십, A/S 대금 등)
        private String orderNo;         // 주문번호
        private String vtext;           // 품목(제품군)
        private String goodsTx;         // 제품명(모델명)
        private String instDt;          // 계약일(설치일)
        private String useMonth;        // 사용월
        private String recpYm;          // 청구월
        private BigDecimal supplyValue; // 공급가액(원)
        private BigDecimal vat;         // 부가세(원)
        private BigDecimal billAmt;     // 합계(원)
    }

    /**
     * Excel 청구 상세 정보 (excelbills)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExcelBill {
        private String orderNo;         // 주문번호
        private String vtext;           // 품목(제품군)
        private String goodsTx;         // 제품명(모델명)
        private String useDutyMonth;    // 의무사용기간
        private String ownerDate;       // 약정기간
        private String useMonth;        // 사용월
        private String recpYm;          // 청구월
        private BigDecimal fixSupplyValue; // 월 렌탈료 (공급가액)
        private BigDecimal fixVat;      // 월 렌탈료 (부가세)
        private BigDecimal fixBillAmt;  // 월 렌탈료 (합계)
        private BigDecimal supplyValue; // 당월 렌탈료 (공급가액)
        private BigDecimal vat;         // 당월 렌탈료 (부가세)
        private BigDecimal billAmt;     // 당월 렌탈료 (합계)
        private BigDecimal membershipBillAmt; // 멤버십
        private BigDecimal asBillAmt;   // A/S 대금
        private BigDecimal consBillAmt; // 소모품 교체비
        private BigDecimal ovdBillAmt;  // 연체이자
        private BigDecimal penaltyBillAmt; // 위약금
        private String payComTx;        // 결제수단명
        private String payNo;           // 계좌/카드번호
        private BigDecimal preAmt;      // 선납금
        private BigDecimal remainAmt;   // 선납 잔여금
        private String preMonth;        // 선납 기간
        private String instJuso;        // 설치처 주소
        private String goodsSn;         // 제품 바코드
        private String deptNm;          // 관리지국명
        private String deptTelnr;       // 관리지국 연락처
        private String reqValue1;       // 요청사항 1
        private String reqValue2;       // 요청사항 2
        private String reqValue3;       // 요청사항 3
        private String reqValue4;       // 요청사항 4
        private String reqValue5;       // 요청사항 5
        private String zbigo;           // 비고
    }
} 