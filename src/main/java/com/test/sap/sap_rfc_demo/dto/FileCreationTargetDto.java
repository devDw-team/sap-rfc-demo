package com.test.sap.sap_rfc_demo.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 파일 생성 대상 조회 응답 DTO
 * LocalDateTime 직렬화 문제 해결을 위해 String으로 변환
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileCreationTargetDto {
    
    private Long seq;
    private String stcd2;
    private String custNm;
    private String kunnr;
    private Long zgrpno;
    private String orderNo;
    private String email;
    private String email2;
    private String dtCreateDate; // LocalDateTime -> String 변환
    private String fileCreateFlag;
    private String sendAuto;
    private String delFlag;
    
    /**
     * AutoMailData 엔티티를 DTO로 변환
     */
    public static FileCreationTargetDto from(com.test.sap.sap_rfc_demo.entity.AutoMailData entity) {
        return FileCreationTargetDto.builder()
                .seq(entity.getSeq())
                .stcd2(entity.getStcd2())
                .custNm(entity.getCustNm())
                .kunnr(entity.getKunnr())
                .zgrpno(entity.getZgrpno())
                .orderNo(entity.getOrderNo())
                .email(entity.getEmail())
                .email2(entity.getEmail2())
                .dtCreateDate(entity.getDtCreateDate() != null ? 
                    entity.getDtCreateDate().toString() : null)
                .fileCreateFlag(entity.getFileCreateFlag())
                .sendAuto(entity.getSendAuto())
                .delFlag(entity.getDelFlag())
                .build();
    }
} 