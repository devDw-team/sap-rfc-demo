package com.test.sap.sap_rfc_demo.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class BundleInfoRepository {

    @PersistenceContext
    private EntityManager em;

    // 1. 고객 정보
    public Map<String, Object> findCustomer(String zgrpno) {
        String sql = "SELECT STCD2, CUST_NM, J_1KFREPRE, J_1KFTBUS, J_1KFTIND, PAY_COM_TX, PAY_NO, PRE_AMT, REMAIN_AMT, PRE_MONTH " +
                "FROM z_re_b2b_cust_info WHERE ZGRPNO = :zgrpno LIMIT 1";
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("zgrpno", zgrpno)
                .getResultList();
        if (rows.isEmpty()) return Collections.emptyMap();
        Object[] row = rows.get(0);
        String[] keys = {"STCD2", "CUST_NM", "J_1KFREPRE", "J_1KFTBUS", "J_1KFTIND", "PAY_COM_TX", "PAY_NO", "PRE_AMT", "REMAIN_AMT", "PRE_MONTH"};
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) map.put(keys[i], row[i]);
        return map;
    }

    // 2. 청구 정보(여러 건)
    public List<Map<String, Object>> findBills(String zgrpno) {
        String sql = "SELECT RECP_TP_TX, ORDER_NO, VTEXT, GOODS_CD, INST_DT, USE_DUTY_MONTH, OWNER_DATE, USE_MONTH, RECP_YM, FIX_SUPPLY_VALUE, FIX_VAT, FIX_BILL_AMT, SUPPLY_VALUE, VAT, BILL_AMT, PAY_COM_TX, PAY_NO, INST_JUSO, GOODS_SN, DEPT_CD_TX, DEPT_TELNR, ZBIGO, GOODS_TX, PRE_AMT, REMAIN_AMT, PRE_MONTH " +
                "FROM z_re_b2b_bill_info WHERE ZGRPNO = :zgrpno";
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("zgrpno", zgrpno)
                .getResultList();
        String[] keys = {"RECP_TP_TX", "ORDER_NO", "VTEXT", "GOODS_CD", "INST_DT", "USE_DUTY_MONTH", "OWNER_DATE", "USE_MONTH", "RECP_YM", "FIX_SUPPLY_VALUE", "FIX_VAT", "FIX_BILL_AMT", "SUPPLY_VALUE", "VAT", "BILL_AMT", "PAY_COM_TX", "PAY_NO", "INST_JUSO", "GOODS_SN", "DEPT_CD_TX", "DEPT_TELNR", "ZBIGO", "GOODS_TX", "PRE_AMT", "REMAIN_AMT", "PRE_MONTH"};
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < keys.length; i++) map.put(keys[i], row[i]);
            result.add(map);
        }
        return result;
    }

    // 3. 청구 합계
    public Map<String, Object> findBillSummary(String zgrpno) {
        String sql = "SELECT RECP_YM as C_RECP_YM, DUE_DATE as C_DUE_DATE, sum(SUPPLY_VALUE)+sum(vat) as TOTAL_AMOUNT, " +
                "(SELECT count(*) as SEL_KUN_CNT FROM z_re_b2b_bill_info WHERE ZGRPNO = :zgrpno AND SEL_KUN='X') as C_SEL_KUN_CNT " +
                "FROM z_re_b2b_bill_info WHERE ZGRPNO = :zgrpno GROUP BY RECP_YM, DUE_DATE";
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("zgrpno", zgrpno)
                .getResultList();
        if (rows.isEmpty()) return Collections.emptyMap();
        Object[] row = rows.get(0);
        String[] keys = {"C_RECP_YM", "C_DUE_DATE", "TOTAL_AMOUNT", "C_SEL_KUN_CNT"};
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) map.put(keys[i], row[i]);
        return map;
    }

    // 4. 청구 유형별 합계
    public List<Map<String, Object>> findBillTypeSummary(String zgrpno) {
        String sql = "SELECT RECP_TP as C_RECP_TP, RECP_TP_TX as C_RECP_TP_TX, count(recp_tp) as SUMMARY_CNT, sum(SUPPLY_VALUE)+sum(vat) as SUMMARY_AMOUNT " +
                "FROM z_re_b2b_bill_info WHERE ZGRPNO = :zgrpno GROUP BY RECP_TP, RECP_TP_TX";
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("zgrpno", zgrpno)
                .getResultList();
        String[] keys = {"C_RECP_TP", "C_RECP_TP_TX", "SUMMARY_CNT", "SUMMARY_AMOUNT"};
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < keys.length; i++) map.put(keys[i], row[i]);
            result.add(map);
        }
        return result;
    }

    // 5. 비고(J_JBIGO)
    public Map<String, Object> findRemarks(String zgrpno) {
        String sql = "SELECT b.ZBIGO as J_JBIGO FROM z_re_b2b_cust_info a, z_re_b2b_bill_info b WHERE a.ORDER_NO = b.ORDER_NO AND b.ZGRPNO = :zgrpno LIMIT 1";
        List<?> rows = em.createNativeQuery(sql)
                .setParameter("zgrpno", zgrpno)
                .getResultList();
        if (rows.isEmpty()) return Collections.emptyMap();
        Object value = rows.get(0); // String 또는 null
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("J_JBIGO", value);
        return map;
    }
} 