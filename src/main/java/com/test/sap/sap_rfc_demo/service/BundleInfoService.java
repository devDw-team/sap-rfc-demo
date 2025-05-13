package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.repository.BundleInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BundleInfoService {
    private final BundleInfoRepository bundleInfoRepository;

    public Map<String, Object> getBundleInfo(String zgrpno) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customer", bundleInfoRepository.findCustomer(zgrpno));
        result.put("bills", bundleInfoRepository.findBills(zgrpno));
        result.put("bill_summary", bundleInfoRepository.findBillSummary(zgrpno));
        result.put("bill_type_summary", bundleInfoRepository.findBillTypeSummary(zgrpno));
        result.put("remarks", bundleInfoRepository.findRemarks(zgrpno));
        return result;
    }
} 