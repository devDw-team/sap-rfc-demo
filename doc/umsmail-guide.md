# Coway B2B Auto-mailing 개발 가이드

---

## 🎯 전체 개발 프로세스 개요

1.  **청구서 발송 대상 조회**: 발송이 필요한 고객 및 주문 정보를 데이터베이스에서 조회합니다. (개발 완료)
2.  **청구서 발송 대상 적재**: 조회된 대상 중 조건에 맞는 데이터를 가공하여 별도의 테이블에 적재합니다. 이 과정에서 메일 본문에 필요한 상세 정보는 JSON 형태로 구성됩니다. (개발 완료)
3.  **청구서 파일 생성**: 적재된 데이터를 기준으로 HTML 및 Excel 형식의 청구서 파일을 생성합니다. (개발 완료)
4.  **메일 발송 및 결과 업데이트**: 생성된 청구서 파일을 첨부하여 메일을 발송하고, 발송 성공 여부 등의 결과를 업데이트합니다. (개발 진행 중)

---

## 🚀 Step 4: 메일 발송 및 결과 업데이트 개발 상세

### Step 4-1: 메일 발송 대상 조회

데이터베이스 `b2b_automail_dt` 테이블에서 현재 발송해야 할 메일 대상을 조회합니다.

* **실행 쿼리:**

    ```sql
    SELECT
        SEQ, -- 업데이트 시 WHERE 조건에 사용될 기본 키
        STCD2,
        CUST_NM,
        RECP_YM,
        SUBSTRING(RECP_YM, 1, 4) AS C_RECP_YEAR,
        CAST(SUBSTRING(RECP_YM, 5, 2) AS UNSIGNED) AS C_RECP_MONTH,
        EMAIL,
        EMAIL2,
        ORI_HTML_FILENM,
        CHG_HTML_FILENM,
        HTML_FILEPATH,
        FXDAY
    FROM
        b2b_automail_dt
    WHERE
        SEND_AUTO = 'Y'                -- 자동 발송 대상
        AND FILE_CREATE_FLAG = 'Y'     -- 청구서 파일 생성 완료 건
        AND MAIL_SEND_FLAG = 'N'       -- 아직 메일 발송되지 않은 건
        AND FXDAY = DAY(CURDATE())     -- 청구서 생성 기준일(FXDAY)이 오늘 날짜의 '일(DAY)'과 일치하는 건
    ORDER BY
        SEQ; -- 처리 순서를 위한 정렬 (결과는 다중 row가 될 수 있음)
    ```

* **주요 조회 조건:**
    * `SEND_AUTO = 'Y'`: 자동 메일 발송이 승인된 대상입니다.
    * `FILE_CREATE_FLAG = 'Y'`: 청구서 HTML 및 Excel 파일 생성이 완료된 대상입니다.
    * `MAIL_SEND_FLAG = 'N'`: 아직 메일 발송 처리가 되지 않은 대상입니다.
    * `FXDAY = DAY(CURDATE())`: `FXDAY` 컬럼의 값(일)이 오늘 날짜의 '일'과 동일한 대상을 필터링합니다. (예: 오늘이 5월 15일이면 `FXDAY`가 15인 데이터)
* **데이터 검증 조건:**
    * 이메일 주소 형식 유효성 검사 (`EMAIL` 필드 정규식 검증)
    * 첨부파일 경로 및 파일 존재 여부 확인
    * 필수 필드 NULL 체크 (`STCD2`, `CUST_NM`, `EMAIL`)
    * 중복 발송 방지를 위한 당일 발송 이력 체크
* **결과:** 위 조건에 부합하는 여러 개의 발송 대상 데이터가 반환될 수 있습니다. 각 row는 개별 메일 발송 건에 해당합니다.

### Step 4-2: 메일 발송 및 발송 결과 업데이트

Step 4-1에서 조회된 각 메일 발송 대상에 대해 다음 작업을 순차적으로 수행합니다.

1.  **메일 콘텐츠 준비**:
    * 메일 템플릿 파일 경로: `/resources/static/mail/webmail.html`
    * 템플릿 내 변수와 Step 4-1에서 조회한 데이터를 매핑합니다.
        * `${CUST_NM}`: `CUST_NM` 필드 값으로 치환
        * `${C_RECP_YEAR}`: `C_RECP_YEAR` 필드 값으로 치환
        * `${C_RECP_MONTH}`: `C_RECP_MONTH` 필드 값으로 치환

2.  **이메일 API 호출** 
    * 이메일 발송 참조 소스: EmailService.java
    * **추가 구현 필요사항:**
        * HTML 템플릿 파일 읽기 및 변수 치환 기능
        * 파일 첨부 경로 검증 및 처리
        * UMS API 응답 JSON 파싱 및 결과 반환
    * **API 요청 파라미터 설정:**
        * `EMAIL`: Step 4-1에서 조회된 `EMAIL` 필드 값
        * `TITLE`: "코웨이(주) \[C\_RECP\_YEAR]년 \[C\_RECP\_MONTH]월 대금청구서" (예: "코웨이(주) 2025년 5월 대금청구서")
        * `CONTENTS`: 1번 항목에서 동적으로 생성된 HTML 소스
        * `FROMNAME`: "Coway"
        * `FROMADDRESS`: "noreply@coway.com"
        * `ATTNAME1`: Step 4-1에서 조회된 `ORI_HTML_FILENM` 필드 값 (원본파일명)
        * `ATTPATH1`: Step 4-1에서 조회된 `HTML_FILEPATH` + `CHG_HTML_FILENM` (실제 첨부될 파일의 전체 경로)

3.  **메일 발송 결과 처리 및 DB 업데이트**:
    * **UMS (통합 메시징 시스템) API 응답 형식 (JSON):**
        ```json
        {
          "code": "13",
          "msg": "요청성공",
          "key": "248395725"
        }
        ```
    * **예외 처리 및 재처리 전략:**
        * API 호출 실패 시: `UMS_CODE = "ERROR"`, `UMS_MSG = [에러 메시지]`, `MAIL_SEND_FLAG = "N"` 유지
        * 파일 첨부 실패 시: 로그 기록 후 관리자 알림
        * 템플릿 변수 치환 실패 시: 기본값 사용 또는 처리 중단
        * 최대 재시도 횟수: 3회 (1시간 간격)
    * **결과 업데이트 쿼리 실행 (각 메일 발송 건당):**
        API 응답으로부터 `code`, `msg`, `key` 값을 추출하여 `b2b_automail_dt` 테이블의 해당 레코드를 업데이트합니다.
        ```sql
        UPDATE b2b_automail_dt
        SET
            UMS_CODE = [API 응답 JSON의 "code" 값],
            UMS_MSG = [API 응답 JSON의 "msg" 값],
            UMS_KEY = [API 응답 JSON의 "key" 값],
            MAIL_SEND_FLAG = 'Y' -- 메일 발송 성공으로 상태 변경
        WHERE
            SEQ = [Step 4-1에서 조회된 해당 건의 SEQ 값];
        ```

### Step 4-3: 배치(Batch Job) 작업 추가

위 Step 4-1(메일 발송 대상 조회)과 Step 4-2(조회 데이터 기반 메일 발송 및 결과 업데이트) 로직을 주기적으로 실행하기 위한 배치 작업을 등록합니다.

* **실행 스케줄**: 매일 오전 8시
* **성능 최적화 고려사항:**
    * 배치 처리 시 한 번에 처리할 최대 건수: 100건
    * 메일 발송 API 호출 시 Rate Limiting 적용 (초당 최대 5건)
    * 대용량 첨부파일 처리 시 메모리 사용량 모니터링
* **보안 고려사항:**
    * 개인정보 포함 로그는 마스킹 처리
    * 메일 발송 실패 시 고객 정보 노출 방지
    * UMS API 인증 키 암호화 저장
* **모니터링 및 알림:**
    * 배치 작업 성공/실패 상태 로깅
    * 연속 실패 시 관리자 이메일 알림
    * 일일 발송 통계 리포트 생성

---