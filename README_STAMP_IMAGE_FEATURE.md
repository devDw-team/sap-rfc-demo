# 📋 도장 이미지 조건부 노출 기능 개발 완료

## 🎯 개발 개요

동적 청구서 템플릿 생성 시스템에서 **인감날인 사용여부(stamp_yn)** 필드값에 따라 엑셀 템플릿의 도장 이미지를 조건부로 노출하는 기능을 개발했습니다.

## 🔧 핵심 기능

### 1. 조건부 도장 이미지 처리
- **stamp_yn = 'Y'**: 도장 이미지 유지 (정상 표시)
- **stamp_yn = 'N'**: 도장 이미지 제거/숨김 처리

### 2. 지능형 도장 이미지 식별
- **위치 기반 판단**: 4개 영역(우상단, 우하단, 중앙우측, 좌상단)에서 도장 이미지 탐지
- **크기 기반 판단**: 작은 도장(1x1~4x4), 중간 도장(3x3~8x8), 큰 도장(6x6~12x12) 범위 지원
- **파일 형식 검증**: PNG, JPG, JPEG, GIF, BMP, TIFF 이미지 파일만 대상

### 3. 설정 기반 제어
```properties
# 도장 이미지 처리 설정
app.excel.stamp.enabled=true          # 도장 처리 활성화/비활성화
app.excel.stamp.debug=true            # 디버그 로깅 활성화
app.excel.stamp.region.enabled=true   # 영역 기반 판단 활성화
```

## 📁 수정된 파일들

### 1. ExcelTemplateService.java
```java
// 새로 추가된 주요 메서드들
- processStampImage()           // 도장 이미지 조건부 처리
- removeStampImages()          // 도장 이미지 제거
- isStampImage()              // 도장 이미지 식별
- isInStampRegion()           // 도장 영역 판단
- isStampSizeRange()          // 도장 크기 판단
- removePicture()             // 이미지 숨김 처리
```

### 2. TmplService.java
```java
// 로깅 개선
- 인감날인 사용여부 로깅 추가
- 템플릿 생성 과정 추적 강화
```

### 3. TemplateController.java (신규 생성)
```java
// API 엔드포인트
POST /api/template/generate/{businessNo}     // 특정 사업자 템플릿 생성
POST /api/template/generate-all              // 전체 템플릿 생성
GET  /api/template/check/{businessNo}        // 사업자 존재 확인
```

### 4. application.properties
```properties
# 도장 이미지 처리 관련 설정 추가
app.excel.stamp.enabled=true
app.excel.stamp.debug=true
app.excel.stamp.region.enabled=true
```

## 🚀 사용 방법

### 1. API를 통한 테스트
```bash
# 특정 사업자 템플릿 생성 (도장 이미지 조건부 처리)
curl -X POST http://localhost:8080/api/template/generate/123-45-67890

# 사업자 존재 여부 확인
curl -X GET http://localhost:8080/api/template/check/123-45-67890

# 전체 사업자 템플릿 생성
curl -X POST http://localhost:8080/api/template/generate-all
```

### 2. 데이터베이스 설정 확인
```sql
-- BBIMCM_INVT 테이블에서 인감날인 설정 확인
SELECT busi_mgmt_id, stamp_yn FROM BBIMCM_INVT WHERE del_yn = 'N';

-- stamp_yn 값 변경 테스트
UPDATE BBIMCM_INVT SET stamp_yn = 'N' WHERE busi_mgmt_id = 1;  -- 도장 숨김
UPDATE BBIMCM_INVT SET stamp_yn = 'Y' WHERE busi_mgmt_id = 1;  -- 도장 표시
```

## 🔍 로그 모니터링

### 1. 도장 처리 로그 예시
```
INFO  - 인감날인 사용여부: Y (표시)
INFO  - 도장 이미지 조건부 처리 시작 - 사업자번호: 123-45-67890, 인감날인 사용여부: Y
INFO  - 도장 이미지 유지 - 사업자번호: 123-45-67890 (인감날인 사용)
INFO  - 템플릿 생성 처리 완료 - 사업자번호: 123-45-67890 (인감날인: Y)
```

### 2. 디버그 로그 예시 (stamp.debug=true)
```
DEBUG - 이미지 위치 및 크기 확인 - 위치: (28,8) ~ (32,12), 크기: 4x4, 도장영역: true, 도장크기: true, 파일형식: png
DEBUG - 도장 이미지로 판단됨 - 영역매치: true, 크기매치: true
DEBUG - 도장 이미지 제거: png
```

## ⚙️ 고급 설정

### 1. 도장 영역 커스터마이징
`ExcelTemplateService.java`의 `isInStampRegion()` 메서드에서 도장 위치 영역을 조정할 수 있습니다:

```java
// 영역 1: 우상단 (일반적인 도장 위치)
boolean region1 = (col1 >= 20 && col1 <= 40) && (row1 >= 0 && row1 <= 20);
```

### 2. 도장 크기 범위 조정
`isStampSizeRange()` 메서드에서 도장 크기 범위를 조정할 수 있습니다:

```java
// 작은 도장: 1x1 ~ 4x4
boolean smallStamp = (width >= 1 && width <= 4) && (height >= 1 && height <= 4);
```

### 3. 처리 방식 변경
현재는 이미지를 화면 밖으로 이동시켜 숨김 처리하지만, 필요에 따라 다른 방식으로 변경 가능:
- 이미지 완전 삭제
- 투명도 조정
- 크기를 0으로 변경

## 🎯 동작 원리

### 1. 템플릿 생성 플로우
```
TmplService.generateTemplateByBusinessNo()
    ↓
ExcelTemplateService.generateExcelTemplate()
    ↓
processStampImage() → stamp_yn 확인
    ↓
stamp_yn = 'N' ? removeStampImages() : 이미지 유지
    ↓
isStampImage() → 위치/크기/형식 기반 도장 식별
    ↓
removePicture() → 화면 밖으로 이동하여 숨김
```

### 2. 도장 식별 알고리즘
```
1. 파일 확장자 검증 (이미지 파일인가?)
    ↓
2. 위치 영역 확인 (4개 도장 가능 영역)
    ↓
3. 크기 범위 확인 (3개 도장 크기 범위)
    ↓
4. OR 조건 판단 (위치 OR 크기 중 하나라도 만족)
    ↓
5. 도장 이미지로 판단 시 숨김 처리
```

## ✅ 테스트 체크리스트

- [ ] stamp_yn = 'Y'인 사업자의 템플릿에서 도장 이미지 정상 표시
- [ ] stamp_yn = 'N'인 사업자의 템플릿에서 도장 이미지 숨김 처리
- [ ] 여러 도장이 있는 경우 모두 조건부 처리
- [ ] 일반 이미지는 도장으로 오인식하지 않음
- [ ] 로그를 통한 처리 과정 추적 가능
- [ ] 설정을 통한 기능 활성화/비활성화 가능

## 🚨 주의사항

1. **백업 필수**: 원본 엑셀 템플릿 파일은 항상 백업 보관
2. **테스트 환경**: 프로덕션 적용 전 충분한 테스트 필요
3. **영역 조정**: 실제 템플릿의 도장 위치에 맞게 영역 범위 조정 필요
4. **성능 고려**: 대량 처리 시 메모리 사용량 모니터링

## 📞 문의 및 지원

도장 이미지 기능과 관련하여 문의사항이 있으시면 개발팀에 연락해주세요.

---
**개발 완료일**: 2024년 12월 27일  
**개발자**: 시니어 풀스택 개발자  
**버전**: v1.0.0 