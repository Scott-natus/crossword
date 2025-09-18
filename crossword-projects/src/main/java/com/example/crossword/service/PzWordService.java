package com.example.crossword.service;

import com.example.crossword.entity.PzWord;
import com.example.crossword.repository.PzWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
}
