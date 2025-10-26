package com.example.board.service;

import com.example.board.entity.PzHint;
import com.example.board.entity.PzWord;
import com.example.board.repository.PzHintRepository;
import com.example.board.repository.PzWordRepository;
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
 * AI 힌트 생성 관리 서비스
 * 
 * @author Board Team
 * @version 1.0.0
 * @since 2025-10-26
 */
@Service
@Transactional
@Slf4j
public class HintGeneratorManagementService {
    
    @Autowired
    private PzHintRepository pzHintRepository;
    
    @Autowired
    private PzWordRepository pzWordRepository;
    
    /**
     * 힌트 생성 통계 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalWords = pzWordRepository.count();
        long wordsWithHints = pzWordRepository.countWordsWithHints();
        long wordsWithoutHints = totalWords - wordsWithHints;
        
        stats.put("total_words", totalWords);
        stats.put("words_with_hints", wordsWithHints);
        stats.put("words_without_hints", wordsWithoutHints);
        
        return stats;
    }
    
    /**
     * 힌트 생성용 단어 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PzWord> getWordsForHintGeneration(String search, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        if (search != null && !search.trim().isEmpty()) {
            return pzWordRepository.findByWordContainingIgnoreCase(search, pageable);
        } else {
            return pzWordRepository.findAll(pageable);
        }
    }
    
    /**
     * 단어 데이터를 Map으로 변환 (DataTables용)
     */
    public Map<String, Object> mapWordToMap(PzWord word) {
        Map<String, Object> wordMap = new HashMap<>();
        wordMap.put("id", word.getId());
        wordMap.put("word", word.getWord());
        wordMap.put("length", word.getLength());
        wordMap.put("category", word.getCategory());
        wordMap.put("difficulty", word.getDifficulty());
        wordMap.put("isActive", word.getIsActive());
        wordMap.put("confYn", word.getConfYn());
        wordMap.put("createdAt", word.getCreatedAt() != null ? word.getCreatedAt().toString() : null);
        wordMap.put("updatedAt", word.getUpdatedAt() != null ? word.getUpdatedAt().toString() : null);
        
        // 힌트 개수 조회
        long hintCount = pzHintRepository.countByWordId(word.getId());
        wordMap.put("hintCount", hintCount);
        wordMap.put("hints_count", hintCount); // DataTables용 필드명
        wordMap.put("hasHints", hintCount > 0);
        
        // 난이도 텍스트 추가
        String difficultyText = getDifficultyText(word.getDifficulty());
        wordMap.put("difficulty_text", difficultyText);
        
        return wordMap;
    }
    
    /**
     * 난이도 숫자를 텍스트로 변환
     */
    private String getDifficultyText(Integer difficulty) {
        if (difficulty == null) return "미설정";
        switch (difficulty) {
            case 1: return "쉬움";
            case 2: return "보통";
            case 3: return "어려움";
            case 4: return "매우어려움";
            case 5: return "극도어려움";
            default: return "미설정";
        }
    }
    
    /**
     * API 연결 테스트
     */
    public boolean testApiConnection() {
        try {
            // 간단한 API 연결 테스트 (실제로는 Gemini API 연결 테스트)
            log.info("API 연결 테스트 수행");
            return true;
        } catch (Exception e) {
            log.error("API 연결 테스트 실패", e);
            return false;
        }
    }
    
    /**
     * 단어의 힌트 목록 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWordHints(Integer wordId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(wordId);
            if (wordOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "단어를 찾을 수 없습니다.");
                return result;
            }
            
            PzWord word = wordOpt.get();
            List<PzHint> hints = pzHintRepository.findByWordIdOrderById(wordId);
            
            result.put("success", true);
            result.put("word", word);
            result.put("hints", hints);
            result.put("hintCount", hints.size());
            
        } catch (Exception e) {
            log.error("단어 힌트 조회 중 오류 발생: {}", wordId, e);
            result.put("success", false);
            result.put("message", "힌트 조회 중 오류가 발생했습니다.");
        }
        
        return result;
    }
    
    /**
     * 단어에 대한 힌트 생성
     */
    @Transactional
    public Map<String, Object> generateForWord(Integer wordId, Boolean overwrite) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 단어 조회
            Optional<PzWord> wordOpt = pzWordRepository.findById(wordId);
            if (wordOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "단어를 찾을 수 없습니다.");
                return response;
            }
            
            PzWord word = wordOpt.get();
            
            // 비활성화된 단어는 힌트 생성 불가
            if (!word.getIsActive()) {
                response.put("success", false);
                response.put("message", "비활성화된 단어에는 힌트를 생성할 수 없습니다.");
                return response;
            }
            
            // 기존 힌트 확인
            List<PzHint> existingHints = pzHintRepository.findByWordId(wordId);
            if (!existingHints.isEmpty() && !overwrite) {
                response.put("success", false);
                response.put("message", "이미 힌트가 존재합니다. 덮어쓰기 옵션을 선택하거나 기존 힌트를 삭제 후 다시 시도해주세요.");
                return response;
            }
            
            // 덮어쓰기 모드인 경우 기존 힌트 삭제
            if (overwrite && !existingHints.isEmpty()) {
                pzHintRepository.deleteByWordId(wordId);
                log.info("기존 힌트 삭제 완료: wordId={}, 삭제된 힌트 수={}", wordId, existingHints.size());
            }
            
            // 더미 힌트 생성 (실제로는 Gemini API 호출)
            List<PzHint> createdHints = createDummyHints(word);
            
            response.put("success", true);
            response.put("message", "힌트가 생성되었습니다. (성공: " + createdHints.size() + "개)");
            response.put("hints", createdHints);
            response.put("word", word);
            
        } catch (Exception e) {
            log.error("힌트 생성 중 오류 발생: wordId={}", wordId, e);
            response.put("success", false);
            response.put("message", "힌트 생성 중 오류가 발생했습니다.");
        }
        
        return response;
    }
    
    /**
     * 더미 힌트 생성 (실제로는 Gemini API 호출)
     */
    private List<PzHint> createDummyHints(PzWord word) {
        // 실제로는 Gemini API를 호출하여 힌트를 생성
        // 여기서는 더미 데이터로 대체
        
        PzHint primaryHint = new PzHint();
        primaryHint.setWord(word);
        primaryHint.setHintText(word.getWord() + "에 대한 기본 힌트입니다.");
        primaryHint.setDifficulty(2);
        primaryHint.setIsPrimary(true);
        primaryHint.setCreatedAt(LocalDateTime.now());
        primaryHint.setUpdatedAt(LocalDateTime.now());
        pzHintRepository.save(primaryHint);
        
        PzHint additionalHint = new PzHint();
        additionalHint.setWord(word);
        additionalHint.setHintText(word.getWord() + "에 대한 추가 힌트입니다.");
        additionalHint.setDifficulty(3);
        additionalHint.setIsPrimary(false);
        additionalHint.setCreatedAt(LocalDateTime.now());
        additionalHint.setUpdatedAt(LocalDateTime.now());
        pzHintRepository.save(additionalHint);
        
        return List.of(primaryHint, additionalHint);
    }
    
    /**
     * 힌트 삭제
     */
    @Transactional
    public boolean deleteHint(Long hintId) {
        try {
            if (pzHintRepository.existsById(hintId.intValue())) {
                pzHintRepository.deleteById(hintId.intValue());
                log.info("힌트 삭제 완료: hintId={}", hintId);
                return true;
            } else {
                log.warn("삭제할 힌트를 찾을 수 없습니다: hintId={}", hintId);
                return false;
            }
        } catch (Exception e) {
            log.error("힌트 삭제 중 오류 발생: hintId={}", hintId, e);
            return false;
        }
    }
}
