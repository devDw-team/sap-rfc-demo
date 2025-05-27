package com.test.sap.sap_rfc_demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.sap.sap_rfc_demo.dto.AutoMailTargetDto;
import com.test.sap.sap_rfc_demo.dto.MailDataDto;
import com.test.sap.sap_rfc_demo.entity.AutoMailData;
import com.test.sap.sap_rfc_demo.repository.AutoMailDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * B2B 자동메일 발송 서비스
 * automail-guide.md Step 1, 2에 정의된 로직 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoMailService {

    private final JdbcTemplate jdbcTemplate;
    private final AutoMailDataRepository autoMailDataRepository;
    private final ObjectMapper objectMapper;

    /**
     * Step 1: 청구서 발송 대상 조회
     * automail-guide.md Step 1에 정의된 쿼리 실행
     */
    public List<AutoMailTargetDto> getAutoMailTargets() {
        log.info("Step 1: 청구서 발송 대상 조회 시작");
        
        String sql = """
            SELECT 
                a.STCD2,    -- 사업자번호
                a.CUST_NM,  -- 고객명
                a.KUNNR,    -- 고객코드
                a.ZGRPNO,   -- 그룹번호
                a.ORDER_NO, -- 주문번호 
                a.FXDAY,    -- 고정일
                a.EMAIL,    -- 이메일 주소1
                a.EMAIL2,   -- 이메일 주소2
                COUNT(b.STCD2) AS chk_cnt -- 관련 청구 정보 건수
            FROM z_re_b2b_cust_info a
            LEFT JOIN z_re_b2b_bill_info b ON (
                b.STCD2 = a.STCD2 
                AND b.KUNNR = a.KUNNR 
                AND (b.ZGRPNO = a.ZGRPNO OR b.ORDER_NO = a.ORDER_NO)
            )
            WHERE a.SEND_AUTO = 'Y'
            GROUP BY a.STCD2, a.CUST_NM, a.KUNNR, a.ZGRPNO, a.ORDER_NO, 
                    a.FXDAY, a.EMAIL, a.EMAIL2
            """;

        List<AutoMailTargetDto> targets = jdbcTemplate.query(sql, (rs, rowNum) -> 
            AutoMailTargetDto.builder()
                .stcd2(rs.getString("STCD2"))
                .custNm(rs.getString("CUST_NM"))
                .kunnr(rs.getString("KUNNR"))
                .zgrpno(rs.getLong("ZGRPNO") == 0 ? null : rs.getLong("ZGRPNO"))
                .orderNo(rs.getString("ORDER_NO"))
                .fxday(rs.getShort("FXDAY") == 0 ? null : rs.getShort("FXDAY"))
                .email(rs.getString("EMAIL"))
                .email2(rs.getString("EMAIL2"))
                .chkCnt(rs.getLong("chk_cnt"))
                .build()
        );

        log.info("Step 1: 청구서 발송 대상 조회 완료. 총 {}건", targets.size());
        return targets;
    }

    /**
     * Step 2: 청구서 발송 대상 적재
     * automail-guide.md Step 2에 정의된 로직 실행
     */
    @Transactional
    public void processAutoMailTargets(List<AutoMailTargetDto> targets) {
        log.info("Step 2: 청구서 발송 대상 적재 시작. 대상 건수: {}", targets.size());
        
        int processedCount = 0;
        int skippedCount = 0;

        for (AutoMailTargetDto target : targets) {
            try {
                // Step 2-1: 적재 조건 확인 (chk_cnt > 0)
                if (!target.isValidForProcessing()) {
                    log.debug("적재 조건 불만족으로 스킵. STCD2: {}, KUNNR: {}, chk_cnt: {}", 
                             target.getStcd2(), target.getKunnr(), target.getChkCnt());
                    skippedCount++;
                    continue;
                }

                // 중복 데이터 체크
                long duplicateCount = autoMailDataRepository.countDuplicateData(
                    target.getStcd2(), target.getKunnr(), target.getZgrpno(), target.getOrderNo());
                
                if (duplicateCount > 0) {
                    log.debug("중복 데이터로 스킵. STCD2: {}, KUNNR: {}", target.getStcd2(), target.getKunnr());
                    skippedCount++;
                    continue;
                }

                // Step 2-3: JSON 구성 데이터 조회 및 생성
                MailDataDto.MailData mailData = createMailData(target);
                String mailDataJson = objectMapper.writeValueAsString(mailData);

                // Step 2-7: 데이터 적재
                AutoMailData autoMailData = AutoMailData.builder()
                    .sendAuto("Y")
                    .stcd2(target.getStcd2())
                    .custNm(target.getCustNm())
                    .kunnr(target.getKunnr())
                    .zgrpno(target.getZgrpno())
                    .orderNo(target.getOrderNo())
                    .fxday(target.getFxday())
                    .email(target.getEmail())
                    .email2(target.getEmail2())
                    .mailData(mailDataJson)
                    .dtCreateDate(LocalDateTime.now())
                    .fileCreateFlag("N")
                    .delFlag("N")
                    .createId("BATCH_JOB")
                    .updateId("BATCH_JOB")
                    .build();

                autoMailDataRepository.save(autoMailData);
                processedCount++;

                log.debug("데이터 적재 완료. STCD2: {}, KUNNR: {}", target.getStcd2(), target.getKunnr());

            } catch (Exception e) {
                log.error("데이터 적재 실패. STCD2: {}, KUNNR: {}", target.getStcd2(), target.getKunnr(), e);
                skippedCount++;
            }
        }

        log.info("Step 2: 청구서 발송 대상 적재 완료. 처리: {}건, 스킵: {}건", processedCount, skippedCount);
    }

    /**
     * Step 2-3: JSON 구성 데이터 생성
     * automail-guide.md Step 2-4에 정의된 4가지 데이터 조회 및 구성
     */
    private MailDataDto.MailData createMailData(AutoMailTargetDto target) {
        log.debug("JSON 구성 데이터 생성 시작. STCD2: {}, KUNNR: {}", target.getStcd2(), target.getKunnr());

        // 고객 정보 조회
        MailDataDto.Customer customer = getCustomerInfo(target);
        
        // 청구 요약 정보 조회
        MailDataDto.BillSummary billSummary = getBillSummary(target);
        
        // 청구 유형별 요약 조회
        List<MailDataDto.BillTypeSummary> billTypeSummary = getBillTypeSummary(target);
        
        // 청구 상세 정보 조회
        List<MailDataDto.Bill> bills = getBillDetails(target);

        return MailDataDto.MailData.builder()
            .customer(customer)
            .billSummary(billSummary)
            .billTypeSummary(billTypeSummary)
            .bills(bills)
            .build();
    }

    /**
     * 고객 정보 조회
     */
    private MailDataDto.Customer getCustomerInfo(AutoMailTargetDto target) {
        String sql = """
            SELECT STCD2, CUST_NM, J_1KFREPRE, J_1KFTBUS, J_1KFTIND, 
                   PAY_COM_TX, PAY_NO, PRE_AMT, REMAIN_AMT, PRE_MONTH
            FROM z_re_b2b_cust_info
            WHERE STCD2 = ? AND KUNNR = ?
            LIMIT 1
            """;

        List<MailDataDto.Customer> customers = jdbcTemplate.query(sql, 
            new Object[]{target.getStcd2(), target.getKunnr()},
            (rs, rowNum) -> MailDataDto.Customer.builder()
                .stcd2(rs.getString("STCD2"))
                .custNm(rs.getString("CUST_NM"))
                .j1kfrepre(rs.getString("J_1KFREPRE"))
                .j1kftbus(rs.getString("J_1KFTBUS"))
                .j1kftind(rs.getString("J_1KFTIND"))
                .payComTx(rs.getString("PAY_COM_TX"))
                .payNo(rs.getString("PAY_NO"))
                .preAmt(rs.getBigDecimal("PRE_AMT"))
                .remainAmt(rs.getBigDecimal("REMAIN_AMT"))
                .preMonth(rs.getString("PRE_MONTH"))
                .build()
        );

        return customers.isEmpty() ? new MailDataDto.Customer() : customers.get(0);
    }

    /**
     * 청구 요약 정보 조회
     */
    private MailDataDto.BillSummary getBillSummary(AutoMailTargetDto target) {
        String conditionSql = target.useOrderNoCondition() ? 
            "AND main.ORDER_NO = ?" : "AND main.ZGRPNO = ?";
        
        String subConditionSql = target.useOrderNoCondition() ? 
            "AND sub.ORDER_NO = ?" : "AND sub.ZGRPNO = ?";

        String sql = String.format("""
            SELECT
                RECP_YM AS C_RECP_YM,
                DUE_DATE AS C_DUE_DATE,
                SUM(SUPPLY_VALUE) + SUM(VAT) AS TOTAL_AMOUNT,
                (
                    SELECT COUNT(*)
                    FROM z_re_b2b_bill_info sub
                    WHERE sub.SEL_KUN = 'X'
                      AND sub.STCD2 = main.STCD2
                      AND sub.KUNNR = main.KUNNR
                      %s
                ) AS C_SEL_KUN_CNT
            FROM z_re_b2b_bill_info main
            WHERE main.STCD2 = ? AND main.KUNNR = ? %s
            GROUP BY RECP_YM, DUE_DATE
            """, subConditionSql, conditionSql);

        Object conditionValue = target.useOrderNoCondition() ? target.getOrderNo() : target.getZgrpno();
        
        List<MailDataDto.BillSummary> summaries = jdbcTemplate.query(sql,
            new Object[]{conditionValue, target.getStcd2(), target.getKunnr(), conditionValue},
            (rs, rowNum) -> MailDataDto.BillSummary.builder()
                .cRecpYm(rs.getString("C_RECP_YM"))
                .cDueDate(rs.getString("C_DUE_DATE"))
                .totalAmount(rs.getBigDecimal("TOTAL_AMOUNT"))
                .cSelKunCnt(rs.getLong("C_SEL_KUN_CNT"))
                .build()
        );

        return summaries.isEmpty() ? new MailDataDto.BillSummary() : summaries.get(0);
    }

    /**
     * 청구 유형별 요약 조회
     */
    private List<MailDataDto.BillTypeSummary> getBillTypeSummary(AutoMailTargetDto target) {
        String conditionSql = target.useOrderNoCondition() ? 
            "AND ORDER_NO = ?" : "AND ZGRPNO = ?";

        String sql = String.format("""
            SELECT
                RECP_TP AS C_RECP_TP,
                RECP_TP_TX AS C_RECP_TP_TX,
                COUNT(RECP_TP) AS SUMMARY_CNT,
                SUM(SUPPLY_VALUE) + SUM(VAT) AS SUMMARY_AMOUNT
            FROM z_re_b2b_bill_info
            WHERE STCD2 = ? AND KUNNR = ? %s
            GROUP BY RECP_TP, RECP_TP_TX
            """, conditionSql);

        Object conditionValue = target.useOrderNoCondition() ? target.getOrderNo() : target.getZgrpno();

        return jdbcTemplate.query(sql,
            new Object[]{target.getStcd2(), target.getKunnr(), conditionValue},
            (rs, rowNum) -> MailDataDto.BillTypeSummary.builder()
                .cRecpTp(rs.getString("C_RECP_TP"))
                .cRecpTpTx(rs.getString("C_RECP_TP_TX"))
                .summaryCnt(rs.getLong("SUMMARY_CNT"))
                .summaryAmount(rs.getBigDecimal("SUMMARY_AMOUNT"))
                .build()
        );
    }

    /**
     * 청구 상세 정보 조회
     */
    private List<MailDataDto.Bill> getBillDetails(AutoMailTargetDto target) {
        String conditionSql = target.useOrderNoCondition() ? 
            "AND ORDER_NO = ?" : "AND ZGRPNO = ?";

        String sql = String.format("""
            SELECT
                RECP_TP_TX, ORDER_NO, VTEXT, GOODS_CD, INST_DT, USE_DUTY_MONTH,
                OWNER_DATE, USE_MONTH, RECP_YM, FIX_SUPPLY_VALUE, FIX_VAT,
                FIX_BILL_AMT, SUPPLY_VALUE, VAT, BILL_AMT, PAY_COM_TX, PAY_NO,
                INST_JUSO, GOODS_SN, DEPT_CD_TX, DEPT_TELNR, ZBIGO, GOODS_TX,
                PRE_AMT, REMAIN_AMT, PRE_MONTH
            FROM z_re_b2b_bill_info
            WHERE STCD2 = ? AND KUNNR = ? %s
            """, conditionSql);

        Object conditionValue = target.useOrderNoCondition() ? target.getOrderNo() : target.getZgrpno();

        return jdbcTemplate.query(sql,
            new Object[]{target.getStcd2(), target.getKunnr(), conditionValue},
            (rs, rowNum) -> MailDataDto.Bill.builder()
                .recpTpTx(rs.getString("RECP_TP_TX"))
                .orderNo(rs.getString("ORDER_NO"))
                .vtext(rs.getString("VTEXT"))
                .goodsCd(rs.getString("GOODS_CD"))
                .instDt(rs.getString("INST_DT"))
                .useDutyMonth(rs.getString("USE_DUTY_MONTH"))
                .ownerDate(rs.getString("OWNER_DATE"))
                .useMonth(rs.getString("USE_MONTH"))
                .recpYm(rs.getString("RECP_YM"))
                .fixSupplyValue(rs.getBigDecimal("FIX_SUPPLY_VALUE"))
                .fixVat(rs.getBigDecimal("FIX_VAT"))
                .fixBillAmt(rs.getBigDecimal("FIX_BILL_AMT"))
                .supplyValue(rs.getBigDecimal("SUPPLY_VALUE"))
                .vat(rs.getBigDecimal("VAT"))
                .billAmt(rs.getBigDecimal("BILL_AMT"))
                .payComTx(rs.getString("PAY_COM_TX"))
                .payNo(rs.getString("PAY_NO"))
                .instJuso(rs.getString("INST_JUSO"))
                .goodsSn(rs.getString("GOODS_SN"))
                .deptCdTx(rs.getString("DEPT_CD_TX"))
                .deptTelnr(rs.getString("DEPT_TELNR"))
                .zbigo(rs.getString("ZBIGO"))
                .goodsTx(rs.getString("GOODS_TX"))
                .preAmt(rs.getBigDecimal("PRE_AMT"))
                .remainAmt(rs.getBigDecimal("REMAIN_AMT"))
                .preMonth(rs.getString("PRE_MONTH"))
                .build()
        );
    }

    /**
     * 전체 프로세스 실행 (Step 1 + Step 2)
     */
    @Transactional
    public void executeAutoMailProcess() {
        log.info("B2B 자동메일 프로세스 시작");
        
        try {
            // Step 1: 청구서 발송 대상 조회
            List<AutoMailTargetDto> targets = getAutoMailTargets();
            
            // Step 2: 청구서 발송 대상 적재
            processAutoMailTargets(targets);
            
            log.info("B2B 자동메일 프로세스 완료");
            
        } catch (Exception e) {
            log.error("B2B 자동메일 프로세스 실행 중 오류 발생", e);
            throw e;
        }
    }
} 