package com.test.sap.sap_rfc_demo.dto;

import java.io.Serializable;

public class EmailSendRequest implements Serializable {
    private String email; // 수신 이메일 주소
    private String title; // 메일 제목
    private String contents; // 메일 본문(HTML)
    private String fromName; // 발신자명
    private String fromAddress; // 발신 이메일 주소
    private String attName1; // 첨부파일명
    private String attPath1; // 첨부파일경로

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContents() { return contents; }
    public void setContents(String contents) { this.contents = contents; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getAttName1() { return attName1; }
    public void setAttName1(String attName1) { this.attName1 = attName1; }

    public String getAttPath1() { return attPath1; }
    public void setAttPath1(String attPath1) { this.attPath1 = attPath1; }

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