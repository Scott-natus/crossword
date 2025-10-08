package com.example.crossword.service;

import com.example.crossword.entity.PzWord;
import com.example.crossword.entity.PzHint;
import com.example.crossword.repository.PzWordRepository;
import com.example.crossword.repository.PzHintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 단어 관리 서비스
 * 라라벨 퍼즐 관리 시스템과 동일한 기능을 제공
 */
@Service
@Transactional
public class WordManagementService {

    private static final Logger logger = LoggerFactory.getLogger(WordManagementService.class);

    @Autowired
    private PzWordRepository pzWordRepository;

    @Autowired
    private PzHintRepository pzHintRepository;

    /**
     * 통계 정보 조회 (라라벨과 동일)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 전체 단어 수
        long totalWords = pzWordRepository.count();
        stats.put("totalWords", totalWords);
        
        // 활성 단어 수
        long activeWords = pzWordRepository.countByIsActiveTrue();
        stats.put("activeWords", activeWords);
        
        // 힌트가 있는 단어 수
        long wordsWithHints = pzWordRepository.countWordsWithHints();
        stats.put("wordsWithHints", wordsWithHints);
        
        // 전체 힌트 수
        long totalHints = pzHintRepository.count();
        stats.put("totalHints", totalHints);
        
        return stats;
    }

    /**
     * 관리자 페이지 통계 조회
     */
    public Map<String, Object> getAdminStats() {
        return getStatistics();
    }

    /**
     * 단어 목록 조회 (DataTables용)
     */
    public Map<String, Object> getWordsData(int draw, int start, int length, String search, String difficultyFilter, String refinement) {
        Map<String, Object> response = new HashMap<>();
        response.put("draw", draw);
        
        // 디버깅 로그 추가
        logger.info("=== 단어 검색 파라미터 ===");
        logger.info("search: '{}'", search);
        logger.info("difficultyFilter: '{}'", difficultyFilter);
        logger.info("refinement: '{}'", refinement);
        
        try {
            // 정렬 설정 (생성일자 내림차순)
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            Pageable pageable = PageRequest.of(start / length, length, sort);
            
            Page<PzWord> page;
            
            // 검색어, 난이도 필터, 정제상태 필터에 따른 조회
            // null 체크와 빈 문자열 체크를 모두 고려
            boolean hasSearch = search != null && !search.trim().isEmpty();
            boolean hasDifficulty = difficultyFilter != null && !difficultyFilter.trim().isEmpty();
            boolean hasRefinement = refinement != null && !refinement.trim().isEmpty();
            
            logger.info("필터 조건 체크 - search: '{}', difficulty: '{}', refinement: '{}'", search, difficultyFilter, refinement);
            logger.info("필터 존재 여부 - hasSearch: {}, hasDifficulty: {}, hasRefinement: {}", hasSearch, hasDifficulty, hasRefinement);
            
            if (hasSearch && hasDifficulty && hasRefinement) {
                // 검색어 + 난이도 필터 + 정제상태 필터
                logger.info("조건: 검색어 + 난이도 + 정제상태");
                Integer difficulty = parseDifficulty(difficultyFilter);
                page = pzWordRepository.findByWordContainingIgnoreCaseAndDifficultyAndConfYn(search, difficulty, refinement, pageable);
            } else if (hasSearch && hasDifficulty) {
                // 검색어 + 난이도 필터
                logger.info("조건: 검색어 + 난이도");
                Integer difficulty = parseDifficulty(difficultyFilter);
                page = pzWordRepository.findByWordContainingIgnoreCaseAndDifficulty(search, difficulty, pageable);
            } else if (hasSearch && hasRefinement) {
                // 검색어 + 정제상태 필터
                logger.info("조건: 검색어 + 정제상태");
                page = pzWordRepository.findByWordContainingIgnoreCaseAndConfYn(search, refinement, pageable);
            } else if (hasDifficulty && hasRefinement) {
                // 난이도 필터 + 정제상태 필터
                logger.info("조건: 난이도 + 정제상태");
                Integer difficulty = parseDifficulty(difficultyFilter);
                page = pzWordRepository.findByDifficultyAndConfYn(difficulty, refinement, pageable);
            } else if (hasSearch) {
                // 검색어만
                logger.info("조건: 검색어만");
                page = pzWordRepository.findByWordContainingIgnoreCase(search, pageable);
            } else if (hasDifficulty) {
                // 난이도 필터만
                logger.info("조건: 난이도만");
                Integer difficulty = parseDifficulty(difficultyFilter);
                page = pzWordRepository.findByDifficulty(difficulty, pageable);
            } else if (hasRefinement) {
                // 정제상태 필터만
                logger.info("조건: 정제상태만");
                page = pzWordRepository.findByConfYn(refinement, pageable);
            } else {
                // 전체 조회
                logger.info("조건: 전체 조회");
                page = pzWordRepository.findAll(pageable);
            }
            
            // DataTables 응답 데이터 구성
            response.put("recordsTotal", pzWordRepository.count());
            response.put("recordsFiltered", page.getTotalElements());
            
            // 단어 데이터 변환
            List<Map<String, Object>> data = page.getContent().stream()
                .map(this::convertWordToMap)
                .collect(Collectors.toList());
            
            response.put("data", data);
            
        } catch (Exception e) {
            System.err.println("단어 데이터 조회 중 오류: " + e.getMessage());
            response.put("recordsTotal", 0);
            response.put("recordsFiltered", 0);
            response.put("data", new ArrayList<>());
        }
        
        return response;
    }

    /**
     * 단어 추가
     */
    public PzWord addWord(String word, String category, Integer difficulty) {
        // 중복 검사
        if (pzWordRepository.existsByWord(word)) {
            throw new IllegalArgumentException("이미 존재하는 단어입니다: " + word);
        }
        
        PzWord newWord = new PzWord();
        newWord.setWord(word);
        newWord.setCategory(category);
        newWord.setDifficulty(difficulty);
        newWord.setIsActive(true);
        
        return pzWordRepository.save(newWord);
    }

    /**
     * 단어 수정
     */
    public PzWord updateWord(Integer id, String word, String category, Integer difficulty, Boolean isActive) {
        PzWord existingWord = pzWordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("단어를 찾을 수 없습니다: " + id));
        
        // 단어 변경 시 중복 검사
        if (!existingWord.getWord().equals(word) && pzWordRepository.existsByWord(word)) {
            throw new IllegalArgumentException("이미 존재하는 단어입니다: " + word);
        }
        
        existingWord.setWord(word);
        existingWord.setCategory(category);
        existingWord.setDifficulty(difficulty);
        existingWord.setIsActive(isActive);
        
        return pzWordRepository.save(existingWord);
    }

    /**
     * 단어 삭제 (비활성화로 변경됨)
     * @deprecated deleteWord는 deactivateWord로 리다이렉트됩니다.
     */
    public void deleteWord(Integer id) {
        // 기존 삭제 메서드를 비활성화로 리다이렉트
        deactivateWord(id);
    }

    /**
     * 단어 비활성화
     */
    public void deactivateWord(Integer id) {
        Optional<PzWord> wordOpt = pzWordRepository.findById(id);
        if (!wordOpt.isPresent()) {
            throw new IllegalArgumentException("단어를 찾을 수 없습니다: " + id);
        }
        
        PzWord word = wordOpt.get();
        word.setIsActive(false);
        pzWordRepository.save(word);
    }

    /**
     * 일괄 난이도 변경
     */
    public int updateDifficultyBatch(List<Integer> wordIds, Integer newDifficulty) {
        if (wordIds == null || wordIds.isEmpty()) {
            return 0;
        }
        
        // 실제로는 JPA의 @Modifying 쿼리를 사용해야 하지만, 
        // 여기서는 개별 업데이트로 처리
        int updatedCount = 0;
        for (Integer wordId : wordIds) {
            Optional<PzWord> wordOpt = pzWordRepository.findById(wordId);
            if (wordOpt.isPresent()) {
                PzWord word = wordOpt.get();
                word.setDifficulty(newDifficulty);
                pzWordRepository.save(word);
                updatedCount++;
            }
        }
        
        return updatedCount;
    }

    /**
     * 일괄 활성화 상태 변경
     */
    public int updateActiveStatusBatch(List<Integer> wordIds, Boolean isActive) {
        if (wordIds == null || wordIds.isEmpty()) {
            return 0;
        }
        
        int updatedCount = 0;
        for (Integer wordId : wordIds) {
            Optional<PzWord> wordOpt = pzWordRepository.findById(wordId);
            if (wordOpt.isPresent()) {
                PzWord word = wordOpt.get();
                word.setIsActive(isActive);
                pzWordRepository.save(word);
                updatedCount++;
            }
        }
        
        return updatedCount;
    }

    /**
     * 단어 ID로 조회
     */
    public Optional<PzWord> getWordById(Integer id) {
        return pzWordRepository.findById(id);
    }

    /**
     * 카테고리 목록 조회
     */
    public List<String> getCategories() {
        return pzWordRepository.findDistinctCategories();
    }

    /**
     * 난이도별 통계 조회
     */
    public List<Object[]> getDifficultyStats() {
        return pzWordRepository.getDifficultyStats();
    }

    /**
     * 단어를 Map으로 변환 (DataTables용)
     */
    private Map<String, Object> convertWordToMap(PzWord word) {
        Map<String, Object> wordMap = new HashMap<>();
        wordMap.put("id", word.getId());
        wordMap.put("word", word.getWord());
        wordMap.put("length", word.getLength());
        wordMap.put("category", word.getCategory());
        wordMap.put("difficulty", word.getDifficulty());
        wordMap.put("difficulty_text", word.getDifficultyText());
        wordMap.put("is_active", word.getIsActive());
        wordMap.put("conf_yn", word.getConfYn());
        wordMap.put("created_at", word.getCreatedAt());
        wordMap.put("updated_at", word.getUpdatedAt());
        
        // 힌트 개수 조회
        List<PzHint> hints = pzHintRepository.findByWordIdOrderById(word.getId());
        wordMap.put("hints_count", hints.size());
        
        // 최신 힌트 생성일자
        if (!hints.isEmpty()) {
            wordMap.put("latest_hint_date", hints.get(hints.size() - 1).getCreatedAt());
        } else {
            wordMap.put("latest_hint_date", null);
        }
        
        return wordMap;
    }

    /**
     * 난이도 문자열을 숫자로 변환
     */
    private Integer parseDifficulty(String difficultyFilter) {
        logger.info("parseDifficulty 호출됨: '{}'", difficultyFilter);
        
        // 쉼표로 구분된 값이 있으면 첫 번째 값만 사용
        if (difficultyFilter.contains(",")) {
            difficultyFilter = difficultyFilter.split(",")[0].trim();
            logger.info("쉼표 구분 값 처리: '{}'", difficultyFilter);
        }
        
        Integer result = null;
        switch (difficultyFilter) {
            case "1":
            case "쉬움": 
                result = PzWord.DIFFICULTY_EASY;
                break;
            case "2":
            case "보통": 
                result = PzWord.DIFFICULTY_MEDIUM;
                break;
            case "3":
            case "어려움": 
                result = PzWord.DIFFICULTY_HARD;
                break;
            case "4":
            case "매우 어려움": 
                result = PzWord.DIFFICULTY_VERY_HARD;
                break;
            case "5":
            case "극도 어려움": 
                result = PzWord.DIFFICULTY_EXTREME;
                break;
            default: 
                result = null;
                break;
        }
        
        logger.info("parseDifficulty 결과: {}", result);
        return result;
    }

    /**
     * 힌트 추가
     */
    @Transactional
    public PzHint addHint(Integer wordId, String hintType, Integer difficulty, String content, Boolean isPrimary, MultipartFile file) throws IOException {
        PzWord word = pzWordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("단어를 찾을 수 없습니다. ID: " + wordId));

        PzHint hint = new PzHint();
        hint.setWord(word);
        hint.setHintType(hintType);
        hint.setDifficulty(difficulty);
        hint.setIsPrimary(isPrimary != null ? isPrimary : false);
        hint.setCreatedAt(LocalDateTime.now());
        hint.setUpdatedAt(LocalDateTime.now());

        if ("TEXT".equals(hintType)) {
            hint.setHintText(content);
        } else if (("IMAGE".equals(hintType) || "SOUND".equals(hintType)) && file != null && !file.isEmpty()) {
            // 파일 저장 로직
            String uploadDir = "uploads/hints";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            hint.setHintText("/" + uploadDir + "/" + fileName); // 웹 접근 경로 저장
        } else {
            throw new IllegalArgumentException("유효하지 않은 힌트 타입 또는 파일이 없습니다.");
        }

        return pzHintRepository.save(hint);
    }

    /**
     * 단어의 힌트 목록 조회
     */
    public List<PzHint> getWordHints(Integer wordId) {
        return pzHintRepository.findByWordIdOrderById(wordId);
    }

    /**
     * 힌트 수정
     */
    @Transactional
    public PzHint updateHint(Integer hintId, String content, Integer difficulty, Boolean isPrimary, MultipartFile file) throws IOException {
        PzHint hint = pzHintRepository.findById(hintId)
                .orElseThrow(() -> new IllegalArgumentException("힌트를 찾을 수 없습니다. ID: " + hintId));

        if (content != null) {
            hint.setHintText(content);
        }
        if (difficulty != null) {
            hint.setDifficulty(difficulty);
        }
        if (isPrimary != null) {
            hint.setIsPrimary(isPrimary);
        }
        hint.setUpdatedAt(LocalDateTime.now());

        // 파일 업로드 처리
        if (file != null && !file.isEmpty() && ("IMAGE".equals(hint.getHintType()) || "SOUND".equals(hint.getHintType()))) {
            // 기존 파일 삭제
            if (hint.getHintText() != null && hint.getHintText().startsWith("/uploads/")) {
                try {
                    Files.deleteIfExists(Paths.get("." + hint.getHintText()));
                } catch (IOException e) {
                    // 파일 삭제 실패는 무시
                }
            }

            // 새 파일 저장
            String uploadDir = "uploads/hints";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            hint.setHintText("/" + uploadDir + "/" + fileName);
        }

        return pzHintRepository.save(hint);
    }

    /**
     * 힌트 삭제
     */
    @Transactional
    public void deleteHint(Integer hintId) {
        PzHint hint = pzHintRepository.findById(hintId)
                .orElseThrow(() -> new IllegalArgumentException("힌트를 찾을 수 없습니다. ID: " + hintId));

        // 파일 삭제
        if (hint.getHintText() != null && hint.getHintText().startsWith("/uploads/")) {
            try {
                Files.deleteIfExists(Paths.get("." + hint.getHintText()));
            } catch (IOException e) {
                // 파일 삭제 실패는 무시
            }
        }

        pzHintRepository.deleteById(hintId);
    }
}
