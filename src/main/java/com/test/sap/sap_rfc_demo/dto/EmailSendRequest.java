package com.test.sap.sap_rfc_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendRequest implements Serializable {
    private String email; // 수신 이메일 주소
    private String title; // 메일 제목
    private String contents; // 메일 본문(HTML)
    private String fromName; // 발신자명
    private String fromAddress; // 발신 이메일 주소
    private String attName1; // 첨부파일명
    private String attPath1; // 첨부파일경로

    @Override
    public String toString() {
        return "EmailSendRequest{" +
                "email='" + email + '\'' +
                ", title='" + title + '\'' +
                ", contents='" + contents + '\'' +
                ", fromName='" + fromName + '\'' +
                ", fromAddress='" + fromAddress + '\'' +
                ", attName1='" + attName1 + '\'' +
                ", attPath1='" + attPath1 + '\'' +
                '}';
    }
} 