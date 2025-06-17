package com.test.sap.sap_rfc_demo.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AutoMailJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job autoMailJob;

    /**
     * 매일 00시에 자동메일 배치 실행
     */
    @Scheduled(cron = "0 20 13 * * *")
    public void runAutoMailJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("jobType", "scheduled")
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            log.info("=== [스케줄러] B2B 자동메일 배치 실행 시작 ===");
            jobLauncher.run(autoMailJob, jobParameters);
            log.info("=== [스케줄러] B2B 자동메일 배치 실행 완료 ===");
        } catch (Exception e) {
            log.error("=== [스케줄러] B2B 자동메일 배치 실행 실패 ===", e);
        }
    }
} 