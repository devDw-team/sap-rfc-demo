package com.test.sap.sap_rfc_demo.repository;

import com.test.sap.sap_rfc_demo.entity.BbimcmBsmg;
import com.test.sap.sap_rfc_demo.dto.BusinessTemplateDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사업자관리 Repository
 * BBIMCM_BSMG 테이블과 BBIMCM_INVT 테이블의 조인 쿼리를 포함
 */
@Repository
public interface BbimcmBsmgRepository extends JpaRepository<BbimcmBsmg, Long> {
    
    /**
     * 전체 사업자 템플릿 데이터 조회
     * BBIMCM_BSMG와 BBIMCM_INVT 테이블 조인
     */
    @Query("""
        SELECT new com.test.sap.sap_rfc_demo.dto.BusinessTemplateDto(
            bsmg.busiMgmtId, bsmg.businessNo, bsmg.businessNm,
            bsmg.ceoNm, bsmg.businessAddr, bsmg.businessType, bsmg.businessCategory,
            invt.stampYn, invt.csGuideYn, invt.csGuide, invt.payInfoYn, invt.payGuideYn,
            invt.payGuide, invt.prepayYn, invt.invoiceNoteYn, invt.rentalYn, invt.membershipYn, invt.ovdIntYn,
            invt.asFeeYn, invt.consumableFeeYn, invt.penaltyFeeYn, invt.orderNoYn,
            invt.prodGroupYn, invt.goodsNmYn, invt.contractDateYn, invt.useDutyMonthYn,
            invt.contractPeriodYn, invt.fixSupplyValueYn, invt.fixVatYn, invt.fixBillAmtYn,
            invt.supplyValueYn, invt.vatYn, invt.billAmtYn, invt.installAddrYn, invt.goodsSnYn,
            invt.deptNmYn, invt.deptTelNoYn, invt.noteYn
        )
        FROM BbimcmBsmg bsmg 
        JOIN BbimcmInvt invt ON bsmg.busiMgmtId = invt.busiMgmtId
        WHERE invt.delYn = 'N'
        ORDER BY invt.busiMgmtId
        """)
    List<BusinessTemplateDto> findAllBusinessTemplateData();
    
    /**
     * 특정 사업자 관리 ID로 템플릿 데이터 조회
     */
    @Query("""
        SELECT new com.test.sap.sap_rfc_demo.dto.BusinessTemplateDto(
            bsmg.busiMgmtId, bsmg.businessNo, bsmg.businessNm,
            bsmg.ceoNm, bsmg.businessAddr, bsmg.businessType, bsmg.businessCategory,
            invt.stampYn, invt.csGuideYn, invt.csGuide, invt.payInfoYn, invt.payGuideYn,
            invt.payGuide, invt.prepayYn, invt.invoiceNoteYn, invt.rentalYn, invt.membershipYn, invt.ovdIntYn,
            invt.asFeeYn, invt.consumableFeeYn, invt.penaltyFeeYn, invt.orderNoYn,
            invt.prodGroupYn, invt.goodsNmYn, invt.contractDateYn, invt.useDutyMonthYn,
            invt.contractPeriodYn, invt.fixSupplyValueYn, invt.fixVatYn, invt.fixBillAmtYn,
            invt.supplyValueYn, invt.vatYn, invt.billAmtYn, invt.installAddrYn, invt.goodsSnYn,
            invt.deptNmYn, invt.deptTelNoYn, invt.noteYn
        )
        FROM BbimcmBsmg bsmg 
        JOIN BbimcmInvt invt ON bsmg.busiMgmtId = invt.busiMgmtId
        WHERE bsmg.busiMgmtId = :busiMgmtId AND invt.delYn = 'N'
        """)
    Optional<BusinessTemplateDto> findBusinessTemplateDataById(@Param("busiMgmtId") Long busiMgmtId);
    
    /**
     * 특정 사업자 번호로 템플릿 데이터 조회 (새로 추가)
     * 웹 폼에서 사업자 번호로 조회할 때 사용
     */
    @Query("""
        SELECT new com.test.sap.sap_rfc_demo.dto.BusinessTemplateDto(
            bsmg.busiMgmtId, bsmg.businessNo, bsmg.businessNm,
            bsmg.ceoNm, bsmg.businessAddr, bsmg.businessType, bsmg.businessCategory,
            invt.stampYn, invt.csGuideYn, invt.csGuide, invt.payInfoYn, invt.payGuideYn,
            invt.payGuide, invt.prepayYn, invt.invoiceNoteYn, invt.rentalYn, invt.membershipYn, invt.ovdIntYn,
            invt.asFeeYn, invt.consumableFeeYn, invt.penaltyFeeYn, invt.orderNoYn,
            invt.prodGroupYn, invt.goodsNmYn, invt.contractDateYn, invt.useDutyMonthYn,
            invt.contractPeriodYn, invt.fixSupplyValueYn, invt.fixVatYn, invt.fixBillAmtYn,
            invt.supplyValueYn, invt.vatYn, invt.billAmtYn, invt.installAddrYn, invt.goodsSnYn,
            invt.deptNmYn, invt.deptTelNoYn, invt.noteYn
        )
        FROM BbimcmBsmg bsmg 
        JOIN BbimcmInvt invt ON bsmg.busiMgmtId = invt.busiMgmtId
        WHERE bsmg.businessNo = :businessNo AND invt.delYn = 'N'
        """)
    Optional<BusinessTemplateDto> findBusinessTemplateDataByBusinessNo(@Param("businessNo") String businessNo);
    
    /**
     * 사업자 번호 존재 여부 확인 (새로 추가)
     */
    @Query("SELECT COUNT(bsmg) > 0 FROM BbimcmBsmg bsmg WHERE bsmg.businessNo = :businessNo")
    boolean existsByBusinessNo(@Param("businessNo") String businessNo);
} 