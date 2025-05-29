package com.test.sap.sap_rfc_demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "Z_RE_B2B_CUST_INFO")
@Getter @Setter
@NoArgsConstructor
public class SapCustomerInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer seq;

    @Column(name = "ORDER_NO", length = 12, nullable = false)
    private String orderNo;

    @Column(name = "STCD2", length = 11)
    private String stcd2;

    @Column(name = "KUNNR", length = 10)
    private String kunnr;

    @Column(name = "CUST_NM", length = 40)
    private String custNm;

    @Column(name = "FXDAY", columnDefinition = "SMALLINT UNSIGNED")
    private Short fxday;

    @Column(name = "ZGRPNO", columnDefinition = "BIGINT UNSIGNED")
    private Long zgrpno;

    @Column(name = "SEL_KUN", length = 1)
    private String selKun;

    @Column(name = "JUSO", length = 255)
    private String juso;

    @Column(name = "PSTLZ", length = 10)
    private String pstlz;

    @Column(name = "J_1KFTBUS", length = 30)
    private String j1kftbus;

    @Column(name = "J_1KFTIND", length = 30)
    private String j1kftind;

    @Column(name = "J_1KFREPRE", length = 20)
    private String j1kfrepre;

    @Column(name = "EMAIL", length = 50)
    private String email;

    @Column(name = "EMAIL2", length = 50)
    private String email2;

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

    @Column(name = "PRE_MONTH", precision = 3, scale = 0)
    private java.math.BigDecimal preMonth;

    @Column(name = "PRE_AMT", precision = 13, scale = 0)
    private java.math.BigDecimal preAmt;

    @Column(name = "REMAIN_AMT", precision = 13, scale = 0)
    private java.math.BigDecimal remainAmt;

    @Column(name = "SEND_AUTO", length = 1)
    private String sendAuto;

    @Column(name = "REGID", length = 20, nullable = false)
    private String regid = "SAP RFC";

    @Column(name = "REGDT", nullable = false)
    private java.time.LocalDateTime regdt = java.time.LocalDateTime.now();
} 