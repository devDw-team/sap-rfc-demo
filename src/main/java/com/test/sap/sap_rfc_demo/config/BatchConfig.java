package com.test.sap.sap_rfc_demo.config;

import com.test.sap.sap_rfc_demo.service.AutoMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
@EnableBatchProcessing
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {
    
    private final AutoMailService autoMailService;

    // ========== AutoMail Batch Job 구성 (automail-guide.md Step 3) ==========
    
    /**
     * B2B 자동메일 배치 Job
     * automail-guide.md Step 3에 정의된 Spring Batch Job
     * 실행 주기: 매주 월~금요일 오전 08:00
     */
    @Bean
    public Job autoMailJob(JobRepository jobRepository, Step autoMailStep) {
        return new JobBuilder("autoMailJob", jobRepository)
                .start(autoMailStep)
                .build();
    }

    /**
     * B2B 자동메일 배치 Step
     * Step 1: 청구서 발송 대상 조회
     * Step 2: 청구서 발송 대상 적재 (JSON 구성 포함)
     */
    @Bean
    public Step autoMailStep(JobRepository jobRepository, 
                            PlatformTransactionManager transactionManager) {
        return new StepBuilder("autoMailStep", jobRepository)
                .tasklet(autoMailTasklet(), transactionManager)
                .build();
    }

    /**
     * B2B 자동메일 Tasklet
     * AutoMailService의 executeAutoMailProcess() 메서드 호출
     */
    @Bean
    public Tasklet autoMailTasklet() {
        return (contribution, chunkContext) -> {
            // Job Parameters에서 실행 정보 추출
            Map<String, Object> jobParams = chunkContext.getStepContext().getJobParameters();
            String jobType = (String) jobParams.getOrDefault("jobType", "manual");
            String executionDate = (String) jobParams.getOrDefault("executionDate", "unknown");
            
            log.info("=== B2B 자동메일 배치 작업 시작 (타입: {}, 실행일: {}) ===", jobType, executionDate);
            
            try {
                // automail-guide.md Step 1, 2 실행
                autoMailService.executeAutoMailProcess();
                
                log.info("=== B2B 자동메일 배치 작업 완료 (타입: {}, 실행일: {}) ===", jobType, executionDate);
                return RepeatStatus.FINISHED;
                
            } catch (Exception e) {
                log.error("=== B2B 자동메일 배치 작업 실패 (타입: {}, 실행일: {}) ===", jobType, executionDate, e);
                throw e;
            }
        };
    }
    
    // ========== 기존 배치 설정 유지 ==========
    // 기본 배치 설정
} 