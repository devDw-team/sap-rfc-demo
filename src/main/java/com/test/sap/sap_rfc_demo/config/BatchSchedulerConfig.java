package com.test.sap.sap_rfc_demo.config;

import com.test.sap.sap_rfc_demo.service.BatchExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@EnableScheduling
public class BatchSchedulerConfig {

    private final JobLauncher jobLauncher;
    private final Job customerInfoJob;
    // ========== AutoMail Batch Job 추가 (automail-guide.md Step 3) ==========
    private final Job autoMailJob;
    private final BatchExecutionService batchExecutionService;

    public BatchSchedulerConfig(JobLauncher jobLauncher, 
                               @Qualifier("customerInfoJob") Job customerInfoJob,
                               @Qualifier("autoMailJob") Job autoMailJob,
                               BatchExecutionService batchExecutionService) {
        this.jobLauncher = jobLauncher;
        this.customerInfoJob = customerInfoJob;
        this.autoMailJob = autoMailJob;
        this.batchExecutionService = batchExecutionService;
    }

    // ========== 기타 배치 - 비활성화됨 ==========
    /**
     * 기타 고객 정보 배치 (비활성화됨)
     * 원래 스케줄: 매일 09:00
     */
    // @Scheduled(cron = "0 00 09 * * ?")  // 비활성화됨 - 기타 배치
    public void runJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("datetime", LocalDateTime.now().toString())
                    .toJobParameters();

            log.info("Batch Job 시작: {}", LocalDateTime.now());
            jobLauncher.run(customerInfoJob, jobParameters);
            
        } catch (Exception e) {
            log.error("Batch job 실행 중 오류 발생: ", e);
        }
    }

    // ========== AutoMail Batch Job 스케줄러 (automail-guide.md Step 3) ==========
    /**
     * B2B 자동메일 배치 Job 스케줄러
     * 실행 주기: 매달 3일 오전 06:00 (3일이 공휴일인 경우 다음 평일에 실행)
     * automail-guide.md Step 3에 정의된 스케줄링 정책
     */
    @Scheduled(cron = "0 0 6 * * *")  // 매일 06:00에 실행 (조건부 실행)
    public void runAutoMailJob() {
        LocalDateTime now = LocalDateTime.now();
        
        // 매달 3일인지 확인
        if (now.getDayOfMonth() != 3) {
            return;
        }
        
        // 공휴일 체크 (토요일, 일요일 포함)
        if (isHoliday(now)) {
            log.info("=== {} 는 공휴일입니다. B2B 자동메일 배치 Job을 다음 평일로 연기합니다. ===", 
                    now.toLocalDate());
            return;
        }
        
        executeAutoMailBatch(now, "scheduled");
    }
    
    /**
     * 공휴일 연기된 B2B 자동메일 배치 Job 실행
     * 매달 4일부터 7일까지 평일에 한 번만 실행
     */
    @Scheduled(cron = "0 0 6 4-7 * *")  // 매달 4~7일 06:00에 실행
    public void runDelayedAutoMailJob() {
        LocalDateTime now = LocalDateTime.now();
        
        // 이번 달 3일이 공휴일이었는지 확인
        LocalDateTime thirdDay = now.withDayOfMonth(3);
        if (!isHoliday(thirdDay)) {
            return; // 3일이 공휴일이 아니었다면 이미 실행됨
        }
        
        // 현재 날짜가 평일인지 확인
        if (isHoliday(now)) {
            return; // 오늘도 공휴일이면 다음날 다시 체크
        }
        
        // 이미 이번 달에 실행되었는지 확인 (중복 실행 방지)
        if (batchExecutionService.hasAutoMailJobExecutedThisMonth(now)) {
            log.info("=== 이번 달에 이미 B2B 자동메일 배치 Job이 실행되었습니다. 중복 실행을 방지합니다. ===");
            return;
        }
        
        log.info("=== 공휴일 연기로 인한 B2B 자동메일 배치 Job 실행: {} ===", now.toLocalDate());
        executeAutoMailBatch(now, "delayed");
    }
    
    /**
     * B2B 자동메일 배치 Job 실행 로직
     */
    private void executeAutoMailBatch(LocalDateTime executeTime, String jobType) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("datetime", executeTime.toString())
                    .addString("jobType", "autoMail_" + jobType)
                    .addString("executionDate", executeTime.toLocalDate().toString())
                    .toJobParameters();

            log.info("=== B2B 자동메일 배치 Job 시작: {} (타입: {}) ===", executeTime, jobType);
            jobLauncher.run(autoMailJob, jobParameters);
            log.info("=== B2B 자동메일 배치 Job 완료: {} (타입: {}) ===", executeTime, jobType);
            
        } catch (Exception e) {
            log.error("=== B2B 자동메일 배치 Job 실행 중 오류 발생 (타입: {}) ===", jobType, e);
        }
    }
    
    /**
     * 공휴일 여부 확인
     * 토요일, 일요일 및 주요 공휴일 체크
     */
    private boolean isHoliday(LocalDateTime date) {
        // 토요일(6), 일요일(7) 체크
        int dayOfWeek = date.getDayOfWeek().getValue();
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            return true;
        }
        
        // 주요 공휴일 체크
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        
        // 신정 (1월 1일)
        if (month == 1 && day == 1) return true;
        
        // 삼일절 (3월 1일)
        if (month == 3 && day == 1) return true;
        
        // 어린이날 (5월 5일)
        if (month == 5 && day == 5) return true;
        
        // 현충일 (6월 6일)
        if (month == 6 && day == 6) return true;
        
        // 광복절 (8월 15일)
        if (month == 8 && day == 15) return true;
        
        // 개천절 (10월 3일)
        if (month == 10 && day == 3) return true;
        
        // 한글날 (10월 9일)
        if (month == 10 && day == 9) return true;
        
        // 크리스마스 (12월 25일)
        if (month == 12 && day == 25) return true;
        
        // TODO: 추석, 설날 등 음력 공휴일은 별도 라이브러리나 API 연동 필요
        // 현재는 양력 공휴일만 체크
        
        return false;
    }
    

} 