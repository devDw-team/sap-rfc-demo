package com.test.sap.sap_rfc_demo.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BillInfoBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RestTemplate restTemplate;

    @Bean
    public Job billInfoJob() {
        return new JobBuilder("billInfoJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(billInfoStep())
                .build();
    }

    @Bean
    public Step billInfoStep() {
        return new StepBuilder("billInfoStep", jobRepository)
                .<Object, Object>chunk(10, transactionManager)
                .reader(billInfoReader())
                .processor(billInfoProcessor())
                .writer(billInfoWriter())
                .faultTolerant()
                .retry(Exception.class)
                .retryLimit(5)
                .build();
    }

    @Bean
    public BillInfoReader billInfoReader() {
        return new BillInfoReader(restTemplate, billInfoRetryTemplate());
    }

    @Bean
    public RetryTemplate billInfoRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(300_000); // 5분(300,000ms)
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);
        return retryTemplate;
    }

    @Bean
    public ItemProcessor<Object, Object> billInfoProcessor() {
        return item -> {
            log.info("Processing bill info: {}", item);
            return item;
        };
    }

    @Bean
    public ItemWriter<Object> billInfoWriter() {
        return items -> {
            for (Object item : items) {
                log.info("Writing bill info: {}", item);
            }
        };
    }
} 