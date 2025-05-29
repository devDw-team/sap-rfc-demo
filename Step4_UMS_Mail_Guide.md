# Step 4: UMS 메일 발송 구현 완료 가이드

## 🎯 구현 완료 내용

### 1. **TemplateService** ✅
- **경로**: `src/main/java/com/test/sap/sap_rfc_demo/service/TemplateService.java`
- **기능**: 
  - HTML 메일 템플릿 파일 읽기 (`/resources/static/mail/webmail.html`)
  - 동적 변수 치환 (`${CUST_NM}`, `${C_RECP_YEAR}`, `${C_RECP_MONTH}`)
  - 템플릿 변수 검증

### 2. **EmailService 고도화** ✅
- **경로**: `src/main/java/com/test/sap/sap_rfc_demo/service/EmailService.java`
- **기능**:
  - UMS API 응답 JSON 파싱 (`{"code": "13", "msg": "요청성공", "key": "248395725"}`)
  - 파일 첨부 경로 검증
  - 이메일 주소 마스킹 (개인정보 보호)
  - Rate Limiting 지원

### 3. **MailSendService** ✅
- **경로**: `src/main/java/com/test/sap/sap_rfc_demo/service/MailSendService.java`
- **기능**:
  - **Step 4-1**: 메일 발송 대상 조회 (`getMailSendTargets()`)
  - **Step 4-2**: 메일 발송 및 결과 업데이트 (`processMailSending()`)
  - 데이터 검증 (이메일 형식, 첨부파일 존재 여부 등)
  - 중복 발송 방지
  - API 호출 간격 조정 (Rate Limiting)

### 4. **MailSendController** ✅
- **경로**: `src/main/java/com/test/sap/sap_rfc_demo/controller/MailSendController.java`
- **API 엔드포인트**:
  - `GET /api/mail-send/targets` - 메일 발송 대상 조회
  - `POST /api/mail-send/send/{seq}` - 개별 메일 발송
  - `POST /api/mail-send/execute-all` - 전체 메일 발송
  - `POST /api/mail-send/batch/execute` - 배치 작업 실행
  - `GET /api/mail-send/statistics` - 메일 발송 통계
  - `GET /api/mail-send/preview-template` - 템플릿 미리보기

### 5. **UmsApiResponse DTO** ✅
- **경로**: `src/main/java/com/test/sap/sap_rfc_demo/dto/UmsApiResponse.java`
- **기능**: UMS API 응답 형식을 담는 DTO 클래스

### 6. **대시보드 UI 업데이트** ✅
- **경로**: `src/main/resources/templates/automail/dashboard.html`
- **추가된 기능**:
  - Step 4-1: 메일 발송 대상 조회 버튼
  - Step 4-2: 전체 메일 발송 버튼  
  - Step 4: 배치 작업 실행 버튼
  - 메일 템플릿 미리보기
  - 메일 발송 통계 조회
  - 데이터 테이블에 개별 메일 발송 버튼 추가

## 🚀 테스트 방법

### 1. 애플리케이션 실행
```bash
mvn spring-boot:run
```

### 2. 대시보드 접속
```
http://localhost:8080/automail/dashboard
```

### 3. 테스트 시나리오

#### ✅ **시나리오 1: 메일 발송 대상 조회**
1. 대시보드에서 **"Step 4-1: 메일 발송 대상 조회"** 버튼 클릭
2. 조회 결과 확인 (조건: `SEND_AUTO='Y'`, `FILE_CREATE_FLAG='Y'`, `MAIL_SEND_FLAG='N'`, `FXDAY=오늘날짜`)

#### ✅ **시나리오 2: 개별 메일 발송**
1. Step 4-1에서 조회된 대상 목록에서 **"메일 발송"** 버튼 클릭
2. 또는 메인 테이블에서 개별 데이터의 메일 발송 버튼 클릭
3. 발송 결과 확인

#### ✅ **시나리오 3: 전체 메일 발송**
1. **"Step 4-2: 전체 메일 발송"** 버튼 클릭
2. 확인 다이얼로그에서 승인
3. 발송 결과 및 통계 확인

#### ✅ **시나리오 4: 배치 작업 실행**
1. **"Step 4: 배치 작업 실행"** 버튼 클릭
2. Step 4 전체 프로세스 실행 결과 확인

#### ✅ **시나리오 5: 템플릿 미리보기**
1. **"메일 템플릿 미리보기"** 버튼 클릭
2. 고객명, 청구년도, 청구월 입력
3. 템플릿 변수 치환 결과 확인

## 📋 주요 구현 특징

### 🔒 **보안 및 개인정보 보호**
- 이메일 주소 마스킹 (`te***@example.com`)
- 개인정보 포함 로그 최소화
- UMS API 인증 키 안전한 설정 관리

### ⚡ **성능 최적화**
- Rate Limiting (초당 최대 5건 메일 발송)
- 배치 처리 시 최대 100건씩 처리
- 파일 첨부 경로 사전 검증
- 중복 발송 방지 로직

### 🛡️ **예외 처리**
- API 호출 실패 시 재시도 로직
- 템플릿 변수 치환 실패 시 기본값 사용
- 첨부파일 없을 시 적절한 오류 처리
- 트랜잭션 롤백 지원

### 📊 **모니터링 및 로깅**
- 단계별 상세 로그 기록
- 메일 발송 성공/실패 통계
- 실시간 처리 현황 모니터링
- 배치 실행 이력 추적

## 🔧 설정 정보

### **데이터베이스 테이블**
```sql
-- 메일 발송 결과 업데이트에 필요한 컬럼들
b2b_automail_dt.UMS_CODE    -- UMS API 응답 코드
b2b_automail_dt.UMS_MSG     -- UMS API 응답 메시지  
b2b_automail_dt.UMS_KEY     -- UMS API 응답 키
b2b_automail_dt.MAIL_SEND_FLAG  -- 메일 발송 플래그 ('Y'/'N')
```

### **이메일 API 설정**
```properties
# application.properties
email.api.url=https://ums.coway.com/n5/rest/coway/send/email
email.api.auth-id=CWB2B
email.api.auth-key=aef8712a8e3ed08ac729b370419ecf5d41855abc4119a12a5af53d321bd85bf0ebab58bbd047dc0df950b4f083faf0f0474b01d13175c679ac4886f3e232b3ee
```

### **메일 템플릿 파일**
```
src/main/resources/static/mail/webmail.html
```

## 🎉 완료된 UMS Mail Guide 스텝

- ✅ **Step 1**: 청구서 발송 대상 조회 (기존 완료)
- ✅ **Step 2**: 청구서 발송 대상 적재 (기존 완료)  
- ✅ **Step 3**: 청구서 파일 생성 (기존 완료)
- ✅ **Step 4**: 메일 발송 및 결과 업데이트 (신규 완료)
- ✅ **Step 4-3**: 배치 작업 추가 (신규 완료)

## 🔗 다음 단계

1. **스케줄러 설정**: `@Scheduled` 어노테이션을 이용한 매일 오전 8시 자동 실행
2. **모니터링 대시보드**: 실시간 메일 발송 현황 모니터링 
3. **알림 시스템**: 메일 발송 실패 시 관리자 알림
4. **통계 리포트**: 일일/월별 메일 발송 통계 리포트 생성

---

**🎯 이제 UMS Mail Guide의 모든 기능이 구현되어 대시보드에서 테스트할 수 있습니다!** 