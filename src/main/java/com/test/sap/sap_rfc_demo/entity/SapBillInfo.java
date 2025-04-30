package com.test.sap.sap_rfc_demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sap_cwb2b_bill_info")
@Getter @Setter
@NoArgsConstructor
public class SapBillInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer seq;

    @Column(name = "ORDER_NO", length = 12)
    private String orderNo;

    @Column(name = "STCD2", length = 11)
    private String stcd2;

    @Column(name = "KUNNR", length = 10)
    private String kunnr;

    @Column(name = "ZGRPNO", length = 10)
    private String zgrpno;

    @Column(name = "SEL_KUN", length = 1)
    private String selKun;

    @Column(name = "PAY_MTHD", length = 4)
    private String payMthd;

    @Column(name = "PAY_MTHD_TX", length = 50)
    private String payMthdTx;

    @Column(name = "PAY_COM", length = 4)
    private String payCom;

    @Column(name = "PAY_COM_TX", length = 50)
    private String payComTx;

    @Column(name = "PAY_NO", length = 20)
    private String payNo;

    @Column(name = "GOODS_SN", length = 18)
    private String goodsSn;

    @Column(name = "GOODS_CD", length = 18)
    private String goodsCd;

    @Column(name = "GOODS_TX", length = 40)
    private String goodsTx;

    @Column(name = "PRODH", length = 18)
    private String prodh;

    @Column(name = "VTEXT", length = 40)
    private String vtext;

    @Column(name = "RECP_YM", length = 6)
    private String recpYm;

    @Column(name = "RECP_TP", length = 4)
    private String recpTp;

    @Column(name = "RECP_TP_TX", length = 20)
    private String recpTpTx;

    @Column(name = "FIX_SUPPLY_VALUE", precision = 13)
    private BigDecimal fixSupplyValue;

    @Column(name = "FIX_VAT", precision = 13)
    private BigDecimal fixVat;

    @Column(name = "FIX_BILL_AMT", precision = 13)
    private BigDecimal fixBillAmt;

    @Column(name = "SUPPLY_VALUE", precision = 13)
    private BigDecimal supplyValue;

    @Column(name = "VAT", precision = 13)
    private BigDecimal vat;

    @Column(name = "BILL_AMT", precision = 13)
    private BigDecimal billAmt;

    @Column(name = "DUE_DATE", length = 8)
    private String dueDate;

    @Column(name = "PRE_AMT", precision = 13)
    private BigDecimal preAmt;

    @Column(name = "REMAIN_AMT", precision = 13)
    private BigDecimal remainAmt;

    @Column(name = "PRE_MONTH", length = 3)
    private String preMonth;

    @Column(name = "INST_DT", length = 8)
    private String instDt;

    @Column(name = "USE_MONTH", length = 3)
    private String useMonth;

    @Column(name = "USE_DUTY_MONTH", length = 3)
    private String useDutyMonth;

    @Column(name = "OWNER_DATE", length = 8)
    private String ownerDate;

    @Column(name = "INST_JUSO", length = 255)
    private String instJuso;

    @Column(name = "DEPT_CD", length = 3)
    private String deptCd;

    @Column(name = "DEPT_CD_TX", length = 20)
    private String deptCdTx;

    @Column(name = "DEPT_TELNR", length = 25)
    private String deptTelnr;

    @Column(name = "ZBIGO", length = 50)
    private String zbigo;

    @Column(name = "REGID", length = 20, nullable = false)
    private String regid = "SAP RFC";

    @Column(name = "REGDT", nullable = false)
    private LocalDateTime regdt = LocalDateTime.now();
} 