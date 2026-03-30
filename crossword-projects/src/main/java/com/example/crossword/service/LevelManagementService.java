package com.example.crossword.service;

import com.example.crossword.entity.PzLevel;
import com.example.crossword.repository.PzLevelRepository;
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

@Service
@Transactional
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
        // 레벨 번호 중복 확인
        if (pzLevelRepository.existsByLevel(level.getLevel())) {
            throw new IllegalArgumentException("레벨 번호 " + level.getLevel() + "는 이미 존재합니다.");
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
        
        // 레벨 번호 변경 시 중복 확인
        if (!existingLevel.getLevel().equals(updatedLevel.getLevel()) && 
            pzLevelRepository.existsByLevel(updatedLevel.getLevel())) {
            throw new IllegalArgumentException("레벨 번호 " + updatedLevel.getLevel() + "는 이미 존재합니다.");
        }
        
        // 기존 데이터 유지하면서 업데이트
        existingLevel.setLevel(updatedLevel.getLevel());
        existingLevel.setLevelName(updatedLevel.getLevelName());
        existingLevel.setWordCount(updatedLevel.getWordCount());
        existingLevel.setWordDifficulty(updatedLevel.getWordDifficulty());
        existingLevel.setHintDifficulty(updatedLevel.getHintDifficulty());
        existingLevel.setIntersectionCount(updatedLevel.getIntersectionCount());
        existingLevel.setTimeLimit(updatedLevel.getTimeLimit());
        existingLevel.setClearCondition(updatedLevel.getClearCondition());
        existingLevel.setUpdatedBy(updatedLevel.getUpdatedBy());
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
        
        // 기본 통계
        stats.put("totalLevels", pzLevelRepository.countTotalLevels());
        stats.put("averageWordCount", pzLevelRepository.getAverageWordCount());
        stats.put("maxWordCount", pzLevelRepository.getMaxWordCount());
        stats.put("minWordCount", pzLevelRepository.getMinWordCount());
        
        // 난이도별 통계
        List<Object[]> wordDifficultyStats = pzLevelRepository.getLevelCountByWordDifficulty();
        Map<Integer, Long> wordDifficultyMap = new HashMap<>();
        for (Object[] stat : wordDifficultyStats) {
            wordDifficultyMap.put((Integer) stat[0], (Long) stat[1]);
        }
        stats.put("wordDifficultyStats", wordDifficultyMap);
        
        List<Object[]> hintDifficultyStats = pzLevelRepository.getLevelCountByHintDifficulty();
        Map<Integer, Long> hintDifficultyMap = new HashMap<>();
        for (Object[] stat : hintDifficultyStats) {
            hintDifficultyMap.put((Integer) stat[0], (Long) stat[1]);
        }
        stats.put("hintDifficultyStats", hintDifficultyMap);
        
        return stats;
    }
    
    /**
     * 레벨 복사
     */
    public PzLevel copyLevel(Long sourceId, Integer newLevel, String newLevelName) {
        PzLevel sourceLevel = pzLevelRepository.findById(sourceId)
            .orElseThrow(() -> new IllegalArgumentException("복사할 레벨을 찾을 수 없습니다: " + sourceId));
        
        // 새 레벨 번호 중복 확인
        if (pzLevelRepository.existsByLevel(newLevel)) {
            throw new IllegalArgumentException("레벨 번호 " + newLevel + "는 이미 존재합니다.");
        }
        
        PzLevel newLevelEntity = new PzLevel();
        newLevelEntity.setLevel(newLevel);
        newLevelEntity.setLevelName(newLevelName);
        newLevelEntity.setWordCount(sourceLevel.getWordCount());
        newLevelEntity.setWordDifficulty(sourceLevel.getWordDifficulty());
        newLevelEntity.setHintDifficulty(sourceLevel.getHintDifficulty());
        newLevelEntity.setIntersectionCount(sourceLevel.getIntersectionCount());
        newLevelEntity.setTimeLimit(sourceLevel.getTimeLimit());
        newLevelEntity.setClearCondition(sourceLevel.getClearCondition());
        newLevelEntity.setUpdatedBy(sourceLevel.getUpdatedBy());
        
        return pzLevelRepository.save(newLevelEntity);
    }
    
    /**
     * 레벨 일괄 업데이트
     */
    public void bulkUpdateLevels(List<Long> levelIds, Map<String, Object> updateData) {
        List<PzLevel> levels = pzLevelRepository.findAllById(levelIds);
        
        for (PzLevel level : levels) {
            if (updateData.containsKey("levelName")) {
                level.setLevelName((String) updateData.get("levelName"));
            }
            if (updateData.containsKey("wordCount")) {
                level.setWordCount((Integer) updateData.get("wordCount"));
            }
            if (updateData.containsKey("wordDifficulty")) {
                level.setWordDifficulty((Integer) updateData.get("wordDifficulty"));
            }
            if (updateData.containsKey("hintDifficulty")) {
                level.setHintDifficulty((Integer) updateData.get("hintDifficulty"));
            }
            if (updateData.containsKey("intersectionCount")) {
                level.setIntersectionCount((Integer) updateData.get("intersectionCount"));
            }
            if (updateData.containsKey("timeLimit")) {
                level.setTimeLimit((Integer) updateData.get("timeLimit"));
            }
            if (updateData.containsKey("clearCondition")) {
                Object clearConditionObj = updateData.get("clearCondition");
                if (clearConditionObj instanceof Integer) {
                    level.setClearCondition((Integer) clearConditionObj);
                } else if (clearConditionObj instanceof String) {
                    try {
                        level.setClearCondition(Integer.valueOf((String) clearConditionObj));
                    } catch (NumberFormatException e) {
                        level.setClearCondition(null);
                    }
                } else {
                    level.setClearCondition(null);
                }
            }
            if (updateData.containsKey("updatedBy")) {
                level.setUpdatedBy((String) updateData.get("updatedBy"));
            }
            level.setUpdatedAt(LocalDateTime.now());
        }
        
        pzLevelRepository.saveAll(levels);
    }
}
