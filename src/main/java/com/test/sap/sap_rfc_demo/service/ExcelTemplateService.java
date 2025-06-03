package com.test.sap.sap_rfc_demo.service;

import com.test.sap.sap_rfc_demo.dto.BusinessTemplateDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Excel 템플릿 생성 서비스
 * 사업자별 Excel 청구서 템플릿 생성 (조건부 렌더링 포함)
 */
@Service
@Slf4j
public class ExcelTemplateService {
    
    @Autowired
    private FileManagementService fileManagementService;
    
    @Value("${app.excel.stamp.enabled:true}")
    private boolean stampProcessingEnabled;
    
    @Value("${app.excel.stamp.debug:false}")
    private boolean stampDebugEnabled;
    
    @Value("${app.excel.stamp.region.enabled:true}")
    private boolean stampRegionCheckEnabled;
    
    /**
     * Excel 템플릿 생성 (조건부 렌더링 포함)
     */
    public void generateExcelTemplate(BusinessTemplateDto business) {
        try {
            log.info("Excel 템플릿 생성 시작 - 사업자번호: {}", business.getBusinessNo());
            
            // 1. 사업자별 디렉토리 생성
            fileManagementService.createBusinessDirectory(business.getBusinessNo());
            
            // 2. 기본 템플릿 파일 로드
            String baseTemplatePath = fileManagementService.getBaseExcelTemplatePath();
            
            try (FileInputStream fis = new FileInputStream(baseTemplatePath);
                 XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
                
                Sheet sheet = workbook.getSheetAt(0);
                
                // 3. 조건부 열 처리 적용
                processConditionalColumns(sheet, business);
                
                // 4. 도장 이미지 조건부 처리 (새로 추가)
                processStampImage(workbook, sheet, business);
                
                // 5. 사업자 데이터 바인딩
                bindBusinessDataToExcel(sheet, business);
                
                // 6. 대상 파일 경로
                Path businessDir = fileManagementService.getBusinessDirectoryPath(business.getBusinessNo());
                String fileName = business.getBusinessNo() + ".xlsx";
                Path targetFile = businessDir.resolve(fileName);
                
                // 7. 파일 저장
                try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                    workbook.write(fos);
                }
                
                log.info("Excel 템플릿 생성 완료 - 사업자번호: {}, 경로: {}", business.getBusinessNo(), targetFile);
            }
            
        } catch (IOException e) {
            log.error("Excel 템플릿 생성 실패 - 사업자번호: {}", business.getBusinessNo(), e);
            throw new RuntimeException("Excel 템플릿 생성 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * Excel 템플릿에 조건부 열 처리 적용
     * 엑셀 템플릿의 27라인부터 시작하는 테이블 영역에 적용
     */
    private void processConditionalColumns(Sheet sheet, BusinessTemplateDto business) {
        log.info("Excel 조건부 열 처리 시작 - 사업자번호: {} (27라인부터)", business.getBusinessNo());
        
        // 테이블 시작 행 (27라인 = 인덱스 26)
        int tableStartRow = 26;
        
        // 테이블 헤더 행과 데이터 행 범위 확인
        int lastRowNum = sheet.getLastRowNum();
        
        // 조건부 열 숨김 처리 (실제 템플릿 구조에 따라 열 인덱스 조정)
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 1, business.isOrderNoVisible(), "주문번호");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 2, business.isProdGroupVisible(), "품목(제품군)");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 3, business.isGoodsNmVisible(), "제품명(모델명)");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 4, business.isContractDateVisible(), "계약일(설치일)");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 5, business.isUseDutyMonthVisible(), "의무사용기간");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 6, business.isContractPeriodVisible(), "약정기간");


        processConditionalColumn(sheet, tableStartRow, lastRowNum, 9, business.isFixSupplyValueVisible(), "월 렌탈료(공급가액)");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 10, business.isFixVatVisible(), "월 렌탈료(부가세)");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 11, business.isFixBillAmtVisible(), "월 렌탈료(합계)");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 12, business.isSupplyValueVisible(), "당월 렌탈료(공급가액)");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 13, business.isVatVisible(), "당월 렌탈료(부가세)");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 14, business.isBillAmtVisible(), "당월 렌탈료(합계)");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 15, business.isMembershipVisible(), "멤버십");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 16, business.isAsFeeVisible(), "A/S 대금");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 17, business.isConsumableFeeVisible(), "소모품 교체비");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 18, business.isOvdIntVisible(), "연체이자");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 19, business.isPenaltyFeeVisible(), "위약금");

        processConditionalColumn(sheet, tableStartRow, lastRowNum, 21, business.isPayInfoVisible(), "결제정보");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 22, business.isPayInfoVisible(), "");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 23, business.isPrepayVisible(), "선납금");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 24, business.isPrepayVisible(), "선납 잔여금");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 25, business.isPrepayVisible(), "선납 기간");

        processConditionalColumn(sheet, tableStartRow, lastRowNum, 26, business.isInstallAddrVisible(), "설치처 주소");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 27, business.isGoodsSnVisible(), "바코드 번호");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 28, business.isDeptNmVisible(), "관리지국명");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 29, business.isDeptTelNoVisible(), "관리지국 연락처");
        processConditionalColumn(sheet, tableStartRow, lastRowNum, 35, business.isNoteVisible(), "비고");
        
        log.info("Excel 조건부 열 처리 완료 - 사업자번호: {}", business.getBusinessNo());
    }
    
    /**
     * 특정 테이블 영역의 열을 조건부로 처리
     */
    private void processConditionalColumn(Sheet sheet, int startRow, int endRow, int columnIndex, 
                                        boolean isVisible, String columnName) {
        if (isVisible) {
            log.debug("Excel 테이블 열 유지: {} (열 인덱스: {})", columnName, columnIndex);
            return; // 표시해야 하므로 그대로 유지
        }
        
        log.info("Excel 테이블 열 숨김 처리: {} (열 인덱스: {}, 행 범위: {}-{})", 
                columnName, columnIndex, startRow + 1, endRow + 1);
        
        try {
            // 테이블 영역의 해당 열 셀들을 숨기거나 제거
            for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Cell cell = row.getCell(columnIndex);
                    if (cell != null) {
                        // 셀 내용을 빈 문자열로 설정하여 숨김 효과
                        cell.setCellValue("");
                        // 또는 셀 스타일을 숨김으로 설정
                        CellStyle hiddenStyle = sheet.getWorkbook().createCellStyle();
                        hiddenStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
                        hiddenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        cell.setCellStyle(hiddenStyle);
                    }
                }
            }
            
            // 전체 열을 숨김 처리 (선택사항)
            sheet.setColumnHidden(columnIndex, true);
            
            log.info("Excel 테이블 열 숨김 완료: {} (열 인덱스: {})", columnName, columnIndex);
            
        } catch (Exception e) {
            log.warn("Excel 테이블 열 숨김 처리 실패: {} (열 인덱스: {}) - {}", 
                    columnName, columnIndex, e.getMessage());
        }
    }
    
    /**
     * Excel 템플릿에 사업자 데이터 바인딩
     */
    private void bindBusinessDataToExcel(Sheet sheet, BusinessTemplateDto business) {
        log.info("Excel 사업자 데이터 바인딩 시작 - 사업자번호: {}", business.getBusinessNo());
        
        try {
            // 사업자 정보 바인딩 (실제 템플릿 구조에 따라 조정 필요)
            // 예시: 첫 번째 시트의 특정 셀에 사업자 정보 입력
            
            // 사업자명 (예: B2 셀)
            //setCellValue(sheet, 1, 1, business.getBusinessNm());
            
            // 대표자명 (예: B3 셀)
            //setCellValue(sheet, 2, 1, business.getCeoNm());
            
            // 사업자번호 (예: B4 셀)
            //setCellValue(sheet, 3, 1, business.getBusinessNo());
            
            // 사업자 주소 (예: B5 셀)
            //setCellValue(sheet, 4, 1, business.getBusinessAddr());
            
            // 업태 (예: B6 셀)
            //setCellValue(sheet, 5, 1, business.getBusinessType());
            
            // 종목 (예: B7 셀)
            //setCellValue(sheet, 6, 1, business.getBusinessCategory());
            
            // 조건부 섹션 데이터 바인딩
            //if (business.isCsGuideVisible() && business.getCsGuide() != null) {
                // 고객센터 안내 (예: B10 셀)
                //setCellValue(sheet, 9, 1, business.getCsGuide());
            //}
            
            //if (business.isPayGuideVisible() && business.getPayGuide() != null) {
                // 결제안내 정보 (예: B11 셀)
                //setCellValue(sheet, 10, 1, business.getPayGuide());
            //}
            
            log.info("Excel 사업자 데이터 바인딩 완료 - 사업자번호: {}", business.getBusinessNo());
            
        } catch (Exception e) {
            log.error("Excel 사업자 데이터 바인딩 실패 - 사업자번호: {}", business.getBusinessNo(), e);
        }
    }
    
    /**
     * 셀에 값 설정 (헬퍼 메서드)
     */
    private void setCellValue(Sheet sheet, int rowIndex, int columnIndex, String value) {
        if (value == null) {
            return;
        }
        
        try {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            
            Cell cell = row.getCell(columnIndex);
            if (cell == null) {
                cell = row.createCell(columnIndex);
            }
            
            cell.setCellValue(value);
            log.debug("Excel 셀 값 설정: [{}, {}] = {}", rowIndex, columnIndex, value);
            
        } catch (Exception e) {
            log.warn("Excel 셀 값 설정 실패: [{}, {}] = {} - {}", rowIndex, columnIndex, value, e.getMessage());
        }
    }

    /**
     * 도장 이미지 조건부 처리
     * stamp_yn이 Y인 경우만 도장 이미지를 유지하고, N인 경우 제거/숨김 처리
     */
    private void processStampImage(XSSFWorkbook workbook, Sheet sheet, BusinessTemplateDto business) {
        try {
            if (!stampProcessingEnabled) {
                log.info("도장 이미지 처리 비활성화 - 사업자번호: {}", business.getBusinessNo());
                return;
            }
            
            log.info("도장 이미지 조건부 처리 시작 - 사업자번호: {}, 인감날인 사용여부: {}", 
                    business.getBusinessNo(), business.getStampYn());
            
            if (!business.isStampVisible()) {
                // stamp_yn이 N인 경우: 기존 도장 이미지들을 제거
                removeStampImages(workbook, sheet);
                log.info("도장 이미지 제거 완료 - 사업자번호: {} (인감날인 미사용)", business.getBusinessNo());
            } else {
                // stamp_yn이 Y인 경우: 도장 이미지 유지 (필요시 추가 처리)
                log.info("도장 이미지 유지 - 사업자번호: {} (인감날인 사용)", business.getBusinessNo());
            }
            
        } catch (Exception e) {
            log.error("도장 이미지 처리 실패 - 사업자번호: {}", business.getBusinessNo(), e);
            // 도장 이미지 처리 실패는 전체 프로세스를 중단시키지 않음
        }
    }
    
    /**
     * 엑셀 워크북에서 도장 이미지들을 제거
     */
    private void removeStampImages(XSSFWorkbook workbook, Sheet sheet) {
        try {
            // XSSFSheet로 캐스팅하여 그림 객체에 접근
            if (sheet instanceof org.apache.poi.xssf.usermodel.XSSFSheet) {
                org.apache.poi.xssf.usermodel.XSSFSheet xssfSheet = 
                    (org.apache.poi.xssf.usermodel.XSSFSheet) sheet;
                
                XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
                
                if (drawing != null) {
                    // 모든 도형/이미지 객체를 순회하며 도장 이미지 제거
                    java.util.List<XSSFPicture> pictures = drawing.getShapes()
                        .stream()
                        .filter(shape -> shape instanceof XSSFPicture)
                        .map(shape -> (XSSFPicture) shape)
                        .collect(java.util.stream.Collectors.toList());
                    
                    for (XSSFPicture picture : pictures) {
                        // 도장 이미지인지 확인 (파일명, 위치, 크기 등으로 판단)
                        if (isStampImage(picture)) {
                            // 이미지 제거
                            removePicture(drawing, picture);
                            log.debug("도장 이미지 제거: {}", picture.getPictureData().suggestFileExtension());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("도장 이미지 제거 처리 중 오류: {}", e.getMessage());
        }
    }
    
    /**
     * 해당 이미지가 도장 이미지인지 판단
     * 이미지의 위치, 크기, 파일명 등을 기준으로 판단
     */
    private boolean isStampImage(XSSFPicture picture) {
        try {
            // 도장 이미지 판단 기준:
            // 1. 파일 확장자가 이미지 파일인지 확인
            String fileExtension = picture.getPictureData().suggestFileExtension();
            if (!isImageFile(fileExtension)) {
                return false;
            }
            
            // 2. 이미지 위치로 판단 (엑셀 템플릿의 도장 위치 영역)
            XSSFClientAnchor anchor = (XSSFClientAnchor) picture.getAnchor();
            if (anchor != null) {
                int col1 = anchor.getCol1(); // 시작 열
                int row1 = anchor.getRow1(); // 시작 행
                int col2 = anchor.getCol2(); // 끝 열
                int row2 = anchor.getRow2(); // 끝 행
                
                // 도장이 위치할 것으로 예상되는 영역들 (복수 영역 지원)
                boolean isInStampArea = isInStampRegion(col1, row1, col2, row2);
                
                // 3. 이미지 크기로 판단 (도장은 보통 작은~중간 크기)
                int width = col2 - col1;
                int height = row2 - row1;
                boolean isStampSize = isStampSizeRange(width, height);
                
                log.debug("이미지 위치 및 크기 확인 - 위치: ({},{}) ~ ({},{}), 크기: {}x{}, 도장영역: {}, 도장크기: {}", 
                         col1, row1, col2, row2, width, height, isInStampArea, isStampSize);
                
                // 두 조건 중 하나라도 만족하면 도장으로 판단 (더 관대한 기준)
                return isInStampArea || isStampSize;
            }
            
            return false;
            
        } catch (Exception e) {
            log.warn("도장 이미지 판단 중 오류: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 도장이 위치할 수 있는 영역인지 확인 (복수 영역 지원)
     */
    private boolean isInStampRegion(int col1, int row1, int col2, int row2) {
        // 영역 1: 우상단 (일반적인 도장 위치)
        boolean region1 = (col1 >= 20 && col1 <= 40) && (row1 >= 0 && row1 <= 20);
        
        // 영역 2: 우하단 (서명란 근처)
        boolean region2 = (col1 >= 20 && col1 <= 40) && (row1 >= 50 && row1 <= 100);
        
        // 영역 3: 중앙 우측 (중간 영역)
        boolean region3 = (col1 >= 25 && col1 <= 35) && (row1 >= 20 && row1 <= 50);
        
        // 영역 4: 좌측 상단 (사업자 정보 근처)
        boolean region4 = (col1 >= 0 && col1 <= 10) && (row1 >= 0 && row1 <= 20);
        
        return region1 || region2 || region3 || region4;
    }
    
    /**
     * 도장 크기 범위인지 확인
     */
    private boolean isStampSizeRange(int width, int height) {
        // 작은 도장: 1x1 ~ 4x4
        boolean smallStamp = (width >= 1 && width <= 4) && (height >= 1 && height <= 4);
        
        // 중간 도장: 3x3 ~ 8x8
        boolean mediumStamp = (width >= 3 && width <= 8) && (height >= 3 && height <= 8);
        
        // 큰 도장: 6x6 ~ 12x12
        boolean largeStamp = (width >= 6 && width <= 12) && (height >= 6 && height <= 12);
        
        return smallStamp || mediumStamp || largeStamp;
    }
    
    /**
     * 파일 확장자가 이미지 파일인지 확인
     */
    private boolean isImageFile(String fileExtension) {
        if (fileExtension == null) {
            return false;
        }
        
        String ext = fileExtension.toLowerCase();
        return ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || 
               ext.equals("gif") || ext.equals("bmp") || ext.equals("tiff");
    }
    
    /**
     * Drawing에서 특정 Picture 제거
     */
    private void removePicture(XSSFDrawing drawing, XSSFPicture picture) {
        try {
            // POI에서 직접적인 Picture 제거 방법이 제한적이므로
            // 이미지를 투명하게 만들거나 위치를 화면 밖으로 이동
            XSSFClientAnchor anchor = (XSSFClientAnchor) picture.getAnchor();
            if (anchor != null) {
                // 이미지를 화면 밖의 위치로 이동 (사실상 숨김)
                anchor.setCol1(1000);
                anchor.setCol2(1001);
                anchor.setRow1(1000);
                anchor.setRow2(1001);
                
                // 또는 크기를 0으로 만들어 보이지 않게 처리
                anchor.setDx1(0);
                anchor.setDy1(0);
                anchor.setDx2(0);
                anchor.setDy2(0);
                
                log.debug("도장 이미지 숨김 처리 완료");
            }
            
        } catch (Exception e) {
            log.warn("도장 이미지 제거 중 오류: {}", e.getMessage());
        }
    }
} 