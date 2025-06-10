package com.test.sap.sap_rfc_demo.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * B2B 자동메일 발송 서비스
 * automail-guide.md Step 1, 2에 정의된 로직 구현 (NEW 쿼리 사용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoMailService {

    private final JdbcTemplate jdbcTemplate;
    private final AutoMailDataRepository autoMailDataRepository;
    private final ObjectMapper objectMapper;

    /**
     * 최적화된 ObjectMapper 생성 (JSON 크기 최소화)
     */
    private ObjectMapper createOptimizedObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper;
    }

    /**
     * Step 1: 청구서 발송 대상 조회 (NEW 쿼리)
     * automail-guide.md Step 1에 정의된 쿼리 실행
     */
    public List<AutoMailTargetDto> getAutoMailTargets() {
        log.info("Step 1: 청구서 발송 대상 조회 시작 (NEW 쿼리)");
        
        String sql = """
            SELECT 
                bsmg.busi_mgmt_id AS MGMT_ID, -- 사업자 키 정보
                bsmg.business_no AS STCD2,    -- 사업자번호
                bsmg.business_nm AS CUST_NM,  -- 고객명
                invc.cust_no AS KUNNR,        -- 고객코드
                invc.group_no AS ZGRPNO,      -- 그룹번호(묶음번호)
                invc.order_no AS ORDER_NO,    -- 주문번호
                invc.bill_date AS FXDAY,      -- 생성일 (메일 발송일)
                invc.email AS EMAIL,          -- 이메일 주소1
                invc.email2 AS EMAIL2,        -- 이메일 주소2
                invc.email3 AS EMAIL3,        -- 이메일 주소3
                invc.email4 AS EMAIL4,        -- 이메일 주소4
                invc.email5 AS EMAIL5,        -- 이메일 주소5
                invc.email6 AS EMAIL6,        -- 이메일 주소6
                invc.email7 AS EMAIL7,        -- 이메일 주소7
                invc.recp_ym AS RECP_YM,      -- 청구년월
                COUNT(invd.invc_id) AS chk_cnt -- invd 테이블에서 매칭되는 레코드 수 카운트
            FROM bbimcm_invc invc
            LEFT JOIN bbimcm_bsmg bsmg ON bsmg.busi_mgmt_id = invc.busi_mgmt_id
            LEFT JOIN bbimcd_invd invd ON invd.invc_id = invc.invc_id
                                      AND invd.cust_no = invc.cust_no
                                      AND (invd.group_no = invc.group_no OR invd.order_no = invc.order_no)
            WHERE invc.send_auto = 'Y'
            GROUP BY
                bsmg.busi_mgmt_id,
                bsmg.business_no,
                bsmg.business_nm,
                invc.cust_no,
                invc.group_no,
                invc.order_no,
                invc.bill_date,
                invc.email,
                invc.email2,
                invc.email3,
                invc.email4,
                invc.email5,
                invc.email6,
                invc.email7,
                invc.recp_ym
            ORDER BY
                bsmg.business_no, invc.cust_no
            """;

        List<AutoMailTargetDto> targets = jdbcTemplate.query(sql, (rs, rowNum) -> 
            AutoMailTargetDto.builder()
                .mgmtId(rs.getString("MGMT_ID"))
                .stcd2(rs.getString("STCD2"))
                .custNm(rs.getString("CUST_NM"))
                .kunnr(rs.getString("KUNNR"))
                .zgrpno(rs.getLong("ZGRPNO") == 0 ? null : rs.getLong("ZGRPNO"))
                .orderNo(rs.getString("ORDER_NO"))
                .fxday(rs.getShort("FXDAY") == 0 ? null : rs.getShort("FXDAY"))
                .email(rs.getString("EMAIL"))
                .email2(rs.getString("EMAIL2"))
                .email3(rs.getString("EMAIL3"))
                .email4(rs.getString("EMAIL4"))
                .email5(rs.getString("EMAIL5"))
                .email6(rs.getString("EMAIL6"))
                .email7(rs.getString("EMAIL7"))
                .recpYm(rs.getString("RECP_YM"))
                .chkCnt(rs.getLong("chk_cnt"))
                .build()
        );

        log.info("Step 1: 청구서 발송 대상 조회 완료. 총 {}건", targets.size());
        return targets;
    }

    /**
     * Step 2: 청구서 발송 대상 적재 (NEW 쿼리)
     * automail-guide.md Step 2-5~2-7에 정의된 데이터 적재 로직
     */
    @Transactional
    public void processAutoMailTargets(List<AutoMailTargetDto> targets) {
        log.info("Step 2: 청구서 발송 대상 적재 시작. 대상: {}건", targets.size());
        
        int processedCount = 0;
        int skippedCount = 0;

        for (AutoMailTargetDto target : targets) {
            try {
                boolean processed = processIndividualTarget(target);
                if (processed) {
                    processedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                log.error("데이터 적재 실패. STCD2: {}, KUNNR: {}", target.getStcd2(), target.getKunnr(), e);
                skippedCount++;
            }
        }

        log.info("Step 2: 청구서 발송 대상 적재 완료. 처리: {}건, 스킵: {}건", processedCount, skippedCount);
    }

    /**
     * 개별 대상 데이터 처리 (별도 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean processIndividualTarget(AutoMailTargetDto target) throws JsonProcessingException {
            try {
                // Step 2-1: 적재 조건 확인 (chk_cnt > 0)
                if (!target.isValidForProcessing()) {
                    log.debug("적재 조건 불만족으로 스킵. STCD2: {}, KUNNR: {}, chk_cnt: {}", 
                             target.getStcd2(), target.getKunnr(), target.getChkCnt());
                return false;
                }

                // 중복 데이터 체크
                long duplicateCount = autoMailDataRepository.countDuplicateData(
                    target.getStcd2(), target.getKunnr(), target.getZgrpno(), target.getOrderNo());
                
                if (duplicateCount > 0) {
                    log.debug("중복 데이터로 스킵. STCD2: {}, KUNNR: {}", target.getStcd2(), target.getKunnr());
                return false;
                }

                // Step 2-3: JSON 구성 데이터 조회 및 생성
                MailDataDto.MailData mailData = createMailData(target);
            ObjectMapper optimizedMapper = createOptimizedObjectMapper();
            String mailDataJson = optimizedMapper.writeValueAsString(mailData);
            
            // JSON 크기 체크 (안전을 위해 4MB 이하로 제한)
            if (mailDataJson.length() > 4 * 1024 * 1024) {
                log.warn("MAILDATA 크기가 너무 큽니다. STCD2: {}, KUNNR: {}, 크기: {}bytes", 
                        target.getStcd2(), target.getKunnr(), mailDataJson.length());
                return false;
            }

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
                .email3(target.getEmail3())
                .email4(target.getEmail4())
                .email5(target.getEmail5())
                .email6(target.getEmail6())
                .email7(target.getEmail7())
                    .recpYm(target.getRecpYm())
                    .mailData(mailDataJson)
                    .dtCreateDate(LocalDateTime.now())
                    .fileCreateFlag("N")
                    .mailSendFlag("N")
                    .delFlag("N")
                    .createId("BATCH_JOB")
                    .updateId("BATCH_JOB")
                    .build();

                autoMailDataRepository.save(autoMailData);
            log.debug("데이터 적재 완료. STCD2: {}, KUNNR: {}, MAILDATA 크기: {}bytes", 
                     target.getStcd2(), target.getKunnr(), mailDataJson.length());
            return true;

            } catch (Exception e) {
            log.error("개별 데이터 적재 중 오류 발생. STCD2: {}, KUNNR: {}", target.getStcd2(), target.getKunnr(), e);
            throw e; // 트랜잭션 롤백을 위해 다시 던짐
        }
    }

    /**
     * Step 2-3: JSON 구성 데이터 생성 (NEW 쿼리)
     * automail-guide.md Step 2-4에 정의된 4가지 데이터 조회 및 구성
     */
    private MailDataDto.MailData createMailData(AutoMailTargetDto target) throws JsonProcessingException {
        log.debug("JSON 구성 데이터 생성 시작. STCD2: {}, KUNNR: {}, MGMT_ID: {}", 
                 target.getStcd2(), target.getKunnr(), target.getMgmtId());

        // 고객 정보 조회
        MailDataDto.Customer customer = getCustomerInfo(target);
        log.debug("Customer 객체 생성 완료 - STCD2: {}, invoiceNote: '{}'", 
                 target.getStcd2(), customer.getInvoiceNote());
        
        // 청구 요약 정보 조회
        MailDataDto.BillSummary billSummary = getBillSummary(target);
        
        // 청구 유형별 요약 조회
        List<MailDataDto.BillTypeSummary> billTypeSummary = getBillTypeSummary(target);
        
        // HTML 청구 상세 정보 조회
        List<MailDataDto.HtmlBill> htmlbills = getHtmlBills(target);
        
        // Excel 청구 상세 정보 조회
        List<MailDataDto.ExcelBill> excelbills = getExcelBills(target);

        // 데이터 최적화: 빈 리스트 제거
        MailDataDto.MailData.MailDataBuilder builder = MailDataDto.MailData.builder()
            .customer(customer)
            .billSummary(billSummary);
            
        if (billTypeSummary != null && !billTypeSummary.isEmpty()) {
            builder.billTypeSummary(billTypeSummary);
        }
        
        if (htmlbills != null && !htmlbills.isEmpty()) {
            builder.htmlbills(htmlbills);
        }
        
        if (excelbills != null && !excelbills.isEmpty()) {
            builder.excelbills(excelbills);
        }
        
        MailDataDto.MailData mailData = builder.build();
        
        // JSON 크기 추정 및 로깅
        ObjectMapper optimizedMapper = createOptimizedObjectMapper();
        String jsonTest = optimizedMapper.writeValueAsString(mailData);
        log.debug("JSON 데이터 생성 완료. STCD2: {}, 예상 크기: {}bytes", target.getStcd2(), jsonTest.length());
        
        // invoiceNote 포함 여부 확인을 위한 추가 로그
        if (jsonTest.contains("invoiceNote")) {
            log.debug("JSON에 invoiceNote 필드 포함 확인됨 - STCD2: {}", target.getStcd2());
        } else {
            log.warn("JSON에 invoiceNote 필드가 누락됨 - STCD2: {}, customer.invoiceNote: '{}'", 
                    target.getStcd2(), customer.getInvoiceNote());
        }
        
        return mailData;
    }

    /**
     * 고객 정보 조회 (NEW 쿼리)
     */
    private MailDataDto.Customer getCustomerInfo(AutoMailTargetDto target) {
        String sql = """
            SELECT
                bsmg.business_no AS STCD2,          -- 사업자 번호
                bsmg.business_nm AS CUST_NM,        -- 사업자명
                bsmg.ceo_nm AS J_1KFREPRE,          -- 대표자명
                bsmg.business_type AS J_1KFTBUS,    -- 업태
                bsmg.business_category AS J_1KFTIND,-- 업종
                invc.pay_com_nm AS PAY_COM_TX,      -- 결제수단명
                invc.pay_no AS PAY_NO,              -- 계좌/카드번호
                invc.prepay_amt AS PRE_AMT,         -- 선납금액
                invc.prepay_remain AS REMAIN_AMT,   -- 선납잔액
                invc.prepay_month AS PRE_MONTH,     -- 선납개월수
                invc.invoice_note AS INVOICE_NOTE   -- 청구서 비고
            FROM
                bbimcm_bsmg bsmg
            LEFT JOIN
                bbimcm_invc invc
            ON
                bsmg.busi_mgmt_id = invc.busi_mgmt_id
                AND invc.cust_no = ?
            WHERE
                bsmg.business_no = ?
            LIMIT 1
            """;

        List<MailDataDto.Customer> customers = jdbcTemplate.query(sql, 
            new Object[]{target.getKunnr(), target.getStcd2()},
            (rs, rowNum) -> {
                String invoiceNote = rs.getString("INVOICE_NOTE");
                log.debug("Customer invoiceNote 조회 결과 - STCD2: {}, KUNNR: {}, invoiceNote: '{}'", 
                         target.getStcd2(), target.getKunnr(), invoiceNote);
                
                return MailDataDto.Customer.builder()
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
                    .invoiceNote(invoiceNote != null ? invoiceNote : "") // null을 빈 문자열로 변환
                    .build();
            }
        );

        MailDataDto.Customer result = customers.isEmpty() ? MailDataDto.Customer.builder().invoiceNote("").build() : customers.get(0);
        log.debug("Customer 최종 결과 - STCD2: {}, invoiceNote: '{}'", target.getStcd2(), result.getInvoiceNote());
        
        return result;
    }

    /**
     * 청구 요약 정보 조회 (NEW 쿼리)
     */
    private MailDataDto.BillSummary getBillSummary(AutoMailTargetDto target) {
        String sql = """
            SELECT
                invc.recp_ym AS C_RECP_YM,          -- 청구년월
                invc.due_date AS C_DUE_DATE,        -- 납부기한
                invc.bill_total_amt AS TOTAL_AMOUNT -- 청구합계
            FROM
                bbimcm_bsmg bsmg
            LEFT JOIN
                bbimcm_invc invc
            ON
                bsmg.busi_mgmt_id = invc.busi_mgmt_id
                AND invc.cust_no = ?
            WHERE
                bsmg.business_no = ?
            LIMIT 1
            """;

        List<MailDataDto.BillSummary> billSummaries = jdbcTemplate.query(sql, 
            new Object[]{target.getKunnr(), target.getStcd2()},
            (rs, rowNum) -> MailDataDto.BillSummary.builder()
                .cRecpYm(rs.getString("C_RECP_YM"))
                .cDueDate(rs.getString("C_DUE_DATE"))
                .totalAmount(rs.getBigDecimal("TOTAL_AMOUNT"))
                .build()
        );

        return billSummaries.isEmpty() ? new MailDataDto.BillSummary() : billSummaries.get(0);
    }

    /**
     * 청구 유형별 요약 조회 (NEW 쿼리)
     */
    private List<MailDataDto.BillTypeSummary> getBillTypeSummary(AutoMailTargetDto target) {
        String sql = """
            SELECT t.item_nm AS C_RECP_TP_TX,    -- 항목명
                  COUNT(*) AS SUMMARY_CNT,       -- 항목별 건수
                  SUM(t.amount) AS SUMMARY_AMOUNT -- 항목별 금액 합계
              FROM (
                    SELECT '렌탈료' AS item_nm,
                          invd.bill_amt AS amount
                      FROM bbimcd_invd invd, bbimcm_invc invc
                      WHERE invd.invc_id = invc.invc_id
                      AND   invc.busi_mgmt_id = ?
                      AND   invc.cust_no  = ?
                    UNION ALL
                    SELECT '멤버십' AS item_nm,
                          invd.membership_bill_amt AS amount
                      FROM bbimcd_invd invd, bbimcm_invc invc
                      WHERE invd.invc_id = invc.invc_id
                      AND   invc.busi_mgmt_id = ?
                      AND   invc.cust_no  = ? 
                      AND   invd.membership_bill_amt > 0
                    UNION ALL
                    SELECT 'A/S' AS item_nm,
                          invd.as_bill_amt AS amount
                      FROM bbimcd_invd invd, bbimcm_invc invc
                      WHERE invd.invc_id = invc.invc_id
                      AND   invc.busi_mgmt_id = ?
                      AND   invc.cust_no  = ? 
                      AND   invd.as_bill_amt > 0
                    UNION ALL
                    SELECT '소모품' AS item_nm,
                          invd.cons_bill_amt AS amount
                      FROM bbimcd_invd invd, bbimcm_invc invc
                      WHERE invd.invc_id = invc.invc_id
                      AND   invc.busi_mgmt_id = ?
                      AND   invc.cust_no  = ? 
                      AND   invd.cons_bill_amt > 0
                    UNION ALL
                    SELECT '연체이자' AS item_nm,
                          invd.ovd_bill_amt AS amount
                      FROM bbimcd_invd invd, bbimcm_invc invc
                      WHERE invd.invc_id = invc.invc_id
                      AND   invc.busi_mgmt_id = ?
                      AND   invc.cust_no  = ? 
                      AND   invd.ovd_bill_amt > 0
                    UNION ALL
                    SELECT '위약금' AS item_nm,
                          invd.penalty_bill_amt AS amount
                      FROM bbimcd_invd invd, bbimcm_invc invc
                      WHERE invd.invc_id = invc.invc_id
                      AND   invc.busi_mgmt_id = ?
                      AND   invc.cust_no  = ? 
                      AND   invd.penalty_bill_amt > 0
                  ) t
            GROUP BY t.item_nm
            ORDER BY t.item_nm
            """;

        return jdbcTemplate.query(sql,
            new Object[]{target.getMgmtId(), target.getKunnr(), 
                        target.getMgmtId(), target.getKunnr(),
                        target.getMgmtId(), target.getKunnr(),
                        target.getMgmtId(), target.getKunnr(),
                        target.getMgmtId(), target.getKunnr(),
                        target.getMgmtId(), target.getKunnr()},
            (rs, rowNum) -> MailDataDto.BillTypeSummary.builder()
                .cRecpTpTx(rs.getString("C_RECP_TP_TX"))
                .summaryCnt(rs.getLong("SUMMARY_CNT"))
                .summaryAmount(rs.getBigDecimal("SUMMARY_AMOUNT"))
                .build()
        );
    }

    /**
     * HTML 청구 상세 정보 조회 (NEW 쿼리)
     */
    private List<MailDataDto.HtmlBill> getHtmlBills(AutoMailTargetDto target) {
        String sql = """
            SELECT t.item_nm AS RECP_TP_TX,
                    t.order_no AS ORDER_NO,
                    t.prod_group AS VTEXT,
                    t.goods_nm AS GOODS_TX,
                    t.contact_date AS INST_DT,
                    t.use_month AS USE_MONTH,
                    t.recp_ym AS RECP_YM,
                    t.supply_value AS SUPPLY_VALUE,
                    t.vat AS VAT,
                    t.bill_amt AS BILL_AMT
            FROM (
            SELECT '렌탈료' AS item_nm, invd.order_no, invd.prod_group, invd.goods_nm, invd.contact_date, invd.use_month, invd.recp_ym, invd.supply_value, invd.vat, invd.bill_amt 
            FROM bbimcd_invd invd, bbimcm_invc invc
            WHERE invd.invc_id = invc.invc_id
            AND   invc.busi_mgmt_id = ?
            AND   invc.cust_no  = ? 
            UNION ALL
            SELECT '멤버십' AS item_nm, invd.order_no, invd.prod_group, invd.goods_nm, invd.contact_date, invd.use_month, invd.recp_ym, invd.membership_supply_value, invd.membership_vat, invd.membership_bill_amt 
            FROM bbimcd_invd invd, bbimcm_invc invc
            WHERE invd.invc_id = invc.invc_id
            AND   invc.busi_mgmt_id = ?
            AND   invc.cust_no  = ?  
            AND   invd.membership_bill_amt > 0 
            UNION ALL
            SELECT 'A/S 대금' AS item_nm, invd.order_no, invd.prod_group, invd.goods_nm, invd.contact_date, invd.use_month, invd.recp_ym,invd.as_supply_value, invd.as_vat, invd.as_bill_amt 
            FROM bbimcd_invd invd, bbimcm_invc invc
            WHERE invd.invc_id = invc.invc_id
            AND   invc.busi_mgmt_id = ?
            AND   invc.cust_no  = ? 
            AND   invd.as_bill_amt > 0
            UNION ALL
            SELECT '소모품' AS item_nm, invd.order_no, invd.prod_group, invd.goods_nm, invd.contact_date, invd.use_month, invd.recp_ym,invd.cons_supply_value, invd.cons_vat, invd.cons_bill_amt
            FROM bbimcd_invd invd, bbimcm_invc invc
            WHERE invd.invc_id = invc.invc_id
            AND   invc.busi_mgmt_id = ?
            AND   invc.cust_no  = ?  
            AND   invd.cons_bill_amt > 0
            UNION ALL
            SELECT '연체이자' AS item_nm, invd.order_no, invd.prod_group, invd.goods_nm, invd.contact_date, invd.use_month, invd.recp_ym,invd.ovd_supply_value, invd.ovd_vat, invd.ovd_bill_amt
            FROM bbimcd_invd invd, bbimcm_invc invc
            WHERE invd.invc_id = invc.invc_id
            AND   invc.busi_mgmt_id = ?
            AND   invc.cust_no  = ? 
            AND   invd.ovd_bill_amt > 0 
            UNION ALL
            SELECT '위약금' AS item_nm, invd.order_no, invd.prod_group, invd.goods_nm, invd.contact_date, invd.use_month, invd.recp_ym,invd.penalty_supply_value, invd.penalty_vat, invd.penalty_bill_amt
            FROM bbimcd_invd invd, bbimcm_invc invc
            WHERE invd.invc_id = invc.invc_id
            AND   invc.busi_mgmt_id = ?
            AND   invc.cust_no  = ? 
            AND   invd.penalty_bill_amt > 0   
            ) t
            ORDER BY t.order_no
            """;

        return jdbcTemplate.query(sql,
            new Object[]{target.getMgmtId(), target.getKunnr(),
                        target.getMgmtId(), target.getKunnr(),
                        target.getMgmtId(), target.getKunnr(),
                        target.getMgmtId(), target.getKunnr(),
                        target.getMgmtId(), target.getKunnr(),
                        target.getMgmtId(), target.getKunnr()},
            (rs, rowNum) -> MailDataDto.HtmlBill.builder()
                .recpTpTx(rs.getString("RECP_TP_TX"))
                .orderNo(rs.getString("ORDER_NO"))
                .vtext(rs.getString("VTEXT"))
                .goodsTx(rs.getString("GOODS_TX"))
                .instDt(rs.getString("INST_DT"))
                .useMonth(rs.getString("USE_MONTH"))
                .recpYm(rs.getString("RECP_YM"))
                .supplyValue(rs.getBigDecimal("SUPPLY_VALUE"))
                .vat(rs.getBigDecimal("VAT"))
                .billAmt(rs.getBigDecimal("BILL_AMT"))
                .build()
        );
    }

    /**
     * Excel 청구 상세 정보 조회 (NEW 쿼리)
     */
    private List<MailDataDto.ExcelBill> getExcelBills(AutoMailTargetDto target) {
        String sql = """
            SELECT invd.order_no AS ORDER_NO,
                    invd.prod_group AS VTEXT,
                    invd.goods_nm AS GOODS_TX,
                    invd.use_duty_month AS USE_DUTY_MONTH,
                    invd.owner_date AS OWNER_DATE,
                    invd.use_month AS USE_MONTH,
                    invd.recp_ym AS RECP_YM,
                    invd.fix_supply_value AS FIX_SUPPLY_VALUE,
                    invd.fix_vat AS FIX_VAT,
                    invd.fix_bill_amt AS FIX_BILL_AMT,
                    invd.supply_value AS SUPPLY_VALUE,
                    invd.vat AS VAT,
                    invd.bill_amt AS BILL_AMT,
                    invd.membership_bill_amt AS MEMBERSHIP_BILL_AMT,
                    invd.as_bill_amt AS AS_BILL_AMT,
                    invd.cons_bill_amt AS CONS_BILL_AMT,
                    invd.ovd_bill_amt AS OVD_BILL_AMT,
                    invd.penalty_bill_amt AS PENALTY_BILL_AMT,
                    invd.pay_com_nm AS PAY_COM_TX,
                    invd.pay_no AS PAY_NO,
                    invd.prepay_amt AS PRE_AMT,
                    invd.prepay_remain AS REMAIN_AMT,
                    invd.prepay_month AS PRE_MONTH,
                    invd.install_addr AS INST_JUSO,
                    invd.goods_sn AS GOODS_SN,
                    invd.dept_nm AS DEPT_NM,
                    invd.dept_tel_no AS DEPT_TELNR,
                    invd.req_value1 AS REQ_VALUE1,
                    invd.req_value2 AS REQ_VALUE2,
                    invd.req_value3 AS REQ_VALUE3,
                    invd.req_value4 AS REQ_VALUE4,
                    invd.req_value5 AS REQ_VALUE5,
                    invd.note AS ZBIGO,
                    invd.contact_date AS INST_DT      -- 계약일(설치일)
            FROM bbimcd_invd invd, bbimcm_invc invc
            WHERE invd.invc_id = invc.invc_id
            AND   invc.busi_mgmt_id = ?
            AND   invc.cust_no  = ? 
            ORDER BY invd.order_no
            """;

        return jdbcTemplate.query(sql,
            new Object[]{target.getMgmtId(), target.getKunnr()},
            (rs, rowNum) -> MailDataDto.ExcelBill.builder()
                .orderNo(rs.getString("ORDER_NO"))
                .vtext(rs.getString("VTEXT"))
                .goodsTx(rs.getString("GOODS_TX"))
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
                .membershipBillAmt(rs.getBigDecimal("MEMBERSHIP_BILL_AMT"))
                .asBillAmt(rs.getBigDecimal("AS_BILL_AMT"))
                .consBillAmt(rs.getBigDecimal("CONS_BILL_AMT"))
                .ovdBillAmt(rs.getBigDecimal("OVD_BILL_AMT"))
                .penaltyBillAmt(rs.getBigDecimal("PENALTY_BILL_AMT"))
                .payComTx(rs.getString("PAY_COM_TX"))
                .payNo(rs.getString("PAY_NO"))
                .preAmt(rs.getBigDecimal("PRE_AMT"))
                .remainAmt(rs.getBigDecimal("REMAIN_AMT"))
                .preMonth(rs.getString("PRE_MONTH"))
                .instJuso(rs.getString("INST_JUSO"))
                .goodsSn(rs.getString("GOODS_SN"))
                .deptNm(rs.getString("DEPT_NM"))
                .deptTelnr(rs.getString("DEPT_TELNR"))
                .reqValue1(rs.getString("REQ_VALUE1"))
                .reqValue2(rs.getString("REQ_VALUE2"))
                .reqValue3(rs.getString("REQ_VALUE3"))
                .reqValue4(rs.getString("REQ_VALUE4"))
                .reqValue5(rs.getString("REQ_VALUE5"))
                .zbigo(rs.getString("ZBIGO"))
                .instDt(rs.getString("INST_DT"))
                .build()
        );
    }

    /**
     * 전체 프로세스 실행 (Step 1 + Step 2)
     */
    @Transactional
    public void executeAutoMailProcess() {
        log.info("B2B 자동메일 프로세스 시작 (NEW 쿼리)");
        
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