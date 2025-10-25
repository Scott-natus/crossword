package com.example.board.service;

import com.example.board.entity.Hint;
import com.example.board.entity.Word;
import com.example.board.repository.HintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 힌트 관리 서비스
 * 힌트 조회, 검색, 통계 등의 비즈니스 로직을 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HintService {
    
    private final HintRepository hintRepository;
    
    /**
     * ID로 힌트 조회
     */
    public Optional<Hint> getHintById(Integer id) {
        log.debug("ID로 힌트 조회: {}", id);
        return hintRepository.findById(id);
    }
    
    /**
     * 특정 단어의 모든 힌트 조회
     */
    public List<Hint> getHintsByWord(Word word) {
        log.debug("단어별 힌트 조회: {}", word.getWord());
        return hintRepository.findByWord(word);
    }
    
    /**
     * 특정 단어의 모든 힌트를 페이징으로 조회
     */
    public Page<Hint> getHintsByWord(Word word, Pageable pageable) {
        log.debug("단어별 힌트 페이징 조회: {}, {}", word.getWord(), pageable);
        return hintRepository.findByWord(word, pageable);
    }
    
    /**
     * 특정 단어 ID의 모든 힌트 조회
     */
    public List<Hint> getHintsByWordId(Integer wordId) {
        log.debug("단어 ID별 힌트 조회: {}", wordId);
        return hintRepository.findByWordId(wordId);
    }
    
    /**
     * 특정 단어 ID의 모든 힌트를 페이징으로 조회
     */
    public Page<Hint> getHintsByWordId(Integer wordId, Pageable pageable) {
        log.debug("단어 ID별 힌트 페이징 조회: {}, {}", wordId, pageable);
        return hintRepository.findByWordId(wordId, pageable);
    }
    
    /**
     * 특정 단어의 주 힌트 조회
     */
    public Optional<Hint> getPrimaryHintByWord(Word word) {
        log.debug("단어의 주 힌트 조회: {}", word.getWord());
        return hintRepository.findByWordAndIsPrimaryTrue(word);
    }
    
    /**
     * 특정 단어 ID의 주 힌트 조회
     */
    public Optional<Hint> getPrimaryHintByWordId(Integer wordId) {
        log.debug("단어 ID의 주 힌트 조회: {}", wordId);
        return hintRepository.findByWordIdAndIsPrimaryTrue(wordId);
    }
    
    /**
     * 특정 힌트 타입의 힌트 조회
     */
    public List<Hint> getHintsByType(String hintType) {
        log.debug("힌트 타입별 조회: {}", hintType);
        return hintRepository.findByHintType(hintType);
    }
    
    /**
     * 특정 힌트 타입의 힌트를 페이징으로 조회
     */
    public Page<Hint> getHintsByType(String hintType, Pageable pageable) {
        log.debug("힌트 타입별 페이징 조회: {}, {}", hintType, pageable);
        return hintRepository.findByHintType(hintType, pageable);
    }
    
    /**
     * 특정 단어의 특정 힌트 타입 조회
     */
    public List<Hint> getHintsByWordAndType(Word word, String hintType) {
        log.debug("단어와 힌트 타입별 조회: {}, {}", word.getWord(), hintType);
        return hintRepository.findByWordAndHintType(word, hintType);
    }
    
    /**
     * 특정 단어 ID의 특정 힌트 타입 조회
     */
    public List<Hint> getHintsByWordIdAndType(Integer wordId, String hintType) {
        log.debug("단어 ID와 힌트 타입별 조회: {}, {}", wordId, hintType);
        return hintRepository.findByWordIdAndHintType(wordId, hintType);
    }
    
    /**
     * 특정 난이도의 힌트 조회
     */
    public List<Hint> getHintsByDifficulty(Integer difficulty) {
        log.debug("난이도별 힌트 조회: {}", difficulty);
        return hintRepository.findByDifficulty(difficulty);
    }
    
    /**
     * 특정 난이도의 힌트를 페이징으로 조회
     */
    public Page<Hint> getHintsByDifficulty(Integer difficulty, Pageable pageable) {
        log.debug("난이도별 힌트 페이징 조회: {}, {}", difficulty, pageable);
        return hintRepository.findByDifficulty(difficulty, pageable);
    }
    
    /**
     * 특정 단어의 특정 난이도 힌트 조회
     */
    public List<Hint> getHintsByWordAndDifficulty(Word word, Integer difficulty) {
        log.debug("단어와 난이도별 힌트 조회: {}, {}", word.getWord(), difficulty);
        return hintRepository.findByWordAndDifficulty(word, difficulty);
    }
    
    /**
     * 특정 단어 ID의 특정 난이도 힌트 조회
     */
    public List<Hint> getHintsByWordIdAndDifficulty(Integer wordId, Integer difficulty) {
        log.debug("단어 ID와 난이도별 힌트 조회: {}, {}", wordId, difficulty);
        return hintRepository.findByWordIdAndDifficulty(wordId, difficulty);
    }
    
    /**
     * 수정된 힌트 조회
     */
    public List<Hint> getHintsByCorrectionStatus(String correctionStatus) {
        log.debug("수정 상태별 힌트 조회: {}", correctionStatus);
        return hintRepository.findByCorrectionStatus(correctionStatus);
    }
    
    /**
     * 수정된 힌트를 페이징으로 조회
     */
    public Page<Hint> getHintsByCorrectionStatus(String correctionStatus, Pageable pageable) {
        log.debug("수정 상태별 힌트 페이징 조회: {}, {}", correctionStatus, pageable);
        return hintRepository.findByCorrectionStatus(correctionStatus, pageable);
    }
    
    /**
     * 특정 단어의 수정된 힌트 조회
     */
    public List<Hint> getHintsByWordAndCorrectionStatus(Word word, String correctionStatus) {
        log.debug("단어와 수정 상태별 힌트 조회: {}, {}", word.getWord(), correctionStatus);
        return hintRepository.findByWordAndCorrectionStatus(word, correctionStatus);
    }
    
    /**
     * 특정 단어 ID의 수정된 힌트 조회
     */
    public List<Hint> getHintsByWordIdAndCorrectionStatus(Integer wordId, String correctionStatus) {
        log.debug("단어 ID와 수정 상태별 힌트 조회: {}, {}", wordId, correctionStatus);
        return hintRepository.findByWordIdAndCorrectionStatus(wordId, correctionStatus);
    }
    
    /**
     * 힌트 텍스트에 특정 문자열이 포함된 힌트 조회
     */
    public List<Hint> searchHintsByText(String hintText) {
        log.debug("힌트 텍스트 검색: {}", hintText);
        return hintRepository.findByHintTextContainingIgnoreCase(hintText);
    }
    
    /**
     * 힌트 텍스트에 특정 문자열이 포함된 힌트를 페이징으로 조회
     */
    public Page<Hint> searchHintsByText(String hintText, Pageable pageable) {
        log.debug("힌트 텍스트 검색 페이징: {}, {}", hintText, pageable);
        return hintRepository.findByHintTextContainingIgnoreCase(hintText, pageable);
    }
    
    /**
     * 특정 단어의 힌트 텍스트에 특정 문자열이 포함된 힌트 조회
     */
    public List<Hint> searchHintsByWordAndText(Word word, String hintText) {
        log.debug("단어와 힌트 텍스트 검색: {}, {}", word.getWord(), hintText);
        return hintRepository.findByWordAndHintTextContainingIgnoreCase(word, hintText);
    }
    
    /**
     * 특정 단어 ID의 힌트 텍스트에 특정 문자열이 포함된 힌트 조회
     */
    public List<Hint> searchHintsByWordIdAndText(Integer wordId, String hintText) {
        log.debug("단어 ID와 힌트 텍스트 검색: {}, {}", wordId, hintText);
        return hintRepository.findByWordIdAndHintTextContainingIgnoreCase(wordId, hintText);
    }
    
    /**
     * 이미지 URL이 있는 힌트 조회
     */
    public List<Hint> getHintsWithImage() {
        log.debug("이미지 힌트 조회");
        return hintRepository.findHintsWithImage();
    }
    
    /**
     * 이미지 URL이 있는 힌트를 페이징으로 조회
     */
    public Page<Hint> getHintsWithImage(Pageable pageable) {
        log.debug("이미지 힌트 페이징 조회: {}", pageable);
        return hintRepository.findHintsWithImage(pageable);
    }
    
    /**
     * 오디오 URL이 있는 힌트 조회
     */
    public List<Hint> getHintsWithAudio() {
        log.debug("오디오 힌트 조회");
        return hintRepository.findHintsWithAudio();
    }
    
    /**
     * 오디오 URL이 있는 힌트를 페이징으로 조회
     */
    public Page<Hint> getHintsWithAudio(Pageable pageable) {
        log.debug("오디오 힌트 페이징 조회: {}", pageable);
        return hintRepository.findHintsWithAudio(pageable);
    }
    
    /**
     * 특정 단어의 랜덤 힌트 조회
     */
    public Optional<Hint> getRandomHintByWordId(Integer wordId) {
        log.debug("단어 ID의 랜덤 힌트 조회: {}", wordId);
        return hintRepository.findRandomHintByWordId(wordId);
    }
    
    /**
     * 특정 단어의 특정 힌트 타입 랜덤 힌트 조회
     */
    public Optional<Hint> getRandomHintByWordIdAndType(Integer wordId, String hintType) {
        log.debug("단어 ID와 힌트 타입의 랜덤 힌트 조회: {}, {}", wordId, hintType);
        return hintRepository.findRandomHintByWordIdAndType(wordId, hintType);
    }
    
    /**
     * 특정 단어의 특정 난이도 랜덤 힌트 조회
     */
    public Optional<Hint> getRandomHintByWordIdAndDifficulty(Integer wordId, Integer difficulty) {
        log.debug("단어 ID와 난이도의 랜덤 힌트 조회: {}, {}", wordId, difficulty);
        return hintRepository.findRandomHintByWordIdAndDifficulty(wordId, difficulty);
    }
    
    /**
     * 힌트 타입별 힌트 개수 조회
     */
    public List<Object[]> getHintCountByType() {
        log.debug("힌트 타입별 개수 조회");
        return hintRepository.countHintsByType();
    }
    
    /**
     * 난이도별 힌트 개수 조회
     */
    public List<Object[]> getHintCountByDifficulty() {
        log.debug("난이도별 힌트 개수 조회");
        return hintRepository.countHintsByDifficulty();
    }
    
    /**
     * 수정 상태별 힌트 개수 조회
     */
    public List<Object[]> getHintCountByCorrectionStatus() {
        log.debug("수정 상태별 힌트 개수 조회");
        return hintRepository.countHintsByCorrectionStatus();
    }
    
    /**
     * 특정 단어의 힌트 개수 조회
     */
    public long getHintCountByWord(Word word) {
        log.debug("단어별 힌트 개수 조회: {}", word.getWord());
        return hintRepository.countByWord(word);
    }
    
    /**
     * 특정 단어 ID의 힌트 개수 조회
     */
    public long getHintCountByWordId(Integer wordId) {
        log.debug("단어 ID별 힌트 개수 조회: {}", wordId);
        return hintRepository.countByWordId(wordId);
    }
    
    /**
     * 특정 단어의 특정 힌트 타입 개수 조회
     */
    public long getHintCountByWordAndType(Word word, String hintType) {
        log.debug("단어와 힌트 타입별 개수 조회: {}, {}", word.getWord(), hintType);
        return hintRepository.countByWordAndHintType(word, hintType);
    }
    
    /**
     * 특정 단어 ID의 특정 힌트 타입 개수 조회
     */
    public long getHintCountByWordIdAndType(Integer wordId, String hintType) {
        log.debug("단어 ID와 힌트 타입별 개수 조회: {}, {}", wordId, hintType);
        return hintRepository.countByWordIdAndHintType(wordId, hintType);
    }
    
    /**
     * 특정 단어의 주 힌트 개수 조회
     */
    public long getPrimaryHintCountByWord(Word word) {
        log.debug("단어의 주 힌트 개수 조회: {}", word.getWord());
        return hintRepository.countByWordAndIsPrimaryTrue(word);
    }
    
    /**
     * 특정 단어 ID의 주 힌트 개수 조회
     */
    public long getPrimaryHintCountByWordId(Integer wordId) {
        log.debug("단어 ID의 주 힌트 개수 조회: {}", wordId);
        return hintRepository.countByWordIdAndIsPrimaryTrue(wordId);
    }
    
    /**
     * 특정 단어의 수정된 힌트 개수 조회
     */
    public long getCorrectedHintCountByWord(Word word) {
        log.debug("단어의 수정된 힌트 개수 조회: {}", word.getWord());
        return hintRepository.countByWordAndCorrectionStatus(word, "y");
    }
    
    /**
     * 특정 단어 ID의 수정된 힌트 개수 조회
     */
    public long getCorrectedHintCountByWordId(Integer wordId) {
        log.debug("단어 ID의 수정된 힌트 개수 조회: {}", wordId);
        return hintRepository.countByWordIdAndCorrectionStatus(wordId, "y");
    }
    
    /**
     * 힌트 저장
     */
    @Transactional
    public Hint saveHint(Hint hint) {
        log.debug("힌트 저장: {}", hint.getHintText());
        return hintRepository.save(hint);
    }
    
    /**
     * 힌트 업데이트
     */
    @Transactional
    public Hint updateHint(Hint hint) {
        log.debug("힌트 업데이트: {}", hint.getHintText());
        return hintRepository.save(hint);
    }
    
    /**
     * 힌트 삭제
     */
    @Transactional
    public void deleteHint(Integer id) {
        log.debug("힌트 삭제: {}", id);
        hintRepository.deleteById(id);
    }
    
    /**
     * 힌트 수정 상태 업데이트
     */
    @Transactional
    public void updateHintCorrectionStatus(Integer id, String correctionStatus) {
        log.debug("힌트 수정 상태 업데이트: {}, {}", id, correctionStatus);
        hintRepository.findById(id).ifPresent(hint -> {
            hint.setCorrectionStatus(correctionStatus);
            hintRepository.save(hint);
        });
    }
    
    /**
     * 힌트 난이도 업데이트
     */
    @Transactional
    public void updateHintDifficulty(Integer id, Integer difficulty) {
        log.debug("힌트 난이도 업데이트: {}, {}", id, difficulty);
        hintRepository.findById(id).ifPresent(hint -> {
            hint.setDifficulty(difficulty);
            hintRepository.save(hint);
        });
    }
    
    /**
     * 힌트를 주 힌트로 설정
     */
    @Transactional
    public void setAsPrimaryHint(Integer id) {
        log.debug("힌트를 주 힌트로 설정: {}", id);
        hintRepository.findById(id).ifPresent(hint -> {
            // 같은 단어의 다른 힌트들을 주 힌트에서 제거
            hintRepository.findByWordIdAndIsPrimaryTrue(hint.getWord().getId())
                .ifPresent(existingPrimary -> {
                    existingPrimary.setIsPrimary(false);
                    hintRepository.save(existingPrimary);
                });
            
            // 현재 힌트를 주 힌트로 설정
            hint.setIsPrimary(true);
            hintRepository.save(hint);
        });
    }
    
    /**
     * 힌트 존재 여부 확인
     */
    public boolean existsHint(Integer id) {
        log.debug("힌트 존재 여부 확인: {}", id);
        return hintRepository.existsById(id);
    }
}
