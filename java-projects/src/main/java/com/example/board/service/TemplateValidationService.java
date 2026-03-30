package com.example.board.service;

import com.example.board.entity.PzLevel;
import com.example.board.repository.PzLevelRepository;
import com.example.board.repository.PzGridTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 템플릿 검증 서비스
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateValidationService {

    private final PzLevelRepository pzLevelRepository;
    private final PzGridTemplateRepository pzGridTemplateRepository;
    private final ObjectMapper objectMapper;

    /**
     * 템플릿 생성 시 검증
     * Laravel의 store 메서드 검증 로직과 동일
     */
    public Map<String, Object> validateTemplateCreation(Map<String, Object> templateData) {
        log.info("템플릿 생성 검증 시작");
        
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // 1. 필수 필드 검증
            validateRequiredFields(templateData, errors);
            if (!errors.isEmpty()) {
                result.put("valid", false);
                result.put("errors", errors);
                result.put("warnings", warnings);
                return result;
            }
            
            // 2. 레벨 정보 조회 및 검증
            Integer levelId = (Integer) templateData.get("level_id");
            PzLevel level = pzLevelRepository.findById(levelId.longValue()).orElse(null);
            if (level == null) {
                errors.add("레벨을 찾을 수 없습니다.");
                result.put("valid", false);
                result.put("errors", errors);
                result.put("warnings", warnings);
                return result;
            }
            
            // 3. 그리드 패턴 검증
            @SuppressWarnings("unchecked")
            List<List<Integer>> gridPattern = parseGridPattern((String) templateData.get("grid_pattern"));
            Map<String, Object> gridValidation = validateGridPattern(gridPattern);
            
            if (!(Boolean) gridValidation.get("valid")) {
                @SuppressWarnings("unchecked")
                List<String> gridErrors = (List<String>) gridValidation.get("errors");
                errors.addAll(gridErrors);
            }
            
            // 4. 레벨 조건 검증
            validateLevelConditions(templateData, level, errors);
            
            // 5. 중복 템플릿 검증
            validateDuplicateTemplate(templateData, levelId, errors);
            
            // 6. 경고사항 체크
            checkWarnings(templateData, level, warnings);
            
            result.put("valid", errors.isEmpty());
            result.put("errors", errors);
            result.put("warnings", warnings);
            
            log.info("템플릿 검증 완료 - 유효: {}, 오류: {}, 경고: {}", errors.isEmpty(), errors.size(), warnings.size());
            
        } catch (Exception e) {
            log.error("템플릿 검증 중 오류 발생", e);
            errors.add("템플릿 검증 중 오류가 발생했습니다: " + e.getMessage());
            result.put("valid", false);
            result.put("errors", errors);
            result.put("warnings", warnings);
        }
        
        return result;
    }

    /**
     * 필수 필드 검증
     */
    private void validateRequiredFields(Map<String, Object> templateData, List<String> errors) {
        if (templateData.get("level_id") == null) {
            errors.add("레벨을 선택해주세요.");
        }
        
        if (templateData.get("grid_pattern") == null || 
            ((String) templateData.get("grid_pattern")).trim().isEmpty()) {
            errors.add("그리드 패턴을 입력해주세요.");
        }
        
        if (templateData.get("word_positions") == null || 
            ((String) templateData.get("word_positions")).trim().isEmpty()) {
            errors.add("단어 위치 정보를 입력해주세요.");
        }
    }

    /**
     * 그리드 패턴 파싱
     */
    @SuppressWarnings("unchecked")
    private List<List<Integer>> parseGridPattern(String gridPattern) {
        try {
            if (gridPattern == null || gridPattern.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(gridPattern, new TypeReference<List<List<Integer>>>() {});
        } catch (Exception e) {
            log.error("그리드 패턴 파싱 오류", e);
            return new ArrayList<>();
        }
    }

    /**
     * 그리드 패턴 검증
     */
    private Map<String, Object> validateGridPattern(List<List<Integer>> gridPattern) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        if (gridPattern == null || gridPattern.isEmpty()) {
            errors.add("그리드 패턴이 비어있습니다.");
            result.put("valid", false);
            result.put("errors", errors);
            return result;
        }
        
        int gridSize = gridPattern.size();
        
        // 정사각형 검증
        for (int i = 0; i < gridSize; i++) {
            if (gridPattern.get(i).size() != gridSize) {
                errors.add("그리드는 정사각형이어야 합니다.");
                break;
            }
        }
        
        // 최소 크기 검증
        if (gridSize < 3) {
            errors.add("그리드 크기는 최소 3x3이어야 합니다.");
        }
        
        // 최대 크기 검증
        if (gridSize > 15) {
            errors.add("그리드 크기는 최대 15x15까지 가능합니다.");
        }
        
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        return result;
    }

    /**
     * 레벨 조건 검증
     */
    private void validateLevelConditions(Map<String, Object> templateData, PzLevel level, List<String> errors) {
        Integer wordCount = (Integer) templateData.get("word_count");
        Integer intersectionCount = (Integer) templateData.get("intersection_count");
        
        if (wordCount != null && level.getWordCount() != null) {
            if (wordCount < level.getWordCount()) {
                errors.add("단어 수가 레벨 조건(" + level.getWordCount() + "개)보다 적습니다.");
            }
        }
        
        if (intersectionCount != null && level.getIntersectionCount() != null) {
            if (intersectionCount < level.getIntersectionCount()) {
                errors.add("교차점 수가 레벨 조건(" + level.getIntersectionCount() + "개)보다 적습니다.");
            }
        }
    }

    /**
     * 중복 템플릿 검증
     */
    private void validateDuplicateTemplate(Map<String, Object> templateData, Integer levelId, List<String> errors) {
        String templateName = (String) templateData.get("template_name");
        
        if (templateName != null && !templateName.trim().isEmpty()) {
            // 같은 레벨에서 동일한 이름의 템플릿이 있는지 확인
            List<com.example.board.entity.PzGridTemplate> existingTemplates = 
                pzGridTemplateRepository.findByLevelIdOrderByCreatedAtDesc(levelId);
            
            for (com.example.board.entity.PzGridTemplate template : existingTemplates) {
                if (templateName.equals(template.getTemplateName())) {
                    errors.add("이미 존재하는 템플릿 이름입니다: " + templateName);
                    break;
                }
            }
        }
    }

    /**
     * 경고사항 체크
     */
    private void checkWarnings(Map<String, Object> templateData, PzLevel level, List<String> warnings) {
        Integer wordCount = (Integer) templateData.get("word_count");
        Integer intersectionCount = (Integer) templateData.get("intersection_count");
        
        if (wordCount != null && level.getWordCount() != null) {
            if (wordCount > level.getWordCount() * 1.5) {
                warnings.add("단어 수가 레벨 조건보다 많이 설정되었습니다.");
            }
        }
        
        if (intersectionCount != null && level.getIntersectionCount() != null) {
            if (intersectionCount > level.getIntersectionCount() * 2) {
                warnings.add("교차점 수가 레벨 조건보다 많이 설정되었습니다.");
            }
        }
    }
}