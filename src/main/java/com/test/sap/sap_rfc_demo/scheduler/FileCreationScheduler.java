package com.test.sap.sap_rfc_demo.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 파일 생성 스케줄러
 * filecreate-guide.md Step 4 구현
 * 실행 주기: 매월 3일 오전 7시
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileCreationScheduler {

    private final JobLauncher jobLauncher;
    private final Job fileCreationJob;

    /**
     * 매월 3일 오전 7시에 파일 생성 배치 작업 실행
     * Cron: 0 0 7 3 * ? (초 분 시 일 월 요일)
     */
    @Scheduled(cron = "0 30 13 * * *")
    public void executeFileCreationBatch() {
        try {
            log.info("=== 파일 생성 배치 스케줄 실행 시작 ===");
            log.info("실행 시간: {}", LocalDateTime.now());

            // JobParameters에 실행 시간을 추가하여 재실행 가능하도록 설정
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("executionTime", System.currentTimeMillis())
                    .addString("scheduledExecution", "true")
                    .toJobParameters();

            // 배치 작업 실행
            jobLauncher.run(fileCreationJob, jobParameters);

            log.info("=== 파일 생성 배치 스케줄 실행 완료 ===");

        } catch (Exception e) {
            log.error("파일 생성 배치 스케줄 실행 실패: {}", e.getMessage(), e);
            // 실패 시 알림 로직 추가 가능 (이메일, 슬랙 등)
        }
    }

    /**
     * 수동 실행용 메서드 (테스트 및 긴급 실행용)
     */
    public void executeFileCreationBatchManually() {
        try {
            log.info("=== 파일 생성 배치 수동 실행 시작 ===");
            log.info("실행 시간: {}", LocalDateTime.now());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("executionTime", System.currentTimeMillis())
                    .addString("manualExecution", "true")
                    .toJobParameters();

            jobLauncher.run(fileCreationJob, jobParameters);

            log.info("=== 파일 생성 배치 수동 실행 완료 ===");

        } catch (Exception e) {
            log.error("파일 생성 배치 수동 실행 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 생성 배치 수동 실행 실패", e);
        }
    }
} 