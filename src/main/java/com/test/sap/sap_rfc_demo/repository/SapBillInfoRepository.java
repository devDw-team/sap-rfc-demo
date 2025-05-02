package com.test.sap.sap_rfc_demo.repository;

import com.test.sap.sap_rfc_demo.entity.SapBillInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SapBillInfoRepository extends JpaRepository<SapBillInfo, Integer> {
    List<SapBillInfo> findByOrderNoAndStcd2AndKunnr(String orderNo, String stcd2, String kunnr);
} 