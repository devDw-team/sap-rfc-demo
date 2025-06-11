-- 메일 발송 결과 테이블 생성
-- umsmail-guide.md Step 4-2에 정의된 b2b_automail_result 테이블

CREATE TABLE IF NOT EXISTS `b2b_automail_result` (
  `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '자동 증가 ID',
  `AUTOMAIL_SEQ` bigint NOT NULL COMMENT 'b2b_automail_dt의 SEQ (FK)',
  `STCD2` varchar(20) NOT NULL COMMENT '사업자번호',
  `CUST_NM` varchar(100) NOT NULL COMMENT '고객명',
  `RECP_YM` varchar(6) NOT NULL COMMENT '청구년월 (YYYYMM)',
  `UMS_CODE` varchar(10) NULL COMMENT 'UMS API 응답 코드',
  `UMS_MSG` varchar(500) NULL COMMENT 'UMS API 응답 메시지',
  `UMS_KEY` varchar(50) NULL COMMENT 'UMS API 응답 키',
  `MAIL_SEND_FLAG` char(1) NOT NULL DEFAULT 'N' COMMENT '메일 발송 플래그 (Y:성공, E:실패, N:대기)',
  `MAIL_SEND_DATE` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '메일 발송 시간',
  `CREATE_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '레코드 생성 시간',
  PRIMARY KEY (`ID`),
  KEY `idx_automail_seq` (`AUTOMAIL_SEQ`),
  KEY `idx_stcd2` (`STCD2`),
  KEY `idx_recp_ym` (`RECP_YM`),
  KEY `idx_mail_send_date` (`MAIL_SEND_DATE`),
  KEY `idx_mail_send_flag` (`MAIL_SEND_FLAG`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='B2B 자동메일 발송 결과 테이블';

-- 인덱스 추가 설명
-- idx_automail_seq: b2b_automail_dt와의 조인을 위한 인덱스
-- idx_stcd2: 사업자번호별 조회를 위한 인덱스  
-- idx_recp_ym: 청구년월별 조회를 위한 인덱스
-- idx_mail_send_date: 발송일시별 조회를 위한 인덱스
-- idx_mail_send_flag: 발송상태별 조회를 위한 인덱스 