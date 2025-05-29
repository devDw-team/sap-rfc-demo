package com.test.sap.sap_rfc_demo.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillInfoJobScheduler {

    private final JobLauncher jobLauncher;
    @Qualifier("billInfoJob")
    private final Job billInfoJob;

    /**
     * 청구서 정보 배치 (비활성화됨)
     * 원래 스케줄: 매일 09:07
     */
    // @Scheduled(cron = "0 07 09 * * ?") // 비활성화됨 - 기타 배치
    public void runJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(billInfoJob, jobParameters);
            log.info("Bill Info Job has been started");
        } catch (Exception e) {
            log.error("Error occurred while starting Bill Info Job: {}", e.getMessage());
        }
    }
} 