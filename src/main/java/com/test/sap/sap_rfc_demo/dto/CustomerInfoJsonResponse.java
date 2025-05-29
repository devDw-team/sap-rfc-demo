package com.test.sap.sap_rfc_demo.dto;

import com.test.sap.sap_rfc_demo.entity.SapCustomerInfo;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class CustomerInfoJsonResponse {
    private Integer seq;
    private String orderNo;
    private String stcd2;
    private String kunnr;
    private String custNm;
    private Short fxday;
    private Long zgrpno;
    private String selKun;
    private String juso;
    private String pstlz;
    private String j1kftbus;
    private String j1kftind;
    private String j1kfrepre;
    private String email;
    private String email2;
    private String payMthd;
    private String payMthdTx;
    private String payCom;
    private String payComTx;
    private String payNo;
    private String regid;
    private LocalDateTime regdt;
    private java.math.BigDecimal preMonth;
    private java.math.BigDecimal preAmt;
    private java.math.BigDecimal remainAmt;
    private String sendAuto;

    public static CustomerInfoJsonResponse from(SapCustomerInfo entity) {
        CustomerInfoJsonResponse dto = new CustomerInfoJsonResponse();
        dto.setSeq(entity.getSeq());
        dto.setOrderNo(entity.getOrderNo());
        dto.setStcd2(entity.getStcd2());
        dto.setKunnr(entity.getKunnr());
        dto.setCustNm(entity.getCustNm());
        dto.setFxday(entity.getFxday());
        dto.setZgrpno(entity.getZgrpno());
        dto.setSelKun(entity.getSelKun());
        dto.setJuso(entity.getJuso());
        dto.setPstlz(entity.getPstlz());
        dto.setJ1kftbus(entity.getJ1kftbus());
        dto.setJ1kftind(entity.getJ1kftind());
        dto.setJ1kfrepre(entity.getJ1kfrepre());
        dto.setEmail(entity.getEmail());
        dto.setEmail2(entity.getEmail2());
        dto.setPayMthd(entity.getPayMthd());
        dto.setPayMthdTx(entity.getPayMthdTx());
        dto.setPayCom(entity.getPayCom());
        dto.setPayComTx(entity.getPayComTx());
        dto.setPayNo(entity.getPayNo());
        dto.setRegid(entity.getRegid());
        dto.setRegdt(entity.getRegdt());
        dto.setPreMonth(entity.getPreMonth());
        dto.setPreAmt(entity.getPreAmt());
        dto.setRemainAmt(entity.getRemainAmt());
        dto.setSendAuto(entity.getSendAuto());
        return dto;
    }

    public void setZgrpno(Long zgrpno) { this.zgrpno = zgrpno; }
    public Long getZgrpno() { return zgrpno; }
} 