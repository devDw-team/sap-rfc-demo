# Coway B2B Auto-mailing 개발 가이드

## 청구서 메일 자동 발송 시스템 개발 (Step 1-3)

다음은 청구서 메일 자동 발송 시스템 개발의 초기 3단계에 대한 명세입니다.

---

### 🎯 전체 개발 프로세스 개요

1.  **청구서 발송 대상 조회**: 발송이 필요한 고객 및 주문 정보를 데이터베이스에서 조회합니다.
2.  **청구서 발송 대상 적재**: 조회된 대상 중 조건에 맞는 데이터를 가공하여 별도의 테이블에 적재합니다. 이 과정에서 메일 본문에 필요한 상세 정보는 JSON 형태로 구성됩니다.
3.  **청구서 파일 생성**: 적재된 데이터를 기준으로 HTML 및 Excel 형식의 청구서 파일을 생성합니다.
4.  **메일 발송 및 결과 업데이트**: 생성된 청구서 파일을 첨부하여 메일을 발송하고, 발송 성공 여부 등의 결과를 업데이트합니다.

**현재 단계에서는 Step 1, 2, 3까지만 개발을 진행합니다.**

---

### Step 1: 청구서 발송 대상 조회

이 단계에서는 자동 메일 발송 플래그(`SEND_AUTO='Y'`)가 설정된 고객 정보를 조회합니다.

* **목적**: 청구서 자동 발송이 필요한 기본 대상 목록을 가져옵니다.
* **대상 조회 쿼리**:
    * 주의: 서브쿼리에서 다중 행 반환 가능성이 있으므로, `a` 테이블의 `STCD2` (사업자번호) 및 `KUNNR` (고객코드) 와의 명확한 상관관계 설정이 필요합니다. (예: `b.STCD2 = a.STCD2 and b.KUNNR = a.KUNNR`) 실제 테이블 컬럼명을 확인하여 `z_re_b2b_bill_info` 테이블의 사업자번호, 고객코드가 `a.STCD2`, `a.KUNNR`과 정확히 일치하도록 수정해야 합니다.
    ```sql
    SELECT 
        a.STCD2,    -- 사업자번호
        a.CUST_NM,  -- 고개명
        a.KUNNR,    -- 고객코드
        a.ZGRPNO,   -- 그룹번호
        a.ORDER_NO, -- 주문번호 
        a.FXDAY,    -- 고정일
        a.EMAIL,    -- 이메일 주소1
        a.EMAIL2,   -- 이메일 주소2
        COUNT(b.STCD2) AS chk_cnt -- 관련 청구 정보 건수
    FROM z_re_b2b_cust_info a
    LEFT JOIN z_re_b2b_bill_info b ON (
        b.STCD2 = a.STCD2 
        AND b.KUNNR = a.KUNNR 
        AND (b.ZGRPNO = a.ZGRPNO OR b.ORDER_NO = a.ORDER_NO)
    )
    WHERE a.SEND_AUTO = 'Y'
    GROUP BY a.STCD2, a.CUST_NM, a.KUNNR, a.ZGRPNO, a.ORDER_NO, 
            a.FXDAY, a.EMAIL, a.EMAIL2;    
    ```

---

### Step 2: 청구서 발송 대상 적재

Step 1에서 조회된 데이터를 필터링하고, 각 대상에 대한 상세 청구 정보를 JSON 형식으로 구성하여 `b2b_automail_dt` 테이블에 적재합니다.

1.  **적재 조건**:
    * Step 1의 조회 결과에서 `chk_cnt` 필드 값이 0보다 큰 경우에만 대상을 적재합니다.

2.  **JSON 구성 데이터 조회 조건 분기**:
    * `ZGRPNO`가 '0'이거나 NULL인 경우 (데이터 타입 및 실제 '0' 값 표현 확인 필요):
        * JSON 구성 데이터 조회 쿼리의 `WHERE` 절은 `ORDER_NO = :order_no` (해당 row의 `ORDER_NO` 값)를 사용합니다.
    * `ZGRPNO`가 '0'이 아니고 NULL도 아닌 유효한 값을 가지는 경우:
        * JSON 구성 데이터 조회 쿼리의 `WHERE` 절은 `ZGRPNO = :zgrpno` (해당 row의 `ZGRPNO` 값)를 사용합니다.

3.  **MAILDATA 필드 JSON 구성**:
    * `MAILDATA` 필드에는 다음 4가지 종류의 데이터를 조회하여 구성한 JSON 객체를 저장합니다.
        * `customer`: 고객 정보 (단일 객체)
        * `bill_summary`: 청구 년월, 납부 기한, 청구 합계 정보 (단일 객체)
        * `bill_type_summary`: 청구 대금 요약, 청구 유형별 합계 (객체 배열)
        * `bills`: 청구 정보 (여러 건, 객체 배열)

4.  **JSON 구성 데이터 조회 쿼리**:
    * 각 쿼리의 `WHERE` 절에는 Step 1에서 조회된 `STCD2` (사업자번호)와 `KUNNR` (고객코드)을 기본 조건으로 포함해야 합니다.

    * **고객 정보 (`customer`)**:
        ```sql
        -- 조건: (ZGRPNO = '0' 또는 ZGRPNO IS NULL 이면 WHERE ORDER_NO = :order_no) 또는 (ZGRPNO IS NOT NULL AND ZGRPNO != '0' 이면 WHERE ZGRPNO = :zgrpno)
        -- AND STCD2 = :stcd2 AND KUNNR = :kunnr 조건은 z_re_b2b_cust_info 테이블의 PK 또는 Unique Key 구성에 따라 필요시 추가
        SELECT STCD2, CUST_NM, J_1KFREPRE, J_1KFTBUS, J_1KFTIND, PAY_COM_TX, PAY_NO, PRE_AMT, REMAIN_AMT, PRE_MONTH
        FROM z_re_b2b_cust_info
        WHERE STCD2 = :stcd2  -- Step 1에서 조회된 STCD2
          AND KUNNR = :kunnr  -- Step 1에서 조회된 KUNNR
        /* Step 2-2 조건에 따라 AND (ORDER_NO = :order_no OR ZGRPNO = :zgrpno) 와 같은 형태로 추가될 수 있으나,
           cust_info는 보통 order_no나 zgrpno에 종속적이지 않으므로 STCD2, KUNNR로 조회.
           만약 z_re_b2b_cust_info에 ORDER_NO 또는 ZGRPNO가 있고, 이에 따라 정보가 달라진다면 조건 추가 필요. */
        LIMIT 1;
        ```

    * **청구 요약 정보 (`bill_summary`)**:
        ```sql
        -- 조건: ((:zgrpno = '0' OR :zgrpno IS NULL) AND ORDER_NO = :order_no) OR (:zgrpno IS NOT NULL AND :zgrpno != '0' AND ZGRPNO = :zgrpno)
        SELECT
            RECP_YM AS C_RECP_YM,                        -- 청구년월
            DUE_DATE AS C_DUE_DATE,                      -- 납부기한
            SUM(SUPPLY_VALUE) + SUM(VAT) AS TOTAL_AMOUNT, -- 청구합계 (공급가액 + 부가세)
            (
                SELECT COUNT(*)
                FROM z_re_b2b_bill_info sub
                WHERE sub.SEL_KUN = 'X'
                  AND sub.STCD2 = main.STCD2 -- 외부 쿼리의 STCD2 (사업자번호)
                  AND sub.KUNNR = main.KUNNR -- 외부 쿼리의 KUNNR (고객코드)
                  AND (
                       ((:zgrpno = 0 OR :zgrpno IS NULL) AND sub.ORDER_NO = :order_no)
                       OR
                       (:zgrpno IS NOT NULL AND :zgrpno != 0 AND sub.ZGRPNO = :zgrpno)
                      )
            ) AS C_SEL_KUN_CNT                   -- 선택된 고객 수 (SEL_KUN='X'인 건수)
        FROM z_re_b2b_bill_info main
        WHERE main.STCD2 = :stcd2  -- Step 1에서 조회된 STCD2
          AND main.KUNNR = :kunnr  -- Step 1에서 조회된 KUNNR
          AND (
               ((:zgrpno = 0 OR :zgrpno IS NULL) AND main.ORDER_NO = :order_no)
               OR
               (:zgrpno IS NOT NULL AND :zgrpno != 0 AND main.ZGRPNO = :zgrpno)
              )
        GROUP BY RECP_YM, DUE_DATE;
        ```

    * **청구 유형별 요약 (`bill_type_summary`)**:
        ```sql
        -- 조건: ((:zgrpno = '0' OR :zgrpno IS NULL) AND ORDER_NO = :order_no) OR (:zgrpno IS NOT NULL AND :zgrpno != '0' AND ZGRPNO = :zgrpno)
        SELECT
            RECP_TP AS C_RECP_TP,                            -- 청구유형
            RECP_TP_TX AS C_RECP_TP_TX,                      -- 청구유형명
            COUNT(RECP_TP) AS SUMMARY_CNT,                   -- 유형별 건수
            SUM(SUPPLY_VALUE) + SUM(VAT) AS SUMMARY_AMOUNT   -- 유형별 합계 (공급가액 + 부가세)
        FROM z_re_b2b_bill_info
        WHERE STCD2 = :stcd2  -- Step 1에서 조회된 STCD2
          AND KUNNR = :kunnr  -- Step 1에서 조회된 KUNNR
          AND (
               ((:zgrpno = 0 OR :zgrpno IS NULL) AND ORDER_NO = :order_no)
               OR
               (:zgrpno IS NOT NULL AND :zgrpno != 0 AND ZGRPNO = :zgrpno)
              )
        GROUP BY RECP_TP, RECP_TP_TX;
        ```

    * **청구 상세 정보 (`bills`)**:
        ```sql
        -- 조건: ((:zgrpno = '0' OR :zgrpno IS NULL) AND ORDER_NO = :order_no) OR (:zgrpno IS NOT NULL AND :zgrpno != '0' AND ZGRPNO = :zgrpno)
        SELECT
            RECP_TP_TX, ORDER_NO, VTEXT, GOODS_CD, INST_DT, USE_DUTY_MONTH,
            OWNER_DATE, USE_MONTH, RECP_YM, FIX_SUPPLY_VALUE, FIX_VAT,
            FIX_BILL_AMT, SUPPLY_VALUE, VAT, BILL_AMT, PAY_COM_TX, PAY_NO,
            INST_JUSO, GOODS_SN, DEPT_CD_TX, DEPT_TELNR, ZBIGO, GOODS_TX,
            PRE_AMT, REMAIN_AMT, PRE_MONTH
        FROM z_re_b2b_bill_info
        WHERE STCD2 = :stcd2  -- Step 1에서 조회된 STCD2
          AND KUNNR = :kunnr  -- Step 1에서 조회된 KUNNR
          AND (
               ((:zgrpno = 0 OR :zgrpno IS NULL) AND ORDER_NO = :order_no)
               OR
               (:zgrpno IS NOT NULL AND :zgrpno != 0 AND ZGRPNO = :zgrpno)
              );
        ```

5.  **JSON 포맷 예시**:
    ```json
    {
      "customer": {
        "STCD2": "123-45-67890",
        "CUST_NM": "고객명 샘플",
        "J_1KFREPRE": "대표자명",
        "J_1KFTBUS": "업태",
        "J_1KFTIND": "업종",
        "PAY_COM_TX": "결제사명",
        "PAY_NO": "결제번호",
        "PRE_AMT": 0,
        "REMAIN_AMT": 0,
        "PRE_MONTH": ""
      },
      "bill_summary": {
        "C_RECP_YM": "202305",
        "C_DUE_DATE": "20230610",
        "TOTAL_AMOUNT": 110000,
        "C_SEL_KUN_CNT": 1
      },
      "bill_type_summary": [
        {
          "C_RECP_TP": "01",
          "C_RECP_TP_TX": "정기청구",
          "SUMMARY_CNT": 2,
          "SUMMARY_AMOUNT": 55000
        },
        {
          "C_RECP_TP": "02",
          "C_RECP_TP_TX": "수시청구",
          "SUMMARY_CNT": 1,
          "SUMMARY_AMOUNT": 55000
        }
      ],
      "bills": [
        {
          "RECP_TP_TX": "정기청구",
          "ORDER_NO": "ORDER001",
          "VTEXT": "서비스 A",
          // ... (기타 청구 상세 필드들)
          "GOODS_TX": "상품명1",
          "PRE_AMT": 0,
          "REMAIN_AMT": 0,
          "PRE_MONTH": ""
        }
        // ... 추가 청구 정보
      ]
    }
    ```

6.  **대상 적재 테이블 스키마 (`b2b_automail_dt`)**:
    ```sql
    CREATE TABLE b2b_automail_dt (
        SEQ               BIGINT NOT NULL AUTO_INCREMENT COMMENT '순번',
        FORM_ID           VARCHAR(50) DEFAULT NULL COMMENT '양식 ID',
        SEND_AUTO         VARCHAR(1) NOT NULL DEFAULT 'Y' COMMENT '자동 발생 대상 Flag : SAP IF에서 조회해 옴.' ,
        STCD2             VARCHAR(11) DEFAULT NULL COMMENT '사업자번호',
        CUST_NM           VARCHAR(40) DEFAULT NULL COMMENT '사업자명',
        KUNNR             VARCHAR(10) DEFAULT NULL COMMENT '대표고객코드',
        ZGRPNO            BIGINT DEFAULT NULL COMMENT '묶음번호',
        ORDER_NO          VARCHAR(20) DEFAULT NULL COMMENT '주문번호 - Step 1 조회 결과에 있으므로 추가 필요. VARCHAR 길이는 원본 테이블 참조', -- 컬럼 추가 및 타입/길이 명시 필요
        FXDAY             SMALLINT DEFAULT NULL COMMENT '발행일',
        EMAIL             VARCHAR(50) DEFAULT NULL COMMENT '이메일1',
        EMAIL2            VARCHAR(50) DEFAULT NULL COMMENT '이메일2',
        MAILDATA          TEXT DEFAULT NULL COMMENT '청구서생성데이터(json 포맷)',
        DT_CREATE_DATE    DATETIME DEFAULT NULL COMMENT '데이터수집일',
        FILE_CREATE_FLAG  VARCHAR(1) NOT NULL DEFAULT 'N' COMMENT '파일생성Flag : 첨부파일 생성 후 Y 업데이트',
        ORI_HTML_FILENM   VARCHAR(100) DEFAULT NULL COMMENT 'HTML파일명(원본)',
        CHG_HTML_FILENM   VARCHAR(100) DEFAULT NULL COMMENT 'HTML파일명(변환)',
        HTML_FILEPATH     VARCHAR(200) DEFAULT NULL COMMENT 'HTML 경로',
        ORI_EXCEL_FILENM  VARCHAR(100) DEFAULT NULL COMMENT 'EXCEL파일명(원본)',
        CHG_EXCEL_FILENM  VARCHAR(100) DEFAULT NULL COMMENT 'EXCEL파일명(변환)',
        EXCEL_FILEPATH    VARCHAR(200) DEFAULT NULL COMMENT 'EXCEL 경로',
        FILE_CREATE_DATE  DATETIME DEFAULT NULL COMMENT '파일생성일',
        UMS_CODE          VARCHAR(20) DEFAULT NULL COMMENT '메일발송결과코드',
        UMS_MSG           VARCHAR(100) DEFAULT NULL COMMENT '메일발송결과메세지',
        UMS_KEY           VARCHAR(20) DEFAULT NULL COMMENT 'UMS Key',
        DEL_FLAG          VARCHAR(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부',
        CREATE_ID         VARCHAR(50) DEFAULT NULL COMMENT '등록자',
        CREATE_DATE       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '최초 등록일',
        UPDATE_ID         VARCHAR(50) DEFAULT NULL COMMENT '수정자',
        UPDATE_DATE       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일',
        PRIMARY KEY (SEQ)
    );
    ```
    * **참고**: `b2b_automail_dt` 테이블 정의에 `ORDER_NO` 컬럼이 누락되어 있다면 추가해야 합니다. (Step 1 조회 결과에 `ORDER_NO`가 포함됨) 위 스크립트에 주석으로 표시해두었습니다. 실제 테이블에 맞게 컬럼 타입과 길이를 지정해주세요.

7.  **데이터 적재 쿼리**:
    * Step 1에서 조회된 각 row 데이터와 위에서 생성된 `MAILDATA` (JSON 문자열)를 사용하여 `b2b_automail_dt` 테이블에 삽입합니다.
    ```sql
    INSERT INTO b2b_automail_dt (
        SEND_AUTO, STCD2, CUST_NM, KUNNR, ZGRPNO, ORDER_NO, FXDAY, EMAIL, EMAIL2,
        MAILDATA, DT_CREATE_DATE, FILE_CREATE_FLAG, DEL_FLAG,
        CREATE_ID, CREATE_DATE, UPDATE_ID, UPDATE_DATE, FORM_ID /* 필요시 추가 */
    ) VALUES (
        :send_auto, :stcd2, :cust_nm, :kunnr, :zgrpno, :order_no, :fxday, :email, :email2,
        :maildata,    -- JSON 문자열
        NOW(),       -- 또는 SYSDATE, CURRENT_TIMESTAMP 등 DBMS에 맞게
        'N',         -- 파일 생성 전이므로 'N'
        'N',         -- 삭제 플래그 기본 'N'
        'BATCH_JOB', -- 생성자 ID
        NOW(),       -- 생성일시
        'BATCH_JOB', -- 수정자 ID
        NOW(),       -- 수정일시
        :form_id    -- 양식 ID (필요한 경우 바인딩)
    );
    ```
    * **참고**: `SEND_AUTO`는 Step 1에서 이미 'Y'로 필터링되었으므로, 해당 값을 그대로 사용하거나 적재 시 별도 관리할 수 있습니다. `ORDER_NO`도 Step 1 조회 결과에 있으므로 적재 대상에 포함합니다. `FORM_ID` 등 다른 필요한 기본값이 있다면 INSERT 문에 포함합니다.

---

### Step 3: Spring Batch 개발 추가 작업

Step 1과 Step 2의 로직을 실행하는 Spring Batch Job을 개발합니다.

* **실행 주기**:
    * 매달 **3일 오전 07:00**에 실행
    * **3일이 공휴일인 경우** 다음 평일에 자동 연기 실행
    * 공휴일 판단: 토요일, 일요일 및 주요 법정공휴일

* **작업 내용**:
    1.  Step 1의 "청구서 발송 대상 조회" 쿼리를 실행 (Reader).
    2.  조회된 각 row에 대해 다음을 수행 (Processor):
        * `chk_cnt > 0` 조건 확인.
        * 조건 불만족 시 해당 데이터는 필터링 (스킵).
        * 조건 만족 시, 해당 row의 `STCD2`, `KUNNR`, `ZGRPNO`, `ORDER_NO` 등을 기반으로 Step 2-2의 분기 로직에 따라 JSON 구성 데이터 조회 쿼리들을 실행하여 각 JSON 부분을 가져옴.
        * 조회된 데이터로 `MAILDATA` JSON 객체 생성.
    3.  Processor에서 가공된 최종 데이터를 Step 2-7의 "데이터 적재 쿼리"를 사용하여 `b2b_automail_dt` 테이블에 데이터 삽입 (Writer).

---

## 📁 개발 완료된 파일 구조 및 설명

### 🗂️ 폴더 구조
```
sap-rfc-demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── test/
│   │   │           └── sap/
│   │   │               └── sap_rfc_demo/
│   │   │                   ├── config/
│   │   │                   │   ├── BatchConfig.java              # [수정] AutoMail + FileCreation Batch Job 추가
│   │   │                   │   ├── BatchSchedulerConfig.java     # [수정] AutoMail + FileCreation 스케줄러 추가
│   │   │                   │   └── FileCreationConfig.java       # [신규] 파일 생성 설정
│   │   │                   ├── controller/
│   │   │                   │   ├── AutoMailController.java       # [수정] AutoMail 관리 컨트롤러 (파일 생성 기능 추가)
│   │   │                   │   └── FileCreationController.java   # [신규] 파일 생성 관리 컨트롤러
│   │   │                   ├── dto/
│   │   │                   │   ├── AutoMailTargetDto.java        # [신규] 발송 대상 조회 DTO
│   │   │                   │   ├── MailDataDto.java              # [신규] JSON 구성 데이터 DTO
│   │   │                   │   └── FileCreationTargetDto.java    # [신규] 파일 생성 대상 DTO
│   │   │                   ├── entity/
│   │   │                   │   └── AutoMailData.java             # [수정] AutoMail 엔티티 (파일 생성 관련 필드 추가)
│   │   │                   ├── repository/
│   │   │                   │   └── AutoMailDataRepository.java   # [수정] AutoMail Repository (파일 생성 대상 조회 추가)
│   │   │                   ├── scheduler/
│   │   │                   │   └── FileCreationScheduler.java    # [신규] 파일 생성 스케줄러
│   │   │                   └── service/
│   │   │                       ├── AutoMailService.java          # [신규] AutoMail 서비스
│   │   │                       └── FileCreationService.java      # [신규] 파일 생성 서비스
│   │   └── resources/
│   │       ├── sql/
│   │       │   └── create_automail_table.sql                     # [신규] 테이블 생성 스크립트
│   │       ├── templates/
│   │       │   └── automail/
│   │       │       └── dashboard.html                            # [수정] 관리 대시보드 (파일 생성 기능 추가)
│   │       └── application.properties                            # [수정] 파일 생성 설정 추가
└── doc/
    ├── automail-guide.md                                         # [수정] 개발 가이드 업데이트
    ├── filecreate-guide.md                                       # [신규] 파일 생성 개발 가이드
    └── README_FILE_CREATION.md                                   # [신규] 파일 생성 기능 상세 문서
```

### 📋 신규 생성 파일 설명

#### 1. **AutoMailData.java** (Entity)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/entity/AutoMailData.java`
- **역할**: `b2b_automail_dt` 테이블과 매핑되는 JPA 엔티티
- **주요 기능**:
  - automail-guide.md Step 2-6에 정의된 테이블 구조 매핑
  - 자동 생성/수정 일시 관리 (`@PrePersist`, `@PreUpdate`)
  - Lombok을 활용한 Builder 패턴 지원
  - 파일 생성 관련 필드 추가 (RECP_YM, MAIL_SEND_FLAG)

#### 2. **AutoMailDataRepository.java** (Repository)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/repository/AutoMailDataRepository.java`
- **역할**: AutoMail 데이터 조회 및 관리를 위한 JPA Repository
- **주요 기능**:
  - 자동메일 플래그별 조회
  - 파일 생성 상태별 조회
  - 중복 데이터 체크
  - 배치 처리 대상 조회
  - 메일 발송 대상 조회
  - 파일 생성 대상 조회 (`findFileCreationTargets()`)

#### 3. **AutoMailTargetDto.java** (DTO)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/dto/AutoMailTargetDto.java`
- **역할**: automail-guide.md Step 1 청구서 발송 대상 조회 결과 매핑
- **주요 기능**:
  - 적재 조건 확인 메서드 (`isValidForProcessing()`)
  - ZGRPNO 조건 분기 확인 메서드 (`useOrderNoCondition()`, `useZgrpnoCondition()`)

#### 4. **MailDataDto.java** (DTO)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/dto/MailDataDto.java`
- **역할**: automail-guide.md Step 2-5 JSON 구성 데이터 매핑
- **주요 기능**:
  - 4가지 JSON 구성 요소 정의 (Customer, BillSummary, BillTypeSummary, Bill)
  - 중첩 클래스 구조로 JSON 계층 표현
  - 모든 필드에 대한 타입 안전성 보장

#### 5. **FileCreationTargetDto.java** (DTO)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/dto/FileCreationTargetDto.java`
- **역할**: filecreate-guide.md Step 1 파일 생성 대상 조회 결과 매핑
- **주요 기능**:
  - AutoMailData 엔티티를 DTO로 변환
  - LocalDateTime 직렬화 문제 해결
  - 파일 생성 상태 정보 제공

#### 6. **AutoMailService.java** (Service)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/service/AutoMailService.java`
- **역할**: automail-guide.md Step 1, 2 로직 구현
- **주요 기능**:
  - Step 1: 청구서 발송 대상 조회 (`getAutoMailTargets()`)
  - Step 2: 청구서 발송 대상 적재 (`processAutoMailTargets()`)
  - JSON 구성 데이터 생성 (`createMailData()`)
  - 4가지 데이터 조회 메서드 (고객정보, 청구요약, 유형별요약, 상세정보)
  - 전체 프로세스 실행 (`executeAutoMailProcess()`)

#### 7. **FileCreationService.java** (Service)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/service/FileCreationService.java`
- **역할**: filecreate-guide.md Step 1-3 파일 생성 로직 구현
- **주요 기능**:
  - Step 1: 파일 생성 대상 조회 (`getFileCreationTargets()`)
  - Step 2: HTML/Excel 파일 생성 (`createHtmlFile()`, `createExcelFile()`)
  - Step 3: DB 업데이트 (`updateFileCreationStatus()`)
  - 보안 기능 (사업자번호 마스킹, Path Traversal 방지)
  - 대용량 Excel 처리 최적화 (SXSSFWorkbook)

#### 8. **AutoMailController.java** (Controller)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/controller/AutoMailController.java`
- **역할**: AutoMail 기능의 수동 실행 및 모니터링 제공
- **주요 기능**:
  - 관리 대시보드 페이지 제공 (`/automail/dashboard`)
  - Step 1 수동 실행 API (`/automail/api/step1/targets`)
  - 전체 프로세스 수동 실행 API (`/automail/api/execute`)
  - Batch Job 수동 실행 API (`/automail/api/batch/run`)
  - 데이터 조회 및 통계 API
  - LocalDateTime 직렬화 문제 해결 (DTO 변환)

#### 9. **FileCreationController.java** (Controller)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/controller/FileCreationController.java`
- **역할**: 파일 생성 기능의 수동 실행 및 모니터링 제공
- **주요 기능**:
  - 파일 생성 대상 조회 API (`/api/file-creation/targets`)
  - 개별 파일 생성 API (`/api/file-creation/create/{seq}`)
  - 전체 파일 생성 API (`/api/file-creation/execute-all`)
  - 배치 실행 API (`/api/file-creation/batch/execute`)
  - 통계 정보 API (`/api/file-creation/statistics`)
  - 테스트용 샘플 데이터 생성 API

#### 10. **FileCreationScheduler.java** (Scheduler)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/scheduler/FileCreationScheduler.java`
- **역할**: 파일 생성 배치 작업 스케줄링
- **주요 기능**:
  - 매월 4일 오전 8시 자동 실행 (`@Scheduled(cron = "0 0 8 4 * ?")`)
  - 수동 실행 메서드 제공
  - 실행 이력 로깅

#### 11. **FileCreationConfig.java** (Configuration)
- **위치**: `src/main/java/com/test/sap/sap_rfc_demo/config/FileCreationConfig.java`
- **역할**: 파일 생성 관련 Bean 설정
- **주요 기능**:
  - ObjectMapper Bean 설정
  - 파일 경로 설정
  - 기타 필요한 Bean 구성

#### 12. **dashboard.html** (Template)
- **위치**: `src/main/resources/templates/automail/dashboard.html`
- **역할**: AutoMail 관리 대시보드 웹 페이지
- **주요 기능**:
  - 실시간 통계 카드 (오늘 생성, 전체 활성, 파일 생성 완료, 메일 발송 완료)
  - 수동 실행 버튼 (Step 1, Step 1+2, Batch Job, 파일 생성 관련)
  - 전체 데이터 목록 표시 (클라이언트 사이드 페이징)
  - 상세 정보 모달 (발송일, 청구년월, 메일발송상태 포함)
  - Bootstrap 5 기반 반응형 디자인

#### 13. **create_automail_table.sql** (SQL)
- **위치**: `src/main/resources/sql/create_automail_table.sql`
- **역할**: `b2b_automail_dt` 테이블 생성 스크립트
- **주요 기능**:
  - automail-guide.md Step 2-6에 정의된 테이블 구조
  - 성능 최적화를 위한 인덱스 생성
  - 한글 지원을 위한 utf8mb4 charset 설정

#### 14. **filecreate-guide.md** (Documentation)
- **위치**: `doc/filecreate-guide.md`
- **역할**: 청구서 파일 생성 기능 개발 가이드
- **주요 내용**:
  - Step 1-4 상세 명세
  - 파일 생성 규칙 및 경로 정의
  - Spring Batch 구성 방법
  - 보안 및 성능 고려사항

#### 15. **README_FILE_CREATION.md** (Documentation)
- **위치**: `doc/README_FILE_CREATION.md`
- **역할**: 파일 생성 기능 상세 문서
- **주요 내용**:
  - 전체 아키텍처 설명
  - API 명세서
  - 설정 방법
  - 트러블슈팅 가이드

### 📝 수정된 파일 설명

#### 1. **BatchConfig.java** (수정)
- **수정 내용**: AutoMail + FileCreation Batch Job 구성 추가
- **추가된 Bean**:
  - `autoMailJob`: AutoMail 배치 Job 정의
  - `autoMailStep`: AutoMail 배치 Step 정의
  - `autoMailTasklet`: AutoMailService 호출 Tasklet
  - `fileCreationBatchJob`: 파일 생성 배치 Job 정의
  - `fileCreationBatchStep`: 파일 생성 배치 Step 정의
- **주석 표시**: `// ========== AutoMail + FileCreation Batch Job 구성 ==========`

#### 2. **BatchSchedulerConfig.java** (수정)
- **수정 내용**: AutoMail + FileCreation 스케줄러 추가
- **추가된 기능**:
  - `runAutoMailJob()`: 월~금요일 08:00 실행 스케줄러
  - `runFileCreationJob()`: 매월 4일 08:00 실행 스케줄러
  - `@Scheduled(cron = "0 0 7 * * *")` 설정 (매달 3일 조건부 실행)
- **주석 표시**: `// ========== AutoMail + FileCreation Batch Job 추가 ==========`

#### 3. **application.properties** (수정)
- **수정 내용**: 파일 생성 관련 설정 추가
- **추가된 설정**:
  - `file.creation.base.path`: 파일 생성 기본 경로
  - `file.creation.html.path`: HTML 파일 경로
  - `file.creation.excel.path`: Excel 파일 경로
  - Jackson 설정 (LocalDateTime 직렬화)

### 🚀 실행 방법

#### 1. **테이블 생성**
```sql
-- src/main/resources/sql/create_automail_table.sql 실행
```

#### 2. **웹 대시보드 접속**
```
http://localhost:8080/automail/dashboard
```

#### 3. **API 직접 호출**
```bash
# AutoMail API
GET /automail/api/step1/targets
POST /automail/api/execute
POST /automail/api/batch/run

# FileCreation API
GET /api/file-creation/targets
POST /api/file-creation/create/{seq}
POST /api/file-creation/execute-all
POST /api/file-creation/batch/execute
```

#### 4. **자동 스케줄링**
- AutoMail: 매주 월~금요일 오전 08:00에 자동 실행
- FileCreation: 매월 4일 오전 08:00에 자동 실행
- 로그에서 실행 결과 확인 가능

### 🔧 주요 특징

1. **완전한 트랜잭션 관리**: `@Transactional` 어노테이션으로 데이터 정합성 보장
2. **중복 데이터 방지**: Repository의 `countDuplicateData()` 메서드로 중복 체크
3. **조건부 분기 처리**: ZGRPNO 값에 따른 동적 쿼리 생성
4. **JSON 데이터 구성**: Jackson ObjectMapper를 활용한 안전한 JSON 변환
5. **실시간 모니터링**: 웹 대시보드를 통한 실행 상태 및 통계 확인
6. **에러 처리**: 각 단계별 예외 처리 및 로깅
7. **성능 최적화**: 인덱스 설정 및 효율적인 쿼리 구성
8. **보안 강화**: 사업자번호 마스킹, Path Traversal 방지
9. **대용량 처리**: SXSSFWorkbook을 활용한 3000행 Excel 최적화
10. **유연한 파일 관리**: 동적 파일명 생성 및 경로 관리
11. **클라이언트 사이드 페이징**: 효율적인 대용량 데이터 표시
12. **메일 발송 상태 관리**: MAIL_SEND_FLAG를 통한 발송 상태 분리

---