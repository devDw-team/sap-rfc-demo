package com.test.sap.sap_rfc_demo.scheduler;

import com.test.sap.sap_rfc_demo.service.MailSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Step 4: 청구서 메일 발송 배치 스케줄러
 * umsmail-guide.md Step 4-3 구현
 * 실행 주기: 매일 오전 8시 (1일, 2일 제외)
 * 조건: SEND_AUTO='Y', FILE_CREATE_FLAG='Y', MAIL_SEND_FLAG='N', FXDAY=현재일자
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailSendJobScheduler {

    private final MailSendService mailSendService;

    /**
     * 매일 오전 8시에 청구서 메일 발송 배치 작업 실행 (1일, 2일 제외)
     * Cron: 0 0 8 3-31 * ? (초 분 시 일 월 요일) - 3일부터 31일까지
     * 
     * 실행 조건:
     * - SEND_AUTO = 'Y' (자동 발송 대상)
     * - FILE_CREATE_FLAG = 'Y' (파일 생성 완료)
     * - MAIL_SEND_FLAG = 'N' (메일 미발송)
     * - FXDAY = DAY(CURDATE()) (오늘이 고정일인 고객)
     */
    @Scheduled(cron = "0 0 8 3-31 * ?")
    public void executeMailSendBatch() {
        try {
            log.info("=== Step 4: 청구서 메일 발송 배치 스케줄 실행 시작 ===");
            log.info("실행 시간: {} (1일, 2일 제외)", LocalDateTime.now());

            // Step 4 전체 프로세스 실행
            // Step 4-1: 메일 발송 대상 조회
            // Step 4-2: 메일 발송 및 발송 결과 업데이트
            mailSendService.executeStep4();

            log.info("=== Step 4: 청구서 메일 발송 배치 스케줄 실행 완료 ===");

        } catch (Exception e) {
            log.error("Step 4: 청구서 메일 발송 배치 스케줄 실행 실패: {}", e.getMessage(), e);
            // 실패 시 알림 로직 추가 가능 (이메일, 슬랙 등)
        }
    }

    /**
     * 수동 실행용 메서드 (테스트 및 긴급 실행용)
     * 관리자가 필요시 호출할 수 있는 수동 실행 메서드
     */
    public void executeMailSendBatchManually() {
        try {
            log.info("=== Step 4: 청구서 메일 발송 배치 수동 실행 시작 ===");
            log.info("실행 시간: {}", LocalDateTime.now());

            mailSendService.executeStep4();

            log.info("=== Step 4: 청구서 메일 발송 배치 수동 실행 완료 ===");

        } catch (Exception e) {
            log.error("Step 4: 청구서 메일 발송 배치 수동 실행 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Step 4: 청구서 메일 발송 배치 수동 실행 실패", e);
        }
    }

    /**
     * 특정 FXDAY에 대해 강제 메일 발송 실행
     * 
     * @param targetDay 발송 대상 일자 (1~31)
     */
    public void executeMailSendForSpecificDay(int targetDay) {
        try {
            log.info("=== Step 4: 특정 일자({}) 청구서 메일 발송 배치 실행 시작 ===", targetDay);
            log.info("실행 시간: {}", LocalDateTime.now());

            // TODO: MailSendService에 특정 일자 처리 메서드 추가 필요 시 구현
            // mailSendService.executeStep4ForSpecificDay(targetDay);
            
            // 현재는 일반적인 Step 4 실행
            mailSendService.executeStep4();

            log.info("=== Step 4: 특정 일자({}) 청구서 메일 발송 배치 실행 완료 ===", targetDay);

        } catch (Exception e) {
            log.error("Step 4: 특정 일자({}) 청구서 메일 발송 배치 실행 실패: {}", targetDay, e.getMessage(), e);
            throw new RuntimeException("Step 4: 특정 일자 청구서 메일 발송 배치 실행 실패", e);
        }
    }
} 