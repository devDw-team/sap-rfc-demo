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
│   │   │                   ├── dto/             # 데이터 전송 객체
│   │   │                   ├── service/         # 서비스
│   │   │                   └── SapRfcDemoApplication.java
│   │   └── resources/
│   │       ├── static/      # 정적 리소스
│   │       ├── templates/   # Thymeleaf 템플릿
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
- `service`: 비즈니스 로직 및 SAP RFC 호출
- `templates`: Thymeleaf 템플릿 파일

### 2.3 주요 파일 설명
#### config 패키지
- `SapConfig.java`: SAP JCo 연결 설정을 관리하는 설정 클래스
- `CustomDestinationDataProvider.java`: SAP JCo Destination 데이터 제공자 구현 클래스

#### controller 패키지
- `SapController.java`: 웹 페이지 요청을 처리하는 컨트롤러
- `SapApiController.java`: JSON API 요청을 처리하는 REST 컨트롤러

#### dto 패키지
- `CustomerInfoResponse.java`: 고객 정보 API 응답을 위한 DTO 클래스
- `BillInfoResponse.java`: 청구 정보 API 응답을 위한 DTO 클래스

#### service 패키지
- `SapService.java`: SAP RFC 호출 및 데이터 처리를 담당하는 서비스 클래스
- `SapConnectionTestService.java`: SAP 연결 테스트를 위한 서비스 클래스

#### templates 패키지
- `customer-info.html`: 고객 정보 조회 웹 페이지 템플릿
- `bill-info.html`: 청구 정보 조회 웹 페이지 템플릿
- `connection-test.html`: SAP 연결 테스트 웹 페이지 템플릿

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
   - 설명: 고객 정보 조회
   
   - URL: `/bill-info`
   - Method: GET
   - 파라미터: `recpYm` (YYYYMM 형식, 기본값: 202501)
   - 설명: 청구 정보 조회

2. JSON API
   - URL: `/api/customer-info`
   - Method: GET
   - 파라미터: `erdat` (YYYYMMDD 형식, 기본값: 20250423)
   - 응답 형식: JSON
   - 설명: 고객 정보 JSON 조회

   - URL: `/api/bill-info`
   - Method: GET
   - 파라미터: `recpYm` (YYYYMM 형식, 기본값: 202501)
   - 응답 형식: JSON
   - 설명: 청구 정보 JSON 조회

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

## 7. 주의사항
1. SAP JCo 라이브러리 버전과 Java 버전이 호환되어야 합니다.
2. SAP 시스템 연결 정보는 보안을 위해 환경 변수나 외부 설정 파일로 관리하는 것이 좋습니다.
3. 실제 운영 환경에서는 에러 처리와 로깅을 더 강화해야 합니다.

## 8. 참고 자료
- [SAP JCo Documentation](https://help.sap.com/doc/saphelp_nwpi71/7.1/en-US/48/8fe37933114e6fe10000000a42189c/content.htm)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html) 