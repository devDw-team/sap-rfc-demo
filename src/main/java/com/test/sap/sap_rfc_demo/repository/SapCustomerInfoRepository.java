package com.test.sap.sap_rfc_demo.repository;

import com.test.sap.sap_rfc_demo.entity.SapCustomerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SapCustomerInfoRepository extends JpaRepository<SapCustomerInfo, Integer> {
    List<SapCustomerInfo> findByZgrpno(Long zgrpno);
} 