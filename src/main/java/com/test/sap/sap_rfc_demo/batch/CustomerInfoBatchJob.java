package com.test.sap.sap_rfc_demo.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

import com.test.sap.sap_rfc_demo.dto.CustomerInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CustomerInfoBatchJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RestTemplate restTemplate;

    @Bean
    public Job customerInfoJob() {
        return new JobBuilder("customerInfoJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerInfoStep())
                .build();
    }

    @Bean
    public Step customerInfoStep() {
        return new StepBuilder("customerInfoStep", jobRepository)
                .<CustomerInfo, CustomerInfo>chunk(10, transactionManager)
                .reader(customerInfoReader())
                .processor(customerInfoProcessor())
                .writer(customerInfoWriter())
                .faultTolerant()
                .retry(Exception.class)
                .retryLimit(5)
                //.backoff(BackoffPolicy.fixed(5000)) // 5초 대기
                .build();
    }

    @Bean
    public ItemReader<CustomerInfo> customerInfoReader() {
        return new CustomerInfoReader(restTemplate, retryTemplate());
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(5000); // 5초 대기
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }

    @Bean
    public ItemProcessor<CustomerInfo, CustomerInfo> customerInfoProcessor() {
        return customerInfo -> {
            log.info("Processing customer info: {}", customerInfo);
            return customerInfo;
        };
    }

    @Bean
    public ItemWriter<CustomerInfo> customerInfoWriter() {
        return items -> {
            for (CustomerInfo customerInfo : items) {
                log.info("Writing customer info: {}", customerInfo);
            }
        };
    }
} 