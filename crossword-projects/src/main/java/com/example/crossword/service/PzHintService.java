package com.example.crossword.service;

import com.example.crossword.entity.PzHint;
import com.example.crossword.repository.PzHintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * PzHint 엔티티 관련 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PzHintService {
    
    private final PzHintRepository pzHintRepository;
    
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
}





