package com.test.sap.sap_rfc_demo.config;

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

    public BatchSchedulerConfig(JobLauncher jobLauncher, 
                               @Qualifier("customerInfoJob") Job customerInfoJob,
                               @Qualifier("autoMailJob") Job autoMailJob) {
        this.jobLauncher = jobLauncher;
        this.customerInfoJob = customerInfoJob;
        this.autoMailJob = autoMailJob;
    }

    @Scheduled(cron = "0 00 09 * * ?")  // 매일 09:00에 실행
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
     * 실행 주기: 매주 월, 화, 수, 목, 금요일 오전 08:00
     * automail-guide.md Step 3에 정의된 스케줄링 정책
     */
    @Scheduled(cron = "0 0 8 * * MON-FRI")  // 월~금요일 08:00에 실행
    public void runAutoMailJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("datetime", LocalDateTime.now().toString())
                    .addString("jobType", "autoMail")
                    .toJobParameters();

            log.info("=== B2B 자동메일 배치 Job 시작: {} ===", LocalDateTime.now());
            jobLauncher.run(autoMailJob, jobParameters);
            log.info("=== B2B 자동메일 배치 Job 완료: {} ===", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("=== B2B 자동메일 배치 Job 실행 중 오류 발생 ===", e);
        }
    }
} 