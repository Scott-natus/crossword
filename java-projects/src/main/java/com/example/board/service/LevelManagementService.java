package com.example.board.service;

import com.example.board.entity.PzLevel;
import com.example.board.repository.PzLevelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 레벨 관리 서비스
 * 
 * @author Board Team
 * @version 1.0.0
 * @since 2025-10-26
 */
@Service
@Transactional
@Slf4j
public class LevelManagementService {
    
    @Autowired
    private PzLevelRepository pzLevelRepository;
    
    /**
     * 모든 레벨 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<PzLevel> getAllLevels(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return pzLevelRepository.findAll(pageable);
    }
    
    /**
     * 레벨 번호로 레벨 조회
     */
    @Transactional(readOnly = true)
    public Optional<PzLevel> getLevelByLevelNumber(Integer level) {
        return pzLevelRepository.findByLevel(level);
    }
    
    /**
     * ID로 레벨 조회
     */
    @Transactional(readOnly = true)
    public Optional<PzLevel> getLevelById(Long id) {
        return pzLevelRepository.findById(id);
    }
    
    /**
     * 레벨 생성
     */
    public PzLevel createLevel(PzLevel level) {
        if (pzLevelRepository.existsByLevel(level.getLevel())) {
            throw new IllegalArgumentException("이미 존재하는 레벨 번호입니다: " + level.getLevel());
        }
        
        level.setCreatedAt(LocalDateTime.now());
        level.setUpdatedAt(LocalDateTime.now());
        
        return pzLevelRepository.save(level);
    }
    
    /**
     * 레벨 수정
     */
    public PzLevel updateLevel(Long id, PzLevel updatedLevel) {
        PzLevel existingLevel = pzLevelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("레벨을 찾을 수 없습니다: " + id));
        
        // 레벨 번호가 변경되는 경우 중복 체크
        if (!existingLevel.getLevel().equals(updatedLevel.getLevel())) {
            if (pzLevelRepository.existsByLevel(updatedLevel.getLevel())) {
                throw new IllegalArgumentException("이미 존재하는 레벨 번호입니다: " + updatedLevel.getLevel());
            }
        }
        
        existingLevel.setLevel(updatedLevel.getLevel());
        existingLevel.setLevelName(updatedLevel.getLevelName());
        existingLevel.setWordDifficulty(updatedLevel.getWordDifficulty());
        existingLevel.setHintDifficulty(updatedLevel.getHintDifficulty());
        existingLevel.setIntersectionCount(updatedLevel.getIntersectionCount());
        existingLevel.setTimeLimit(updatedLevel.getTimeLimit());
        existingLevel.setClearCondition(updatedLevel.getClearCondition());
        existingLevel.setWordCount(updatedLevel.getWordCount());
        existingLevel.setUpdatedBy(updatedLevel.getUpdatedBy());
        existingLevel.setUpdatedAt(LocalDateTime.now());
        
        return pzLevelRepository.save(existingLevel);
    }
    
    /**
     * 레벨 부분 수정 (Map 기반)
     */
    public PzLevel updateLevelPartial(Long id, Map<String, Object> updateData) {
        PzLevel existingLevel = pzLevelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("레벨을 찾을 수 없습니다: " + id));
        
        // 업데이트할 필드들만 적용
        if (updateData.containsKey("levelName")) {
            existingLevel.setLevelName((String) updateData.get("levelName"));
        }
        if (updateData.containsKey("wordDifficulty")) {
            existingLevel.setWordDifficulty(parseInteger(updateData.get("wordDifficulty")));
        }
        if (updateData.containsKey("hintDifficulty")) {
            existingLevel.setHintDifficulty(parseInteger(updateData.get("hintDifficulty")));
        }
        if (updateData.containsKey("intersectionCount")) {
            existingLevel.setIntersectionCount(parseInteger(updateData.get("intersectionCount")));
        }
        if (updateData.containsKey("timeLimit")) {
            existingLevel.setTimeLimit(parseInteger(updateData.get("timeLimit")));
        }
        if (updateData.containsKey("clearCondition")) {
            existingLevel.setClearCondition(parseInteger(updateData.get("clearCondition")));
        }
        if (updateData.containsKey("wordCount")) {
            existingLevel.setWordCount(parseInteger(updateData.get("wordCount")));
        }
        if (updateData.containsKey("updatedBy")) {
            existingLevel.setUpdatedBy((String) updateData.get("updatedBy"));
        }
        
        existingLevel.setUpdatedAt(LocalDateTime.now());
        
        return pzLevelRepository.save(existingLevel);
    }
    
    /**
     * 레벨 삭제
     */
    public void deleteLevel(Long id) {
        if (!pzLevelRepository.existsById(id)) {
            throw new IllegalArgumentException("레벨을 찾을 수 없습니다: " + id);
        }
        pzLevelRepository.deleteById(id);
    }
    
    /**
     * 레벨 번호로 삭제
     */
    public void deleteLevelByLevelNumber(Integer level) {
        if (!pzLevelRepository.existsByLevel(level)) {
            throw new IllegalArgumentException("레벨을 찾을 수 없습니다: " + level);
        }
        pzLevelRepository.deleteByLevel(level);
    }
    
    /**
     * 검색 조건으로 레벨 조회
     */
    @Transactional(readOnly = true)
    public Page<PzLevel> searchLevels(String searchTerm, Integer minWords, Integer maxWords, 
                                     Integer wordDifficulty, Integer hintDifficulty,
                                     int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return pzLevelRepository.findBySearchCriteria(
            searchTerm, minWords, maxWords, wordDifficulty, hintDifficulty, pageable);
    }
    
    /**
     * 레벨 통계 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLevelStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalLevels = pzLevelRepository.count();
        List<PzLevel> allLevels = pzLevelRepository.findAllOrderByLevel();
        
        stats.put("totalLevels", totalLevels);
        stats.put("maxLevel", allLevels.isEmpty() ? 0 : allLevels.get(allLevels.size() - 1).getLevel());
        stats.put("minLevel", allLevels.isEmpty() ? 0 : allLevels.get(0).getLevel());
        
        // 난이도별 통계
        Map<String, Long> difficultyStats = new HashMap<>();
        for (PzLevel level : allLevels) {
            String key = "difficulty_" + (level.getWordDifficulty() != null ? level.getWordDifficulty() : "null");
            difficultyStats.put(key, difficultyStats.getOrDefault(key, 0L) + 1);
        }
        stats.put("difficultyStats", difficultyStats);
        
        return stats;
    }
    
    /**
     * 레벨 데이터를 Map으로 변환 (DataTables용)
     */
    public Map<String, Object> mapLevelToMap(PzLevel level) {
        Map<String, Object> levelMap = new HashMap<>();
        levelMap.put("id", level.getId());
        levelMap.put("level", level.getLevel());
        levelMap.put("levelName", level.getLevelName());
        levelMap.put("wordDifficulty", level.getWordDifficulty());
        levelMap.put("hintDifficulty", level.getHintDifficulty());
        levelMap.put("intersectionCount", level.getIntersectionCount());
        levelMap.put("timeLimit", level.getTimeLimit());
        levelMap.put("clearCondition", level.getClearCondition());
        levelMap.put("wordCount", level.getWordCount());
        levelMap.put("updatedBy", level.getUpdatedBy());
        levelMap.put("createdAt", level.getCreatedAt() != null ? level.getCreatedAt().toString() : null);
        levelMap.put("updatedAt", level.getUpdatedAt() != null ? level.getUpdatedAt().toString() : null);
        return levelMap;
    }
    
    /**
     * 레벨 일괄 수정
     */
    public int bulkUpdateLevels(List<Integer> levelIds, Map<String, Object> updateData) {
        int updatedCount = 0;
        
        for (Integer levelId : levelIds) {
            try {
                Optional<PzLevel> levelOpt = pzLevelRepository.findById(levelId.longValue());
                if (levelOpt.isPresent()) {
                    PzLevel level = levelOpt.get();
                    
                    // 업데이트할 필드들 적용
                    if (updateData.containsKey("levelName")) {
                        level.setLevelName((String) updateData.get("levelName"));
                    }
                    if (updateData.containsKey("wordDifficulty")) {
                        level.setWordDifficulty(parseInteger(updateData.get("wordDifficulty")));
                    }
                    if (updateData.containsKey("hintDifficulty")) {
                        level.setHintDifficulty(parseInteger(updateData.get("hintDifficulty")));
                    }
                    if (updateData.containsKey("intersectionCount")) {
                        level.setIntersectionCount(parseInteger(updateData.get("intersectionCount")));
                    }
                    if (updateData.containsKey("timeLimit")) {
                        level.setTimeLimit(parseInteger(updateData.get("timeLimit")));
                    }
                    if (updateData.containsKey("clearCondition")) {
                        level.setClearCondition(parseInteger(updateData.get("clearCondition")));
                    }
                    if (updateData.containsKey("wordCount")) {
                        level.setWordCount(parseInteger(updateData.get("wordCount")));
                    }
                    
                    level.setUpdatedAt(LocalDateTime.now());
                    pzLevelRepository.save(level);
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("레벨 일괄 수정 중 오류 발생: levelId={}", levelId, e);
            }
        }
        
        return updatedCount;
    }
    
    /**
     * 레벨 일괄 삭제
     */
    public int bulkDeleteLevels(List<Integer> levelIds) {
        int deletedCount = 0;
        
        for (Integer levelId : levelIds) {
            try {
                if (pzLevelRepository.existsById(levelId.longValue())) {
                    pzLevelRepository.deleteById(levelId.longValue());
                    deletedCount++;
                }
            } catch (Exception e) {
                log.error("레벨 일괄 삭제 중 오류 발생: levelId={}", levelId, e);
            }
        }
        
        return deletedCount;
    }
    
    /**
     * Object를 Integer로 안전하게 변환하는 헬퍼 메서드
     */
    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Integer) {
            return (Integer) value;
        }
        
        if (value instanceof String) {
            String strValue = ((String) value).trim();
            if (strValue.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(strValue);
            } catch (NumberFormatException e) {
                log.warn("숫자 변환 실패: {}", strValue);
                return null;
            }
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        log.warn("지원하지 않는 타입: {}", value.getClass().getSimpleName());
        return null;
    }
}
