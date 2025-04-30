# SAP RFC Demo 개발 가이드

## 1. 프로젝트 개요
이 프로젝트는 SAP 시스템과의 RFC 통신을 구현한 Spring Boot 기반의 웹 애플리케이션입니다. SAP JCo를 사용하여 SAP 시스템의 RFC를 호출하고, 그 결과를 웹 페이지와 JSON API로 제공합니다.

## 2. 프로젝트 구조

### 2.1 폴더 구조
```
sap-rfc-demo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── test/
│   │   │           └── sap/
│   │   │               └── sap_rfc_demo/
│   │   │                   ├── config/           # 설정 클래스
│   │   │                   ├── controller/       # 컨트롤러
│   │   │                   │   ├── SapController.java      # 웹 페이지 컨트롤러
│   │   │                   │   ├── SapApiController.java   # API 컨트롤러
│   │   │                   │   └── EmailController.java    # 이메일 발송 컨트롤러
│   │   │                   ├── dto/             # 데이터 전송 객체
│   │   │                   │   ├── CustomerInfoResponse.java  # 고객 정보 응답 DTO
│   │   │                   │   ├── BillInfoResponse.java     # 청구 정보 응답 DTO
│   │   │                   │   └── EmailSendRequest.java     # 이메일 발송 DTO
│   │   │                   ├── entity/          # JPA 엔티티
│   │   │                   │   ├── SapCustomerInfo.java     # 고객 정보 엔티티
│   │   │                   │   └── SapBillInfo.java         # 청구 정보 엔티티
│   │   │                   ├── repository/      # JPA 레포지토리
│   │   │                   │   ├── SapCustomerInfoRepository.java  # 고객 정보 레포지토리
│   │   │                   │   └── SapBillInfoRepository.java      # 청구 정보 레포지토리
│   │   │                   ├── service/         # 서비스
│   │   │                   │   ├── SapService.java              # SAP RFC 서비스
│   │   │                   │   ├── SapCustomerInfoService.java  # 고객 정보 서비스
│   │   │                   │   ├── SapBillInfoService.java      # 청구 정보 서비스
│   │   │                   │   ├── SapConnectionTestService.java # 연결 테스트 서비스
│   │   │                   │   └── EmailService.java            # 이메일 발송 서비스
│   │   │                   └── SapRfcDemoApplication.java
│   │   └── resources/
│   │       ├── static/      # 정적 리소스
│   │       ├── templates/   # Thymeleaf 템플릿
│   │       │   ├── customer-info.html     # 고객 정보 조회 페이지
│   │       │   ├── bill-info.html         # 청구 정보 조회 페이지
│   │       │   ├── connection-test.html   # 연결 테스트 페이지
│   │       │   └── email-form.html        # 이메일 발송 폼
│   │       └── application.properties
│   └── test/               # 테스트 코드
├── doc/                    # 문서
├── target/                 # 빌드 결과물
└── pom.xml                 # Maven 설정
```

### 2.2 주요 패키지 설명
- `config`: SAP 연결 설정 및 기타 설정 클래스
- `controller`: 웹 요청 처리 및 API 엔드포인트
- `dto`: 데이터 전송 객체
- `entity`: JPA 엔티티
- `repository`: JPA 레포지토리 인터페이스
- `service`: 비즈니스 로직 및 SAP RFC 호출
- `templates`: Thymeleaf 템플릿 파일

### 2.3 주요 파일 설명
#### config 패키지
- `SapConfig.java`: SAP JCo 연결 설정을 관리하는 설정 클래스
- `CustomDestinationDataProvider.java`: SAP JCo Destination 데이터 제공자 구현 클래스

#### controller 패키지
- `SapController.java`: 웹 페이지 요청을 처리하는 컨트롤러
- `SapApiController.java`: JSON API 요청을 처리하는 REST 컨트롤러
- `EmailController.java`: 이메일 발송 컨트롤러

#### dto 패키지
- `CustomerInfoResponse.java`: 고객 정보 API 응답을 위한 DTO 클래스
- `BillInfoResponse.java`: 청구 정보 API 응답을 위한 DTO 클래스
- `EmailSendRequest.java`: 이메일 발송 DTO

#### entity 패키지
- `SapCustomerInfo.java`: 고객 정보 JPA 엔티티
- `SapBillInfo.java`: 청구 정보 JPA 엔티티

#### repository 패키지
- `SapCustomerInfoRepository.java`: 고객 정보 JPA 레포지토리
- `SapBillInfoRepository.java`: 청구 정보 JPA 레포지토리

#### service 패키지
- `SapService.java`: SAP RFC 호출 및 데이터 처리를 담당하는 서비스 클래스
- `SapCustomerInfoService.java`: 고객 정보 처리 서비스
- `SapBillInfoService.java`: 청구 정보 처리 서비스
- `SapConnectionTestService.java`: SAP 연결 테스트를 위한 서비스 클래스
- `EmailService.java`: 이메일 발송 REST API 연동 서비스

#### templates 패키지
- `customer-info.html`: 고객 정보 조회 웹 페이지 템플릿
- `bill-info.html`: 청구 정보 조회 웹 페이지 템플릿
- `connection-test.html`: SAP 연결 테스트 웹 페이지 템플릿
- `email-form.html`: 이메일 발송 입력 폼 템플릿

#### resources 패키지
- `application.properties`: 애플리케이션 설정 파일
- `lib/sapjco3.jar`: SAP JCo 라이브러리 파일

## 3. 주요 기능

### 3.1 SAP RFC 호출
- RFC 함수명: `Z_RE_B2B_CUST_INFO`
  - 입력 파라미터: `IV_ERDAT` (날짜)
  - 출력 파라미터: 
    - `ES_RETURN`: 반환 정보
    - `ET_CUST_DATA`: 고객 데이터 테이블
- RFC 함수명: `Z_RE_B2B_BILL_INFO`
  - 입력 파라미터: `IV_RECP_YM` (청구년월)
  - 출력 파라미터:
    - `ES_RETURN`: 반환 정보
    - `ET_BILL_DATA`: 청구 데이터 테이블

### 3.2 API 엔드포인트
1. 웹 페이지
   - URL: `/customer-info`
   - Method: GET
   - 파라미터: `erdat` (YYYYMMDD 형식, 기본값: 20250423)
   - 설명: 고객 정보 조회 및 DB 저장
   
   - URL: `/bill-info`
   - Method: GET
   - 파라미터: `recpYm` (YYYYMM 형식, 기본값: 202501)
   - 설명: 청구 정보 조회

2. JSON API
   - URL: `/api/customer-info`
   - Method: GET
   - 파라미터: `erdat` (YYYYMMDD 형식, 기본값: 20250423)
   - 응답 형식: JSON
   - 설명: 고객 정보 조회 후 DB 저장 및 원본 데이터 반환
   - 응답 예시:
     ```json
     {
       "ES_RETURN": {
         "TYPE": "S",
         "MESSAGE": "정상처리되었습니다"
       },
       "ET_CUST_DATA": [
         {
           "ORDER_NO": "...",
           "STCD2": "...",
           // ... 기타 필드들
         }
       ]
     }
     ```

   - URL: `/api/customer-info-json`
   - Method: GET
   - 파라미터: `erdat` (YYYYMMDD 형식, 기본값: 20250423)
   - 응답 형식: JSON
   - 설명: SAP RFC 호출 결과를 가공하여 JSON 형식으로 반환 (DB 저장 없음)
   - 응답 예시:
     ```json
     {
       "status": "S",
       "message": "정상처리되었습니다",
       "customerList": [
         {
           "ORDER_NO": "...",
           "STCD2": "...",
           // ... 기타 필드들
         }
       ],
       "searchDate": "20250423"
     }
     ```

   - URL: `/api/bill-info`
   - Method: GET
   - 파라미터: `recpYm` (YYYYMM 형식, 기본값: 202501)
   - 응답 형식: JSON
   - 설명: 청구 정보 조회 후 DB 저장 및 원본 데이터 반환
   - 응답 예시:
     ```json
     {
       "returnInfo": {
         "TYPE": "S",
         "MESSAGE": "정상처리되었습니다"
       },
       "billList": [
         {
           "ORDER_NO": "...",
           "STCD2": "...",
           // ... 기타 필드들
         }
       ]
     }
     ```

### 3.3 이메일 발송 기능
- 이메일 발송 폼에서 수신자, 제목, 본문(HTML), 발신자명, 발신 이메일을 입력받아 외부 REST API로 이메일을 발송
- 외부 API 연동 정보는 application.properties에서 관리

## 4. 개발 환경 설정

### 4.1 필수 요구사항
- Java 17
- Maven
- SAP JCo 3.0
- Spring Boot 3.4.4

### 4.2 SAP JCo 설정
1. `sapjco3.jar` 파일을 `src/main/resources/lib` 폴더에 복사
2. `sapjco3.dll` 파일을 시스템 경로에 복사
   - Windows: `C:\Windows\System32`
   - Linux: `/usr/lib`

### 4.3 application.properties 설정
```properties
# SAP Connection Properties
sap.client.mshost=xxx.xxx.xxx.xxx
sap.client.client=100
sap.client.user=userid
sap.client.passwd=userpwd
sap.client.lang=KO
sap.client.group=TITAN
sap.client.r3name=COQ
sap.destination.peak_limit=5
sap.destination.pool_capacity=2

# Database Properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# 이메일 발송 API 설정
email.api.url=https://xxx.xxx.xxx.xxx
email.api.auth-id=userid
email.api.auth-key=...
```

## 5. 빌드 및 실행

### 5.1 빌드
```bash
mvn clean package
```

### 5.2 실행
```bash
java -jar target/sap-rfc-demo-0.0.1-SNAPSHOT.jar
```

## 6. 테스트

### 6.1 웹 페이지 테스트
1. 웹 브라우저에서 접속:
   - 고객 정보:  
     `http://localhost:8080/customer-info`
   - 청구 정보:  
     `http://localhost:8080/bill-info`
2. 날짜(erdat) 또는 청구년월(recpYm) 입력 및 검색

### 6.2 API 테스트
1. 웹 브라우저 또는 curl 등으로 접속:
   - 고객 정보:  
     `http://localhost:8080/api/customer-info?erdat=20250423`
   - 청구 정보:  
     `http://localhost:8080/api/bill-info?recpYm=202501`
2. curl 명령어 예시:
   ```bash
   curl http://localhost:8080/api/customer-info?erdat=20250423
   curl http://localhost:8080/api/bill-info?recpYm=202501
   ```

### 6.3 이메일 발송 테스트
1. 웹 브라우저에서 접속:
   - 이메일 발송 폼:  
     `http://localhost:8080/email/form`
2. 폼 입력 후 '발송하기' 클릭 시 외부 API로 이메일 발송

## 7. 주의사항
1. SAP JCo 라이브러리 버전과 Java 버전이 호환되어야 합니다.
2. SAP 시스템 연결 정보는 보안을 위해 환경 변수나 외부 설정 파일로 관리하는 것이 좋습니다.
3. 실제 운영 환경에서는 에러 처리와 로깅을 더 강화해야 합니다.

## 8. 참고 자료
- [SAP JCo Documentation](https://help.sap.com/doc/saphelp_nwpi71/7.1/en-US/48/8fe37933114e6fe10000000a42189c/content.htm)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

## 9. 테이블 구조
1. SAP 고객 정보 저장 테이블 : sap_cwb2b_cust_info 
    ```sql
    CREATE TABLE `sap_cwb2b_cust_info` (
      `SEQ` 		INT 		UNSIGNED NOT NULL AUTO_INCREMENT,	
      `ORDER_NO`    VARCHAR(12)          NULL     COMMENT '고객주문번호',
      `STCD2`       VARCHAR(11)          NULL     COMMENT '사업자번호',
      `KUNNR`       VARCHAR(10)          NULL     COMMENT '고객 번호',
      `CUST_NM`     VARCHAR(40)          NULL     COMMENT '고객명',
      `FXDAY`       TINYINT UNSIGNED     NULL     COMMENT '발행일 (일자 2자리)',
      `ZGRPNO`      INT UNSIGNED         NULL     COMMENT '묶음번호',
      `SEL_KUN`     VARCHAR(1)           NULL     COMMENT '대표고객 여부',
      `JUSO`        VARCHAR(255)         NULL     COMMENT '주소',
      `PSTLZ`       VARCHAR(10)          NULL     COMMENT '도시우편번호',
      `J_1KFTBUS`   VARCHAR(30)          NULL     COMMENT '업태',
      `J_1KFTIND`   VARCHAR(30)          NULL     COMMENT '업종',
      `J_1KFREPRE`  VARCHAR(20)          NULL     COMMENT '대표자명',
      `EMAIL`       VARCHAR(50)          NULL     COMMENT '이메일주소',
      `EMAIL2`      VARCHAR(50)          NULL     COMMENT '이메일주소2',
      `PAY_MTHD`    VARCHAR(4)           NULL     COMMENT '결제구분',
      `PAY_MTHD_TX` VARCHAR(50)          NULL     COMMENT '입금유형명',
      `PAY_COM`     VARCHAR(4)           NULL     COMMENT '결제수단',
      `PAY_COM_TX`  VARCHAR(50)          NULL     COMMENT '결제수단명',
      `PAY_NO`      VARCHAR(20)          NULL     COMMENT '계좌/카드번호',
      `REGID`  		VARCHAR(20) 		 NOT NULL DEFAULT 'SAP RFC',
      `REGDT`  		DATETIME    		 NOT NULL DEFAULT NOW(),
      PRIMARY KEY (`SEQ`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SAP 고객정보';
    ```

2. SAP 고객 청구 정보 저장 테이블 : sap_cwb2b_bill_info
    ```sql
    CREATE TABLE `sap_cwb2b_bill_info` (
      `SEQ` int unsigned NOT NULL AUTO_INCREMENT,
      `ORDER_NO` varchar(12) DEFAULT NULL,
      `STCD2` varchar(11) DEFAULT NULL,
      `KUNNR` varchar(10) DEFAULT NULL,
      `ZGRPNO` varchar(10) DEFAULT NULL,
      `SEL_KUN` varchar(1) DEFAULT NULL,
      `PAY_MTHD` varchar(4) DEFAULT NULL,
      `PAY_MTHD_TX` varchar(50) DEFAULT NULL,
      `PAY_COM` varchar(4) DEFAULT NULL,
      `PAY_COM_TX` varchar(50) DEFAULT NULL,
      `PAY_NO` varchar(20) DEFAULT NULL,
      `GOODS_SN` varchar(18) DEFAULT NULL,
      `GOODS_CD` varchar(18) DEFAULT NULL,
      `GOODS_TX` varchar(40) DEFAULT NULL,
      `PRODH` varchar(18) DEFAULT NULL,
      `VTEXT` varchar(40) DEFAULT NULL,
      `RECP_YM` varchar(6) DEFAULT NULL,
      `RECP_TP` varchar(4) DEFAULT NULL,
      `RECP_TP_TX` varchar(20) DEFAULT NULL,
      `FIX_SUPPLY_VALUE` decimal(13,0) DEFAULT NULL,
      `FIX_VAT` decimal(13,0) DEFAULT NULL,
      `FIX_BILL_AMT` decimal(13,0) DEFAULT NULL,
      `SUPPLY_VALUE` decimal(13,0) DEFAULT NULL,
      `VAT` decimal(13,0) DEFAULT NULL,
      `BILL_AMT` decimal(13,0) DEFAULT NULL,
      `DUE_DATE` varchar(8) DEFAULT NULL,
      `PRE_AMT` decimal(13,0) DEFAULT NULL,
      `REMAIN_AMT` decimal(13,0) DEFAULT NULL,
      `PRE_MONTH` varchar(3) DEFAULT NULL,
      `INST_DT` varchar(8) DEFAULT NULL,
      `USE_MONTH` varchar(3) DEFAULT NULL,
      `USE_DUTY_MONTH` varchar(3) DEFAULT NULL,
      `OWNER_DATE` varchar(8) DEFAULT NULL,
      `INST_JUSO` varchar(255) DEFAULT NULL,
      `DEPT_CD` varchar(3) DEFAULT NULL,
      `DEPT_CD_TX` varchar(20) DEFAULT NULL,
      `DEPT_TELNR` varchar(25) DEFAULT NULL,
      `ZBIGO` varchar(50) DEFAULT NULL,
      `REGID` varchar(20) NOT NULL DEFAULT 'SAP RFC',
      `REGDT` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`SEQ`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
    ```

## 10. JPA 엔티티 구조
1. SapCustomerInfo 엔티티
   ```java
   @Entity
   @Table(name = "sap_cwb2b_cust_info")
   public class SapCustomerInfo {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       @Column(columnDefinition = "INT UNSIGNED")
       private Integer seq;

       @Column(name = "ORDER_NO", length = 12)
       private String orderNo;

       @Column(name = "STCD2", length = 11)
       private String stcd2;

       // ... 기타 필드들

       @Column(name = "FXDAY", columnDefinition = "TINYINT UNSIGNED")
       private Short fxday;

       @Column(name = "ZGRPNO", columnDefinition = "INT UNSIGNED")
       private Integer zgrpno;

       // ... 나머지 필드들
   }
   ```

2. SapBillInfo 엔티티
   ```java
   @Entity
   @Table(name = "sap_cwb2b_bill_info")
   public class SapBillInfo {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       @Column(columnDefinition = "INT UNSIGNED")
       private Integer seq;

       @Column(name = "ORDER_NO", length = 12)
       private String orderNo;

       // ... 기타 필드들

       @Column(name = "FIX_SUPPLY_VALUE", precision = 13)
       private BigDecimal fixSupplyValue;

       @Column(name = "FIX_VAT", precision = 13)
       private BigDecimal fixVat;

       // ... 나머지 필드들
   }
   ```

## 11. 데이터 처리 흐름
1. `/api/customer-info` 엔드포인트
   - SAP RFC 호출
   - 응답 데이터를 DB에 저장
   - 원본 응답 데이터 반환

2. `/api/customer-info-json` 엔드포인트
   - SAP RFC 호출
   - 응답 데이터를 가공하여 JSON 형식으로 변환
   - DB 저장 없이 바로 반환

3. `/api/bill-info` 엔드포인트
   - SAP RFC 호출
   - 응답 데이터를 DB에 저장
   - 원본 응답 데이터 반환

4. 데이터 저장 프로세스
   - SAP RFC 응답 데이터를 엔티티로 변환
   - JPA를 통한 데이터 저장
   - 트랜잭션 관리

## 12. 에러 처리
1. RFC 호출 에러
   - JCoException 처리
   - 에러 메시지를 JSON 형식으로 반환

2. 데이터베이스 에러
   - JPA 예외 처리
   - 트랜잭션 롤백

3. 데이터 변환 에러
   - 숫자 형식 변환 실패 시 null 처리
   - 날짜 형식 변환 실패 시 기본값 처리

4. 응답 형식
   ```json
   {
     "error": "에러 메시지"
   }
   ```

## 13. 로깅 전략
1. Controller 레벨
   - DEBUG: API 요청 파라미터, 응답 데이터
   - INFO: API 호출 결과
   - ERROR: 예외 발생 상황

2. Service 레벨
   - DEBUG: 데이터 처리 과정
   - INFO: 데이터 저장 결과
   - WARN: 데이터 변환 실패
   - ERROR: 비즈니스 로직 실패

3. Repository 레벨
   - DEBUG: 쿼리 실행
   - ERROR: DB 작업 실패