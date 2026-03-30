package com.example.crossword.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 프롬프트 관리 서비스
 * 사용자가 재미나이 API 프롬프트를 직접 수정할 수 있도록 지원
 */
@Service
@Slf4j
public class PromptManagementService {
    
    private static final String PROMPT_FILE_PATH = "prompts/word-generation-prompt.txt";
    private static final String PROMPT_BACKUP_DIR = "/var/www/html/crossword-projects/prompt-backups/";
    
    /**
     * 현재 프롬프트 내용 조회
     * @return 프롬프트 내용
     */
    public String getCurrentPrompt() {
        try {
            Resource resource = new ClassPathResource(PROMPT_FILE_PATH);
            if (resource.exists()) {
                return Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
            } else {
                log.warn("프롬프트 파일을 찾을 수 없습니다: {}", PROMPT_FILE_PATH);
                return getDefaultPrompt();
            }
        } catch (Exception e) {
            log.error("프롬프트 파일 읽기 중 오류 발생", e);
            return getDefaultPrompt();
        }
    }
    
    /**
     * 프롬프트 내용 업데이트
     * @param newPrompt 새로운 프롬프트 내용
     * @return 업데이트 결과
     */
    public Map<String, Object> updatePrompt(String newPrompt) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 백업 생성
            createBackup();
            
            // 새 프롬프트 저장
            Resource resource = new ClassPathResource(PROMPT_FILE_PATH);
            Path promptPath = Paths.get(resource.getURI());
            
            Files.write(promptPath, newPrompt.getBytes(StandardCharsets.UTF_8));
            
            result.put("success", true);
            result.put("message", "프롬프트가 성공적으로 업데이트되었습니다.");
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("프롬프트 업데이트 완료");
            
        } catch (Exception e) {
            log.error("프롬프트 업데이트 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "프롬프트 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 프롬프트 백업 생성
     */
    private void createBackup() {
        try {
            // 백업 디렉토리 생성
            Path backupDir = Paths.get(PROMPT_BACKUP_DIR);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
            
            // 현재 프롬프트 읽기
            String currentPrompt = getCurrentPrompt();
            
            // 백업 파일명 생성 (타임스탬프 포함)
            String backupFileName = "word-generation-prompt-backup-" + System.currentTimeMillis() + ".txt";
            Path backupPath = backupDir.resolve(backupFileName);
            
            // 백업 파일 생성
            Files.write(backupPath, currentPrompt.getBytes(StandardCharsets.UTF_8));
            
            log.info("프롬프트 백업 생성 완료: {}", backupPath);
            
        } catch (Exception e) {
            log.error("프롬프트 백업 생성 중 오류 발생", e);
        }
    }
    
    /**
     * 백업 목록 조회
     * @return 백업 파일 목록
     */
    public Map<String, Object> getBackupList() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Path backupDir = Paths.get(PROMPT_BACKUP_DIR);
            if (!Files.exists(backupDir)) {
                result.put("success", true);
                result.put("backups", new String[0]);
                result.put("message", "백업 파일이 없습니다.");
                return result;
            }
            
            String[] backups = Files.list(backupDir)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .map(path -> path.getFileName().toString())
                    .sorted((a, b) -> b.compareTo(a)) // 최신순 정렬
                    .toArray(String[]::new);
            
            result.put("success", true);
            result.put("backups", backups);
            result.put("count", backups.length);
            
        } catch (Exception e) {
            log.error("백업 목록 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "백업 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 백업에서 프롬프트 복원
     * @param backupFileName 백업 파일명
     * @return 복원 결과
     */
    public Map<String, Object> restoreFromBackup(String backupFileName) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Path backupPath = Paths.get(PROMPT_BACKUP_DIR, backupFileName);
            
            if (!Files.exists(backupPath)) {
                result.put("success", false);
                result.put("message", "백업 파일을 찾을 수 없습니다: " + backupFileName);
                return result;
            }
            
            // 백업 파일 읽기
            String backupContent = Files.readString(backupPath, StandardCharsets.UTF_8);
            
            // 현재 프롬프트 백업
            createBackup();
            
            // 백업 내용으로 복원
            Resource resource = new ClassPathResource(PROMPT_FILE_PATH);
            Path promptPath = Paths.get(resource.getURI());
            Files.write(promptPath, backupContent.getBytes(StandardCharsets.UTF_8));
            
            result.put("success", true);
            result.put("message", "백업에서 프롬프트가 성공적으로 복원되었습니다: " + backupFileName);
            
            log.info("백업에서 프롬프트 복원 완료: {}", backupFileName);
            
        } catch (Exception e) {
            log.error("백업에서 프롬프트 복원 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "백업에서 프롬프트 복원 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 기본 프롬프트 반환
     * @return 기본 프롬프트
     */
    private String getDefaultPrompt() {
        return """
            당신은 한글 십자낱말 퍼즐을 위한 단어를 만드는 전문가입니다.
            
            '{category}' 카테고리에서 십자낱말 퍼즐에 적합한 단어 {count}개를 생성해주세요.
            
            **단어 생성 규칙:**
            1. 한글 단어만 생성 (영어, 숫자, 특수문자 제외)
            2. 2글자 이상 10글자 이하의 단어
            3. 일반인이 알 수 있는 단어 (너무 전문적이거나 생소한 단어 제외)
            4. 십자낱말 퍼즐에 적합한 단어 (너무 추상적이지 않은 구체적인 단어)
            5. 중복되지 않는 다양한 단어
            
            **응답 형식:**
            단어1
            단어2
            단어3
            ...
            
            (다른 설명 없이 단어만 한 줄씩 나열해주세요)
            """;
    }
}
