package com.example.crossword.service;

import com.example.crossword.entity.PzWord;
import com.example.crossword.repository.PzWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
     * ID로 단어 조회 (Long 타입)
     */
    public PzWord findById(Long id) {
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
     * 단어 삭제 (Long 타입)
     */
    public void deleteById(Long id) {
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
    public Map<String, Long> getDifficultyStats() {
        return pzWordRepository.getDifficultyStats();
    }
}
