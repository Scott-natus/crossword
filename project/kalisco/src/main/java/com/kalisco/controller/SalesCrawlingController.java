package com.kalisco.controller;

import com.kalisco.service.ExcelExportService;
import com.kalisco.service.SalesCrawlingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 매출 크롤링 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SalesCrawlingController {
    
    private final SalesCrawlingService scraperService;
    private final ExcelExportService excelExportService;
    
    /**
     * 크롤링 페이지
     * GET /sales/crawling/saleSituation/
     */
    @GetMapping("/sales/crawling/saleSituation/")
    public ResponseEntity<String> crawlingPage() {
        try {
            log.info("크롤링 페이지 접근: /sales/crawling/saleSituation/");
            
            ClassPathResource resource = new ClassPathResource("static/sales/crawling/saleSituation/index.html");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (IOException e) {
            log.error("크롤링 페이지 로드 중 오류 발생", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 매장 목록 조회 API
     * GET /api/sales/store-list?saleType=menu
     */
    @GetMapping("/api/sales/store-list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStoreList(@RequestParam(required = false, defaultValue = "menu") String saleType) {
        log.info("매장 목록 조회 요청: saleType={}", saleType);
        try {
            Map<String, Object> result = scraperService.getStoreList(saleType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("매장 목록 조회 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "매장 목록 조회 중 오류 발생: " + e.getMessage(),
                    "stores", List.of()
                ));
        }
    }
    
    /**
     * 매출 데이터 크롤링 API (메뉴별 또는 일별)
     * POST /api/sales/menu-sales
     */
    @PostMapping("/api/sales/menu-sales")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> scrapeMenuSales(@RequestBody(required = false) Map<String, String> requestBody) {
        log.info("매출 데이터 크롤링 요청");
        
        String saleType = requestBody != null ? requestBody.get("saleType") : "menu";
        String startDate = requestBody != null ? requestBody.get("startDate") : null;
        String endDate = requestBody != null ? requestBody.get("endDate") : null;
        String year = requestBody != null ? requestBody.get("year") : null;
        String month = requestBody != null ? requestBody.get("month") : null;
        String storeValue = requestBody != null ? requestBody.get("storeValue") : null;
        
        if ("daily".equals(saleType)) {
            log.info("일별 매출 크롤링: {}년 {}월, 매장: {}", year, month, storeValue);
        } else {
            log.info("메뉴별 매출 크롤링: {} ~ {}, 매장: {}", startDate, endDate, storeValue);
        }
        
        try {
            Map<String, Object> result = scraperService.scrapeMenuSales(saleType, startDate, endDate, year, month, storeValue);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("크롤링 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "크롤링 중 오류 발생: " + e.getMessage()
                ));
        }
    }
    
    /**
     * 매출 데이터 엑셀 다운로드
     * GET /api/sales/menu-sales/excel?startDate=2025-11-11&endDate=2025-11-11&saleType=menu
     * GET /api/sales/menu-sales/excel?year=2025&month=11&saleType=daily
     */
    @GetMapping("/api/sales/menu-sales/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String saleType,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String storeValue) {
        log.info("매출 엑셀 다운로드 요청: saleType={}, startDate={}, endDate={}, year={}, month={}, storeValue={}", 
            saleType, startDate, endDate, year, month, storeValue);
        
        try {
            if (saleType == null) saleType = "menu";
            Map<String, Object> result = scraperService.scrapeMenuSales(saleType, startDate, endDate, year, month, storeValue);
            
            @SuppressWarnings("unchecked")
            List<Map<String, String>> dataList = (List<Map<String, String>>) result.get("data");
            
            if (dataList == null || dataList.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("데이터가 없습니다.".getBytes(StandardCharsets.UTF_8));
            }
            
            // 헤더 추출 (result에서 가져오거나 기본 헤더 사용)
            @SuppressWarnings("unchecked")
            List<String> headers = (List<String>) result.get("headers");
            if (headers == null || headers.isEmpty()) {
                // result에 헤더가 없으면 기본 헤더 사용
                if ("daily".equals(saleType)) {
                    headers = List.of("일자", "매출액(원)", "영수건수", "고객수");
                } else {
                    headers = List.of("메뉴명", "단가", "수량", "매출액(원)", "구성비");
                }
            }
            
            byte[] excelBytes = excelExportService.exportToExcel(dataList, headers);
            
            // 파일명 생성 (기능별_일시분_매장명)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String storeName = "";
            if (storeValue != null && !storeValue.isEmpty()) {
                // 매장명에서 파일명에 사용할 수 없는 특수문자 제거
                storeName = storeValue.replaceAll("[\\/\\\\:*?\"<>|]", "_").replaceAll("\\s+", "_");
                storeName = "_" + storeName;
            }
            String filename;
            if ("daily".equals(saleType)) {
                filename = String.format("일별매출_%s%s.xlsx", timestamp, storeName);
            } else {
                filename = String.format("메뉴별매출_%s%s.xlsx", timestamp, storeName);
            }
            
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDispositionFormData("attachment", filename);
            responseHeaders.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(excelBytes);
                
        } catch (Exception e) {
            log.error("엑셀 다운로드 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(("엑셀 다운로드 중 오류 발생: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
}
