package com.test.sap.sap_rfc_demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * B2B 자동메일 발송 대상 엔티티
 * automail-guide.md Step 2-6에 정의된 b2b_automail_dt 테이블 매핑
 */
@Entity
@Table(name = "b2b_automail_dt")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoMailData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ")
    private Long seq;

    @Column(name = "FORM_ID", length = 50)
    private String formId;

    @Column(name = "SEND_AUTO", length = 1, nullable = false)
    @Builder.Default
    private String sendAuto = "Y";

    @Column(name = "STCD2", length = 11)
    private String stcd2;

    @Column(name = "CUST_NM", length = 40)
    private String custNm;

    @Column(name = "KUNNR", length = 10)
    private String kunnr;

    @Column(name = "ZGRPNO")
    private Long zgrpno;

    @Column(name = "ORDER_NO", length = 20)
    private String orderNo;

    @Column(name = "FXDAY")
    private Short fxday;

    @Column(name = "EMAIL", length = 50)
    private String email;

    @Column(name = "EMAIL2", length = 50)
    private String email2;

    @Column(name = "RECP_YM", length = 6)
    private String recpYm;

    @Column(name = "MAILDATA", columnDefinition = "TEXT")
    private String mailData;

    @Column(name = "DT_CREATE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dtCreateDate;

    @Column(name = "FILE_CREATE_FLAG", length = 1, nullable = false)
    @Builder.Default
    private String fileCreateFlag = "N";

    @Column(name = "MAIL_SEND_FLAG", length = 1, nullable = false)
    @Builder.Default
    private String mailSendFlag = "N";

    @Column(name = "ORI_HTML_FILENM", length = 100)
    private String oriHtmlFilenm;

    @Column(name = "CHG_HTML_FILENM", length = 100)
    private String chgHtmlFilenm;

    @Column(name = "HTML_FILEPATH", length = 200)
    private String htmlFilepath;

    @Column(name = "ORI_EXCEL_FILENM", length = 100)
    private String oriExcelFilenm;

    @Column(name = "CHG_EXCEL_FILENM", length = 100)
    private String chgExcelFilenm;

    @Column(name = "EXCEL_FILEPATH", length = 200)
    private String excelFilepath;

    @Column(name = "FILE_CREATE_DATE")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fileCreateDate;

    @Column(name = "UMS_CODE", length = 20)
    private String umsCode;

    @Column(name = "UMS_MSG", length = 100)
    private String umsMsg;

    @Column(name = "UMS_KEY", length = 20)
    private String umsKey;

    @Column(name = "DEL_FLAG", length = 1, nullable = false)
    @Builder.Default
    private String delFlag = "N";

    @Column(name = "CREATE_ID", length = 50)
    private String createId;

    @Column(name = "CREATE_DATE", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createDate;

    @Column(name = "UPDATE_ID", length = 50)
    private String updateId;

    @Column(name = "UPDATE_DATE", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateDate;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createDate = now;
        this.updateDate = now;
        this.dtCreateDate = now;
        
        if (this.createId == null) {
            this.createId = "BATCH_JOB";
        }
        if (this.updateId == null) {
            this.updateId = "BATCH_JOB";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDateTime.now();
        if (this.updateId == null) {
            this.updateId = "BATCH_JOB";
        }
    }
} 