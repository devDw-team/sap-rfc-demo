package com.test.sap.sap_rfc_demo.batch;

import org.springframework.batch.item.ItemReader;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;
import com.test.sap.sap_rfc_demo.dto.CustomerInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CustomerInfoReader implements ItemReader<CustomerInfo> {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private static final String API_URL = "http://localhost:8080/api/customer-info";
    private boolean dataRead = false;

    @Override
    public CustomerInfo read() {
        if (dataRead) {
            return null; // 더 이상 읽을 데이터가 없음
        }

        try {
            CustomerInfo customerInfo = retryTemplate.execute(context -> {
                return restTemplate.getForObject(API_URL, CustomerInfo.class);
            });
            dataRead = true;
            log.info("Read customer info: {}", customerInfo);
            return customerInfo;
        } catch (Exception e) {
            log.error("Error reading customer info: {}", e.getMessage());
            throw e;
        }
    }
} 