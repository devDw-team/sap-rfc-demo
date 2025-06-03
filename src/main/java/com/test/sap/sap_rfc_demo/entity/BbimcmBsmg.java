package com.test.sap.sap_rfc_demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사업자관리 엔티티
 * 테이블명: BBIMCM_BSMG
 */
@Entity
@Table(name = "BBIMCM_BSMG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BbimcmBsmg {
    
    @Id
    @Column(name = "busi_mgmt_id")
    private Long busiMgmtId;                // 사업자 관리 ID (UK)
    
    @Column(name = "business_no")
    private String businessNo;              // 사업자번호 (파일명으로 사용)
    
    @Column(name = "business_nm")
    private String businessNm;              // 사업자명
    
    @Column(name = "ceo_nm")
    private String ceoNm;                   // 대표자명
    
    @Column(name = "business_addr")
    private String businessAddr;            // 사업자 주소
    
    @Column(name = "business_type")
    private String businessType;            // 업태
    
    @Column(name = "business_category")
    private String businessCategory;        // 종목
    
    // 시스템 필드 (실제 테이블에 있는 경우만 매핑)
    @Column(name = "del_yn")
    private String delYn = "N";             // 삭제여부
    
    // 아래 컬럼들이 실제 테이블에 없다면 @Transient 처리 또는 제거
    // 실제 테이블 구조에 따라 조정이 필요합니다.
    
    /* 실제 테이블에 존재하지 않는 경우 주석 처리
    @Column(name = "regid")
    private String regid;                   // 최초생성ID
    
    @Column(name = "regdate")
    private LocalDateTime regdate;          // 최초생성시각
    
    @Column(name = "updid")
    private String updid;                   // 최종변경ID
    
    @Column(name = "upddate")
    private LocalDateTime upddate;          // 최종변경시각
    */
} 