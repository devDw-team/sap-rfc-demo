package com.test.sap.sap_rfc_demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 배치 실행 이력 관리 서비스
 * Spring Batch의 메타데이터 테이블을 활용하여 배치 실행 이력 확인
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchExecutionService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 이번 달에 AutoMail Job이 성공적으로 실행되었는지 확인
     * 
     * @param currentDate 현재 날짜
     * @return 이번 달에 실행되었으면 true, 아니면 false
     */
    public boolean hasAutoMailJobExecutedThisMonth(LocalDateTime currentDate) {
        try {
            // 이번 달 1일 00:00:00
            LocalDateTime startOfMonth = currentDate.withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            
            // 다음 달 1일 00:00:00
            LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
            
            String sql = """
                SELECT COUNT(*) 
                FROM BATCH_JOB_EXECUTION bje
                INNER JOIN BATCH_JOB_INSTANCE bji ON bje.JOB_INSTANCE_ID = bji.JOB_INSTANCE_ID
                WHERE bji.JOB_NAME = 'autoMailJob'
                  AND bje.STATUS = 'COMPLETED'
                  AND bje.START_TIME >= ?
                  AND bje.START_TIME < ?
                """;
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
                    startOfMonth, startOfNextMonth);
            
            boolean hasExecuted = count != null && count > 0;
            
            log.debug("이번 달({}) AutoMail Job 실행 이력: {}건", 
                    currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM")), count);
            
            return hasExecuted;
            
        } catch (Exception e) {
            log.error("AutoMail Job 실행 이력 확인 중 오류 발생", e);
            // 오류 발생 시 안전하게 false 반환 (재실행 허용)
            return false;
        }
    }

    /**
     * 특정 날짜에 AutoMail Job이 실행되었는지 확인
     * 
     * @param targetDate 확인할 날짜
     * @return 해당 날짜에 실행되었으면 true, 아니면 false
     */
    public boolean hasAutoMailJobExecutedOnDate(LocalDateTime targetDate) {
        try {
            // 해당 날짜 00:00:00 ~ 23:59:59
            LocalDateTime startOfDay = targetDate.withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = targetDate.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            
            String sql = """
                SELECT COUNT(*) 
                FROM BATCH_JOB_EXECUTION bje
                INNER JOIN BATCH_JOB_INSTANCE bji ON bje.JOB_INSTANCE_ID = bji.JOB_INSTANCE_ID
                WHERE bji.JOB_NAME = 'autoMailJob'
                  AND bje.STATUS = 'COMPLETED'
                  AND bje.START_TIME >= ?
                  AND bje.START_TIME <= ?
                """;
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
                    startOfDay, endOfDay);
            
            boolean hasExecuted = count != null && count > 0;
            
            log.debug("{}일 AutoMail Job 실행 이력: {}건", 
                    targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), count);
            
            return hasExecuted;
            
        } catch (Exception e) {
            log.error("특정 날짜 AutoMail Job 실행 이력 확인 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 최근 AutoMail Job 실행 이력 조회
     * 
     * @param limit 조회할 최대 건수
     * @return 실행 이력 정보
     */
    public String getRecentAutoMailJobExecutions(int limit) {
        try {
            String sql = """
                SELECT bje.START_TIME, bje.END_TIME, bje.STATUS, bje.EXIT_CODE
                FROM BATCH_JOB_EXECUTION bje
                INNER JOIN BATCH_JOB_INSTANCE bji ON bje.JOB_INSTANCE_ID = bji.JOB_INSTANCE_ID
                WHERE bji.JOB_NAME = 'autoMailJob'
                ORDER BY bje.START_TIME DESC
                LIMIT ?
                """;
            
            return jdbcTemplate.query(sql, new Object[]{limit}, (rs) -> {
                StringBuilder result = new StringBuilder();
                result.append("=== 최근 AutoMail Job 실행 이력 ===\n");
                
                while (rs.next()) {
                    result.append(String.format("시작: %s, 종료: %s, 상태: %s, 종료코드: %s\n",
                            rs.getTimestamp("START_TIME"),
                            rs.getTimestamp("END_TIME"),
                            rs.getString("STATUS"),
                            rs.getString("EXIT_CODE")));
                }
                
                return result.toString();
            });
            
        } catch (Exception e) {
            log.error("AutoMail Job 실행 이력 조회 중 오류 발생", e);
            return "실행 이력 조회 실패: " + e.getMessage();
        }
    }
} 