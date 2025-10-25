package com.example.board.service;

import com.example.board.entity.PzHint;
import com.example.board.entity.PzWord;
import com.example.board.repository.PzHintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PzHint 엔티티 관련 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PzHintService {
    
    private final PzHintRepository pzHintRepository;
    private final PzWordService pzWordService;
    
    /**
     * ID로 힌트 조회
     */
    public Optional<PzHint> getPzHintById(Integer id) {
        log.debug("힌트 조회: ID {}", id);
        return pzHintRepository.findById(id);
    }
    
    /**
     * 단어 ID로 힌트 목록 조회
     */
    public List<PzHint> getHintsByWordId(Integer wordId) {
        log.debug("단어 ID로 힌트 조회: {}", wordId);
        return pzHintRepository.findByWordIdOrderById(wordId);
    }
    
    /**
     * 단어 ID로 주요 힌트 조회
     */
    public List<PzHint> getPrimaryHintsByWordId(Integer wordId) {
        log.debug("단어 ID로 주요 힌트 조회: {}", wordId);
        return pzHintRepository.findPrimaryHintsByWordId(wordId);
    }
    
    /**
     * 단어 ID로 텍스트 힌트 조회
     */
    public List<PzHint> getTextHintsByWordId(Integer wordId) {
        log.debug("단어 ID로 텍스트 힌트 조회: {}", wordId);
        return pzHintRepository.findTextHintsByWordId(wordId);
    }
    
    /**
     * 단어 ID와 is_primary 조건으로 힌트 조회 (Laravel과 동일한 로직)
     */
    public List<PzHint> getHintsByWordIdAndIsPrimary(Integer wordId, Boolean isPrimary) {
        log.debug("단어 ID와 is_primary 조건으로 힌트 조회: wordId={}, isPrimary={}", wordId, isPrimary);
        return pzHintRepository.findByWordIdAndIsPrimaryOrderByDifficulty(wordId, isPrimary);
    }
    
    /**
     * 단어 ID로 힌트 조회하되 특정 힌트 ID는 제외 (Laravel의 base_hint_id 제외 로직)
     */
    public List<PzHint> getHintsByWordIdExcludingHintId(Integer wordId, Integer excludeHintId) {
        log.debug("단어 ID로 힌트 조회 (특정 힌트 ID 제외): wordId={}, excludeHintId={}", wordId, excludeHintId);
        return pzHintRepository.findByWordIdAndIdNotOrderByDifficulty(wordId, excludeHintId);
    }
    
    /**
     * 힌트 저장
     */
    public PzHint savePzHint(PzHint pzHint) {
        log.debug("힌트 저장: {}", pzHint.getHintText());
        return pzHintRepository.save(pzHint);
    }
    
    /**
     * 힌트 삭제
     */
    public void deletePzHint(Integer id) {
        log.debug("힌트 삭제: ID {}", id);
        pzHintRepository.deleteById(id);
    }
    
    /**
     * 기본 힌트 조회 (primary=true)
     */
    public List<Map<String, Object>> getPrimaryHints(Long wordId) {
        log.debug("기본 힌트 조회: wordId={}", wordId);
        List<PzHint> hints = pzHintRepository.findByWordIdAndIsPrimaryOrderByDifficulty(wordId.intValue(), true);
        
        // 단어 정보 조회 (카테고리 포함)
        PzWord word = pzWordService.getById(wordId);
        String category = word != null ? word.getCategory() : "";
        
        return hints.stream().map(hint -> {
            Map<String, Object> hintMap = new HashMap<>();
            hintMap.put("id", hint.getId());
            hintMap.put("hint_text", hint.getHintText());
            hintMap.put("difficulty", hint.getDifficulty());
            hintMap.put("category", category);
            return hintMap;
        }).collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 추가 힌트 조회 (primary=false, id가 가장 작은 힌트)
     */
    public List<Map<String, Object>> getAdditionalHints(Long wordId) {
        log.debug("추가 힌트 조회: wordId={}", wordId);
        List<PzHint> hints = pzHintRepository.findByWordIdAndIsPrimaryOrderByDifficulty(wordId.intValue(), false);
        
        // 단어 정보 조회 (카테고리 포함)
        PzWord word = pzWordService.getById(wordId);
        String category = word != null ? word.getCategory() : "";
        
        return hints.stream().map(hint -> {
            Map<String, Object> hintMap = new HashMap<>();
            hintMap.put("id", hint.getId());
            hintMap.put("hint_text", hint.getHintText());
            hintMap.put("difficulty", hint.getDifficulty());
            hintMap.put("category", category);
            return hintMap;
        }).collect(java.util.stream.Collectors.toList());
    }
}





