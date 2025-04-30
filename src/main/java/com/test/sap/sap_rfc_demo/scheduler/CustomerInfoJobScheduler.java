package com.test.sap.sap_rfc_demo.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerInfoJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job customerInfoJob;

    @Scheduled(cron = "0 40 16 * * ?") // 매일 16시에 실행
    public void runJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(customerInfoJob, jobParameters);
            log.info("Customer Info Job has been started");
        } catch (Exception e) {
            log.error("Error occurred while starting Customer Info Job: {}", e.getMessage());
        }
    }
} 