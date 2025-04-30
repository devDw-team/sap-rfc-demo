package com.test.sap.sap_rfc_demo.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
public class BillInfoReader implements ItemReader<Object> {
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private static final String API_URL = "http://localhost:8080/api/bill-info";
    private boolean dataRead = false;

    @Override
    public Object read() {
        if (dataRead) return null;
        try {
            Object billInfo = retryTemplate.execute(context -> restTemplate.getForObject(API_URL, Object.class));
            dataRead = true;
            log.info("Read bill info: {}", billInfo);
            return billInfo;
        } catch (Exception e) {
            log.error("Error reading bill info: {}", e.getMessage());
            throw e;
        }
    }
} 