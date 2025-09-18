package com.example.crossword.service;

import com.example.crossword.entity.Word;
import com.example.crossword.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 단어 관리 서비스
 * 단어 조회, 검색, 통계 등의 비즈니스 로직을 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WordService {
    
    private final WordRepository wordRepository;
    
    /**
     * 모든 활성 단어 조회
     */
    public List<Word> getAllActiveWords() {
        log.debug("모든 활성 단어 조회");
        return wordRepository.findByIsActiveTrue();
    }
    
    /**
     * 활성 단어를 페이징으로 조회
     */
    public Page<Word> getActiveWords(Pageable pageable) {
        log.debug("활성 단어 페이징 조회: {}", pageable);
        return wordRepository.findByIsActiveTrue(pageable);
    }
    
    /**
     * ID로 단어 조회
     */
    public Optional<Word> getWordById(Integer id) {
        log.debug("ID로 단어 조회: {}", id);
        return wordRepository.findById(id);
    }
    
    /**
     * 단어명으로 정확히 일치하는 단어 조회
     */
    public Optional<Word> getWordByName(String word) {
        log.debug("단어명으로 조회: {}", word);
        return wordRepository.findByWordAndIsActiveTrue(word);
    }
    
    /**
     * 특정 난이도의 단어 조회
     */
    public List<Word> getWordsByDifficulty(Integer difficulty) {
        log.debug("난이도별 단어 조회: {}", difficulty);
        return wordRepository.findByDifficultyAndIsActiveTrue(difficulty);
    }
    
    /**
     * 특정 난이도의 단어를 페이징으로 조회
     */
    public Page<Word> getWordsByDifficulty(Integer difficulty, Pageable pageable) {
        log.debug("난이도별 단어 페이징 조회: {}, {}", difficulty, pageable);
        return wordRepository.findByDifficultyAndIsActiveTrue(difficulty, pageable);
    }
    
    /**
     * 특정 카테고리의 단어 조회
     */
    public List<Word> getWordsByCategory(String category) {
        log.debug("카테고리별 단어 조회: {}", category);
        return wordRepository.findByCategoryAndIsActiveTrue(category);
    }
    
    /**
     * 특정 카테고리의 단어를 페이징으로 조회
     */
    public Page<Word> getWordsByCategory(String category, Pageable pageable) {
        log.debug("카테고리별 단어 페이징 조회: {}, {}", category, pageable);
        return wordRepository.findByCategoryAndIsActiveTrue(category, pageable);
    }
    
    /**
     * 특정 길이의 단어 조회
     */
    public List<Word> getWordsByLength(Integer length) {
        log.debug("길이별 단어 조회: {}", length);
        return wordRepository.findByLengthAndIsActiveTrue(length);
    }
    
    /**
     * 특정 길이의 단어를 페이징으로 조회
     */
    public Page<Word> getWordsByLength(Integer length, Pageable pageable) {
        log.debug("길이별 단어 페이징 조회: {}, {}", length, pageable);
        return wordRepository.findByLengthAndIsActiveTrue(length, pageable);
    }
    
    /**
     * 난이도와 카테고리로 단어 조회
     */
    public List<Word> getWordsByDifficultyAndCategory(Integer difficulty, String category) {
        log.debug("난이도와 카테고리별 단어 조회: {}, {}", difficulty, category);
        return wordRepository.findByDifficultyAndCategoryAndIsActiveTrue(difficulty, category);
    }
    
    /**
     * 난이도와 카테고리로 단어를 페이징으로 조회
     */
    public Page<Word> getWordsByDifficultyAndCategory(Integer difficulty, String category, Pageable pageable) {
        log.debug("난이도와 카테고리별 단어 페이징 조회: {}, {}, {}", difficulty, category, pageable);
        return wordRepository.findByDifficultyAndCategoryAndIsActiveTrue(difficulty, category, pageable);
    }
    
    /**
     * 난이도 범위로 단어 조회
     */
    public List<Word> getWordsByDifficultyRange(Integer minDifficulty, Integer maxDifficulty) {
        log.debug("난이도 범위별 단어 조회: {} - {}", minDifficulty, maxDifficulty);
        return wordRepository.findByDifficultyRangeAndIsActiveTrue(minDifficulty, maxDifficulty);
    }
    
    /**
     * 난이도 범위로 단어를 페이징으로 조회
     */
    public Page<Word> getWordsByDifficultyRange(Integer minDifficulty, Integer maxDifficulty, Pageable pageable) {
        log.debug("난이도 범위별 단어 페이징 조회: {} - {}, {}", minDifficulty, maxDifficulty, pageable);
        return wordRepository.findByDifficultyRangeAndIsActiveTrue(minDifficulty, maxDifficulty, pageable);
    }
    
    /**
     * 길이 범위로 단어 조회
     */
    public List<Word> getWordsByLengthRange(Integer minLength, Integer maxLength) {
        log.debug("길이 범위별 단어 조회: {} - {}", minLength, maxLength);
        return wordRepository.findByLengthRangeAndIsActiveTrue(minLength, maxLength);
    }
    
    /**
     * 길이 범위로 단어를 페이징으로 조회
     */
    public Page<Word> getWordsByLengthRange(Integer minLength, Integer maxLength, Pageable pageable) {
        log.debug("길이 범위별 단어 페이징 조회: {} - {}, {}", minLength, maxLength, pageable);
        return wordRepository.findByLengthRangeAndIsActiveTrue(minLength, maxLength, pageable);
    }
    
    /**
     * 단어명에 특정 문자열이 포함된 단어 조회
     */
    public List<Word> searchWordsByName(String word) {
        log.debug("단어명 검색: {}", word);
        return wordRepository.findByWordContainingIgnoreCaseAndIsActiveTrue(word);
    }
    
    /**
     * 단어명에 특정 문자열이 포함된 단어를 페이징으로 조회
     */
    public Page<Word> searchWordsByName(String word, Pageable pageable) {
        log.debug("단어명 검색 페이징: {}, {}", word, pageable);
        return wordRepository.findByWordContainingIgnoreCaseAndIsActiveTrue(word, pageable);
    }
    
    /**
     * 랜덤하게 단어 조회 (크로스워드 퍼즐 생성용)
     */
    public List<Word> getRandomWords(Integer limit) {
        log.debug("랜덤 단어 조회: {}개", limit);
        return wordRepository.findRandomWords(limit);
    }
    
    /**
     * 특정 난이도의 랜덤 단어 조회
     */
    public List<Word> getRandomWordsByDifficulty(Integer difficulty, Integer limit) {
        log.debug("난이도별 랜덤 단어 조회: {}, {}개", difficulty, limit);
        return wordRepository.findRandomWordsByDifficulty(difficulty, limit);
    }
    
    /**
     * 특정 카테고리의 랜덤 단어 조회
     */
    public List<Word> getRandomWordsByCategory(String category, Integer limit) {
        log.debug("카테고리별 랜덤 단어 조회: {}, {}개", category, limit);
        return wordRepository.findRandomWordsByCategory(category, limit);
    }
    
    /**
     * 특정 길이의 랜덤 단어 조회
     */
    public List<Word> getRandomWordsByLength(Integer length, Integer limit) {
        log.debug("길이별 랜덤 단어 조회: {}, {}개", length, limit);
        return wordRepository.findRandomWordsByLength(length, limit);
    }
    
    /**
     * 난이도별 단어 개수 조회
     */
    public List<Object[]> getWordCountByDifficulty() {
        log.debug("난이도별 단어 개수 조회");
        return wordRepository.countWordsByDifficulty();
    }
    
    /**
     * 카테고리별 단어 개수 조회
     */
    public List<Object[]> getWordCountByCategory() {
        log.debug("카테고리별 단어 개수 조회");
        return wordRepository.countWordsByCategory();
    }
    
    /**
     * 길이별 단어 개수 조회
     */
    public List<Object[]> getWordCountByLength() {
        log.debug("길이별 단어 개수 조회");
        return wordRepository.countWordsByLength();
    }
    
    /**
     * 전체 활성 단어 개수 조회
     */
    public long getTotalActiveWordCount() {
        log.debug("전체 활성 단어 개수 조회");
        return wordRepository.countByIsActiveTrue();
    }
    
    /**
     * 특정 난이도의 활성 단어 개수 조회
     */
    public long getActiveWordCountByDifficulty(Integer difficulty) {
        log.debug("특정 난이도의 활성 단어 개수 조회: {}", difficulty);
        return wordRepository.countByDifficultyAndIsActiveTrue(difficulty);
    }
    
    /**
     * 특정 카테고리의 활성 단어 개수 조회
     */
    public long getActiveWordCountByCategory(String category) {
        log.debug("특정 카테고리의 활성 단어 개수 조회: {}", category);
        return wordRepository.countByCategoryAndIsActiveTrue(category);
    }
    
    /**
     * 특정 길이의 활성 단어 개수 조회
     */
    public long getActiveWordCountByLength(Integer length) {
        log.debug("특정 길이의 활성 단어 개수 조회: {}", length);
        return wordRepository.countByLengthAndIsActiveTrue(length);
    }
    
    /**
     * 단어 저장
     */
    @Transactional
    public Word saveWord(Word word) {
        log.debug("단어 저장: {}", word.getWord());
        return wordRepository.save(word);
    }
    
    /**
     * 단어 업데이트
     */
    @Transactional
    public Word updateWord(Word word) {
        log.debug("단어 업데이트: {}", word.getWord());
        return wordRepository.save(word);
    }
    
    /**
     * 단어 삭제 (비활성화)
     */
    @Transactional
    public void deactivateWord(Integer id) {
        log.debug("단어 비활성화: {}", id);
        wordRepository.findById(id).ifPresent(word -> {
            word.setIsActive(false);
            wordRepository.save(word);
        });
    }
    
    /**
     * 단어 활성화
     */
    @Transactional
    public void activateWord(Integer id) {
        log.debug("단어 활성화: {}", id);
        wordRepository.findById(id).ifPresent(word -> {
            word.setIsActive(true);
            wordRepository.save(word);
        });
    }
    
    /**
     * 단어 존재 여부 확인
     */
    public boolean existsWord(String word) {
        log.debug("단어 존재 여부 확인: {}", word);
        return wordRepository.findByWordAndIsActiveTrue(word).isPresent();
    }
    
    /**
     * 단어 난이도 업데이트
     */
    @Transactional
    public void updateWordDifficulty(Integer id, Integer difficulty) {
        log.debug("단어 난이도 업데이트: {}, {}", id, difficulty);
        wordRepository.findById(id).ifPresent(word -> {
            word.setDifficulty(difficulty);
            wordRepository.save(word);
        });
    }
    
    /**
     * 단어 카테고리 업데이트
     */
    @Transactional
    public void updateWordCategory(Integer id, String category) {
        log.debug("단어 카테고리 업데이트: {}, {}", id, category);
        wordRepository.findById(id).ifPresent(word -> {
            word.setCategory(category);
            wordRepository.save(word);
        });
    }
}
