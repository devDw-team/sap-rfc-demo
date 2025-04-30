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

    public BatchSchedulerConfig(JobLauncher jobLauncher, @Qualifier("customerInfoJob") Job customerInfoJob) {
        this.jobLauncher = jobLauncher;
        this.customerInfoJob = customerInfoJob;
    }

    @Scheduled(cron = "0 40 16 * * ?")  // 매일 15:30에 실행
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
} 