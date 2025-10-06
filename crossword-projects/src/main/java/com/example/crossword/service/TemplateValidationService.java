package com.example.crossword.service;

import com.example.crossword.entity.PzGridTemplate;
import com.example.crossword.entity.PzLevel;
import com.example.crossword.repository.PzLevelRepository;
import com.example.crossword.repository.PzGridTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 템플릿 검증 서비스
 * 그리드 템플릿의 유효성을 검증하는 로직
 * Laravel의 GridTemplateController 검증 로직과 동일한 기능
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
    private final IntersectionCalculationService intersectionCalculationService;

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
            Map<String, Object> gridValidation = intersectionCalculationService.validateGridPattern(gridPattern);
            
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
            result.put("level", level);
            result.put("grid_analysis", gridValidation);
            
            log.info("템플릿 생성 검증 완료 - 유효: {}, 오류: {}개, 경고: {}개", 
                    errors.isEmpty(), errors.size(), warnings.size());
            
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
     * 템플릿 수정 시 검증
     */
    public Map<String, Object> validateTemplateUpdate(Long templateId, Map<String, Object> templateData) {
        log.info("템플릿 수정 검증 시작 - 템플릿 ID: {}", templateId);
        
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
            Map<String, Object> gridValidation = intersectionCalculationService.validateGridPattern(gridPattern);
            
            if (!(Boolean) gridValidation.get("valid")) {
                @SuppressWarnings("unchecked")
                List<String> gridErrors = (List<String>) gridValidation.get("errors");
                errors.addAll(gridErrors);
            }
            
            // 4. 레벨 조건 검증
            validateLevelConditions(templateData, level, errors);
            
            // 5. 경고사항 체크
            checkWarnings(templateData, level, warnings);
            
            result.put("valid", errors.isEmpty());
            result.put("errors", errors);
            result.put("warnings", warnings);
            result.put("level", level);
            result.put("grid_analysis", gridValidation);
            
            log.info("템플릿 수정 검증 완료 - 유효: {}, 오류: {}개, 경고: {}개", 
                    errors.isEmpty(), errors.size(), warnings.size());
            
        } catch (Exception e) {
            log.error("템플릿 수정 검증 중 오류 발생", e);
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
            errors.add("레벨 ID는 필수입니다.");
        }
        
        if (templateData.get("grid_size") == null) {
            errors.add("그리드 크기는 필수입니다.");
        } else {
            Integer gridSize = (Integer) templateData.get("grid_size");
            if (gridSize < 3 || gridSize > 20) {
                errors.add("그리드 크기는 3x3부터 20x20까지 가능합니다.");
            }
        }
        
        if (templateData.get("grid_pattern") == null) {
            errors.add("그리드 패턴은 필수입니다.");
        }
        
        if (templateData.get("word_positions") == null) {
            errors.add("단어 위치 정보는 필수입니다.");
        }
        
        if (templateData.get("word_count") == null) {
            errors.add("단어 수는 필수입니다.");
        } else {
            Integer wordCount = (Integer) templateData.get("word_count");
            if (wordCount < 1) {
                errors.add("단어 수는 1개 이상이어야 합니다.");
            }
        }
        
        if (templateData.get("intersection_count") == null) {
            errors.add("교차점 수는 필수입니다.");
        } else {
            Integer intersectionCount = (Integer) templateData.get("intersection_count");
            if (intersectionCount < 0) {
                errors.add("교차점 수는 0개 이상이어야 합니다.");
            }
        }
    }

    /**
     * 레벨 조건 검증
     * Laravel의 조건 검증 로직과 동일
     */
    private void validateLevelConditions(Map<String, Object> templateData, PzLevel level, List<String> errors) {
        Integer wordCount = (Integer) templateData.get("word_count");
        Integer intersectionCount = (Integer) templateData.get("intersection_count");
        
        // 단어 개수 검증
        if (!wordCount.equals(level.getWordCount())) {
            errors.add(String.format("단어 개수가 일치하지 않습니다. 레벨 %d은 %d개 단어가 필요합니다.", 
                    level.getLevel(), level.getWordCount()));
        }
        
        // 교차점 개수 검증 (최소값 기준)
        if (intersectionCount < level.getIntersectionCount()) {
            errors.add(String.format("교차점 개수가 부족합니다. 레벨 %d은 최소 %d개 교차점이 필요합니다. (현재: %d개)", 
                    level.getLevel(), level.getIntersectionCount(), intersectionCount));
        }
    }

    /**
     * 중복 템플릿 검증
     * Laravel의 중복 검사 로직과 동일
     */
    private void validateDuplicateTemplate(Map<String, Object> templateData, Integer levelId, List<String> errors) {
        try {
            // 동일 레벨의 기존 템플릿들 조회
            List<PzGridTemplate> existingTemplates = pzGridTemplateRepository.findByLevelIdOrderByCreatedAtDesc(levelId);
            
            List<List<Integer>> newGridPattern = parseGridPattern((String) templateData.get("grid_pattern"));
            
            for (PzGridTemplate existingTemplate : existingTemplates) {
                // 기존 템플릿의 그리드 패턴 파싱
                List<List<Integer>> existingGridPattern = parseGridPattern(existingTemplate.getGridPattern());
                
                // 그리드 크기가 다르면 건너뛰기
                if (existingGridPattern.size() != newGridPattern.size()) {
                    continue;
                }
                
                // 그리드 패턴 비교
                boolean isSame = true;
                for (int i = 0; i < existingGridPattern.size() && isSame; i++) {
                    for (int j = 0; j < existingGridPattern.get(i).size(); j++) {
                        if (!existingGridPattern.get(i).get(j).equals(newGridPattern.get(i).get(j))) {
                            isSame = false;
                            break;
                        }
                    }
                }
                
                if (isSame) {
                    errors.add(String.format("동일한 그리드 패턴의 템플릿이 이미 존재합니다.\n\n템플릿명: %s\n템플릿 ID: %d\n\n다른 그리드 패턴을 사용하거나 기존 템플릿을 수정해주세요.", 
                            existingTemplate.getTemplateName(), existingTemplate.getId()));
                    return;
                }
            }
            
            log.debug("중복 템플릿 검증 완료 - 레벨 ID: {}, 중복 없음", levelId);
        } catch (Exception e) {
            log.error("중복 템플릿 검증 중 오류 발생", e);
            errors.add("템플릿 중복 검증 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 그리드 패턴 JSON 문자열을 List<List<Integer>>로 파싱
     */
    @SuppressWarnings("unchecked")
    private List<List<Integer>> parseGridPattern(String gridPatternJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(gridPatternJson, List.class);
        } catch (Exception e) {
            log.error("그리드 패턴 파싱 오류", e);
            return new ArrayList<>();
        }
    }

    /**
     * 경고사항 체크
     */
    private void checkWarnings(Map<String, Object> templateData, PzLevel level, List<String> warnings) {
        Integer intersectionCount = (Integer) templateData.get("intersection_count");
        
        // 교차점이 없는 경우 경고
        if (intersectionCount == 0) {
            warnings.add("교차점이 없는 템플릿입니다. 교차점이 있는 패턴을 권장합니다.");
        }
        
        // 그리드 크기가 큰 경우 경고
        Integer gridSize = (Integer) templateData.get("grid_size");
        if (gridSize > 15) {
            warnings.add("그리드 크기가 큽니다. 성능에 영향을 줄 수 있습니다.");
        }
        
        // 단어 수가 많은 경우 경고
        Integer wordCount = (Integer) templateData.get("word_count");
        if (wordCount > 20) {
            warnings.add("단어 수가 많습니다. 게임 난이도가 높아질 수 있습니다.");
        }
    }

    /**
     * 그리드 패턴 실시간 검증
     * 프론트엔드에서 사용할 수 있는 실시간 검증
     */
    public Map<String, Object> validateGridPatternRealtime(List<List<Integer>> gridPattern) {
        log.debug("그리드 패턴 실시간 검증 시작");
        
        Map<String, Object> result = intersectionCalculationService.validateGridPattern(gridPattern);
        
        // 추가 검증 로직
        List<String> additionalErrors = new ArrayList<>();
        List<String> additionalWarnings = new ArrayList<>();
        
        if ((Boolean) result.get("valid")) {
            Integer wordCount = (Integer) result.get("wordCount");
            Integer intersectionCount = (Integer) result.get("intersectionCount");
            
            // 단어 수가 너무 적은 경우
            if (wordCount < 2) {
                additionalErrors.add("최소 2개 이상의 단어가 필요합니다.");
            }
            
            // 교차점이 없는 경우 경고
            if (intersectionCount == 0 && wordCount > 1) {
                additionalWarnings.add("단어들이 교차하지 않습니다. 교차점이 있는 패턴을 권장합니다.");
            }
            
            // 그리드가 너무 복잡한 경우 경고
            if (intersectionCount > 10) {
                additionalWarnings.add("교차점이 너무 많습니다. 게임 난이도가 높아질 수 있습니다.");
            }
        }
        
        // 기존 오류와 경고에 추가
        @SuppressWarnings("unchecked")
        List<String> existingErrors = (List<String>) result.get("errors");
        @SuppressWarnings("unchecked")
        List<String> existingWarnings = (List<String>) result.get("warnings");
        
        existingErrors.addAll(additionalErrors);
        existingWarnings.addAll(additionalWarnings);
        
        result.put("valid", existingErrors.isEmpty());
        result.put("errors", existingErrors);
        result.put("warnings", existingWarnings);
        
        return result;
    }

    /**
     * 레벨별 템플릿 조건 조회
     * 프론트엔드에서 레벨 선택 시 조건 정보 제공
     */
    public Map<String, Object> getLevelConditions(Integer levelId) {
        log.info("레벨 조건 조회 - 레벨 ID: {}", levelId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            PzLevel level = pzLevelRepository.findById(levelId.longValue()).orElse(null);
            if (level == null) {
                result.put("success", false);
                result.put("message", "레벨을 찾을 수 없습니다.");
                return result;
            }
            
            result.put("success", true);
            result.put("level", Map.of(
                    "id", level.getId(),
                    "level", level.getLevel(),
                    "level_name", level.getLevelName(),
                    "word_count", level.getWordCount(),
                    "intersection_count", level.getIntersectionCount(),
                    "word_difficulty", level.getWordDifficulty(),
                    "hint_difficulty", level.getHintDifficulty(),
                    "time_limit", level.getTimeLimit()
            ));
            
            // 권장 그리드 크기 계산
            Integer recommendedSize = calculateRecommendedGridSize(level);
            result.put("recommended_grid_size", recommendedSize);
            
            // 조건 설명
            result.put("conditions", Map.of(
                    "word_count_required", String.format("정확히 %d개 단어 필요", level.getWordCount()),
                    "intersection_count_minimum", String.format("최소 %d개 교차점 필요", level.getIntersectionCount()),
                    "word_difficulty_range", getDifficultyRangeText(level.getWordDifficulty()),
                    "hint_difficulty_range", getDifficultyRangeText(level.getHintDifficulty())
            ));
            
        } catch (Exception e) {
            log.error("레벨 조건 조회 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "레벨 조건 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 권장 그리드 크기 계산
     */
    private Integer calculateRecommendedGridSize(PzLevel level) {
        Integer wordCount = level.getWordCount();
        Integer intersectionCount = level.getIntersectionCount();
        
        // 단어 수와 교차점 수를 기반으로 권장 크기 계산
        if (wordCount <= 5) {
            return 5;
        } else if (wordCount <= 8) {
            return 6;
        } else if (wordCount <= 12) {
            return 7;
        } else if (wordCount <= 16) {
            return 8;
        } else {
            return 9;
        }
    }

    /**
     * 난이도 범위 텍스트 생성
     */
    private String getDifficultyRangeText(Integer difficulty) {
        return switch (difficulty) {
            case 1 -> "난이도 1,2";
            case 2 -> "난이도 1,2,3";
            case 3 -> "난이도 2,3,4";
            case 4 -> "난이도 3,4,5";
            case 5 -> "난이도 4,5";
            default -> "모든 난이도";
        };
    }
}

