package com.example.crossword.service;

import com.example.crossword.entity.PuzzleLevel;
import com.example.crossword.repository.PuzzleLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 퍼즐 레벨 관리 서비스
 * 퍼즐 레벨 조회, 검색, 통계 등의 비즈니스 로직을 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PuzzleLevelService {
    
    private final PuzzleLevelRepository puzzleLevelRepository;
    
    /**
     * ID로 퍼즐 레벨 조회
     */
    public Optional<PuzzleLevel> getPuzzleLevelById(Long id) {
        log.debug("ID로 퍼즐 레벨 조회: {}", id);
        return puzzleLevelRepository.findById(id);
    }
    
    /**
     * 특정 레벨 번호로 퍼즐 레벨 조회
     */
    public Optional<PuzzleLevel> getPuzzleLevelByLevel(Integer level) {
        log.debug("레벨 번호로 퍼즐 레벨 조회: {}", level);
        return puzzleLevelRepository.findByLevel(level);
    }
    
    /**
     * 레벨 번호로 퍼즐 레벨 존재 여부 확인
     */
    public boolean existsByLevel(Integer level) {
        log.debug("레벨 번호 존재 여부 확인: {}", level);
        return puzzleLevelRepository.existsByLevel(level);
    }
    
    /**
     * 레벨 번호 순으로 모든 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getAllPuzzleLevelsOrdered() {
        log.debug("모든 퍼즐 레벨 조회 (레벨 순)");
        return puzzleLevelRepository.findAllByOrderByLevelAsc();
    }
    
    /**
     * 레벨 번호 순으로 모든 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getAllPuzzleLevelsOrdered(Pageable pageable) {
        log.debug("모든 퍼즐 레벨 페이징 조회 (레벨 순): {}", pageable);
        return puzzleLevelRepository.findAllByOrderByLevelAsc(pageable);
    }
    
    /**
     * 특정 단어 개수 이상의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByMinWordCount(Integer wordCount) {
        log.debug("최소 단어 개수별 퍼즐 레벨 조회: {}", wordCount);
        return puzzleLevelRepository.findByWordCountGreaterThanEqual(wordCount);
    }
    
    /**
     * 특정 단어 개수 이상의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByMinWordCount(Integer wordCount, Pageable pageable) {
        log.debug("최소 단어 개수별 퍼즐 레벨 페이징 조회: {}, {}", wordCount, pageable);
        return puzzleLevelRepository.findByWordCountGreaterThanEqual(wordCount, pageable);
    }
    
    /**
     * 특정 단어 개수 이하의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByMaxWordCount(Integer wordCount) {
        log.debug("최대 단어 개수별 퍼즐 레벨 조회: {}", wordCount);
        return puzzleLevelRepository.findByWordCountLessThanEqual(wordCount);
    }
    
    /**
     * 특정 단어 개수 이하의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByMaxWordCount(Integer wordCount, Pageable pageable) {
        log.debug("최대 단어 개수별 퍼즐 레벨 페이징 조회: {}, {}", wordCount, pageable);
        return puzzleLevelRepository.findByWordCountLessThanEqual(wordCount, pageable);
    }
    
    /**
     * 단어 개수 범위로 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByWordCountRange(Integer minWordCount, Integer maxWordCount) {
        log.debug("단어 개수 범위별 퍼즐 레벨 조회: {} - {}", minWordCount, maxWordCount);
        return puzzleLevelRepository.findByWordCountRange(minWordCount, maxWordCount);
    }
    
    /**
     * 단어 개수 범위로 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByWordCountRange(Integer minWordCount, Integer maxWordCount, Pageable pageable) {
        log.debug("단어 개수 범위별 퍼즐 레벨 페이징 조회: {} - {}, {}", minWordCount, maxWordCount, pageable);
        return puzzleLevelRepository.findByWordCountRange(minWordCount, maxWordCount, pageable);
    }
    
    /**
     * 특정 단어 난이도 이상의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByMinWordDifficulty(Integer wordDifficulty) {
        log.debug("최소 단어 난이도별 퍼즐 레벨 조회: {}", wordDifficulty);
        return puzzleLevelRepository.findByWordDifficultyGreaterThanEqual(wordDifficulty);
    }
    
    /**
     * 특정 단어 난이도 이상의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByMinWordDifficulty(Integer wordDifficulty, Pageable pageable) {
        log.debug("최소 단어 난이도별 퍼즐 레벨 페이징 조회: {}, {}", wordDifficulty, pageable);
        return puzzleLevelRepository.findByWordDifficultyGreaterThanEqual(wordDifficulty, pageable);
    }
    
    /**
     * 특정 단어 난이도 이하의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByMaxWordDifficulty(Integer wordDifficulty) {
        log.debug("최대 단어 난이도별 퍼즐 레벨 조회: {}", wordDifficulty);
        return puzzleLevelRepository.findByWordDifficultyLessThanEqual(wordDifficulty);
    }
    
    /**
     * 특정 단어 난이도 이하의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByMaxWordDifficulty(Integer wordDifficulty, Pageable pageable) {
        log.debug("최대 단어 난이도별 퍼즐 레벨 페이징 조회: {}, {}", wordDifficulty, pageable);
        return puzzleLevelRepository.findByWordDifficultyLessThanEqual(wordDifficulty, pageable);
    }
    
    /**
     * 특정 힌트 난이도 이상의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByMinHintDifficulty(Integer hintDifficulty) {
        log.debug("최소 힌트 난이도별 퍼즐 레벨 조회: {}", hintDifficulty);
        return puzzleLevelRepository.findByHintDifficultyGreaterThanEqual(hintDifficulty);
    }
    
    /**
     * 특정 힌트 난이도 이상의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByMinHintDifficulty(Integer hintDifficulty, Pageable pageable) {
        log.debug("최소 힌트 난이도별 퍼즐 레벨 페이징 조회: {}, {}", hintDifficulty, pageable);
        return puzzleLevelRepository.findByHintDifficultyGreaterThanEqual(hintDifficulty, pageable);
    }
    
    /**
     * 특정 힌트 난이도 이하의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByMaxHintDifficulty(Integer hintDifficulty) {
        log.debug("최대 힌트 난이도별 퍼즐 레벨 조회: {}", hintDifficulty);
        return puzzleLevelRepository.findByHintDifficultyLessThanEqual(hintDifficulty);
    }
    
    /**
     * 특정 힌트 난이도 이하의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByMaxHintDifficulty(Integer hintDifficulty, Pageable pageable) {
        log.debug("최대 힌트 난이도별 퍼즐 레벨 페이징 조회: {}, {}", hintDifficulty, pageable);
        return puzzleLevelRepository.findByHintDifficultyLessThanEqual(hintDifficulty, pageable);
    }
    
    /**
     * 특정 교차점 개수 이상의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByMinIntersectionCount(Integer intersectionCount) {
        log.debug("최소 교차점 개수별 퍼즐 레벨 조회: {}", intersectionCount);
        return puzzleLevelRepository.findByIntersectionCountGreaterThanEqual(intersectionCount);
    }
    
    /**
     * 특정 교차점 개수 이상의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByMinIntersectionCount(Integer intersectionCount, Pageable pageable) {
        log.debug("최소 교차점 개수별 퍼즐 레벨 페이징 조회: {}, {}", intersectionCount, pageable);
        return puzzleLevelRepository.findByIntersectionCountGreaterThanEqual(intersectionCount, pageable);
    }
    
    /**
     * 특정 교차점 개수 이하의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByMaxIntersectionCount(Integer intersectionCount) {
        log.debug("최대 교차점 개수별 퍼즐 레벨 조회: {}", intersectionCount);
        return puzzleLevelRepository.findByIntersectionCountLessThanEqual(intersectionCount);
    }
    
    /**
     * 특정 교차점 개수 이하의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByMaxIntersectionCount(Integer intersectionCount, Pageable pageable) {
        log.debug("최대 교차점 개수별 퍼즐 레벨 페이징 조회: {}, {}", intersectionCount, pageable);
        return puzzleLevelRepository.findByIntersectionCountLessThanEqual(intersectionCount, pageable);
    }
    
    /**
     * 특정 시간 제한 이상의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByMinTimeLimit(Integer timeLimit) {
        log.debug("최소 시간 제한별 퍼즐 레벨 조회: {}", timeLimit);
        return puzzleLevelRepository.findByTimeLimitGreaterThanEqual(timeLimit);
    }
    
    /**
     * 특정 시간 제한 이상의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByMinTimeLimit(Integer timeLimit, Pageable pageable) {
        log.debug("최소 시간 제한별 퍼즐 레벨 페이징 조회: {}, {}", timeLimit, pageable);
        return puzzleLevelRepository.findByTimeLimitGreaterThanEqual(timeLimit, pageable);
    }
    
    /**
     * 특정 시간 제한 이하의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByMaxTimeLimit(Integer timeLimit) {
        log.debug("최대 시간 제한별 퍼즐 레벨 조회: {}", timeLimit);
        return puzzleLevelRepository.findByTimeLimitLessThanEqual(timeLimit);
    }
    
    /**
     * 특정 시간 제한 이하의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByMaxTimeLimit(Integer timeLimit, Pageable pageable) {
        log.debug("최대 시간 제한별 퍼즐 레벨 페이징 조회: {}, {}", timeLimit, pageable);
        return puzzleLevelRepository.findByTimeLimitLessThanEqual(timeLimit, pageable);
    }
    
    /**
     * 특정 클리어 조건의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByClearCondition(Integer clearCondition) {
        log.debug("클리어 조건별 퍼즐 레벨 조회: {}", clearCondition);
        return puzzleLevelRepository.findByClearCondition(clearCondition);
    }
    
    /**
     * 특정 클리어 조건의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByClearCondition(Integer clearCondition, Pageable pageable) {
        log.debug("클리어 조건별 퍼즐 레벨 페이징 조회: {}, {}", clearCondition, pageable);
        return puzzleLevelRepository.findByClearCondition(clearCondition, pageable);
    }
    
    /**
     * 특정 업데이트자로 업데이트된 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByUpdatedBy(String updatedBy) {
        log.debug("업데이트자별 퍼즐 레벨 조회: {}", updatedBy);
        return puzzleLevelRepository.findByUpdatedBy(updatedBy);
    }
    
    /**
     * 특정 업데이트자로 업데이트된 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByUpdatedBy(String updatedBy, Pageable pageable) {
        log.debug("업데이트자별 퍼즐 레벨 페이징 조회: {}, {}", updatedBy, pageable);
        return puzzleLevelRepository.findByUpdatedBy(updatedBy, pageable);
    }
    
    /**
     * 레벨명에 특정 문자열이 포함된 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> searchPuzzleLevelsByName(String levelName) {
        log.debug("레벨명 검색: {}", levelName);
        return puzzleLevelRepository.findByLevelNameContainingIgnoreCase(levelName);
    }
    
    /**
     * 레벨명에 특정 문자열이 포함된 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> searchPuzzleLevelsByName(String levelName, Pageable pageable) {
        log.debug("레벨명 검색 페이징: {}, {}", levelName, pageable);
        return puzzleLevelRepository.findByLevelNameContainingIgnoreCase(levelName, pageable);
    }
    
    /**
     * 가장 높은 레벨 번호 조회
     */
    public Optional<Integer> getMaxLevel() {
        log.debug("최대 레벨 번호 조회");
        return puzzleLevelRepository.findMaxLevel();
    }
    
    /**
     * 가장 낮은 레벨 번호 조회
     */
    public Optional<Integer> getMinLevel() {
        log.debug("최소 레벨 번호 조회");
        return puzzleLevelRepository.findMinLevel();
    }
    
    /**
     * 특정 레벨 범위의 퍼즐 레벨 조회
     */
    public List<PuzzleLevel> getPuzzleLevelsByLevelRange(Integer minLevel, Integer maxLevel) {
        log.debug("레벨 범위별 퍼즐 레벨 조회: {} - {}", minLevel, maxLevel);
        return puzzleLevelRepository.findByLevelRange(minLevel, maxLevel);
    }
    
    /**
     * 특정 레벨 범위의 퍼즐 레벨을 페이징으로 조회
     */
    public Page<PuzzleLevel> getPuzzleLevelsByLevelRange(Integer minLevel, Integer maxLevel, Pageable pageable) {
        log.debug("레벨 범위별 퍼즐 레벨 페이징 조회: {} - {}, {}", minLevel, maxLevel, pageable);
        return puzzleLevelRepository.findByLevelRange(minLevel, maxLevel, pageable);
    }
    
    /**
     * 단어 개수별 퍼즐 레벨 개수 조회
     */
    public List<Object[]> getPuzzleLevelCountByWordCount() {
        log.debug("단어 개수별 퍼즐 레벨 개수 조회");
        return puzzleLevelRepository.countLevelsByWordCount();
    }
    
    /**
     * 단어 난이도별 퍼즐 레벨 개수 조회
     */
    public List<Object[]> getPuzzleLevelCountByWordDifficulty() {
        log.debug("단어 난이도별 퍼즐 레벨 개수 조회");
        return puzzleLevelRepository.countLevelsByWordDifficulty();
    }
    
    /**
     * 힌트 난이도별 퍼즐 레벨 개수 조회
     */
    public List<Object[]> getPuzzleLevelCountByHintDifficulty() {
        log.debug("힌트 난이도별 퍼즐 레벨 개수 조회");
        return puzzleLevelRepository.countLevelsByHintDifficulty();
    }
    
    /**
     * 교차점 개수별 퍼즐 레벨 개수 조회
     */
    public List<Object[]> getPuzzleLevelCountByIntersectionCount() {
        log.debug("교차점 개수별 퍼즐 레벨 개수 조회");
        return puzzleLevelRepository.countLevelsByIntersectionCount();
    }
    
    /**
     * 시간 제한별 퍼즐 레벨 개수 조회
     */
    public List<Object[]> getPuzzleLevelCountByTimeLimit() {
        log.debug("시간 제한별 퍼즐 레벨 개수 조회");
        return puzzleLevelRepository.countLevelsByTimeLimit();
    }
    
    /**
     * 클리어 조건별 퍼즐 레벨 개수 조회
     */
    public List<Object[]> getPuzzleLevelCountByClearCondition() {
        log.debug("클리어 조건별 퍼즐 레벨 개수 조회");
        return puzzleLevelRepository.countLevelsByClearCondition();
    }
    
    /**
     * 업데이트자별 퍼즐 레벨 개수 조회
     */
    public List<Object[]> getPuzzleLevelCountByUpdatedBy() {
        log.debug("업데이트자별 퍼즐 레벨 개수 조회");
        return puzzleLevelRepository.countLevelsByUpdatedBy();
    }
    
    /**
     * 전체 퍼즐 레벨 개수 조회
     */
    public long getTotalPuzzleLevelCount() {
        log.debug("전체 퍼즐 레벨 개수 조회");
        return puzzleLevelRepository.count();
    }
    
    /**
     * 특정 단어 개수의 퍼즐 레벨 개수 조회
     */
    public long getPuzzleLevelCountByWordCount(Integer wordCount) {
        log.debug("특정 단어 개수의 퍼즐 레벨 개수 조회: {}", wordCount);
        return puzzleLevelRepository.countByWordCount(wordCount);
    }
    
    /**
     * 특정 단어 난이도의 퍼즐 레벨 개수 조회
     */
    public long getPuzzleLevelCountByWordDifficulty(Integer wordDifficulty) {
        log.debug("특정 단어 난이도의 퍼즐 레벨 개수 조회: {}", wordDifficulty);
        return puzzleLevelRepository.countByWordDifficulty(wordDifficulty);
    }
    
    /**
     * 특정 힌트 난이도의 퍼즐 레벨 개수 조회
     */
    public long getPuzzleLevelCountByHintDifficulty(Integer hintDifficulty) {
        log.debug("특정 힌트 난이도의 퍼즐 레벨 개수 조회: {}", hintDifficulty);
        return puzzleLevelRepository.countByHintDifficulty(hintDifficulty);
    }
    
    /**
     * 특정 교차점 개수의 퍼즐 레벨 개수 조회
     */
    public long getPuzzleLevelCountByIntersectionCount(Integer intersectionCount) {
        log.debug("특정 교차점 개수의 퍼즐 레벨 개수 조회: {}", intersectionCount);
        return puzzleLevelRepository.countByIntersectionCount(intersectionCount);
    }
    
    /**
     * 특정 시간 제한의 퍼즐 레벨 개수 조회
     */
    public long getPuzzleLevelCountByTimeLimit(Integer timeLimit) {
        log.debug("특정 시간 제한의 퍼즐 레벨 개수 조회: {}", timeLimit);
        return puzzleLevelRepository.countByTimeLimit(timeLimit);
    }
    
    /**
     * 특정 클리어 조건의 퍼즐 레벨 개수 조회
     */
    public long getPuzzleLevelCountByClearCondition(Integer clearCondition) {
        log.debug("특정 클리어 조건의 퍼즐 레벨 개수 조회: {}", clearCondition);
        return puzzleLevelRepository.countByClearCondition(clearCondition);
    }
    
    /**
     * 퍼즐 레벨 저장
     */
    @Transactional
    public PuzzleLevel savePuzzleLevel(PuzzleLevel puzzleLevel) {
        log.debug("퍼즐 레벨 저장: {}", puzzleLevel.getLevelName());
        return puzzleLevelRepository.save(puzzleLevel);
    }
    
    /**
     * 퍼즐 레벨 업데이트
     */
    @Transactional
    public PuzzleLevel updatePuzzleLevel(PuzzleLevel puzzleLevel) {
        log.debug("퍼즐 레벨 업데이트: {}", puzzleLevel.getLevelName());
        return puzzleLevelRepository.save(puzzleLevel);
    }
    
    /**
     * 퍼즐 레벨 삭제
     */
    @Transactional
    public void deletePuzzleLevel(Long id) {
        log.debug("퍼즐 레벨 삭제: {}", id);
        puzzleLevelRepository.deleteById(id);
    }
    
    /**
     * 퍼즐 레벨 존재 여부 확인
     */
    public boolean existsPuzzleLevel(Long id) {
        log.debug("퍼즐 레벨 존재 여부 확인: {}", id);
        return puzzleLevelRepository.existsById(id);
    }
    
    /**
     * 퍼즐 레벨 업데이트자 설정
     */
    @Transactional
    public void updatePuzzleLevelUpdatedBy(Long id, String updatedBy) {
        log.debug("퍼즐 레벨 업데이트자 설정: {}, {}", id, updatedBy);
        puzzleLevelRepository.findById(id).ifPresent(puzzleLevel -> {
            puzzleLevel.setUpdatedBy(updatedBy);
            puzzleLevelRepository.save(puzzleLevel);
        });
    }
    
    /**
     * 퍼즐 레벨 클리어 조건 업데이트
     */
    @Transactional
    public void updatePuzzleLevelClearCondition(Long id, Integer clearCondition) {
        log.debug("퍼즐 레벨 클리어 조건 업데이트: {}, {}", id, clearCondition);
        puzzleLevelRepository.findById(id).ifPresent(puzzleLevel -> {
            puzzleLevel.setClearCondition(clearCondition);
            puzzleLevelRepository.save(puzzleLevel);
        });
    }
    
    /**
     * 레벨 번호로 퍼즐 레벨 조회 (퍼즐 게임용)
     */
    public PuzzleLevel getByLevel(Integer level) {
        log.debug("레벨 번호로 퍼즐 레벨 조회: {}", level);
        return puzzleLevelRepository.findByLevel(level).orElse(null);
    }
}
