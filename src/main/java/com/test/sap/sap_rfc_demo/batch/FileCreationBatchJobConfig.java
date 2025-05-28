package com.test.sap.sap_rfc_demo.batch;

import com.test.sap.sap_rfc_demo.entity.AutoMailData;
import com.test.sap.sap_rfc_demo.service.FileCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 파일 생성 배치 작업 설정
 * filecreate-guide.md Step 4 구현
 * 실행 주기: 매월 4일 오전 8시 (Cron: 0 0 8 4 * ?)
 */
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class FileCreationBatchJobConfig {

    private final FileCreationService fileCreationService;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * 파일 생성 배치 Job
     */
    @Bean
    public Job fileCreationJob() {
        return new JobBuilder("fileCreationJob", jobRepository)
                .start(fileCreationStep())
                .listener(fileCreationJobListener())
                .build();
    }

    /**
     * 파일 생성 Step
     */
    @Bean
    public Step fileCreationStep() {
        return new StepBuilder("fileCreationStep", jobRepository)
                .<AutoMailData, AutoMailData>chunk(10, transactionManager) // 청크 사이즈 10
                .reader(fileCreationItemReader())
                .processor(fileCreationItemProcessor())
                .writer(fileCreationItemWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(5) // 최대 5건까지 스킵 허용
                .listener(fileCreationStepListener())
                .build();
    }

    /**
     * ItemReader: Step 1의 쿼리를 실행하여 파일 생성 대상 조회
     */
    @Bean
    public ItemReader<AutoMailData> fileCreationItemReader() {
        return new ItemReader<AutoMailData>() {
            private List<AutoMailData> targets;
            private int currentIndex = 0;

            @Override
            public AutoMailData read() throws Exception {
                if (targets == null) {
                    targets = fileCreationService.getFileCreationTargets();
                    log.info("파일 생성 대상 로드 완료: {}건", targets.size());
                }

                if (currentIndex < targets.size()) {
                    return targets.get(currentIndex++);
                }
                return null; // 더 이상 읽을 데이터가 없음
            }
        };
    }

    /**
     * ItemProcessor: Step 2의 로직에 따라 HTML 및 Excel 파일 생성
     */
    @Bean
    public ItemProcessor<AutoMailData, AutoMailData> fileCreationItemProcessor() {
        return new ItemProcessor<AutoMailData, AutoMailData>() {
            @Override
            public AutoMailData process(AutoMailData item) throws Exception {
                try {
                    log.debug("파일 생성 처리 시작 - SEQ: {}", item.getSeq());
                    fileCreationService.createFilesForData(item);
                    log.debug("파일 생성 처리 완료 - SEQ: {}", item.getSeq());
                    return item;
                } catch (Exception e) {
                    log.error("파일 생성 처리 실패 - SEQ: {}, 오류: {}", item.getSeq(), e.getMessage());
                    // 개별 건 실패 시 스킵하고 다음 건 처리 계속
                    throw e;
                }
            }
        };
    }

    /**
     * ItemWriter: 처리 완료된 데이터 로깅 (실제 DB 업데이트는 Processor에서 수행)
     */
    @Bean
    public ItemWriter<AutoMailData> fileCreationItemWriter() {
        return items -> {
            for (AutoMailData item : items) {
                log.info("파일 생성 완료 - SEQ: {}, 사업자번호: {}", 
                        item.getSeq(), maskBusinessNumber(item.getStcd2()));
            }
            log.info("청크 처리 완료: {}건", items.size());
        };
    }

    /**
     * Job 실행 리스너
     */
    @Bean
    public JobExecutionListener fileCreationJobListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("=== 파일 생성 배치 작업 시작 ===");
                log.info("작업 시작 시간: {}", LocalDateTime.now());
                jobExecution.getExecutionContext().putString("startTime", LocalDateTime.now().toString());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                BatchStatus status = jobExecution.getStatus();
                long processedCount = jobExecution.getStepExecutions().stream()
                        .mapToLong(StepExecution::getWriteCount)
                        .sum();
                long skipCount = jobExecution.getStepExecutions().stream()
                        .mapToLong(StepExecution::getSkipCount)
                        .sum();

                log.info("=== 파일 생성 배치 작업 완료 ===");
                log.info("작업 상태: {}", status);
                log.info("처리 건수: {}건", processedCount);
                log.info("스킵 건수: {}건", skipCount);
                log.info("작업 종료 시간: {}", LocalDateTime.now());

                if (status == BatchStatus.FAILED) {
                    log.error("배치 작업 실패!");
                    // 실패 시 알림 로직 추가 가능
                } else if (status == BatchStatus.COMPLETED) {
                    log.info("배치 작업 성공적으로 완료!");
                }
            }
        };
    }

    /**
     * Step 실행 리스너
     */
    @Bean
    public StepExecutionListener fileCreationStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.info("파일 생성 Step 시작");
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                long readCount = stepExecution.getReadCount();
                long writeCount = stepExecution.getWriteCount();
                long skipCount = stepExecution.getSkipCount();

                log.info("파일 생성 Step 완료 - 읽기: {}건, 처리: {}건, 스킵: {}건", 
                        readCount, writeCount, skipCount);

                // 스킵 비율이 10% 초과 시 경고
                if (readCount > 0 && (skipCount * 100.0 / readCount) > 10) {
                    log.warn("스킵 비율이 10%를 초과했습니다: {:.2f}%", skipCount * 100.0 / readCount);
                }

                return ExitStatus.COMPLETED;
            }
        };
    }

    /**
     * 사업자번호 마스킹 (보안)
     */
    private String maskBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.length() < 3) {
            return businessNumber;
        }
        return businessNumber.substring(0, 3) + "*******";
    }
} 