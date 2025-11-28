package com.example.board.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 우리홈 메뉴별매출 데이터 크롤링 서비스
 */
@Service
@Slf4j
public class OurHomeScraperService {
    
    private static final String BASE_URL = "https://ohrf.ourhome.co.kr/fnbm/main.do";
    private static final String MENU_SALES_URL = "https://ohrf.ourhome.co.kr/fnbm/sale/menuSaleForm.do";
    private static final String DAILY_SALES_URL = "https://ohrf.ourhome.co.kr/fnbm/sale/dailySaleForm.do";
    private static final String USERNAME = "kali2023";
    private static final String PASSWORD = "kali2023";
    
    /**
     * 매출 데이터 크롤링 (메뉴별 또는 일별)
     * @param saleType 매출 타입 ("menu" 또는 "daily")
     * @param startDate 시작일 (YYYY-MM-DD 형식, 메뉴별일 때만 사용)
     * @param endDate 종료일 (YYYY-MM-DD 형식, 메뉴별일 때만 사용)
     * @param year 연도 (일별일 때만 사용)
     * @param month 월 (일별일 때만 사용)
     * @param storeValue 매장 선택값 (선택사항)
     * @return 크롤링된 데이터 리스트
     */
    public Map<String, Object> scrapeMenuSales(String saleType, String startDate, String endDate, String year, String month, String storeValue) {
        WebDriver driver = null;
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("우리홈 매출 데이터 크롤링 시작 (타입: {})", saleType);
            
            // Chrome WebDriver 설정
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
            options.addArguments("--disable-software-rasterizer");
            options.addArguments("--disable-extensions");
            
            // snap으로 설치된 chromium 사용 시 경로 설정
            try {
                String chromiumPath = "/usr/bin/chromium-browser";
                if (new java.io.File(chromiumPath).exists()) {
                    options.setBinary(chromiumPath);
                    log.info("Chromium 바이너리 경로 설정: {}", chromiumPath);
                } else {
                    log.warn("Chromium 바이너리를 찾을 수 없습니다. 기본 경로 사용");
                }
            } catch (Exception e) {
                log.warn("Chromium 바이너리 경로 설정 실패: {}", e.getMessage());
            }
            
            log.info("ChromeDriver 초기화 시작...");
            try {
                // ChromeDriver 경로 명시적 지정
                String chromedriverPath = "/usr/bin/chromedriver";
                File chromedriverFile = new File(chromedriverPath);
                
                if (!chromedriverFile.exists()) {
                    log.warn("ChromeDriver를 찾을 수 없습니다: {}. 시스템 PATH에서 찾습니다.", chromedriverPath);
                    chromedriverPath = null; // null이면 시스템 PATH에서 찾음
                } else {
                    log.info("ChromeDriver 경로: {}", chromedriverPath);
                }
                
                // ChromeDriverService 설정
                ChromeDriverService.Builder serviceBuilder = new ChromeDriverService.Builder();
                if (chromedriverPath != null) {
                    serviceBuilder.usingDriverExecutable(chromedriverFile);
                }
                ChromeDriverService service = serviceBuilder.build();
                
                driver = new ChromeDriver(service, options);
                log.info("ChromeDriver 초기화 완료");
            } catch (Exception e) {
                log.error("ChromeDriver 초기화 실패: {}", e.getMessage(), e);
                throw new Exception("ChromeDriver 초기화 실패: " + e.getMessage(), e);
            }
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            // 1. 로그인 페이지 접속
            log.info("로그인 페이지 접속: {}", BASE_URL);
            driver.get(BASE_URL);
            Thread.sleep(500);
            
            // 2. 로그인 처리
            log.info("로그인 처리 중...");
            WebElement usernameField = findElement(driver, 
                "input[name='userId']", 
                "input[name='id']", 
                "input[type='text']",
                "#userId",
                "#id"
            );
            
            WebElement passwordField = findElement(driver,
                "input[name='password']",
                "input[name='pwd']",
                "input[type='password']",
                "#password",
                "#pwd"
            );
            
            if (usernameField == null || passwordField == null) {
                throw new Exception("로그인 필드를 찾을 수 없습니다");
            }
            
            usernameField.clear();
            usernameField.sendKeys(USERNAME);
            passwordField.clear();
            passwordField.sendKeys(PASSWORD);
            
            Thread.sleep(200);
            
            // 로그인 버튼 클릭
            WebElement loginButton = findElement(driver,
                "button[type='submit']",
                "input[type='submit']",
                "//button[contains(text(), '로그인')]",
                ".btn-login",
                "#loginBtn"
            );
            
            if (loginButton != null) {
                loginButton.click();
            } else {
                passwordField.submit();
            }
            
            // 로그인 완료 대기 (명시적 대기로 변경)
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search")));
                log.info("로그인 완료");
            } catch (Exception e) {
                Thread.sleep(1000);
                log.info("로그인 완료 (대기 타임아웃)");
            }
            
            // 3. 선택된 타입에 따라 URL로 직접 이동
            String targetUrl;
            if ("daily".equals(saleType)) {
                targetUrl = DAILY_SALES_URL;
                log.info("일별 매출 페이지로 이동: {}", targetUrl);
            } else {
                targetUrl = MENU_SALES_URL;
                log.info("메뉴별매출 페이지로 이동: {}", targetUrl);
            }
            
            driver.get(targetUrl);
            Thread.sleep(1000);
            
            // 조회 버튼이 나타날 때까지 대기
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search")));
                log.info("매출 페이지 로드 완료");
            } catch (Exception e) {
                Thread.sleep(1000);
                log.info("매출 페이지 로드 완료 (대기 타임아웃)");
            }
            
            // 페이지 로드 후 기본값 확인 및 로깅
            log.info("=== 페이지 기본값 확인 시작 ===");
            if ("daily".equals(saleType)) {
                // 일별: 연도/월 기본값 확인
                try {
                    WebElement yearSelect = driver.findElement(By.cssSelector("select[name*='year'], select[id*='year'], select[name*='YEAR'], select[id*='YEAR']"));
                    String defaultYear = yearSelect.getAttribute("value");
                    String selectedYearText = ((JavascriptExecutor) driver).executeScript(
                        "return arguments[0].options[arguments[0].selectedIndex] ? arguments[0].options[arguments[0].selectedIndex].text : '';", 
                        yearSelect).toString();
                    log.info("기본 연도: {} (선택된 텍스트: {})", defaultYear, selectedYearText);
                } catch (Exception e) {
                    log.warn("연도 선택 필드 기본값 확인 실패: {}", e.getMessage());
                }
                
                try {
                    WebElement monthSelect = driver.findElement(By.cssSelector("select[name*='month'], select[id*='month'], select[name*='MONTH'], select[id*='MONTH']"));
                    String defaultMonth = monthSelect.getAttribute("value");
                    String selectedMonthText = ((JavascriptExecutor) driver).executeScript(
                        "return arguments[0].options[arguments[0].selectedIndex] ? arguments[0].options[arguments[0].selectedIndex].text : '';", 
                        monthSelect).toString();
                    log.info("기본 월: {} (선택된 텍스트: {})", defaultMonth, selectedMonthText);
                } catch (Exception e) {
                    log.warn("월 선택 필드 기본값 확인 실패: {}", e.getMessage());
                }
            } else {
                // 메뉴별: 날짜 기본값 확인
                List<WebElement> dateFields = driver.findElements(By.cssSelector("input[type='text']"));
                for (int i = 0; i < dateFields.size(); i++) {
                    try {
                        WebElement field = dateFields.get(i);
                        String value = field.getAttribute("value");
                        String placeholder = field.getAttribute("placeholder");
                        String id = field.getAttribute("id");
                        String name = field.getAttribute("name");
                        if (value != null && (value.matches("\\d{4}\\.\\d{2}\\.\\d{2}") || value.matches("\\d{4}-\\d{2}-\\d{2}"))) {
                            log.info("날짜 필드 #{} 기본값: {} (id={}, name={}, placeholder={})", i + 1, value, id, name, placeholder);
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            // 페이지의 모든 콤보박스(select) 확인 및 로깅
            log.info("=== 페이지 콤보박스 확인 시작 ===");
            try {
                List<WebElement> allSelects = driver.findElements(By.tagName("select"));
                log.info("발견된 콤보박스 개수: {}", allSelects.size());
                
                for (int i = 0; i < allSelects.size(); i++) {
                    try {
                        WebElement select = allSelects.get(i);
                        String selectId = select.getAttribute("id");
                        String selectName = select.getAttribute("name");
                        String selectClass = select.getAttribute("class");
                        String selectedValue = select.getAttribute("value");
                        
                        // 선택된 옵션 텍스트
                        String selectedText = "";
                        try {
                            selectedText = ((JavascriptExecutor) driver).executeScript(
                                "return arguments[0].options[arguments[0].selectedIndex] ? arguments[0].options[arguments[0].selectedIndex].text : '';", 
                                select).toString();
                        } catch (Exception e) {
                            // 무시
                        }
                        
                        // 모든 옵션 추출
                        List<WebElement> selectOptions = select.findElements(By.tagName("option"));
                        List<String> optionValues = new ArrayList<>();
                        List<String> optionTexts = new ArrayList<>();
                        
                        for (WebElement option : selectOptions) {
                            try {
                                String optValue = option.getAttribute("value");
                                String optText = option.getText();
                                optionValues.add(optValue != null ? optValue : "");
                                optionTexts.add(optText != null ? optText : "");
                            } catch (Exception e) {
                                continue;
                            }
                        }
                        
                        log.info("--- 콤보박스 #{} ---", i + 1);
                        log.info("  ID: {}", selectId != null ? selectId : "(없음)");
                        log.info("  Name: {}", selectName != null ? selectName : "(없음)");
                        log.info("  Class: {}", selectClass != null ? selectClass : "(없음)");
                        log.info("  선택된 값: {} (텍스트: {})", selectedValue != null ? selectedValue : "(없음)", selectedText);
                        log.info("  옵션 개수: {}", selectOptions.size());
                        
                        if (optionValues.size() <= 20) {
                            // 옵션이 20개 이하면 모두 출력
                            for (int j = 0; j < optionValues.size(); j++) {
                                log.info("    [{}] value='{}' text='{}'", 
                                    j, 
                                    optionValues.get(j), 
                                    optionTexts.get(j));
                            }
                        } else {
                            // 옵션이 많으면 처음 10개와 마지막 10개만 출력
                            log.info("  (옵션이 많아 처음 10개와 마지막 10개만 표시)");
                            for (int j = 0; j < 10; j++) {
                                log.info("    [{}] value='{}' text='{}'", 
                                    j, 
                                    optionValues.get(j), 
                                    optionTexts.get(j));
                            }
                            log.info("    ... (중간 생략) ...");
                            for (int j = optionValues.size() - 10; j < optionValues.size(); j++) {
                                log.info("    [{}] value='{}' text='{}'", 
                                    j, 
                                    optionValues.get(j), 
                                    optionTexts.get(j));
                            }
                        }
                        
                    } catch (Exception e) {
                        log.warn("콤보박스 #{} 정보 추출 실패: {}", i + 1, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("콤보박스 확인 중 오류: {}", e.getMessage());
            }
            log.info("=== 페이지 콤보박스 확인 완료 ===");
            log.info("=== 페이지 기본값 확인 완료 ===");
            
            // 4. 입력 필드 설정 (메뉴별: 날짜, 일별: 연도/월)
            if ("daily".equals(saleType)) {
                // 일별: 연도와 월 선택
                if (year != null && month != null) {
                    log.info("연도/월 입력 중: {}년 {}월", year, month);
                    
                    // 연도 선택 필드 찾기
                    try {
                        WebElement yearSelect = driver.findElement(By.cssSelector("select[name*='year'], select[id*='year'], select[name*='YEAR'], select[id*='YEAR']"));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", yearSelect, year);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", yearSelect);
                        log.info("연도 선택: {}", year);
                        Thread.sleep(200);
                    } catch (Exception e) {
                        log.warn("연도 선택 필드를 찾을 수 없습니다: {}", e.getMessage());
                    }
                    
                    // 월 선택 필드 찾기
                    try {
                        WebElement monthSelect = driver.findElement(By.cssSelector("select[name*='month'], select[id*='month'], select[name*='MONTH'], select[id*='MONTH']"));
                        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", monthSelect, month);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", monthSelect);
                        log.info("월 선택: {}", month);
                        Thread.sleep(200);
                    } catch (Exception e) {
                        log.warn("월 선택 필드를 찾을 수 없습니다: {}", e.getMessage());
                    }
                }
            } else {
                // 메뉴별: 시작일, 종료일 입력
                if (startDate != null && endDate != null) {
                    log.info("날짜 입력 중: {} ~ {}", startDate, endDate);
                    
                    // 날짜 형식 변환 (YYYY-MM-DD -> YYYY.MM.DD)
                    String startDateFormatted = startDate.replace("-", ".");
                    String endDateFormatted = endDate.replace("-", ".");
                    
                    // 날짜 입력 필드 찾기 (여러 개일 수 있음)
                    List<WebElement> dateFields = driver.findElements(By.cssSelector("input[type='text']"));
                    List<WebElement> dateInputs = new ArrayList<>();
                    
                    for (WebElement field : dateFields) {
                        try {
                            String value = field.getAttribute("value");
                            String placeholder = field.getAttribute("placeholder");
                            // 날짜 형식이거나 날짜 관련 placeholder가 있는 필드 찾기
                            if ((value != null && value.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) ||
                                (placeholder != null && (placeholder.contains("날짜") || placeholder.contains("일자")))) {
                                dateInputs.add(field);
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    
                    log.info("날짜 입력 필드 {}개 발견", dateInputs.size());
                    
                    // 첫 번째 날짜 필드에 시작일, 두 번째에 종료일 입력
                    if (dateInputs.size() >= 1) {
                        WebElement firstDateField = dateInputs.get(0);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", firstDateField, startDateFormatted);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", firstDateField);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));", firstDateField);
                        log.info("첫 번째 날짜 필드에 시작일 입력: {}", startDateFormatted);
                        Thread.sleep(100);
                    }
                    
                    if (dateInputs.size() >= 2) {
                        WebElement secondDateField = dateInputs.get(1);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", secondDateField, endDateFormatted);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", secondDateField);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));", secondDateField);
                        log.info("두 번째 날짜 필드에 종료일 입력: {}", endDateFormatted);
                        Thread.sleep(100);
                    } else if (dateInputs.size() == 1) {
                        // 날짜 필드가 하나만 있으면 같은 날짜를 입력 (당일 조회)
                        WebElement dateField = dateInputs.get(0);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", dateField, endDateFormatted);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", dateField);
                        log.info("날짜 필드에 종료일 입력: {}", endDateFormatted);
                        Thread.sleep(100);
                    } else {
                        log.warn("날짜 입력 필드를 찾을 수 없습니다.");
                    }
                } else {
                    log.info("날짜가 지정되지 않아 기본 조회를 진행합니다.");
                }
            }
            
            // 4-1. 매장 선택 (storeValue가 제공된 경우)
            if (storeValue != null && !storeValue.isEmpty()) {
                log.info("매장 선택 중: {}", storeValue);
                try {
                    List<WebElement> allSelects = driver.findElements(By.tagName("select"));
                    WebElement storeSelect = null;
                    
                    // 매장 선택 콤보박스 찾기 (연도/월이 아닌 콤보박스 중 두 번째 것)
                    List<WebElement> candidateSelects = new ArrayList<>();
                    for (WebElement select : allSelects) {
                        String selectId = select.getAttribute("id");
                        String selectName = select.getAttribute("name");
                        // 연도/월이 아닌 것 찾기
                        boolean isYearMonth = (selectId != null && (selectId.toLowerCase().contains("year") || selectId.toLowerCase().contains("month"))) ||
                                            (selectName != null && (selectName.toLowerCase().contains("year") || selectName.toLowerCase().contains("month")));
                        
                        if (!isYearMonth) {
                            candidateSelects.add(select);
                        }
                    }
                    
                    // 두 번째 콤보박스 선택 (인덱스 1)
                    if (candidateSelects.size() >= 2) {
                        storeSelect = candidateSelects.get(1);
                        String selectId = storeSelect.getAttribute("id");
                        String selectName = storeSelect.getAttribute("name");
                        log.info("매장 선택 콤보박스 (두 번째): id={}, name={}", selectId, selectName);
                    } else if (candidateSelects.size() == 1) {
                        // 하나만 있으면 그것 사용
                        storeSelect = candidateSelects.get(0);
                        String selectId = storeSelect.getAttribute("id");
                        String selectName = storeSelect.getAttribute("name");
                        log.info("매장 선택 콤보박스 (유일): id={}, name={}", selectId, selectName);
                    } else {
                        log.warn("매장 선택 콤보박스를 찾을 수 없습니다. (연도/월 제외 후 {}개 발견)", candidateSelects.size());
                    }
                    
                    if (storeSelect != null) {
                        // storeValue가 텍스트인 경우 실제 value를 찾아서 설정
                        List<WebElement> storeOptions = storeSelect.findElements(By.tagName("option"));
                        String actualValue = null;
                        
                        // 먼저 value로 직접 매칭 시도
                        for (WebElement option : storeOptions) {
                            String optValue = option.getAttribute("value");
                            if (storeValue.equals(optValue)) {
                                actualValue = optValue;
                                log.info("value로 매칭 성공: {}", actualValue);
                                break;
                            }
                        }
                        
                        // value로 매칭 실패 시 텍스트로 매칭 시도
                        if (actualValue == null) {
                            for (WebElement option : storeOptions) {
                                String optText = option.getText();
                                String optValue = option.getAttribute("value");
                                if (optText != null && optText.equals(storeValue)) {
                                    actualValue = optValue;
                                    log.info("텍스트로 매칭 성공: '{}' -> value='{}'", storeValue, actualValue);
                                    break;
                                }
                            }
                        }
                        
                        // 부분 매칭 시도 (텍스트에 value가 포함된 경우)
                        if (actualValue == null && storeValue.contains("(") && storeValue.contains(")")) {
                            String codeInText = storeValue.substring(storeValue.indexOf("(") + 1, storeValue.indexOf(")"));
                            for (WebElement option : storeOptions) {
                                String optValue = option.getAttribute("value");
                                if (codeInText.equals(optValue)) {
                                    actualValue = optValue;
                                    log.info("코드 추출로 매칭 성공: '{}' -> value='{}'", codeInText, actualValue);
                                    break;
                                }
                            }
                        }
                        
                        if (actualValue != null) {
                            ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", storeSelect, actualValue);
                            ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", storeSelect);
                            log.info("매장 선택 완료: {} (실제 value: {})", storeValue, actualValue);
                            Thread.sleep(300);
                        } else {
                            log.warn("매장 선택 실패: '{}'에 해당하는 옵션을 찾을 수 없습니다.", storeValue);
                            // 옵션 목록 로깅
                            log.warn("사용 가능한 옵션 목록:");
                            for (int i = 0; i < Math.min(10, storeOptions.size()); i++) {
                                WebElement opt = storeOptions.get(i);
                                log.warn("  [{}] value='{}' text='{}'", i, opt.getAttribute("value"), opt.getText());
                            }
                        }
                    } else {
                        log.warn("매장 선택 콤보박스를 찾을 수 없습니다.");
                    }
                } catch (Exception e) {
                    log.warn("매장 선택 중 오류: {}", e.getMessage());
                }
            }
            
            // 5. 조회 버튼 클릭
            log.info("조회 버튼 찾는 중...");
            
            // 더 다양한 방법으로 조회 버튼 찾기
            WebElement searchButton = null;
            
            // 방법 1: id="search"로 직접 찾기 (가장 확실한 방법)
            try {
                searchButton = driver.findElement(By.id("search"));
                log.info("조회 버튼 발견: id=search");
            } catch (Exception e) {
                log.warn("id='search'로 조회 버튼을 찾을 수 없습니다: {}", e.getMessage());
            }
            
            // 방법 2: 일반적인 선택자들
            if (searchButton == null) {
                searchButton = findElement(driver,
                    "#search",
                    "//button[@id='search']",
                    "//input[@id='search']",
                    "//a[@id='search']",
                    "//button[contains(text(), '조회')]",
                    "//a[contains(text(), '조회')]",
                    "//input[@value='조회']",
                    "//input[@type='button' and contains(@value, '조회')]",
                    "//input[@type='submit' and contains(@value, '조회')]",
                    "button.btn-search",
                    "button.search",
                    "#searchBtn",
                    "#btnSearch",
                    "button[type='button']",
                    "button[type='submit']",
                    "input[type='button']",
                    "input[type='submit']"
                );
            }
            
            // 방법 2: onclick 속성이 있는 버튼 찾기
            if (searchButton == null) {
                try {
                    List<WebElement> buttons = driver.findElements(By.xpath("//button | //input[@type='button'] | //input[@type='submit'] | //a"));
                    for (WebElement btn : buttons) {
                        try {
                            String text = btn.getText().trim();
                            String onclick = btn.getAttribute("onclick");
                            String className = btn.getAttribute("class");
                            if ((text.contains("조회") || text.contains("검색") || text.contains("Search")) ||
                                (onclick != null && (onclick.contains("search") || onclick.contains("조회"))) ||
                                (className != null && className.contains("search"))) {
                                searchButton = btn;
                                log.info("조회 버튼 발견 (텍스트/속성 기반): text={}, onclick={}", text, onclick);
                                break;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                } catch (Exception e) {
                    log.warn("버튼 검색 중 오류: {}", e.getMessage());
                }
            }
            
            if (searchButton != null) {
                log.info("조회 버튼 찾음, 이벤트 확인 중...");
                
                // 조회 버튼의 이벤트 정보 확인
                try {
                    String onclick = searchButton.getAttribute("onclick");
                    String id = searchButton.getAttribute("id");
                    String className = searchButton.getAttribute("class");
                    String name = searchButton.getAttribute("name");
                    String text = searchButton.getText().trim();
                    String value = searchButton.getAttribute("value");
                    
                    log.info("조회 버튼 정보 - id={}, class={}, name={}, text={}, value={}, onclick={}", 
                        id, className, name, text, value, onclick);
                    
                    // onclick 속성이 있으면 그 함수를 직접 호출
                    if (onclick != null && !onclick.trim().isEmpty()) {
                        log.info("onclick 이벤트 발견: {}", onclick);
                        // onclick에서 함수명 추출 (예: "search()" 또는 "doSearch(); return false;")
                        String functionCall = onclick.trim();
                        // return false 같은 것 제거
                        if (functionCall.contains(";")) {
                            functionCall = functionCall.split(";")[0].trim();
                        }
                        // 괄호가 없으면 추가
                        if (!functionCall.endsWith("()") && !functionCall.contains("(")) {
                            functionCall = functionCall + "()";
                        }
                        log.info("onclick 함수 직접 호출: {}", functionCall);
                        ((JavascriptExecutor) driver).executeScript(functionCall);
                        log.info("onclick 함수 호출 완료");
                    } else {
                        // onclick이 없으면 이벤트 리스너 확인
                        log.info("onclick 속성이 없음. 이벤트 리스너 확인 중...");
                        
                        // JavaScript로 이벤트 리스너 확인 및 함수 추출
                        String eventInfo = (String) ((JavascriptExecutor) driver).executeScript(
                            "var btn = arguments[0]; " +
                            "var info = { " +
                            "  onclick: btn.onclick ? btn.onclick.toString() : null, " +
                            "  id: btn.id, " +
                            "  className: btn.className, " +
                            "  tagName: btn.tagName, " +
                            "  text: btn.textContent || btn.value, " +
                            "  listeners: [] " +
                            "}; " +
                            "try { " +
                            "  var listeners = getEventListeners ? getEventListeners(btn) : {}; " +
                            "  for(var event in listeners) { " +
                            "    listeners[event].forEach(function(l) { " +
                            "      info.listeners.push({event: event, listener: l.listener.toString()}); " +
                            "    }); " +
                            "  } " +
                            "} catch(e) { " +
                            "  info.listenersError = e.toString(); " +
                            "} " +
                            "return JSON.stringify(info);",
                            searchButton
                        );
                        log.info("이벤트 정보: {}", eventInfo);
                        
                        // 일반 클릭 시도
                        log.info("일반 클릭 시도 중...");
                        try {
                            searchButton.click();
                            log.info("일반 클릭 성공");
                        } catch (Exception e) {
                            log.warn("일반 클릭 실패, JavaScript 클릭 시도: {}", e.getMessage());
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", searchButton);
                        }
                    }
                } catch (Exception e) {
                    log.warn("이벤트 확인 중 오류 발생, 일반 클릭 시도: {}", e.getMessage());
                    try {
                        searchButton.click();
                    } catch (Exception e2) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", searchButton);
                    }
                }
                
                log.info("조회 버튼 클릭 완료, 데이터 로딩 대기 중...");
                
                // 테이블 데이터가 로드될 때까지 명시적으로 대기 (최소 대기)
                try {
                    // 데이터 행이 나타날 때까지 대기 (td 태그가 여러 개 있는 행)
                    WebDriverWait dataWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                    dataWait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//table//tbody//tr[count(td) > 2]")
                    ));
                    log.info("데이터 행 로드 완료 확인");
                } catch (Exception e) {
                    log.warn("데이터 행 로드 대기 중 타임아웃, 최소 대기: {}", e.getMessage());
                    Thread.sleep(2000);
                }
            } else {
                log.warn("조회 버튼을 찾을 수 없습니다. 페이지 소스 확인 중...");
                // 페이지 소스 일부 확인
                String pageSource = driver.getPageSource();
                if (pageSource.contains("조회")) {
                    log.info("페이지에 '조회' 텍스트가 있습니다. 다른 방법으로 시도합니다.");
                    
                    // 모든 버튼/링크의 텍스트와 속성 로깅
                    try {
                        List<WebElement> allButtons = driver.findElements(By.xpath("//button | //input | //a"));
                        log.info("페이지에서 발견된 버튼/입력 요소 개수: {}", allButtons.size());
                        for (int i = 0; i < Math.min(allButtons.size(), 10); i++) {
                            try {
                                WebElement elem = allButtons.get(i);
                                String text = elem.getText().trim();
                                String tag = elem.getTagName();
                                String type = elem.getAttribute("type");
                                String onclick = elem.getAttribute("onclick");
                                log.info("요소 {}: tag={}, type={}, text={}, onclick={}", i, tag, type, text, onclick);
                            } catch (Exception e) {
                                continue;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("요소 검색 중 오류: {}", e.getMessage());
                    }
                    
                    // 페이지 소스에서 조회 관련 함수 찾기
                    try {
                        String pageSourceForSearch = driver.getPageSource();
                        log.info("페이지 소스에서 조회 함수 검색 중...");
                        
                        // 일반적인 조회 함수명 패턴 찾기
                        String[] searchFunctionPatterns = {
                            "function\\s+search\\s*\\(", "function\\s+doSearch\\s*\\(", 
                            "function\\s+fnSearch\\s*\\(", "function\\s+searchData\\s*\\(",
                            "search\\s*=\\s*function", "doSearch\\s*=\\s*function",
                            "onclick\\s*=\\s*[\"']([^\"']*search[^\"']*)[\"']"
                        };
                        
                        String foundFunction = null;
                        for (String pattern : searchFunctionPatterns) {
                            if (pageSourceForSearch.matches("(?s).*" + pattern + ".*")) {
                                log.info("조회 함수 패턴 발견: {}", pattern);
                                // 패턴에서 함수명 추출
                                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                                java.util.regex.Matcher m = p.matcher(pageSourceForSearch);
                                if (m.find()) {
                                    foundFunction = m.group(1) != null ? m.group(1) : pattern.split("\\s+")[1];
                                    log.info("추출된 함수명: {}", foundFunction);
                                    break;
                                }
                            }
                        }
                        
                        // JavaScript로 직접 조회 함수 호출 시도
                        String jsCode = 
                            "var result = {tried: [], found: false}; " +
                            "var functions = ['search', 'doSearch', 'fnSearch', 'searchData', 'fn_search', 'searchMenu', 'loadData']; " +
                            "for(var i=0; i<functions.length; i++) { " +
                            "  var fnName = functions[i]; " +
                            "  result.tried.push(fnName); " +
                            "  if(typeof window[fnName] == 'function') { " +
                            "    try { " +
                            "      window[fnName](); " +
                            "      result.found = fnName; " +
                            "      result.success = true; " +
                            "      break; " +
                            "    } catch(e) { " +
                            "      result.error = e.toString(); " +
                            "    } " +
                            "  } " +
                            "} " +
                            "if(!result.found) { " +
                            "  var buttons = document.querySelectorAll('button, input[type=\"button\"], input[type=\"submit\"], a'); " +
                            "  for(var i=0; i<buttons.length; i++) { " +
                            "    var btn = buttons[i]; " +
                            "    var text = (btn.textContent || btn.value || '').trim(); " +
                            "    if(text.includes('조회') || text.includes('검색')) { " +
                            "      result.tried.push('click: ' + text); " +
                            "      try { " +
                            "        btn.click(); " +
                            "        result.found = 'clicked: ' + text; " +
                            "        result.success = true; " +
                            "        break; " +
                            "      } catch(e) { " +
                            "        result.error = e.toString(); " +
                            "      } " +
                            "    } " +
                            "  } " +
                            "} " +
                            "return JSON.stringify(result);";
                        
                        String jsResult = (String) ((JavascriptExecutor) driver).executeScript(jsCode);
                        log.info("JavaScript 조회 시도 결과: {}", jsResult);
                        Thread.sleep(10000);
                        log.info("JavaScript로 조회 함수 호출 완료");
                    } catch (Exception e) {
                        log.warn("JavaScript 조회 함수 호출 실패: {}", e.getMessage(), e);
                    }
                } else {
                    log.error("페이지에 '조회' 텍스트가 없습니다.");
                }
            }
            
            // 6. 데이터 추출
            log.info("데이터 추출 시작...");
            
            // 현재 페이지 URL 확인
            String currentUrl = driver.getCurrentUrl();
            log.info("현재 페이지 URL: {}", currentUrl);
            
            // 페이지 소스 일부 확인 (디버깅용)
            String pageSource = driver.getPageSource();
            // 타입에 따라 적절한 키워드 확인
            boolean hasData = false;
            if ("daily".equals(saleType)) {
                hasData = pageSource.contains("일자") || 
                         pageSource.contains("매출액") || 
                         pageSource.contains("영수건수") ||
                         pageSource.contains("고객수");
                log.info("페이지에 일별 매출 데이터 포함 여부: {}", hasData);
            } else {
                hasData = pageSource.contains("메뉴명") || 
                         pageSource.contains("트리플") || 
                         pageSource.contains("김치치즈") ||
                         pageSource.contains("단가") ||
                         pageSource.contains("수량");
                log.info("페이지에 메뉴 데이터 포함 여부: {}", hasData);
            }
            
            if (!hasData) {
                log.warn("페이지에 {} 데이터가 없는 것으로 보입니다. 조회 버튼이 제대로 클릭되었는지 확인 필요", 
                    "daily".equals(saleType) ? "일별 매출" : "메뉴");
            }
            
            Map<String, Object> extractResult = extractTableData(driver, saleType);
            @SuppressWarnings("unchecked")
            List<Map<String, String>> data = (List<Map<String, String>>) extractResult.get("data");
            @SuppressWarnings("unchecked")
            List<String> headers = (List<String>) extractResult.get("headers");
            
            if (data.isEmpty()) {
                log.warn("추출된 데이터가 없습니다. 페이지 스크린샷 저장 중...");
                try {
                    // 스크린샷 저장
                    if (driver instanceof TakesScreenshot) {
                        TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
                        byte[] screenshot = screenshotDriver.getScreenshotAs(OutputType.BYTES);
                        
                        // 스크린샷 저장 디렉토리 생성
                        String screenshotDir = "/var/www/html/java-projects/screenshots";
                        Path dirPath = Paths.get(screenshotDir);
                        if (!Files.exists(dirPath)) {
                            Files.createDirectories(dirPath);
                        }
                        
                        // 파일명: screenshot_YYYYMMDD_HHMMSS.png
                        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                        String filename = String.format("screenshot_%s.png", timestamp);
                        Path filePath = dirPath.resolve(filename);
                        
                        Files.write(filePath, screenshot);
                        log.info("스크린샷 저장 완료: {}", filePath.toString());
                    }
                } catch (Exception e) {
                    log.warn("스크린샷 저장 실패: {}", e.getMessage(), e);
                }
            }
            
            result.put("success", true);
            result.put("data", data);
            result.put("headers", headers); // 헤더 정보 추가
            result.put("count", data.size());
            result.put("message", data.isEmpty() ? "크롤링 완료되었으나 데이터가 없습니다. 조회 조건을 확인해주세요." : "크롤링 성공");
            
            log.info("크롤링 완료: {} 개의 데이터 추출", data.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("크롤링 중 오류 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "크롤링 실패: " + e.getMessage());
            result.put("data", new ArrayList<>());
            return result;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.error("WebDriver 종료 중 오류: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 매장 목록 조회 (콤보박스 옵션)
     * @param saleType 매출 타입 ("menu" 또는 "daily")
     * @return 매장 목록 (value, text 쌍)
     */
    public Map<String, Object> getStoreList(String saleType) {
        WebDriver driver = null;
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("매장 목록 조회 시작 (타입: {})", saleType);
            
            // Chrome WebDriver 설정
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
            options.addArguments("--disable-software-rasterizer");
            options.addArguments("--disable-extensions");
            
            try {
                String chromiumPath = "/usr/bin/chromium-browser";
                if (new java.io.File(chromiumPath).exists()) {
                    options.setBinary(chromiumPath);
                }
            } catch (Exception e) {
                log.warn("Chromium 바이너리 경로 설정 실패: {}", e.getMessage());
            }
            
            // ChromeDriver 초기화
            String chromedriverPath = "/usr/bin/chromedriver";
            File chromedriverFile = new File(chromedriverPath);
            ChromeDriverService.Builder serviceBuilder = new ChromeDriverService.Builder();
            if (chromedriverFile.exists()) {
                serviceBuilder.usingDriverExecutable(chromedriverFile);
            }
            ChromeDriverService service = serviceBuilder.build();
            driver = new ChromeDriver(service, options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            // 로그인 처리
            driver.get(BASE_URL);
            Thread.sleep(500);
            
            WebElement usernameField = findElement(driver, 
                "input[name='userId']", "input[name='id']", "input[type='text']", "#userId", "#id");
            WebElement passwordField = findElement(driver,
                "input[name='password']", "input[name='pwd']", "input[type='password']", "#password", "#pwd");
            
            if (usernameField != null && passwordField != null) {
                usernameField.clear();
                usernameField.sendKeys(USERNAME);
                passwordField.clear();
                passwordField.sendKeys(PASSWORD);
                Thread.sleep(200);
                
                WebElement loginButton = findElement(driver,
                    "button[type='submit']", "input[type='submit']", ".btn-login", "#loginBtn");
                if (loginButton != null) {
                    loginButton.click();
                } else {
                    passwordField.submit();
                }
                Thread.sleep(1000);
            }
            
            // 타겟 URL로 이동
            String targetUrl = "daily".equals(saleType) ? DAILY_SALES_URL : MENU_SALES_URL;
            driver.get(targetUrl);
            Thread.sleep(1000);
            
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search")));
            } catch (Exception e) {
                Thread.sleep(1000);
            }
            
            // 매장 선택 콤보박스 찾기 (연도/월이 아닌 콤보박스 중 두 번째 것)
            List<WebElement> allSelects = driver.findElements(By.tagName("select"));
            WebElement storeSelect = null;
            
            // 연도/월이 아닌 콤보박스들 필터링
            List<WebElement> candidateSelects = new ArrayList<>();
            log.info("전체 콤보박스 개수: {}", allSelects.size());
            
            for (int i = 0; i < allSelects.size(); i++) {
                WebElement select = allSelects.get(i);
                String selectId = select.getAttribute("id");
                String selectName = select.getAttribute("name");
                // 연도/월이 아닌 것 찾기
                boolean isYearMonth = (selectId != null && (selectId.toLowerCase().contains("year") || selectId.toLowerCase().contains("month"))) ||
                                    (selectName != null && (selectName.toLowerCase().contains("year") || selectName.toLowerCase().contains("month")));
                
                if (!isYearMonth) {
                    candidateSelects.add(select);
                    // 첫 번째 옵션의 텍스트 확인
                    try {
                        List<WebElement> selectOpts = select.findElements(By.tagName("option"));
                        String firstOptionText = selectOpts.size() > 0 ? selectOpts.get(0).getText() : "";
                        log.info("후보 콤보박스 #{}: id={}, name={}, 첫 옵션='{}'", candidateSelects.size(), selectId, selectName, firstOptionText);
                    } catch (Exception e) {
                        log.info("후보 콤보박스 #{}: id={}, name={}", candidateSelects.size(), selectId, selectName);
                    }
                } else {
                    log.info("연도/월 콤보박스 제외: id={}, name={}", selectId, selectName);
                }
            }
            
            log.info("연도/월 제외 후 후보 콤보박스 개수: {}", candidateSelects.size());
            
            // 두 번째 콤보박스 선택 (인덱스 1)
            if (candidateSelects.size() >= 2) {
                storeSelect = candidateSelects.get(1);
                String selectId = storeSelect.getAttribute("id");
                String selectName = storeSelect.getAttribute("name");
                log.info("매장 선택 콤보박스 (두 번째, 인덱스 1): id={}, name={}", selectId, selectName);
                
                // 선택된 콤보박스의 옵션 샘플 출력
                try {
                    List<WebElement> storeOpts = storeSelect.findElements(By.tagName("option"));
                    int sampleCount = Math.min(5, storeOpts.size());
                    log.info("선택된 콤보박스의 옵션 샘플 (처음 {}개):", sampleCount);
                    for (int i = 0; i < sampleCount; i++) {
                        String optText = storeOpts.get(i).getText();
                        String optValue = storeOpts.get(i).getAttribute("value");
                        log.info("  [{}] value='{}' text='{}'", i, optValue, optText);
                    }
                } catch (Exception e) {
                    // 무시
                }
            } else if (candidateSelects.size() == 1) {
                // 하나만 있으면 경고하고 그것 사용
                storeSelect = candidateSelects.get(0);
                String selectId = storeSelect.getAttribute("id");
                String selectName = storeSelect.getAttribute("name");
                log.warn("매장 선택 콤보박스가 하나만 발견됨 (유일): id={}, name={}", selectId, selectName);
            } else {
                log.warn("매장 선택 콤보박스를 찾을 수 없습니다. (연도/월 제외 후 {}개 발견)", candidateSelects.size());
            }
            
            if (storeSelect != null) {
                List<WebElement> storeOptions = storeSelect.findElements(By.tagName("option"));
                List<Map<String, String>> storeList = new ArrayList<>();
                
                for (WebElement option : storeOptions) {
                    try {
                        String value = option.getAttribute("value");
                        String text = option.getText();
                        if (value != null && !value.isEmpty() && text != null && !text.isEmpty()) {
                            Map<String, String> store = new HashMap<>();
                            store.put("value", value);
                            store.put("text", text);
                            storeList.add(store);
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
                
                result.put("success", true);
                result.put("stores", storeList);
                log.info("매장 목록 조회 완료: {}개", storeList.size());
            } else {
                result.put("success", false);
                result.put("message", "매장 선택 콤보박스를 찾을 수 없습니다.");
                result.put("stores", new ArrayList<>());
            }
            
        } catch (Exception e) {
            log.error("매장 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "매장 목록 조회 실패: " + e.getMessage());
            result.put("stores", new ArrayList<>());
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.error("WebDriver 종료 중 오류: {}", e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    /**
     * 다양한 선택자로 요소 찾기
     */
    private WebElement findElement(WebDriver driver, String... selectors) {
        for (String selector : selectors) {
            try {
                if (selector.startsWith("//")) {
                    return driver.findElement(By.xpath(selector));
                } else {
                    return driver.findElement(By.cssSelector(selector));
                }
            } catch (Exception e) {
                // 다음 선택자 시도
                continue;
            }
        }
        return null;
    }
    
    /**
     * 테이블 데이터 추출 (데이터와 헤더를 함께 반환)
     * @param driver WebDriver
     * @param saleType 매출 타입 ("menu" 또는 "daily")
     */
    private Map<String, Object> extractTableData(WebDriver driver, String saleType) {
        List<Map<String, String>> dataList = new ArrayList<>();
        List<String> headers = null; // 나중에 초기화
        
            try {
            log.info("테이블 데이터 추출 시작...");
            
            // 페이지가 완전히 로드될 때까지 최소 대기
            Thread.sleep(500);
            
            // 테이블 찾기 (여러 방법 시도)
            WebElement table = null;
            
            // 방법 0: tbody에 데이터 행이 많은 테이블 우선 찾기 (가장 확실한 방법)
            try {
                List<WebElement> tbodies = driver.findElements(By.tagName("tbody"));
                log.info("페이지에서 {}개의 tbody 발견", tbodies.size());
                
                for (WebElement tbody : tbodies) {
                    try {
                        List<WebElement> dataRows = tbody.findElements(By.xpath(".//tr[count(td) > 0]"));
                        if (dataRows.size() > 5) { // 충분한 데이터 행이 있는 tbody
                            table = tbody.findElement(By.xpath("./ancestor::table"));
                            log.info("tbody를 통해 데이터 테이블 발견: {}개의 데이터 행", dataRows.size());
                            break;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            } catch (Exception e) {
                log.warn("tbody 찾기 중 오류: {}", e.getMessage());
            }
            
            // 방법 1: 타입에 따라 적절한 헤더가 있는 테이블 찾기 (조회 후 나타나는 테이블)
            if (table == null) {
                List<WebElement> tables = driver.findElements(By.tagName("table"));
                log.info("페이지에서 {}개의 테이블 발견", tables.size());
                
                // 타입에 따라 적절한 키워드로 테이블 찾기
                String[] keywords;
                if ("daily".equals(saleType)) {
                    keywords = new String[]{"일자", "매출액", "영수건수", "고객수"};
                } else {
                    keywords = new String[]{"메뉴명", "단가", "수량", "매출액"};
                }
                
                for (WebElement t : tables) {
                    try {
                        String tableText = t.getText();
                        // 키워드가 모두 포함된 테이블 찾기
                        boolean matches = true;
                        for (String keyword : keywords) {
                            if (!tableText.contains(keyword)) {
                                matches = false;
                                break;
                            }
                        }
                        
                        if (matches) {
                            // tbody에 데이터 행이 있는지 확인
                            List<WebElement> tbodies = t.findElements(By.tagName("tbody"));
                            int dataRowCount = 0;
                            for (WebElement tbody : tbodies) {
                                dataRowCount += tbody.findElements(By.xpath(".//tr[count(td) > 0]")).size();
                            }
                            
                                if (dataRowCount > 5 || (tbodies.isEmpty() && t.findElements(By.tagName("tr")).size() > 2)) {
                                    table = t;
                                    log.info("{} 매출 데이터 테이블 발견: {}개의 데이터 행", 
                                        "daily".equals(saleType) ? "일별" : "메뉴별", dataRowCount);
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
                
                // 방법 1-2: 타입에 따라 적절한 헤더로 직접 찾기 (tbody에 데이터가 있는 테이블만)
                if (table == null) {
                    try {
                        String headerKeyword = "daily".equals(saleType) ? "일자" : "메뉴명";
                        WebElement headerElement = driver.findElement(By.xpath(
                            String.format("//th[contains(text(), '%s')] | //td[contains(text(), '%s')]", headerKeyword, headerKeyword)));
                        WebElement candidateTable = headerElement.findElement(By.xpath("./ancestor::table"));
                    
                    // 이 테이블의 tbody에 데이터 행이 있는지 확인
                    List<WebElement> tbodies = candidateTable.findElements(By.tagName("tbody"));
                    int dataRowCount = 0;
                    for (WebElement tbody : tbodies) {
                        dataRowCount += tbody.findElements(By.xpath(".//tr[count(td) > 0]")).size();
                    }
                    
                        if (dataRowCount > 5) {
                            table = candidateTable;
                            log.info("{} 헤더를 통해 테이블 발견: {}개의 데이터 행", 
                                "daily".equals(saleType) ? "일자" : "메뉴명", dataRowCount);
                        } else {
                            log.warn("{} 헤더 테이블에 데이터 행이 부족함: {}개", 
                                "daily".equals(saleType) ? "일자" : "메뉴명", dataRowCount);
                        }
                    } catch (Exception e) {
                        log.warn("{} 헤더를 통한 테이블 찾기 실패: {}", 
                            "daily".equals(saleType) ? "일자" : "메뉴명", e.getMessage());
                    }
                }
            
            // 방법 1-3: 가장 큰 테이블 찾기 (데이터가 많은 테이블)
            if (table == null) {
                List<WebElement> tables = driver.findElements(By.tagName("table"));
                for (WebElement t : tables) {
                    try {
                        List<WebElement> rows = t.findElements(By.tagName("tr"));
                        if (rows.size() > 5) { // 충분한 데이터 행이 있는 테이블
                            table = t;
                            log.info("큰 데이터 테이블 발견: {} 행", rows.size());
                            break;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            
            // 방법 2: 특정 클래스나 ID로 찾기
            if (table == null) {
                table = findElement(driver,
                    "table.data-table",
                    "table.table",
                    "#dataTable",
                    "table.dataTable",
                    "table.tbl",
                    "table.list"
                );
            }
            
            if (table == null) {
                log.warn("테이블을 찾을 수 없습니다. 페이지 소스 확인 중...");
                // 페이지 소스에서 테이블 관련 정보 확인
                String pageSource = driver.getPageSource();
                if (pageSource.contains("메뉴명") || pageSource.contains("단가") || pageSource.contains("수량")) {
                    log.info("페이지에 테이블 관련 텍스트가 있습니다. 다른 방법으로 시도합니다.");
                    // tbody나 div로 감싸진 테이블 데이터 찾기
                    try {
                        WebElement tbody = driver.findElement(By.tagName("tbody"));
                        if (tbody != null) {
                            table = tbody.findElement(By.xpath("./ancestor::table"));
                        }
                    } catch (Exception e) {
                        log.warn("tbody를 통한 테이블 찾기 실패: {}", e.getMessage());
                    }
                }
            }
            
            if (table == null) {
                log.error("테이블을 찾을 수 없습니다. 페이지 소스 일부 확인 중...");
                // 페이지 소스에서 테이블 관련 정보 확인
                String pageSource = driver.getPageSource();
                log.info("페이지 소스 길이: {} bytes", pageSource.length());
                
                // 메뉴명이 포함된 부분 찾기
                int menuNameIndex = pageSource.indexOf("메뉴명");
                if (menuNameIndex > 0) {
                    String snippet = pageSource.substring(Math.max(0, menuNameIndex - 200), Math.min(pageSource.length(), menuNameIndex + 500));
                    log.info("메뉴명 주변 텍스트: {}", snippet);
                }
                
                // tbody 태그로 다시 시도
                try {
                    List<WebElement> tbodies = driver.findElements(By.tagName("tbody"));
                    log.info("tbody 태그 {}개 발견", tbodies.size());
                    for (WebElement tbody : tbodies) {
                        try {
                            List<WebElement> rows = tbody.findElements(By.tagName("tr"));
                            if (rows.size() > 2) {
                                table = tbody.findElement(By.xpath("./ancestor::table"));
                                log.info("tbody를 통해 테이블 발견: {} 행", rows.size());
                                break;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                } catch (Exception e) {
                    log.warn("tbody 찾기 실패: {}", e.getMessage());
                }
                
                if (table == null) {
                    log.error("모든 방법으로 테이블을 찾을 수 없습니다.");
                    Map<String, Object> emptyResult = new HashMap<>();
                    emptyResult.put("data", dataList);
                    emptyResult.put("headers", new ArrayList<>());
                    return emptyResult;
                }
            }
            
            log.info("테이블 찾기 완료, 데이터 추출 시작...");
            
            // 헤더 추출
            headers = new ArrayList<>();
            List<WebElement> headerRows = new ArrayList<>();
            try {
                // thead에서 헤더 찾기
                headerRows = table.findElements(By.tagName("thead"));
                if (!headerRows.isEmpty()) {
                    List<WebElement> headerCells = headerRows.get(0).findElements(By.tagName("th"));
                    if (headerCells.isEmpty()) {
                        headerCells = headerRows.get(0).findElements(By.tagName("td"));
                    }
                    for (WebElement cell : headerCells) {
                        String headerText = cell.getText().trim();
                        if (!headerText.isEmpty()) {
                            headers.add(headerText);
                        }
                    }
                }
                
                // thead에 헤더가 없으면 첫 번째 행에서 찾기
                if (headers.isEmpty()) {
                    List<WebElement> rows = table.findElements(By.tagName("tr"));
                    if (!rows.isEmpty()) {
                        // 첫 번째 행이 헤더인지 확인 (th가 있거나, "메뉴명" 같은 키워드가 있으면)
                        WebElement firstRow = rows.get(0);
                        String firstRowText = firstRow.getText().trim();
                        List<WebElement> firstRowCells = firstRow.findElements(By.tagName("th"));
                        if (firstRowCells.isEmpty()) {
                            firstRowCells = firstRow.findElements(By.tagName("td"));
                        }
                        
                        // 타입에 따라 적절한 키워드가 포함되어 있으면 헤더 행으로 판단
                        String headerKeyword = "daily".equals(saleType) ? "일자" : "메뉴명";
                        if (firstRowText.contains(headerKeyword) || !firstRow.findElements(By.tagName("th")).isEmpty()) {
                            for (WebElement cell : firstRowCells) {
                                String headerText = cell.getText().trim();
                                if (!headerText.isEmpty()) {
                                    headers.add(headerText);
                                }
                            }
                        }
                    }
                }
                
                // 헤더가 여전히 비어있으면 타입에 따라 기본 헤더 사용
                if (headers.isEmpty()) {
                    log.warn("헤더를 추출할 수 없어 기본 헤더 사용");
                    if ("daily".equals(saleType)) {
                        // 일별 매출 기본 헤더 (실제 페이지 구조에 맞춤)
                        headers = new ArrayList<>(List.of("일자", "매출액(원)", "영수건수", "고객수"));
                    } else {
                        // 메뉴별 매출 기본 헤더
                        headers = new ArrayList<>(List.of("메뉴명", "단가", "수량", "매출액(원)", "구성비"));
                    }
                }
                
                log.info("헤더 추출 완료: {} ({}개)", headers, headers.size());
            } catch (Exception e) {
                log.warn("헤더 추출 실패: {}", e.getMessage(), e);
                // 타입에 따라 기본 헤더 사용
                if ("daily".equals(saleType)) {
                    // 일별 매출 기본 헤더 (실제 페이지 구조에 맞춤)
                    headers = new ArrayList<>(List.of("일자", "매출액(원)", "영수건수", "고객수"));
                } else {
                    headers = new ArrayList<>(List.of("메뉴명", "단가", "수량", "매출액(원)", "구성비"));
                }
            }
            
            // 데이터 행 추출
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            log.info("총 {}개의 행 발견", rows.size());
            
            // 각 행의 정보 로깅 (디버깅용)
            for (int idx = 0; idx < Math.min(rows.size(), 10); idx++) {
                try {
                    WebElement row = rows.get(idx);
                    List<WebElement> rowCells = row.findElements(By.tagName("td"));
                    List<WebElement> rowHeaderCells = row.findElements(By.tagName("th"));
                    String rowText = row.getText().trim();
                    log.info("행 {}: th={}개, td={}개, 텍스트={}", idx, rowHeaderCells.size(), rowCells.size(), 
                        rowText.length() > 100 ? rowText.substring(0, 100) + "..." : rowText);
                } catch (Exception e) {
                    log.warn("행 {} 정보 확인 중 오류: {}", idx, e.getMessage());
                }
            }
            
            // 헤더 행 개수 동적 계산
            int headerRowCount = 0;
            if (!headerRows.isEmpty()) {
                headerRowCount = 1; // thead가 있으면 1개
            } else {
                // 첫 번째 행이 th만 있으면 헤더 행
                if (!rows.isEmpty()) {
                    List<WebElement> firstRowCells = rows.get(0).findElements(By.tagName("th"));
                    if (!firstRowCells.isEmpty()) {
                        headerRowCount = 1;
                    }
                }
            }
            log.info("헤더 행 개수: {}, 데이터 행 시작 인덱스: {}", headerRowCount, headerRowCount);
            
            int dataRowCount = 0;
            
            for (int i = headerRowCount; i < rows.size(); i++) {
                try {
                    WebElement row = rows.get(i);
                    List<WebElement> cells = row.findElements(By.tagName("td"));
                    
                    // td가 없으면 건너뜀 (헤더 행일 수 있음)
                    if (cells.isEmpty()) {
                        log.info("행 {}: td가 없음 (th만 있거나 빈 행), 건너뜀", i);
                        continue;
                    }
                    
                    log.info("행 {}: td {}개 발견", i, cells.size());
                    
                    String rowText = row.getText().trim();
                    log.info("행 {} 텍스트: {}", i, rowText.length() > 200 ? rowText.substring(0, 200) + "..." : rowText);
                    
                    // 합계 행은 제외 (텍스트에 "합계"가 포함된 경우)
                    if (rowText.contains("합계") || rowText.contains("TOTAL") || rowText.isEmpty()) {
                        log.info("합계 행 또는 빈 행 건너뜀: {}", rowText.length() > 50 ? rowText.substring(0, 50) : rowText);
                        continue;
                    }
                    
                    // 헤더 행은 제외 (타입에 따라 적절한 헤더 키워드가 모두 포함된 경우)
                    boolean isHeaderRow = false;
                    if ("daily".equals(saleType)) {
                        isHeaderRow = rowText.contains("일자") && 
                                     (rowText.contains("매출액") || rowText.contains("영수건수") || rowText.contains("고객수"));
                    } else {
                        isHeaderRow = rowText.contains("메뉴명") && 
                                     (rowText.contains("단가") || rowText.contains("수량") || rowText.contains("매출액"));
                    }
                    if (isHeaderRow) {
                        log.info("헤더 행으로 판단, 건너뜀: {}", rowText.length() > 50 ? rowText.substring(0, 50) : rowText);
                        continue;
                    }
                    
                    // 데이터 행 추출 (헤더가 아니고 합계가 아닌 모든 행)
                    // LinkedHashMap을 사용하여 헤더 순서 보장
                    Map<String, String> rowData = new LinkedHashMap<>();
                    int maxCols = Math.min(headers.size(), cells.size());
                    for (int j = 0; j < maxCols; j++) {
                        String cellText = cells.get(j).getText().trim();
                        String header = j < headers.size() ? headers.get(j) : "컬럼" + (j + 1);
                        rowData.put(header, cellText);
                    }
                    
                    // 행에 실제 데이터가 있는지 확인 (모든 셀이 비어있지 않은지)
                    boolean hasData = rowData.values().stream().anyMatch(v -> v != null && !v.isEmpty());
                    if (hasData) {
                        dataList.add(rowData);
                        dataRowCount++;
                        log.info("데이터 행 {} 추출: {}", dataRowCount, rowData);
                    } else {
                        log.info("행 {}: 데이터가 없음, 건너뜀", i);
                    }
                } catch (Exception e) {
                    log.warn("행 {} 추출 중 오류: {}", i, e.getMessage());
                }
            }
            
            log.info("데이터 추출 완료: {}개의 데이터 행 추출", dataRowCount);
            
        } catch (Exception e) {
            log.error("테이블 데이터 추출 중 오류: {}", e.getMessage(), e);
        }
        
        // headers가 null이면 빈 리스트로 초기화
        if (headers == null) {
            headers = new ArrayList<>();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", dataList);
        result.put("headers", headers);
        return result;
    }
}

