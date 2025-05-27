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
        private BillSummary billSummary;
        private List<BillTypeSummary> billTypeSummary;
        private List<Bill> bills;
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
        private String custNm;          // 고객명
        private String j1kfrepre;       // 대표자명
        private String j1kftbus;        // 업태
        private String j1kftind;        // 업종
        private String payComTx;        // 결제사명
        private String payNo;           // 결제번호
        private BigDecimal preAmt;      // 선납금액
        private BigDecimal remainAmt;   // 선납잔액
        private String preMonth;        // 선납개월수
    }

    /**
     * 청구 요약 정보 (bill_summary)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillSummary {
        private String cRecpYm;         // 청구년월
        private String cDueDate;        // 납부기한
        private BigDecimal totalAmount; // 청구합계
        private Long cSelKunCnt;        // 선택된 고객 수
    }

    /**
     * 청구 유형별 요약 (bill_type_summary)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillTypeSummary {
        private String cRecpTp;         // 청구유형
        private String cRecpTpTx;       // 청구유형명
        private Long summaryCnt;        // 유형별 건수
        private BigDecimal summaryAmount; // 유형별 합계
    }

    /**
     * 청구 상세 정보 (bills)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Bill {
        private String recpTpTx;        // 청구유형명
        private String orderNo;         // 주문번호
        private String vtext;           // 내역
        private String goodsCd;         // 제품코드
        private String instDt;          // 설치일자
        private String useDutyMonth;    // 의무사용기간
        private String ownerDate;       // 소유권도래일
        private String useMonth;        // 사용월
        private String recpYm;          // 청구년월
        private BigDecimal fixSupplyValue; // 고정 공급가액
        private BigDecimal fixVat;      // 고정 부가세
        private BigDecimal fixBillAmt;  // 고정 청구금액
        private BigDecimal supplyValue; // 공급가액
        private BigDecimal vat;         // 부가세
        private BigDecimal billAmt;     // 청구금액
        private String payComTx;        // 결제사명
        private String payNo;           // 결제번호
        private String instJuso;        // 설치처 주소
        private String goodsSn;         // 제품바코드
        private String deptCdTx;        // 관리지국명
        private String deptTelnr;       // 관리지국번호
        private String zbigo;           // 비고
        private String goodsTx;         // 자재 내역
        private BigDecimal preAmt;      // 선납금액
        private BigDecimal remainAmt;   // 선납잔액
        private String preMonth;        // 선납개월수
    }
} 