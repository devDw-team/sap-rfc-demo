package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.dto.InvoiceSearchRequest;
import com.test.sap.sap_rfc_demo.entity.SapBillInfo;
import com.test.sap.sap_rfc_demo.entity.SapCustomerInfo;
import com.test.sap.sap_rfc_demo.repository.SapBillInfoRepository;
import com.test.sap.sap_rfc_demo.repository.SapCustomerInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final SapCustomerInfoRepository customerInfoRepository;
    private final SapBillInfoRepository billInfoRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getInvoiceData(InvoiceSearchRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 고객 정보 조회
        List<SapCustomerInfo> customerInfo = customerInfoRepository.findByOrderNoAndStcd2AndKunnr(
            request.getOrderNo(), 
            request.getStcd2(), 
            request.getKunnr()
        );

        // 청구 정보 조회
        List<SapBillInfo> billInfo = billInfoRepository.findByOrderNoAndStcd2AndKunnr(
            request.getOrderNo(),
            request.getStcd2(),
            request.getKunnr()
        );

        result.put("customerInfo", customerInfo);
        result.put("billInfo", billInfo);

        return result;
    }
} 