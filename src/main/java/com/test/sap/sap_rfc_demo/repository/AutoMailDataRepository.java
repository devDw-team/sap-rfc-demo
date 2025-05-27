package com.test.sap.sap_rfc_demo.repository;

import com.test.sap.sap_rfc_demo.entity.AutoMailData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * B2B 자동메일 발송 대상 Repository
 * automail-guide.md에 정의된 데이터 조회 및 관리 기능 제공
 */
@Repository
public interface AutoMailDataRepository extends JpaRepository<AutoMailData, Long> {

    /**
     * 자동메일 플래그가 'Y'인 데이터 조회
     */
    List<AutoMailData> findBySendAutoAndDelFlag(String sendAuto, String delFlag);

    /**
     * 파일 생성 플래그가 'N'인 데이터 조회 (파일 생성 대상)
     */
    List<AutoMailData> findByFileCreateFlagAndDelFlag(String fileCreateFlag, String delFlag);

    /**
     * 사업자번호와 고객코드로 조회
     */
    List<AutoMailData> findByStcd2AndKunnrAndDelFlag(String stcd2, String kunnr, String delFlag);

    /**
     * 묶음번호로 조회
     */
    List<AutoMailData> findByZgrpnoAndDelFlag(Long zgrpno, String delFlag);

    /**
     * 주문번호로 조회
     */
    List<AutoMailData> findByOrderNoAndDelFlag(String orderNo, String delFlag);

    /**
     * 데이터 수집일 기준 조회
     */
    List<AutoMailData> findByDtCreateDateBetweenAndDelFlag(
            LocalDateTime startDate, LocalDateTime endDate, String delFlag);

    /**
     * 중복 데이터 체크 (사업자번호, 고객코드, 묶음번호, 주문번호 기준)
     */
    @Query("SELECT COUNT(a) FROM AutoMailData a WHERE a.stcd2 = :stcd2 AND a.kunnr = :kunnr " +
           "AND (a.zgrpno = :zgrpno OR a.orderNo = :orderNo) AND a.delFlag = 'N'")
    long countDuplicateData(@Param("stcd2") String stcd2, 
                           @Param("kunnr") String kunnr,
                           @Param("zgrpno") Long zgrpno, 
                           @Param("orderNo") String orderNo);

    /**
     * 메일 발송 대상 조회 (파일 생성 완료된 데이터)
     */
    @Query("SELECT a FROM AutoMailData a WHERE a.fileCreateFlag = 'Y' " +
           "AND a.delFlag = 'N' AND (a.umsCode IS NULL OR a.umsCode = '')")
    List<AutoMailData> findMailSendTargets();

    /**
     * 배치 처리 대상 조회 (오늘 생성된 데이터 중 파일 미생성)
     */
    @Query("SELECT a FROM AutoMailData a WHERE DATE(a.dtCreateDate) = CURRENT_DATE " +
           "AND a.fileCreateFlag = 'N' AND a.delFlag = 'N'")
    List<AutoMailData> findTodayBatchTargets();
} 