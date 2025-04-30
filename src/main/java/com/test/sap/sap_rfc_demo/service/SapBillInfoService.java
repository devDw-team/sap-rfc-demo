package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.entity.SapBillInfo;
import com.test.sap.sap_rfc_demo.repository.SapBillInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class SapBillInfoService {

    private static final Logger logger = LoggerFactory.getLogger(SapBillInfoService.class);
    private final SapBillInfoRepository billInfoRepository;

    @Autowired
    public SapBillInfoService(SapBillInfoRepository billInfoRepository) {
        this.billInfoRepository = billInfoRepository;
    }

    @Transactional
    public void saveBillInfoFromApi(List<Map<String, String>> billDataList) {
        logger.debug("Starting to save {} bill records", billDataList.size());
        
        for (Map<String, String> billData : billDataList) {
            try {
                SapBillInfo billInfo = new SapBillInfo();
                
                // API 응답 데이터를 엔티티에 매핑
                billInfo.setOrderNo(billData.get("ORDER_NO"));
                billInfo.setStcd2(billData.get("STCD2"));
                billInfo.setKunnr(billData.get("KUNNR"));
                billInfo.setZgrpno(billData.get("ZGRPNO"));
                billInfo.setSelKun(billData.get("SEL_KUN"));
                billInfo.setPayMthd(billData.get("PAY_MTHD"));
                billInfo.setPayMthdTx(billData.get("PAY_MTHD_TX"));
                billInfo.setPayCom(billData.get("PAY_COM"));
                billInfo.setPayComTx(billData.get("PAY_COM_TX"));
                billInfo.setPayNo(billData.get("PAY_NO"));
                billInfo.setGoodsSn(billData.get("GOODS_SN"));
                billInfo.setGoodsCd(billData.get("GOODS_CD"));
                billInfo.setGoodsTx(billData.get("GOODS_TX"));
                billInfo.setProdh(billData.get("PRODH"));
                billInfo.setVtext(billData.get("VTEXT"));
                billInfo.setRecpYm(billData.get("RECP_YM"));
                billInfo.setRecpTp(billData.get("RECP_TP"));
                billInfo.setRecpTpTx(billData.get("RECP_TP_TX"));
                
                // BigDecimal 필드 변환
                billInfo.setFixSupplyValue(parseBigDecimal(billData.get("FIX_SUPPLY_VALUE")));
                billInfo.setFixVat(parseBigDecimal(billData.get("FIX_VAT")));
                billInfo.setFixBillAmt(parseBigDecimal(billData.get("FIX_BILL_AMT")));
                billInfo.setSupplyValue(parseBigDecimal(billData.get("SUPPLY_VALUE")));
                billInfo.setVat(parseBigDecimal(billData.get("VAT")));
                billInfo.setBillAmt(parseBigDecimal(billData.get("BILL_AMT")));
                billInfo.setPreAmt(parseBigDecimal(billData.get("PRE_AMT")));
                billInfo.setRemainAmt(parseBigDecimal(billData.get("REMAIN_AMT")));
                
                billInfo.setDueDate(billData.get("DUE_DATE"));
                billInfo.setPreMonth(billData.get("PRE_MONTH"));
                billInfo.setInstDt(billData.get("INST_DT"));
                billInfo.setUseMonth(billData.get("USE_MONTH"));
                billInfo.setUseDutyMonth(billData.get("USE_DUTY_MONTH"));
                billInfo.setOwnerDate(billData.get("OWNER_DATE"));
                billInfo.setInstJuso(billData.get("INST_JUSO"));
                billInfo.setDeptCd(billData.get("DEPT_CD"));
                billInfo.setDeptCdTx(billData.get("DEPT_CD_TX"));
                billInfo.setDeptTelnr(billData.get("DEPT_TELNR"));
                billInfo.setZbigo(billData.get("ZBIGO"));

                billInfoRepository.save(billInfo);
                logger.debug("Successfully saved bill info with ORDER_NO: {}", billInfo.getOrderNo());
            } catch (Exception e) {
                logger.error("Error saving bill data: {}", billData, e);
                throw new RuntimeException("Failed to save bill data", e);
            }
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? new BigDecimal(value.trim()) : null;
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse BigDecimal value: {}", value);
            return null;
        }
    }

    public List<SapBillInfo> getAllBillInfo() {
        return billInfoRepository.findAll();
    }
} 