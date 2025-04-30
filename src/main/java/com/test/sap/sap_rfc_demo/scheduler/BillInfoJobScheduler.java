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

    @Scheduled(cron = "0 50 16 * * ?") // 매일 18시에 실행
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