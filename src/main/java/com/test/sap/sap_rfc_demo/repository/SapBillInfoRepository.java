package com.test.sap.sap_rfc_demo.repository;

import com.test.sap.sap_rfc_demo.entity.SapBillInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SapBillInfoRepository extends JpaRepository<SapBillInfo, Integer> {
} 