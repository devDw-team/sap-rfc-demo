package com.test.sap.sap_rfc_demo.repository;

import com.test.sap.sap_rfc_demo.entity.BbimcmInvt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 청구서양식관리 Repository
 * BBIMCM_INVT 테이블 관련 Repository
 */
@Repository
public interface BbimcmInvtRepository extends JpaRepository<BbimcmInvt, Long> {
    
    /**
     * 사업자 관리 ID로 청구서양식 조회
     */
    BbimcmInvt findByBusiMgmtIdAndDelYn(Long busiMgmtId, String delYn);
    
    /**
     * 사업자 관리 ID로 청구서양식 존재 여부 확인
     */
    boolean existsByBusiMgmtIdAndDelYn(Long busiMgmtId, String delYn);
} 