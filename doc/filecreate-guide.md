# Coway B2B Auto-mailing 개발 가이드

---

## 🎯 청구서 파일 생성 개발 프로세스 (3단계 ~ 4단계)

이 문서는 전체 개발 프로세스 중 3단계(청구서 파일 생성) 및 4단계(Batch JOB 추가)에 대한 개발 내용을 기술합니다.
앞선 1단계(청구서 발송 대상 조회)와 2단계(청구서 발송 대상 적재)는 이미 개발 완료된 상태입니다.

### 📝 Step 1. 파일 생성을 위한 데이터 조회

청구서 HTML 및 Excel 파일 생성을 위해 `b2b_automail_dt` 테이블에서 다음 조건에 맞는 데이터를 조회합니다.

* **조건**:
    * 자동 발송 대상 (`SEND_AUTO = 'Y'`)
    * 아직 파일이 생성되지 않은 건 (`FILE_CREATE_FLAG = 'N'`)
    * 생성 요청일자(`DT_CREATE_DATE`)의 년월이 현재 년월과 동일한 건
* **SQL 조회 쿼리**:

```sql
SELECT
    SEQ,
    MAILDATA,  -- 청구서 상세 정보가 담긴 JSON 데이터
    LEFT(DT_CREATE_DATE, 7) AS CREATION_YEAR_MONTH -- 생성 요청 년월
FROM
    b2b_automail_dt
WHERE
    SEND_AUTO = 'Y'
AND FILE_CREATE_FLAG = 'N'
AND LEFT(DT_CREATE_DATE, 7) = DATE_FORMAT(NOW(), '%Y-%m')
ORDER BY SEQ; -- 순차 처리를 위한 정렬
```

* 조회된 `MAILDATA` 필드의 JSON 데이터를 기반으로 HTML 및 Excel 파일을 생성합니다.

#### MAILDATA JSON 포맷 예시:

```json
{
  "customer": {
    "stcd2": "1048118121",                 // 사업자번호
    "custNm": "지에스건설(주)",
    "j1kfrepre": "허윤홍외1",
    "j1kftbus": "건설업",
    "j1kftind": "일반건축공사",
    "payComTx": "",
    "payNo": "",
    "preAmt": 0,
    "remainAmt": 0,
    "preMonth": "0"
  },
  "billSummary": {
    "totalAmount": 15680,
    "crecpYm": "202501",                   // 청구년월 (YYYYMM)
    "cdueDate": "2025-01-31",
    "cselKunCnt": 0
  },
  "billTypeSummary": [
    {
      "summaryCnt": 1,
      "summaryAmount": 15680,
      "crecpTpTx": "렌탈료",
      "crecpTp": "11"
    }
  ],
  "bills": [
    {
      "recpTpTx": "렌탈료",
      "orderNo": "20BC42405078",
      "vtext": "정수기",
      "goodsCd": "000000000000113402",
      "instDt": "2024-12-24",
      "useDutyMonth": "36",
      "ownerDate": "2029-12-24",
      "useMonth": "202412",
      "recpYm": "202501",
      "fixSupplyValue": 14255,
      "fixVat": 1425,
      "fixBillAmt": 15680,
      "supplyValue": 14254,
      "vat": 1426,
      "billAmt": 15680,
      "payComTx": "",
      "payNo": "",
      "instJuso": "경남 창원시 진해구 신항2로 114상가219호 (용원동)",
      "goodsSn": "10502FI224C0600029",
      "deptCdTx": "법인경남중부지국-C팀",
      "deptTelnr": "055-267-7881",
      "zbigo": "",
      "goodsTx": "CHPI-5810L",
      "preAmt": 0,
      "remainAmt": 0,
      "preMonth": "0"
    }
  ]
}
```

---

### ⚙️ Step 2. HTML, Excel 파일 생성 프로세스

조회된 `MAILDATA`를 사용하여 각 고객사별 청구서 HTML 파일과 상세내역 Excel 파일을 생성합니다.

#### 템플릿 파일 위치:

* **HTML 템플릿**: `/resources/static/html/bill-template.html`
* **Excel 템플릿**: `/resources/static/excel/billinfo_template.xlsx`

#### HTML 파일 생성 규칙:

1.  **원본 HTML 파일명 (`ORI_HTML_FILENM`)**:
    * 형식: `"코웨이(주) {YYYY}년 {M}월 대금청구서.html"`
    * 예시: `billSummary.crecpYm` 값이 `"202501"` 이면, `"코웨이(주) 2025년 1월 대금청구서.html"` 로 설정됩니다.
2.  **변환 HTML 파일명 (`CHG_HTML_FILENM`)**:
    * 형식: `"{사업자번호}_timestamp.html"`
    * `{사업자번호}`: `customer.stcd2` 값 사용
    * `timestamp`: 파일 생성 시점의 타임스탬프 (예: YYYYMMDDHHMMSSmmm)
3.  **HTML 파일 생성 경로**:
    * 형식: `/resources/static/html/download/{사업자번호}/{crecpYm}/`
    * `{사업자번호}`: `customer.stcd2` 값 사용
    * `{crecpYm}`: `billSummary.crecpYm` 값 사용 (예: "202501")
    * **주의**: 해당 경로에 폴더(`{사업자번호}`, `{crecpYm}`)가 존재하지 않으면, 폴더를 먼저 생성한 후 파일을 저장합니다.
4.  **HTML 파일 다운로드 경로 (`HTML_FILEPATH`)**:
    * 형식: `/html/download/{사업자번호}/{crecpYm}/`

#### Excel 파일 생성 규칙:

1.  **원본 Excel 파일명 (`ORI_EXCEL_FILENM`)**:
    * 고정값: `"코웨이 청구 상세내역.xlsx"`
2.  **변환 Excel 파일명 (`CHG_EXCEL_FILENM`)**:
    * 형식: `"{사업자번호}_timestamp.xlsx"`
    * `{사업자번호}`: `customer.stcd2` 값 사용
    * `timestamp`: 파일 생성 시점의 타임스탬프 (예: YYYYMMDDHHMMSSmmm)
3.  **Excel 파일 생성 경로**:
    * 형식: `/resources/static/excel/download/{사업자번호}/{crecpYm}/`
    * `{사업자번호}`: `customer.stcd2` 값 사용
    * `{crecpYm}`: `billSummary.crecpYm` 값 사용 (예: "202501")
    * **주의**: 해당 경로에 폴더(`{사업자번호}`, `{crecpYm}`)가 존재하지 않으면, 폴더를 먼저 생성한 후 파일을 저장합니다.
4.  **Excel 파일 다운로드 경로 (`EXCEL_FILEPATH`)**:
    * 형식: `/excel/download/{사업자번호}/{crecpYm}/`

---

### 💾 Step 3. 파일 생성 완료 후 결과 업데이트

개별 데이터 (`SEQ` 기준)에 대한 HTML 및 Excel 파일 생성이 성공적으로 완료되면, `b2b_automail_dt` 테이블의 해당 row를 다음 쿼리로 업데이트합니다.

* **SQL 업데이트 쿼리**:

```sql
UPDATE b2b_automail_dt
SET
    FILE_CREATE_FLAG = 'Y',             -- 파일 생성 완료 플래그
    ORI_HTML_FILENM = ?,                -- Step 2에서 정의된 원본 HTML 파일명
    CHG_HTML_FILENM = ?,                -- Step 2에서 정의된 변환 HTML 파일명
    HTML_FILEPATH = ?,                  -- Step 2에서 정의된 HTML 파일 다운로드 경로
    ORI_EXCEL_FILENM = ?,               -- Step 2에서 정의된 원본 Excel 파일명
    CHG_EXCEL_FILENM = ?,               -- Step 2에서 정의된 변환 Excel 파일명
    EXCEL_FILEPATH = ?,                 -- Step 2에서 정의된 Excel 파일 다운로드 경로
    FILE_CREATE_DATE = NOW(),           -- 파일 생성 완료 일시
    UPDATE_DATE = NOW()                 -- 업데이트 일시
WHERE
    SEQ = ?;                            -- 해당 데이터의 고유 시퀀스 번호
```

---

### ⏱️ Step 4. Batch JOB 추가

위 Step 1 ~ Step 3의 파일 생성 프로세스를 Spring Batch Job으로 구현합니다.

* **실행 주기**: 매월 4일 오전 8시 (Cron: `0 0 8 4 * ?`)
* **선행 조건**: "청구서 발행 대상 조회 및 적재 프로세스"(매월 3일 오전 7시 실행)가 완료된 이후에 실행되어야 합니다.
* **작업 내용**:
    1.  Step 1의 쿼리를 실행하여 파일 생성 대상 조회 (ItemReader)
    2.  조회된 데이터를 기반으로 Step 2의 로직에 따라 HTML 및 Excel 파일 생성 (ItemProcessor)
    3.  파일 생성 완료 후 Step 3의 쿼리를 실행하여 DB 업데이트 (ItemWriter)

#### Batch 설정 권장사항:
* **청크 사이즈**: 10~50건 (메모리 사용량과 성능 고려)
* **스킵 정책**: 개별 건 실패 시 로그 기록 후 다음 건 처리 계속
* **재시작 정책**: 실패 지점부터 재시작 가능하도록 설정

---

## 🛡️ 데이터 검증 및 예외 처리

### 필수 데이터 검증 항목:

1. **사업자번호 검증**:
   ```java
   // 10자리 숫자 형식 검증
   if (!stcd2.matches("\\d{10}")) {
       throw new ValidationException("유효하지 않은 사업자번호: " + stcd2);
   }
   ```

2. **청구년월 검증**:
   ```java
   // YYYYMM 형식 검증
   if (!crecpYm.matches("\\d{6}")) {
       throw new ValidationException("유효하지 않은 청구년월: " + crecpYm);
   }
   ```

3. **필수 필드 존재 여부**:
   - `customer.stcd2` (사업자번호)
   - `customer.custNm` (고객명)
   - `billSummary.crecpYm` (청구년월)
   - `billSummary.totalAmount` (총 금액)

### 예외 처리 시나리오:

1. **JSON 파싱 오류**:
   ```sql
   UPDATE b2b_automail_dt 
   SET FILE_CREATE_FLAG = 'E', ERROR_MSG = 'JSON 파싱 오류'
   WHERE SEQ = ?;
   ```

2. **템플릿 파일 없음**:
   ```sql
   UPDATE b2b_automail_dt 
   SET FILE_CREATE_FLAG = 'E', ERROR_MSG = '템플릿 파일 없음'
   WHERE SEQ = ?;
   ```

3. **디스크 용량 부족**:
   ```sql
   UPDATE b2b_automail_dt 
   SET FILE_CREATE_FLAG = 'E', ERROR_MSG = '디스크 용량 부족'
   WHERE SEQ = ?;
   ```

---

## 🔒 보안 고려사항

### 1. 파일 경로 보안:
```java
// Path Traversal 공격 방지
String sanitizedPath = Paths.get(basePath, stcd2, crecpYm)
    .normalize()
    .toString();
if (!sanitizedPath.startsWith(basePath)) {
    throw new SecurityException("유효하지 않은 파일 경로");
}
```

### 2. 파일 접근 권한:
- 생성된 파일: 읽기 전용 (644)
- 디렉토리: 읽기/실행 (755)

### 3. 민감 정보 로깅 방지:
```java
// 사업자번호 마스킹 처리
String maskedStcd2 = stcd2.substring(0, 3) + "*******";
log.info("파일 생성 완료: {}", maskedStcd2);
```

---

## 📊 모니터링 및 로깅

### 1. 배치 작업 모니터링:
```java
@Component
public class FileCreationJobListener implements JobExecutionListener {
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("파일 생성 배치 작업 시작: {}", LocalDateTime.now());
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        BatchStatus status = jobExecution.getStatus();
        long processedCount = jobExecution.getStepExecutions().stream()
            .mapToLong(StepExecution::getWriteCount)
            .sum();
        
        log.info("파일 생성 배치 작업 완료 - 상태: {}, 처리건수: {}", 
                status, processedCount);
    }
}
```

### 2. 성능 메트릭 수집:
- 전체 처리 시간
- 건당 평균 처리 시간
- 성공/실패 건수
- 생성된 파일 크기

### 3. 알림 설정:
- 배치 작업 실패 시 즉시 알림
- 처리 시간이 임계값 초과 시 알림
- 실패율이 10% 초과 시 알림

---

## 📈 성능 최적화 방안

### 1. 메모리 최적화:
```java
// 대용량 Excel 파일 처리 시 SXSSFWorkbook 사용
// 윈도우 사이즈: 메모리에 유지할 행 수 (예상 최대 3000행 기준)
SXSSFWorkbook workbook = new SXSSFWorkbook(500); // 메모리에 500행 유지

// 윈도우 사이즈 설정 가이드:
// - 100행: 매우 제한적 메모리 환경 (1-2MB)
// - 500행: 일반적인 환경, 3000행 처리 권장 (5-10MB)
// - 1000행: 충분한 메모리 환경 (10-20MB)
// - -1: 모든 행을 메모리에 유지 (일반 XSSFWorkbook과 동일)

// 주의사항: 플러시된 행은 수정 불가능
// 따라서 헤더나 요약 정보는 마지막에 추가하거나 별도 처리 필요
```

### 2. 파일 I/O 최적화:
```java
// NIO를 활용한 파일 복사
Files.copy(templatePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
```

### 3. 동시성 제어:
```java
// 파일 생성 시 동시성 제어
@Async("fileCreationExecutor")
public CompletableFuture<Void> createFileAsync(MailData mailData) {
    // 파일 생성 로직
}
```

### 4. Excel 파일 생성 최적화 (3000행 기준):
```java
public void createExcelFile(MailData mailData, String outputPath) {
    // 윈도우 사이즈 500으로 설정 (3000행 처리 최적화)
    try (SXSSFWorkbook workbook = new SXSSFWorkbook(500)) {
        Sheet sheet = workbook.createSheet("청구서 상세내역");
        
        // 1. 헤더 생성 (첫 번째로 생성하여 플러시 방지)
        createHeaderRow(sheet, 0);
        
        // 2. 데이터 행 생성 (순차적으로 처리)
        List<BillItem> bills = mailData.getBills();
        for (int i = 0; i < bills.size(); i++) {
            Row dataRow = sheet.createRow(i + 1); // 헤더 다음부터
            populateDataRow(dataRow, bills.get(i));
            
            // 500행마다 진행 상황 로깅
            if ((i + 1) % 500 == 0) {
                log.debug("Excel 생성 진행률: {}/{}", i + 1, bills.size());
            }
        }
        
        // 3. 요약 정보는 마지막에 추가 (별도 시트 또는 상단 고정)
        addSummaryInfo(sheet, mailData.getBillSummary());
        
        // 4. 파일 저장
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        }
        
        // 5. 임시 파일 정리 (SXSSFWorkbook이 생성한 임시 파일들)
        workbook.dispose();
        
    } catch (IOException e) {
        log.error("Excel 파일 생성 실패: {}", outputPath, e);
        throw new FileCreationException("Excel 파일 생성 실패", e);
    }
}

private void createHeaderRow(Sheet sheet, int rowIndex) {
    Row headerRow = sheet.createRow(rowIndex);
    String[] headers = {"순번", "청구유형", "주문번호", "상품명", "설치일자", 
                       "사용월", "공급가액", "부가세", "청구금액", "설치주소"};
    
    for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        // 헤더 스타일 적용
        cell.setCellStyle(getHeaderStyle(sheet.getWorkbook()));
    }
}

private void populateDataRow(Row row, BillItem bill) {
    row.createCell(0).setCellValue(row.getRowNum()); // 순번
    row.createCell(1).setCellValue(bill.getRecpTpTx()); // 청구유형
    row.createCell(2).setCellValue(bill.getOrderNo()); // 주문번호
    row.createCell(3).setCellValue(bill.getVtext()); // 상품명
    row.createCell(4).setCellValue(bill.getInstDt()); // 설치일자
    row.createCell(5).setCellValue(bill.getUseMonth()); // 사용월
    row.createCell(6).setCellValue(bill.getSupplyValue()); // 공급가액
    row.createCell(7).setCellValue(bill.getVat()); // 부가세
    row.createCell(8).setCellValue(bill.getBillAmt()); // 청구금액
    row.createCell(9).setCellValue(bill.getInstJuso()); // 설치주소
}
```

---

## 🔄 장애 복구 및 재처리

### 1. 재처리 대상 조회:
```sql
SELECT SEQ, MAILDATA, ERROR_MSG
FROM b2b_automail_dt
WHERE FILE_CREATE_FLAG = 'E'
AND LEFT(DT_CREATE_DATE, 7) = DATE_FORMAT(NOW(), '%Y-%m');
```

### 2. 수동 재처리 API:
```java
@PostMapping("/api/batch/file-creation/retry")
public ResponseEntity<String> retryFileCreation(@RequestParam Long seq) {
    // 특정 SEQ에 대한 파일 생성 재시도
}
```

### 3. 배치 재시작:
```java
// Spring Batch 재시작 기능 활용
JobParameters jobParameters = new JobParametersBuilder()
    .addLong("restart.seq", lastProcessedSeq)
    .toJobParameters();
```

---