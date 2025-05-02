package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.dto.InvoiceSearchRequest;
import com.test.sap.sap_rfc_demo.entity.SapBillInfo;
import com.test.sap.sap_rfc_demo.entity.SapCustomerInfo;
import com.test.sap.sap_rfc_demo.repository.SapBillInfoRepository;
import com.test.sap.sap_rfc_demo.repository.SapCustomerInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
        List<SapCustomerInfo> customerInfoList = customerInfoRepository.findByOrderNoAndStcd2AndKunnr(
            request.getOrderNo(), 
            request.getStcd2(), 
            request.getKunnr()
        );

        // 청구 정보 조회
        List<SapBillInfo> billInfoList = billInfoRepository.findByOrderNoAndStcd2AndKunnr(
            request.getOrderNo(),
            request.getStcd2(),
            request.getKunnr()
        );

        // customerInfo는 첫 번째 데이터만 사용 (단일 객체로 변환)
        Map<String, Object> customerInfo = new HashMap<>();
        if (!customerInfoList.isEmpty()) {
            SapCustomerInfo customer = customerInfoList.get(0);
            customerInfo.put("custNm", customer.getCustNm());
            customerInfo.put("stcd2", customer.getStcd2());
            customerInfo.put("j1kfrepre", customer.getJ1kfrepre());
            customerInfo.put("j1kftbus", customer.getJ1kftbus());
            customerInfo.put("j1kftind", customer.getJ1kftind());
        }

        // billInfo 리스트 변환
        List<Map<String, Object>> billInfoMapped = new ArrayList<>();
        for (SapBillInfo bill : billInfoList) {
            Map<String, Object> billMap = new HashMap<>();
            billMap.put("orderNo", bill.getOrderNo());
            billMap.put("vtext", bill.getVtext());
            billMap.put("goodsTx", bill.getGoodsTx());
            billMap.put("instDt", bill.getInstDt());
            billMap.put("useDutyMonth", bill.getUseDutyMonth());
            billMap.put("recpTp", bill.getRecpTp());
            billMap.put("fixSupplyValue", bill.getFixSupplyValue());
            billMap.put("fixVat", bill.getFixVat());
            billMap.put("fixBillAmt", bill.getFixBillAmt());
            billMap.put("supplyValue", bill.getSupplyValue());
            billMap.put("vat", bill.getVat());
            billMap.put("billAmt", bill.getBillAmt());
            billInfoMapped.add(billMap);
        }

        result.put("customerInfo", customerInfo);
        result.put("billInfo", billInfoMapped);

        return result;
    }
} 