package com.example.crossword.service;

import com.example.crossword.entity.PzWord;
import com.example.crossword.repository.PzWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PzWord 엔티티 관련 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PzWordService {
    
    private final PzWordRepository pzWordRepository;
    
    /**
     * ID로 단어 조회
     */
    public Optional<PzWord> getPzWordById(Integer id) {
        log.debug("단어 조회: ID {}", id);
        return pzWordRepository.findById(id);
    }
    
    /**
     * 활성화된 모든 단어 조회
     */
    public List<PzWord> getAllActiveWords() {
        log.debug("활성화된 모든 단어 조회");
        return pzWordRepository.findByIsActiveTrue();
    }
    
    /**
     * 단어 저장
     */
    public PzWord savePzWord(PzWord pzWord) {
        log.debug("단어 저장: {}", pzWord.getWord());
        return pzWordRepository.save(pzWord);
    }
    
    /**
     * 단어 삭제
     */
    public void deletePzWord(Integer id) {
        log.debug("단어 삭제: ID {}", id);
        pzWordRepository.deleteById(id);
    }
    
    /**
     * 단어 텍스트로 활성화된 단어 조회
     */
    public Optional<PzWord> getPzWordByWord(String word) {
        log.debug("단어 텍스트로 조회: {}", word);
        return pzWordRepository.findByWordAndIsActiveTrue(word);
    }
    
    // ==================== 관리자 기능 추가 ====================
    
    /**
     * ID로 단어 조회 (Integer 타입)
     */
    public PzWord findById(Integer id) {
        log.debug("단어 조회: ID {}", id);
        return pzWordRepository.findById(id).orElse(null);
    }
    
    /**
     * 단어 저장 (Long 타입)
     */
    public PzWord save(PzWord pzWord) {
        log.debug("단어 저장: {}", pzWord.getWord());
        return pzWordRepository.save(pzWord);
    }
    
    /**
     * 단어 삭제 (Integer 타입)
     */
    public void deleteById(Integer id) {
        log.debug("단어 삭제: ID {}", id);
        pzWordRepository.deleteById(id);
    }
    
    /**
     * 단어 존재 여부 확인
     */
    public boolean existsByWord(String word) {
        return pzWordRepository.existsByWord(word);
    }
    
    /**
     * 필터링된 단어 목록 조회
     */
    public Page<PzWord> getWordsWithFilters(Pageable pageable, String search, String category, 
                                           Integer difficulty, Boolean isActive) {
        log.debug("필터링된 단어 목록 조회: search={}, category={}, difficulty={}, isActive={}", 
                 search, category, difficulty, isActive);
        
        if (search != null && !search.trim().isEmpty()) {
            if (category != null && !category.trim().isEmpty()) {
                if (difficulty != null) {
                    if (isActive != null) {
                        return pzWordRepository.findByWordContainingIgnoreCaseAndCategoryAndDifficultyAndIsActive(
                            search, category, difficulty, isActive, pageable);
                    } else {
                        return pzWordRepository.findByWordContainingIgnoreCaseAndCategoryAndDifficulty(
                            search, category, difficulty, pageable);
                    }
                } else {
                    if (isActive != null) {
                        return pzWordRepository.findByWordContainingIgnoreCaseAndCategoryAndIsActive(
                            search, category, isActive, pageable);
                    } else {
                        return pzWordRepository.findByWordContainingIgnoreCaseAndCategory(
                            search, category, pageable);
                    }
                }
            } else {
                if (difficulty != null) {
                    if (isActive != null) {
                        return pzWordRepository.findByWordContainingIgnoreCaseAndDifficultyAndIsActive(
                            search, difficulty, isActive, pageable);
                    } else {
                        return pzWordRepository.findByWordContainingIgnoreCaseAndDifficulty(
                            search, difficulty, pageable);
                    }
                } else {
                    if (isActive != null) {
                        return pzWordRepository.findByWordContainingIgnoreCaseAndIsActive(
                            search, isActive, pageable);
                    } else {
                        return pzWordRepository.findByWordContainingIgnoreCase(search, pageable);
                    }
                }
            }
        } else {
            if (category != null && !category.trim().isEmpty()) {
                if (difficulty != null) {
                    if (isActive != null) {
                        return pzWordRepository.findByCategoryAndDifficultyAndIsActive(
                            category, difficulty, isActive, pageable);
                    } else {
                        return pzWordRepository.findByCategoryAndDifficulty(category, difficulty, pageable);
                    }
                } else {
                    if (isActive != null) {
                        return pzWordRepository.findByCategoryAndIsActive(category, isActive, pageable);
                    } else {
                        return pzWordRepository.findByCategory(category, pageable);
                    }
                }
            } else {
                if (difficulty != null) {
                    if (isActive != null) {
                        return pzWordRepository.findByDifficultyAndIsActive(difficulty, isActive, pageable);
                    } else {
                        return pzWordRepository.findByDifficulty(difficulty, pageable);
                    }
                } else {
                    if (isActive != null) {
                        return pzWordRepository.findByIsActive(isActive, pageable);
                    } else {
                        return pzWordRepository.findAll(pageable);
                    }
                }
            }
        }
    }
    
    /**
     * 전체 단어 수 조회
     */
    public long getTotalWordCount() {
        return pzWordRepository.count();
    }
    
    /**
     * 활성 단어 수 조회
     */
    public long getActiveWordCount() {
        return pzWordRepository.countByIsActiveTrue();
    }
    
    /**
     * 힌트를 보유한 단어 수 조회
     */
    public long getWordsWithHintsCount() {
        return pzWordRepository.countWordsWithHints();
    }
    
    /**
     * 고유 카테고리 목록 조회
     */
    public List<String> getDistinctCategories() {
        return pzWordRepository.findDistinctCategories();
    }
    
    /**
     * 난이도별 단어 수 통계
     */
    public List<Object[]> getDifficultyStats() {
        return pzWordRepository.getDifficultyStats();
    }

    // ===== 라라벨 PzWordController와 동일한 메서드들 =====

    /**
     * 필터링된 단어 목록 조회 (라라벨 getData() 메서드와 동일)
     */
    public Page<PzWord> getWordsWithFilters(String difficultyFilter, String search, Pageable pageable) {
        log.debug("필터링된 단어 목록 조회: difficulty={}, search={}", difficultyFilter, search);
        
        if (difficultyFilter != null && !difficultyFilter.isEmpty()) {
            Integer difficulty = Integer.parseInt(difficultyFilter);
            if (search != null && !search.isEmpty()) {
                return pzWordRepository.findByWordContainingIgnoreCaseAndIsActive("%" + search + "%", true, pageable);
            } else {
                return pzWordRepository.findByDifficultyAndIsActive(difficulty, true, pageable);
            }
        } else if (search != null && !search.isEmpty()) {
            return pzWordRepository.findByWordContainingIgnoreCaseAndIsActive("%" + search + "%", true, pageable);
        } else {
            return pzWordRepository.findByIsActive(true, pageable);
        }
    }

    /**
     * 단어 생성 (라라벨 store() 메서드와 동일)
     */
    @Transactional
    public PzWord createWord(String category, String word, Integer difficulty) {
        log.debug("단어 생성: category={}, word={}, difficulty={}", category, word, difficulty);
        
        PzWord newWord = new PzWord();
        newWord.setCategory(category);
        newWord.setWord(word);
        newWord.setDifficulty(difficulty);
        newWord.setLength(word.length());
        newWord.setIsActive(true);
        
        return pzWordRepository.save(newWord);
    }

    /**
     * 단어 활성화/비활성화 토글 (라라벨 toggleActive() 메서드와 동일)
     */
    @Transactional
    public boolean toggleActive(Integer id) {
        log.debug("단어 상태 토글: ID {}", id);
        
        Optional<PzWord> wordOpt = pzWordRepository.findById(id);
        if (wordOpt.isPresent()) {
            PzWord word = wordOpt.get();
            word.setIsActive(!word.getIsActive());
            pzWordRepository.save(word);
            return word.getIsActive();
        } else {
            throw new RuntimeException("단어를 찾을 수 없습니다: ID " + id);
        }
    }

    /**
     * 일괄 변경 (라라벨 batchUpdate() 메서드와 동일)
     */
    @Transactional
    public int batchUpdate(List<Integer> wordIds, Boolean updateDifficulty, Boolean updateActive, 
                          Integer difficulty, Integer isActive) {
        log.debug("일괄 변경: wordIds={}, updateDifficulty={}, updateActive={}", 
                 wordIds, updateDifficulty, updateActive);
        
        int updatedCount = 0;
        
        for (Integer wordId : wordIds) {
            Optional<PzWord> wordOpt = pzWordRepository.findById(wordId);
            if (wordOpt.isPresent()) {
                PzWord word = wordOpt.get();
                boolean modified = false;
                
                if (updateDifficulty != null && updateDifficulty && difficulty != null) {
                    word.setDifficulty(difficulty);
                    modified = true;
                }
                
                if (updateActive != null && updateActive && isActive != null) {
                    word.setIsActive(isActive == 1);
                    modified = true;
                }
                
                if (modified) {
                    pzWordRepository.save(word);
                    updatedCount++;
                }
            }
        }
        
        return updatedCount;
    }

    /**
     * 통계 데이터 조회 (라라벨 getStats() 메서드와 동일)
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("total_words", pzWordRepository.count());
            stats.put("active_words", pzWordRepository.countByIsActiveTrue());
            stats.put("inactive_words", pzWordRepository.countByIsActiveFalse());
            stats.put("words_with_hints", pzWordRepository.countWordsWithHints());
            stats.put("words_without_hints", pzWordRepository.countWordsWithoutHints());
            
            return stats;
        } catch (Exception e) {
            log.error("통계 데이터 조회 오류: {}", e.getMessage());
            return stats;
        }
    }
    
    /**
     * ID로 단어 조회 (퍼즐 게임용)
     */
    public PzWord getById(Long id) {
        log.debug("ID로 단어 조회: {}", id);
        return pzWordRepository.findById(id.intValue()).orElse(null);
    }
}
