package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.dto.EmailSendRequest;
import com.test.sap.sap_rfc_demo.dto.UmsApiResponse;
import com.test.sap.sap_rfc_demo.entity.AutoMailData;
import com.test.sap.sap_rfc_demo.repository.AutoMailDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 메일 발송 서비스 (Step 4 구현)
 * umsmail-guide.md Step 4: 메일 발송 및 결과 업데이트 로직 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailSendService {

    private final JdbcTemplate jdbcTemplate;
    private final AutoMailDataRepository autoMailDataRepository;
    private final TemplateService templateService;
    private final EmailService emailService;

    @Value("${email.attachment.base-url}")
    private String attachmentBaseUrl;

    // 이메일 주소 유효성 검사를 위한 정규식
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // 날짜 포맷터 (YYYY.MM.DD 시:분:초)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    /**
     * Step 4-1: 메일 발송 대상 조회 (전체/배치 발송용)
     * umsmail-guide.md에 정의된 개선된 SQL 쿼리 적용 (여러 이메일 필드를 UNION으로 처리)
     * FXDAY 조건 포함 - 오늘이 발송일인 고객만 조회
     */
    public List<AutoMailData> getMailSendTargets() {
        log.info("Step 4-1: 메일 발송 대상 조회 시작 (전체/배치 발송용)");

        String sql = """
            WITH base_data AS (
                SELECT
                    SEQ, STCD2, CUST_NM, RECP_YM, 
                    SUBSTRING(RECP_YM, 1, 4) AS C_RECP_YEAR, 
                    CAST(SUBSTRING(RECP_YM, 5, 2) AS UNSIGNED) AS C_RECP_MONTH,
                    EMAIL, EMAIL2, EMAIL3, EMAIL4, EMAIL5, EMAIL6, EMAIL7, 
                    ORI_HTML_FILENM, CHG_HTML_FILENM, HTML_FILEPATH, FXDAY
                FROM b2b_automail_dt
                WHERE SEND_AUTO = 'Y' 
                  AND FILE_CREATE_FLAG = 'Y' 
                  AND MAIL_SEND_FLAG = 'N' 
                  AND FXDAY = DAY(CURDATE())
            )
            SELECT SEQ, STCD2, CUST_NM, RECP_YM, C_RECP_YEAR, C_RECP_MONTH, EMAIL AS EMAIL, ORI_HTML_FILENM, CHG_HTML_FILENM, HTML_FILEPATH, FXDAY FROM base_data WHERE EMAIL IS NOT NULL AND EMAIL != ''
            UNION ALL
            SELECT SEQ, STCD2, CUST_NM, RECP_YM, C_RECP_YEAR, C_RECP_MONTH, EMAIL2 AS EMAIL, ORI_HTML_FILENM, CHG_HTML_FILENM, HTML_FILEPATH, FXDAY FROM base_data WHERE EMAIL2 IS NOT NULL AND EMAIL2 != ''
            UNION ALL
            SELECT SEQ, STCD2, CUST_NM, RECP_YM, C_RECP_YEAR, C_RECP_MONTH, EMAIL3 AS EMAIL, ORI_HTML_FILENM, CHG_HTML_FILENM, HTML_FILEPATH, FXDAY FROM base_data WHERE EMAIL3 IS NOT NULL AND EMAIL3 != ''
            UNION ALL
            SELECT SEQ, STCD2, CUST_NM, RECP_YM, C_RECP_YEAR, C_RECP_MONTH, EMAIL4 AS EMAIL, ORI_HTML_FILENM, CHG_HTML_FILENM, HTML_FILEPATH, FXDAY FROM base_data WHERE EMAIL4 IS NOT NULL AND EMAIL4 != ''
            UNION ALL
            SELECT SEQ, STCD2, CUST_NM, RECP_YM, C_RECP_YEAR, C_RECP_MONTH, EMAIL5 AS EMAIL, ORI_HTML_FILENM, CHG_HTML_FILENM, HTML_FILEPATH, FXDAY FROM base_data WHERE EMAIL5 IS NOT NULL AND EMAIL5 != ''
            UNION ALL
            SELECT SEQ, STCD2, CUST_NM, RECP_YM, C_RECP_YEAR, C_RECP_MONTH, EMAIL6 AS EMAIL, ORI_HTML_FILENM, CHG_HTML_FILENM, HTML_FILEPATH, FXDAY FROM base_data WHERE EMAIL6 IS NOT NULL AND EMAIL6 != ''
            UNION ALL
            SELECT SEQ, STCD2, CUST_NM, RECP_YM, C_RECP_YEAR, C_RECP_MONTH, EMAIL7 AS EMAIL, ORI_HTML_FILENM, CHG_HTML_FILENM, HTML_FILEPATH, FXDAY FROM base_data WHERE EMAIL7 IS NOT NULL AND EMAIL7 != ''
            ORDER BY SEQ, EMAIL
            """;

        List<AutoMailData> targets = jdbcTemplate.query(sql, (rs, rowNum) -> {
            AutoMailData data = new AutoMailData();
            data.setSeq(rs.getLong("SEQ"));
            data.setStcd2(rs.getString("STCD2"));
            data.setCustNm(rs.getString("CUST_NM"));
            data.setRecpYm(rs.getString("RECP_YM"));
            data.setEmail(rs.getString("EMAIL"));
            data.setOriHtmlFilenm(rs.getString("ORI_HTML_FILENM"));
            data.setChgHtmlFilenm(rs.getString("CHG_HTML_FILENM"));
            data.setHtmlFilepath(rs.getString("HTML_FILEPATH"));
            data.setFxday(rs.getShort("FXDAY"));
            return data;
        });

        log.info("Step 4-1: 메일 발송 대상 조회 완료 (전체/배치 발송용). 총 {}건", targets.size());
        return targets;
    }

    /**
     * Step 4-1: 특정 SEQ 메일 발송 대상 조회 (개별 발송용)
     * 수동 발송을 위해 특정 SEQ의 데이터만 조회
     * FXDAY 조건 없음 - 수동 발송이므로 발송일과 관계없이 조회
     */
    public AutoMailData getMailSendTargetBySeq(Long seq) {
        log.info("Step 4-1: 특정 SEQ 메일 발송 대상 조회 시작 (개별 발송용) - SEQ: {}", seq);

        String sql = """
            SELECT
                SEQ, STCD2, CUST_NM, RECP_YM,
                SUBSTRING(RECP_YM, 1, 4) AS C_RECP_YEAR,
                CAST(SUBSTRING(RECP_YM, 5, 2) AS UNSIGNED) AS C_RECP_MONTH,
                EMAIL, EMAIL2, ORI_HTML_FILENM, CHG_HTML_FILENM,
                HTML_FILEPATH, FXDAY
            FROM b2b_automail_dt
            WHERE SEQ = ?
                AND SEND_AUTO = 'Y'
                AND FILE_CREATE_FLAG = 'Y'
                -- MAIL_SEND_FLAG는 체크하지 않음 (수동 발송이므로 재발송 허용)
            """;

        List<AutoMailData> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            AutoMailData data = new AutoMailData();
            data.setSeq(rs.getLong("SEQ"));
            data.setStcd2(rs.getString("STCD2"));
            data.setCustNm(rs.getString("CUST_NM"));
            data.setRecpYm(rs.getString("RECP_YM"));
            data.setEmail(rs.getString("EMAIL"));
            data.setEmail2(rs.getString("EMAIL2"));
            data.setOriHtmlFilenm(rs.getString("ORI_HTML_FILENM"));
            data.setChgHtmlFilenm(rs.getString("CHG_HTML_FILENM"));
            data.setHtmlFilepath(rs.getString("HTML_FILEPATH"));
            data.setFxday(rs.getShort("FXDAY"));
            return data;
        }, seq);

        AutoMailData target = results.isEmpty() ? null : results.get(0);
        
        if (target != null) {
            log.info("Step 4-1: 특정 SEQ 메일 발송 대상 조회 완료 (개별 발송용) - SEQ: {}, 고객: {}", 
                     seq, target.getCustNm());
        } else {
            log.warn("Step 4-1: 특정 SEQ 메일 발송 대상 없음 (개별 발송용) - SEQ: {}", seq);
        }
        
        return target;
    }

    /**
     * Step 4-1: 전체 발송 가능한 데이터 조회 (대시보드 표시용)
     * FXDAY 조건 없이 모든 발송 대기 데이터 조회 (대시보드에서 목록 표시용)
     */
    public List<AutoMailData> getAllMailSendTargets() {
        log.info("Step 4-1: 전체 메일 발송 가능 대상 조회 시작 (대시보드 표시용)");

        String sql = """
            SELECT
                SEQ, STCD2, CUST_NM, RECP_YM,
                SUBSTRING(RECP_YM, 1, 4) AS C_RECP_YEAR,
                CAST(SUBSTRING(RECP_YM, 5, 2) AS UNSIGNED) AS C_RECP_MONTH,
                EMAIL, EMAIL2, ORI_HTML_FILENM, CHG_HTML_FILENM,
                HTML_FILEPATH, FXDAY, MAIL_SEND_FLAG
            FROM b2b_automail_dt
            WHERE SEND_AUTO = 'Y'
                AND FILE_CREATE_FLAG = 'Y'
                -- FXDAY, MAIL_SEND_FLAG 조건 없음 (대시보드 표시용)
            ORDER BY SEQ
            """;

        List<AutoMailData> targets = jdbcTemplate.query(sql, (rs, rowNum) -> {
            AutoMailData data = new AutoMailData();
            data.setSeq(rs.getLong("SEQ"));
            data.setStcd2(rs.getString("STCD2"));
            data.setCustNm(rs.getString("CUST_NM"));
            data.setRecpYm(rs.getString("RECP_YM"));
            data.setEmail(rs.getString("EMAIL"));
            data.setEmail2(rs.getString("EMAIL2"));
            data.setOriHtmlFilenm(rs.getString("ORI_HTML_FILENM"));
            data.setChgHtmlFilenm(rs.getString("CHG_HTML_FILENM"));
            data.setHtmlFilepath(rs.getString("HTML_FILEPATH"));
            data.setFxday(rs.getShort("FXDAY"));
            data.setMailSendFlag(rs.getString("MAIL_SEND_FLAG"));
            return data;
        });

        log.info("Step 4-1: 전체 메일 발송 가능 대상 조회 완료 (대시보드 표시용). 총 {}건", targets.size());
        return targets;
    }

    /**
     * Step 4-2: 메일 발송 및 발송 결과 업데이트
     * 각 발송 대상에 대해 메일 발송 및 결과 업데이트를 수행
     */
    @Transactional
    public void processMailSending(List<AutoMailData> targets) {
        log.info("Step 4-2: 메일 발송 처리 시작. 대상 건수: {}", targets.size());

        int successCount = 0;
        int failCount = 0;

        for (AutoMailData target : targets) {
            try {
                // 데이터 검증
                if (!validateMailSendData(target)) {
                    log.warn("메일 발송 데이터 검증 실패. SEQ: {}", target.getSeq());
                    updateMailSendResult(target.getSeq(), target.getEmail(), "VALIDATION_ERROR", 
                                       "데이터 검증 실패", null, false);
                    failCount++;
                    continue;
                }

                // 메일 콘텐츠 준비
                String htmlContent = prepareMailContent(target);
                if (htmlContent == null) {
                    log.error("메일 콘텐츠 생성 실패. SEQ: {}", target.getSeq());
                    updateMailSendResult(target.getSeq(), target.getEmail(), "TEMPLATE_ERROR", 
                                       "메일 콘텐츠 생성 실패", null, false);
                    failCount++;
                    continue;
                }

                // 이메일 발송
                UmsApiResponse response = sendMailToCustomer(target, htmlContent);
                
                // 발송 결과 업데이트
                updateMailSendResult(target.getSeq(), target.getEmail(), response.getCode(), 
                                   response.getMessage(), response.getKey(), response.isSuccess());

                if (response.isSuccess()) {
                    successCount++;
                    log.info("메일 발송 성공. SEQ: {}, 고객: {}", target.getSeq(), target.getCustNm());
                } else {
                    failCount++;
                    log.error("메일 발송 실패. SEQ: {}, 오류: {}", target.getSeq(), response.getMessage());
                }

                // API 호출 간격 조정 (Rate Limiting)
                Thread.sleep(200); // 초당 최대 5건 발송

            } catch (Exception e) {
                log.error("메일 발송 처리 중 예외 발생. SEQ: {}", target.getSeq(), e);
                updateMailSendResult(target.getSeq(), target.getEmail(), "ERROR", 
                                   "처리 중 예외 발생: " + e.getMessage(), null, false);
                failCount++;
            }
        }

        log.info("Step 4-2: 메일 발송 처리 완료. 성공: {}건, 실패: {}건", successCount, failCount);
    }

    /**
     * 메일 발송 데이터 검증
     */
    private boolean validateMailSendData(AutoMailData target) {
        // 필수 필드 NULL 체크
        if (target.getStcd2() == null || target.getStcd2().trim().isEmpty()) {
            log.warn("사업자번호가 비어있습니다. SEQ: {}", target.getSeq());
            return false;
        }

        if (target.getCustNm() == null || target.getCustNm().trim().isEmpty()) {
            log.warn("고객명이 비어있습니다. SEQ: {}", target.getSeq());
            return false;
        }

        if (target.getEmail() == null || target.getEmail().trim().isEmpty()) {
            log.warn("이메일 주소가 비어있습니다. SEQ: {}", target.getSeq());
            return false;
        }

        // 이메일 주소 형식 유효성 검사
        if (!EMAIL_PATTERN.matcher(target.getEmail()).matches()) {
            log.warn("이메일 주소 형식이 유효하지 않습니다. SEQ: {}, Email: {}", 
                     target.getSeq(), target.getEmail());
            return false;
        }

        // 첨부파일 경로 및 파일 존재 여부 확인
        String attachmentPath = buildAttachmentPath(target);
        if (attachmentPath == null) {
            log.warn("첨부파일 경로 구성 실패. SEQ: {}", target.getSeq());
            return false;
        }

        // 중복 발송 방지를 위한 당일 발송 이력 체크
        // if (hasMailSentToday(target.getSeq())) {
        //     log.warn("당일 이미 발송된 메일입니다. SEQ: {}", target.getSeq());
        //     return false;
        // }

        return true;
    }

    /**
     * 메일 콘텐츠 준비
     */
    private String prepareMailContent(AutoMailData target) {
        try {
            String recpYm = target.getRecpYm();
            if (recpYm == null || recpYm.length() < 6) {
                log.error("청구년월 형식이 잘못되었습니다. SEQ: {}, RECP_YM: {}", 
                         target.getSeq(), recpYm);
                return null;
            }

            String recpYear = recpYm.substring(0, 4);
            Integer recpMonth = Integer.parseInt(recpYm.substring(4, 6));

            // 템플릿 변수 검증
            if (!templateService.validateTemplateVariables(target.getCustNm(), recpYear, recpMonth)) {
                log.error("템플릿 변수 검증 실패. SEQ: {}", target.getSeq());
                return null;
            }

            return templateService.generateMailContent(target.getCustNm(), recpYear, recpMonth);

        } catch (Exception e) {
            log.error("메일 콘텐츠 생성 중 오류 발생. SEQ: {}", target.getSeq(), e);
            return null;
        }
    }

    /**
     * 이메일 발송
     */
    private UmsApiResponse sendMailToCustomer(AutoMailData target, String htmlContent) {
        try {
            String recpYm = target.getRecpYm();
            String recpYear = recpYm.substring(0, 4);
            Integer recpMonth = Integer.parseInt(recpYm.substring(4, 6));

            EmailSendRequest request = EmailSendRequest.builder()
                    .email(target.getEmail())
                    .title(emailService.generateMailTitle(recpYear, recpMonth))
                    .contents(htmlContent)
                    .fromName("Coway")
                    .fromAddress("noreply@coway.com")
                    .attName1(target.getOriHtmlFilenm())
                    .attPath1(buildAttachmentPath(target))
                    .build();

            return emailService.sendEmailWithResponse(request);

        } catch (Exception e) {
            log.error("이메일 발송 중 오류 발생. SEQ: {}", target.getSeq(), e);
            return UmsApiResponse.builder()
                    .success(false)
                    .code("ERROR")
                    .message("이메일 발송 중 오류: " + e.getMessage())
                    .key(null)
                    .build();
        }
    }

    /**
     * 첨부파일 경로 구성 (웹 접근 가능한 HTTP URL)
     */
    private String buildAttachmentPath(AutoMailData target) {
        if (target.getHtmlFilepath() == null || target.getChgHtmlFilenm() == null) {
            log.warn("첨부파일 경로 정보가 없습니다. SEQ: {}, HTML_FILEPATH: {}, CHG_HTML_FILENM: {}", 
                     target.getSeq(), target.getHtmlFilepath(), target.getChgHtmlFilenm());
            return null;
        }
        
        // 웹에서 접근 가능한 HTTP URL 구성
        String filePath = target.getHtmlFilepath();
        String fileName = target.getChgHtmlFilenm();
        
        // HTML_FILEPATH가 이미 웹 경로 형태인지 확인 (/ 로 시작)
        if (!filePath.startsWith("/")) {
            filePath = "/" + filePath;
        }
        
        // 경로 끝에 슬래시가 없으면 추가
        if (!filePath.endsWith("/")) {
            filePath += "/";
        }
        
        // 최종 웹 접근 가능한 URL 구성
        String webUrl = attachmentBaseUrl + filePath + fileName;
        
        log.debug("첨부파일 웹 URL 구성 완료. SEQ: {}, URL: {}", target.getSeq(), webUrl);
        return webUrl;
    }

    /**
     * 당일 발송 이력 확인 (b2b_automail_result 테이블 기준)
     */
    private boolean hasMailSentToday(Long seq) {
        String sql = """
            SELECT COUNT(*) FROM b2b_automail_result 
            WHERE AUTOMAIL_SEQ = ? AND MAIL_SEND_FLAG = 'Y' 
            AND DATE(MAIL_SEND_DATE) = CURDATE()
            """;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, seq);
        return count != null && count > 0;
    }

    /**
     * 메일 발송 결과 처리 및 업데이트
     * umsmail-guide.md Step 4-2에 정의된 로직:
     * 1. b2b_automail_result 테이블에 INSERT
     * 2. b2b_automail_dt 테이블의 MAIL_SEND_FLAG 업데이트 (성공 시에만)
     */
    @Transactional
    public void updateMailSendResult(Long seq, String email, String umsCode, String umsMsg, String umsKey, boolean success) {
        try {
            // Step 1: b2b_automail_result 테이블에 발송 결과 INSERT
            insertMailSendResult(seq, email, umsCode, umsMsg, umsKey, success);
            
            // Step 2: b2b_automail_dt 테이블의 MAIL_SEND_FLAG 업데이트 (각 메일 건당 발송 이후 최종 한번만 실행)
            // 단, UMS_CODE=13인 경우에만 'Y', 그 외의 경우에는 'E'로 업데이트
            String mailSendFlag = "13".equals(umsCode) ? "Y" : "E";
            updateMailSendFlag(seq, mailSendFlag);

            log.info("메일 발송 결과 처리 완료. SEQ: {}, Email: {}, 성공: {}, UMS_CODE: {}", seq, email, success, umsCode);

        } catch (Exception e) {
            log.error("메일 발송 결과 처리 중 오류 발생. SEQ: {}", seq, e);
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    /**
     * b2b_automail_result 테이블에 메일 발송 결과 INSERT
     */
    private void insertMailSendResult(Long seq, String email, String umsCode, String umsMsg, String umsKey, boolean success) {
        try {
            // 먼저 b2b_automail_dt에서 필요한 정보 조회
            String selectSql = """
                SELECT STCD2, CUST_NM, RECP_YM 
                FROM b2b_automail_dt 
                WHERE SEQ = ?
                """;
            
            Map<String, Object> mailInfo = jdbcTemplate.queryForMap(selectSql, seq);
            
            // b2b_automail_result 테이블에 INSERT (MAIL_SEND_DATE는 DB에서 자동 설정)
            String insertSql = """
                INSERT INTO b2b_automail_result 
                (AUTOMAIL_SEQ, STCD2, CUST_NM, RECP_YM, EMAIL, UMS_CODE, UMS_MSG, UMS_KEY, MAIL_SEND_FLAG)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            String mailSendFlag = "13".equals(umsCode) ? "Y" : "E";
            
            int insertedRows = jdbcTemplate.update(insertSql,
                    seq,  // AUTOMAIL_SEQ (b2b_automail_dt의 seq)
                    mailInfo.get("STCD2"),  // STCD2
                    mailInfo.get("CUST_NM"),  // CUST_NM
                    mailInfo.get("RECP_YM"),  // RECP_YM
                    email,  // EMAIL (발송 대상 이메일 주소)
                    umsCode,  // UMS_CODE (API 응답 JSON의 "code" 값)
                    umsMsg,   // UMS_MSG (API 응답 JSON의 "msg" 값)
                    umsKey,   // UMS_KEY (API 응답 JSON의 "key" 값)
                    mailSendFlag  // MAIL_SEND_FLAG (ums_code=13인 경우에만 Y, 그 외의 경우에는 E)
            );

            if (insertedRows == 1) {
                log.debug("메일 발송 결과 INSERT 완료. SEQ: {}, Email: {}, UMS_CODE: {}", seq, email, umsCode);
            } else {
                log.warn("메일 발송 결과 INSERT 실패. SEQ: {}, 삽입된 행 수: {}", seq, insertedRows);
            }

        } catch (Exception e) {
            log.error("메일 발송 결과 INSERT 중 오류 발생. SEQ: {}", seq, e);
            throw e;
        }
    }

    /**
     * b2b_automail_dt 테이블의 MAIL_SEND_FLAG 업데이트
     */
    private void updateMailSendFlag(Long seq, String mailSendFlag) {
        try {
            String sql = """
                UPDATE b2b_automail_dt
                SET MAIL_SEND_FLAG = ?
                WHERE SEQ = ?
                """;

            int updatedRows = jdbcTemplate.update(sql, mailSendFlag, seq);

            if (updatedRows == 1) {
                log.debug("MAIL_SEND_FLAG 업데이트 완료. SEQ: {}, FLAG: {}", seq, mailSendFlag);
            } else {
                log.warn("MAIL_SEND_FLAG 업데이트 실패. SEQ: {}, 업데이트된 행 수: {}", seq, updatedRows);
            }

        } catch (Exception e) {
            log.error("MAIL_SEND_FLAG 업데이트 중 오류 발생. SEQ: {}", seq, e);
            throw e;
        }
    }

    /**
     * Step 4 전체 프로세스 실행
     */
    @Transactional
    public void executeStep4() {
        log.info("Step 4: 메일 발송 및 결과 업데이트 프로세스 시작");
        
        try {
            // Step 4-1: 메일 발송 대상 조회
            List<AutoMailData> targets = getMailSendTargets();
            
            if (targets.isEmpty()) {
                log.info("메일 발송 대상이 없습니다");
                return;
            }

            // Step 4-2: 메일 발송 및 결과 업데이트
            processMailSending(targets);
            
            log.info("Step 4: 메일 발송 및 결과 업데이트 프로세스 완료");
            
        } catch (Exception e) {
            log.error("Step 4 프로세스 실행 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * Step 4-2: 개별 메일 발송 처리 (수동 발송)
     * 개별 발송이므로 중복 발송 체크를 완화함
     */
    @Transactional
    public void processIndividualMailSending(AutoMailData target) {
        log.info("Step 4-2: 개별 메일 발송 처리 시작. SEQ: {}, 고객: {}", target.getSeq(), target.getCustNm());

        try {
            // 개별 발송용 데이터 검증 (중복 발송 체크 완화)
            if (!validateMailSendDataForIndividual(target)) {
                log.warn("개별 메일 발송 데이터 검증 실패. SEQ: {}", target.getSeq());
                updateMailSendResult(target.getSeq(), target.getEmail(), "VALIDATION_ERROR", 
                                   "개별 발송 데이터 검증 실패", null, false);
                return;
            }

            // 메일 콘텐츠 준비
            String htmlContent = prepareMailContent(target);
            if (htmlContent == null) {
                log.error("개별 메일 콘텐츠 생성 실패. SEQ: {}", target.getSeq());
                updateMailSendResult(target.getSeq(), target.getEmail(), "TEMPLATE_ERROR", 
                                   "개별 메일 콘텐츠 생성 실패", null, false);
                return;
            }

            // 이메일 발송
            UmsApiResponse response = sendMailToCustomer(target, htmlContent);
            
            // 발송 결과 업데이트
            updateMailSendResult(target.getSeq(), target.getEmail(), response.getCode(), 
                               response.getMessage(), response.getKey(), response.isSuccess());

            if (response.isSuccess()) {
                log.info("개별 메일 발송 성공. SEQ: {}, 고객: {}", target.getSeq(), target.getCustNm());
            } else {
                log.error("개별 메일 발송 실패. SEQ: {}, 오류: {}", target.getSeq(), response.getMessage());
            }

        } catch (Exception e) {
            log.error("개별 메일 발송 처리 중 예외 발생. SEQ: {}", target.getSeq(), e);
            updateMailSendResult(target.getSeq(), target.getEmail(), "ERROR", 
                               "개별 발송 처리 중 예외 발생: " + e.getMessage(), null, false);
        }

        log.info("Step 4-2: 개별 메일 발송 처리 완료. SEQ: {}", target.getSeq());
    }

    /**
     * 개별 발송용 메일 데이터 검증 (중복 발송 체크 완화)
     */
    private boolean validateMailSendDataForIndividual(AutoMailData target) {
        // 필수 필드 NULL 체크
        if (target.getStcd2() == null || target.getStcd2().trim().isEmpty()) {
            log.warn("사업자번호가 비어있습니다. SEQ: {}", target.getSeq());
            return false;
        }

        if (target.getCustNm() == null || target.getCustNm().trim().isEmpty()) {
            log.warn("고객명이 비어있습니다. SEQ: {}", target.getSeq());
            return false;
        }

        if (target.getEmail() == null || target.getEmail().trim().isEmpty()) {
            log.warn("이메일 주소가 비어있습니다. SEQ: {}", target.getSeq());
            return false;
        }

        // 이메일 주소 형식 유효성 검사
        if (!EMAIL_PATTERN.matcher(target.getEmail()).matches()) {
            log.warn("이메일 주소 형식이 유효하지 않습니다. SEQ: {}, Email: {}", 
                     target.getSeq(), target.getEmail());
            return false;
        }

        // 첨부파일 경로 구성 체크 (개별 발송은 파일 존재 여부를 엄격히 체크하지 않음)
        String attachmentPath = buildAttachmentPath(target);
        if (attachmentPath == null) {
            log.warn("첨부파일 경로 구성 실패하지만 개별 발송은 계속 진행. SEQ: {}", target.getSeq());
        } else {
            log.info("첨부파일 경로 구성 성공. SEQ: {}, Path: {}", target.getSeq(), attachmentPath);
        }

        // 개별 발송은 중복 발송 체크를 하지 않음 (수동 발송이므로 재발송 허용)
        log.info("개별 발송 데이터 검증 완료. SEQ: {} (중복 발송 체크 및 첨부파일 존재 여부 체크 생략)", target.getSeq());

        return true;
    }

    /**
     * 메일 발송 결과 조회 (Dashboard 용)
     * umsmail-guide.md Step 4-4에 정의된 메일 발송 결과 확인 기능
     */
    public List<Map<String, Object>> getMailSendResults() {
        log.info("메일 발송 결과 조회 시작");

        String sql = """
            SELECT 
                r.AUTOMAIL_SEQ,
                r.STCD2,
                r.CUST_NM,
                r.RECP_YM,
                r.EMAIL,
                r.UMS_CODE,
                r.UMS_MSG,
                r.UMS_KEY,
                r.MAIL_SEND_FLAG,
                r.MAIL_SEND_DATE
            FROM b2b_automail_result r
            ORDER BY r.MAIL_SEND_DATE DESC
            LIMIT 100
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        // 날짜 포맷 변환
        for (Map<String, Object> result : results) {
            Object mailSendDate = result.get("MAIL_SEND_DATE");
            if (mailSendDate instanceof LocalDateTime) {
                String formattedDate = ((LocalDateTime) mailSendDate).format(DATE_FORMATTER);
                result.put("MAIL_SEND_DATE", formattedDate);
            }
        }
        
        log.info("메일 발송 결과 조회 완료. 총 {}건", results.size());
        return results;
    }

    /**
     * 특정 날짜의 메일 발송 결과 조회
     */
    public List<Map<String, Object>> getMailSendResultsByDate(String targetDate) {
        log.info("특정 날짜 메일 발송 결과 조회 시작. 날짜: {}", targetDate);

        String sql = """
            SELECT 
                r.AUTOMAIL_SEQ,
                r.STCD2,
                r.CUST_NM,
                r.RECP_YM,
                r.EMAIL,
                r.UMS_CODE,
                r.UMS_MSG,
                r.UMS_KEY,
                r.MAIL_SEND_FLAG,
                r.MAIL_SEND_DATE
            FROM b2b_automail_result r
            WHERE DATE(r.MAIL_SEND_DATE) = ?
            ORDER BY r.MAIL_SEND_DATE DESC
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, targetDate);
        
        // 날짜 포맷 변환
        for (Map<String, Object> result : results) {
            Object mailSendDate = result.get("MAIL_SEND_DATE");
            if (mailSendDate instanceof LocalDateTime) {
                String formattedDate = ((LocalDateTime) mailSendDate).format(DATE_FORMATTER);
                result.put("MAIL_SEND_DATE", formattedDate);
            }
        }
        
        log.info("특정 날짜 메일 발송 결과 조회 완료. 날짜: {}, 총 {}건", targetDate, results.size());
        return results;
    }

    /**
     * 메일 발송 통계 조회
     */
    public Map<String, Object> getMailSendStatistics() {
        log.info("메일 발송 통계 조회 시작");

        Map<String, Object> statistics = new HashMap<>();

        // 오늘의 발송 대상 수
        List<AutoMailData> todayTargets = getMailSendTargets();
        statistics.put("todayTargetCount", todayTargets.size());

        // 오늘 발송 완료된 건수
        String todaySentSql = """
            SELECT COUNT(*) FROM b2b_automail_result 
            WHERE DATE(MAIL_SEND_DATE) = CURDATE() AND MAIL_SEND_FLAG = 'Y'
            """;
        Integer todaySentCount = jdbcTemplate.queryForObject(todaySentSql, Integer.class);
        statistics.put("todaySentCount", todaySentCount != null ? todaySentCount : 0);

        // 오늘 발송 실패한 건수
        String todayFailedSql = """
            SELECT COUNT(*) FROM b2b_automail_result 
            WHERE DATE(MAIL_SEND_DATE) = CURDATE() AND MAIL_SEND_FLAG = 'E'
            """;
        Integer todayFailedCount = jdbcTemplate.queryForObject(todayFailedSql, Integer.class);
        statistics.put("todayFailedCount", todayFailedCount != null ? todayFailedCount : 0);

        // 전체 발송 가능한 데이터 수
        List<AutoMailData> allTargets = getAllMailSendTargets();
        statistics.put("totalTargetCount", allTargets.size());

        // 대기 중인 건수
        int pendingCount = todayTargets.size() - (todaySentCount != null ? todaySentCount : 0);
        statistics.put("pendingCount", Math.max(pendingCount, 0));

        statistics.put("lastUpdated", LocalDateTime.now().toString());

        log.info("메일 발송 통계 조회 완료: {}", statistics);
        return statistics;
    }
} 