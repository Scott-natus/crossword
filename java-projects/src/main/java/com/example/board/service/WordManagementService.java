package com.example.board.service;

import com.example.board.entity.PzWord;
import com.example.board.entity.PzHint;
import com.example.board.repository.PzWordRepository;
import com.example.board.repository.PzHintRepository;
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
    public Map<String, Object> getWordsData(int draw, int start, int length, String search, String difficultyFilter, String refinement, String activeFilter) {
        Map<String, Object> response = new HashMap<>();
        response.put("draw", draw);
        
        // 디버깅 로그 추가
        logger.info("=== 단어 검색 파라미터 ===");
        logger.info("search: '{}'", search);
        logger.info("difficultyFilter: '{}'", difficultyFilter);
        logger.info("refinement: '{}'", refinement);
        logger.info("activeFilter: '{}'", activeFilter);
        
        try {
            // 정렬 설정 (생성일자 내림차순)
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            Pageable pageable = PageRequest.of(start / length, length, sort);
            
            Page<PzWord> page;
            
            // 검색어, 난이도 필터, 정제상태 필터, 활성화 필터에 따른 조회
            // null 체크와 빈 문자열 체크를 모두 고려
            boolean hasSearch = search != null && !search.trim().isEmpty();
            boolean hasDifficulty = difficultyFilter != null && !difficultyFilter.trim().isEmpty();
            boolean hasRefinement = refinement != null && !refinement.trim().isEmpty();
            boolean hasActiveFilter = activeFilter != null && !activeFilter.trim().isEmpty();
            
            logger.info("필터 조건 체크 - search: '{}', difficulty: '{}', refinement: '{}', active: '{}'", 
                search, difficultyFilter, refinement, activeFilter);
            logger.info("필터 존재 여부 - hasSearch: {}, hasDifficulty: {}, hasRefinement: {}, hasActiveFilter: {}", 
                hasSearch, hasDifficulty, hasRefinement, hasActiveFilter);
            
            // activeFilter를 포함한 조건부 조회
            Boolean isActive = hasActiveFilter ? Boolean.parseBoolean(activeFilter) : null;
            logger.info("activeFilter 파싱 결과: '{}' -> {}", activeFilter, isActive);
            
            // 단순화된 필터 로직 - 각 조건이 있으면 AND로 추가
            // 기본적으로 findAll()에서 시작해서 조건에 따라 적절한 메서드 호출
            if (hasSearch && hasDifficulty && hasRefinement && hasActiveFilter) {
                // 모든 필터 적용
                logger.info("모든 필터 적용 - search: {}, difficulty: {}, refinement: {}, active: {}", 
                    search, difficultyFilter, refinement, isActive);
                page = pzWordRepository.findByWordContainingAndDifficultyAndConfYnAndIsActive(search, 
                    Integer.parseInt(difficultyFilter), refinement, isActive, pageable);
            } else if (hasSearch && hasDifficulty && hasRefinement) {
                // 검색어 + 난이도 + 정제상태
                page = pzWordRepository.findByWordContainingAndDifficultyAndConfYn(search, 
                    Integer.parseInt(difficultyFilter), refinement, pageable);
            } else if (hasSearch && hasDifficulty && hasActiveFilter) {
                // 검색어 + 난이도 + 활성화
                page = pzWordRepository.findByWordContainingAndDifficultyAndIsActive(search, 
                    Integer.parseInt(difficultyFilter), isActive, pageable);
            } else if (hasSearch && hasRefinement && hasActiveFilter) {
                // 검색어 + 정제상태 + 활성화
                page = pzWordRepository.findByWordContainingAndConfYnAndIsActive(search, refinement, isActive, pageable);
            } else if (hasDifficulty && hasRefinement && hasActiveFilter) {
                // 난이도 + 정제상태 + 활성화
                page = pzWordRepository.findByDifficultyAndConfYnAndIsActive(
                    Integer.parseInt(difficultyFilter), refinement, isActive, pageable);
            } else if (hasSearch && hasDifficulty) {
                // 검색어 + 난이도
                page = pzWordRepository.findByWordContainingAndDifficulty(search, 
                    Integer.parseInt(difficultyFilter), pageable);
            } else if (hasSearch && hasRefinement) {
                // 검색어 + 정제상태
                page = pzWordRepository.findByWordContainingAndConfYn(search, refinement, pageable);
            } else if (hasSearch && hasActiveFilter) {
                // 검색어 + 활성화
                page = pzWordRepository.findByWordContainingAndIsActive(search, isActive, pageable);
            } else if (hasDifficulty && hasRefinement) {
                // 난이도 + 정제상태
                page = pzWordRepository.findByDifficultyAndConfYn(
                    Integer.parseInt(difficultyFilter), refinement, pageable);
            } else if (hasDifficulty && hasActiveFilter) {
                // 난이도 + 활성화
                page = pzWordRepository.findByDifficultyAndIsActive(
                    Integer.parseInt(difficultyFilter), isActive, pageable);
            } else if (hasRefinement && hasActiveFilter) {
                // 정제상태 + 활성화
                page = pzWordRepository.findByConfYnAndIsActive(refinement, isActive, pageable);
            } else if (hasSearch) {
                // 검색어만
                page = pzWordRepository.findByWordContainingIgnoreCase(search, pageable);
            } else if (hasDifficulty) {
                // 난이도만
                page = pzWordRepository.findByDifficulty(Integer.parseInt(difficultyFilter), pageable);
            } else if (hasRefinement) {
                // 정제상태만
                page = pzWordRepository.findByConfYn(refinement, pageable);
            } else if (hasActiveFilter) {
                // 활성화만
                page = pzWordRepository.findByIsActive(isActive, pageable);
            } else {
                // 필터 없음
                page = pzWordRepository.findAll(pageable);
            }
            
            logger.info("조회된 페이지 정보 - 총 개수: {}, 현재 페이지: {}, 페이지 크기: {}", 
                page.getTotalElements(), page.getNumber(), page.getSize());
            
            // DataTables 형식으로 변환
            List<Map<String, Object>> data = page.getContent().stream()
                .map(this::convertToWordMap)
                .collect(Collectors.toList());
            
            response.put("recordsTotal", page.getTotalElements());
            response.put("recordsFiltered", page.getTotalElements());
            response.put("data", data);
            
            logger.info("최종 응답 데이터 개수: {}", data.size());
            
        } catch (Exception e) {
            logger.error("단어 데이터 조회 중 오류 발생", e);
            response.put("error", "데이터 조회 중 오류가 발생했습니다.");
            response.put("recordsTotal", 0);
            response.put("recordsFiltered", 0);
            response.put("data", new ArrayList<>());
        }
        
        return response;
    }

    /**
     * PzWord 엔티티를 Map으로 변환
     */
    private Map<String, Object> convertToWordMap(PzWord word) {
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
        
        return wordMap;
    }

    /**
     * 단어 상세 정보 조회
     */
    public Map<String, Object> getWordDetail(Integer id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(id);
            if (wordOpt.isPresent()) {
                PzWord word = wordOpt.get();
                result.put("success", true);
                result.put("message", "단어를 성공적으로 조회했습니다.");
                
                // 8081과 동일한 구조로 단어 정보 구성
                Map<String, Object> wordData = new HashMap<>();
                wordData.put("id", word.getId());
                wordData.put("word", word.getWord());
                wordData.put("length", word.getLength());
                wordData.put("category", word.getCategory());
                wordData.put("difficulty", word.getDifficulty());
                wordData.put("isActive", word.getIsActive());
                wordData.put("confYn", word.getConfYn());
                wordData.put("createdAt", word.getCreatedAt().toString());
                wordData.put("updatedAt", word.getUpdatedAt().toString());
                wordData.put("wordActive", word.getIsActive());
                wordData.put("difficultyLevel", word.getDifficultyText());
                
                result.put("data", wordData);
                
                // 힌트 목록 조회
                List<PzHint> hints = pzHintRepository.findByWordIdOrderById(id);
                List<Map<String, Object>> hintsList = hints.stream()
                    .map(hint -> {
                        Map<String, Object> hintMap = new HashMap<>();
                        hintMap.put("id", hint.getId());
                        hintMap.put("hint_text", hint.getHintText());
                        hintMap.put("difficulty", hint.getDifficulty());
                        hintMap.put("is_primary", hint.getIsPrimary());
                        hintMap.put("created_at", hint.getCreatedAt());
                        return hintMap;
                    })
                    .collect(Collectors.toList());
                result.put("hints", hintsList);
            } else {
                result.put("success", false);
                result.put("message", "단어를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            logger.error("단어 상세 정보 조회 중 오류 발생: {}", id, e);
            result.put("success", false);
            result.put("message", "단어 정보 조회 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 단어 생성
     */
    public Map<String, Object> createWord(Map<String, Object> wordData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            PzWord word = new PzWord();
            word.setWord((String) wordData.get("word"));
            word.setCategory((String) wordData.get("category"));
            word.setDifficulty((Integer) wordData.get("difficulty"));
            word.setIsActive((Boolean) wordData.getOrDefault("is_active", true));
            word.setConfYn((String) wordData.getOrDefault("conf_yn", "N"));
            word.setIsApproved((Boolean) wordData.getOrDefault("is_approved", false));
            word.setCreatedAt(LocalDateTime.now());
            word.setUpdatedAt(LocalDateTime.now());
            
            PzWord savedWord = pzWordRepository.save(word);
            
            result.put("success", true);
            result.put("message", "단어가 성공적으로 생성되었습니다.");
            result.put("word", convertToWordMap(savedWord));
            
        } catch (Exception e) {
            logger.error("단어 생성 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "단어 생성 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 단어 수정
     */
    public Map<String, Object> updateWord(Integer id, Map<String, Object> wordData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(id);
            if (wordOpt.isPresent()) {
                PzWord word = wordOpt.get();
                
                if (wordData.containsKey("word")) {
                    word.setWord((String) wordData.get("word"));
                }
                if (wordData.containsKey("category")) {
                    word.setCategory((String) wordData.get("category"));
                }
                if (wordData.containsKey("difficulty")) {
                    word.setDifficulty((Integer) wordData.get("difficulty"));
                }
                if (wordData.containsKey("is_active")) {
                    word.setIsActive((Boolean) wordData.get("is_active"));
                }
                if (wordData.containsKey("conf_yn")) {
                    word.setConfYn((String) wordData.get("conf_yn"));
                }
                if (wordData.containsKey("is_approved")) {
                    word.setIsApproved((Boolean) wordData.get("is_approved"));
                }
                
                word.setUpdatedAt(LocalDateTime.now());
                
                PzWord savedWord = pzWordRepository.save(word);
                
                result.put("success", true);
                result.put("message", "단어가 성공적으로 수정되었습니다.");
                result.put("word", convertToWordMap(savedWord));
            } else {
                result.put("success", false);
                result.put("message", "단어를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            logger.error("단어 수정 중 오류 발생: {}", id, e);
            result.put("success", false);
            result.put("message", "단어 수정 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 단어 삭제
     */
    public Map<String, Object> deleteWord(Integer id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(id);
            if (wordOpt.isPresent()) {
                // 관련 힌트도 함께 삭제
                List<PzHint> hints = pzHintRepository.findByWordIdOrderById(id);
                pzHintRepository.deleteAll(hints);
                
                pzWordRepository.deleteById(id);
                
                result.put("success", true);
                result.put("message", "단어가 성공적으로 삭제되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "단어를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            logger.error("단어 삭제 중 오류 발생: {}", id, e);
            result.put("success", false);
            result.put("message", "단어 삭제 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 단어 활성화/비활성화 토글
     */
    public Map<String, Object> toggleActive(Integer id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(id);
            if (wordOpt.isPresent()) {
                PzWord word = wordOpt.get();
                word.setIsActive(!word.getIsActive());
                word.setUpdatedAt(LocalDateTime.now());
                
                PzWord savedWord = pzWordRepository.save(word);
                
                result.put("success", true);
                result.put("message", "단어 상태가 변경되었습니다.");
                result.put("word", convertToWordMap(savedWord));
            } else {
                result.put("success", false);
                result.put("message", "단어를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            logger.error("단어 활성화 상태 변경 중 오류 발생: {}", id, e);
            result.put("success", false);
            result.put("message", "단어 상태 변경 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 단어 승인/미승인 토글
     */
    public Map<String, Object> toggleApproval(Integer id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(id);
            if (wordOpt.isPresent()) {
                PzWord word = wordOpt.get();
                word.setIsApproved(!word.getIsApproved());
                word.setUpdatedAt(LocalDateTime.now());
                
                PzWord savedWord = pzWordRepository.save(word);
                
                result.put("success", true);
                result.put("message", "단어 승인 상태가 변경되었습니다.");
                result.put("word", convertToWordMap(savedWord));
            } else {
                result.put("success", false);
                result.put("message", "단어를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            logger.error("단어 승인 상태 변경 중 오류 발생: {}", id, e);
            result.put("success", false);
            result.put("message", "단어 승인 상태 변경 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 단어 일괄 처리
     */
    public Map<String, Object> batchProcess(Map<String, Object> batchData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Integer> wordIds = (List<Integer>) batchData.get("wordIds");
            String action = (String) batchData.get("action");
            
            if (wordIds == null || wordIds.isEmpty()) {
                result.put("success", false);
                result.put("message", "처리할 단어를 선택해주세요.");
                return result;
            }
            
            int processedCount = 0;
            
            for (Integer id : wordIds) {
                Optional<PzWord> wordOpt = pzWordRepository.findById(id);
                if (wordOpt.isPresent()) {
                    PzWord word = wordOpt.get();
                    
                    switch (action) {
                        case "activate":
                            word.setIsActive(true);
                            break;
                        case "deactivate":
                            word.setIsActive(false);
                            break;
                        case "approve":
                            word.setIsApproved(true);
                            break;
                        case "disapprove":
                            word.setIsApproved(false);
                            break;
                        case "delete":
                            // 관련 힌트도 함께 삭제
                            List<PzHint> hints = pzHintRepository.findByWordIdOrderById(id);
                            pzHintRepository.deleteAll(hints);
                            pzWordRepository.deleteById(id);
                            processedCount++;
                            continue;
                    }
                    
                    if (!"delete".equals(action)) {
                        word.setUpdatedAt(LocalDateTime.now());
                        pzWordRepository.save(word);
                    }
                    
                    processedCount++;
                }
            }
            
            result.put("success", true);
            result.put("message", String.format("%d개의 단어가 처리되었습니다.", processedCount));
            result.put("processedCount", processedCount);
            
        } catch (Exception e) {
            logger.error("단어 일괄 처리 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "일괄 처리 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 단어 파일 업로드
     */
    public Map<String, Object> uploadWords(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "업로드할 파일을 선택해주세요.");
                return result;
            }
            
            // 파일 내용 읽기
            String content = new String(file.getBytes());
            String[] lines = content.split("\n");
            
            int successCount = 0;
            int errorCount = 0;
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                try {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        PzWord word = new PzWord();
                        word.setWord(parts[0].trim());
                        word.setCategory(parts[1].trim());
                        word.setDifficulty(parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 1);
                        word.setIsActive(true);
                        word.setConfYn("N");
                        word.setIsApproved(false);
                        word.setCreatedAt(LocalDateTime.now());
                        word.setUpdatedAt(LocalDateTime.now());
                        
                        pzWordRepository.save(word);
                        successCount++;
                    }
                } catch (Exception e) {
                    logger.warn("단어 업로드 중 오류 발생: {}", line, e);
                    errorCount++;
                }
            }
            
            result.put("success", true);
            result.put("message", String.format("업로드 완료: 성공 %d개, 실패 %d개", successCount, errorCount));
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
            
        } catch (IOException e) {
            logger.error("파일 업로드 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "파일 읽기 중 오류가 발생했습니다.");
        } catch (Exception e) {
            logger.error("단어 업로드 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "업로드 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 단어 비활성화
     */
    public Map<String, Object> deactivateWord(Integer id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(id);
            if (wordOpt.isPresent()) {
                PzWord word = wordOpt.get();
                word.setIsActive(false);
                pzWordRepository.save(word);
                
                result.put("success", true);
                result.put("message", "단어가 비활성화되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "단어를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            logger.error("단어 비활성화 중 오류 발생: {}", id, e);
            result.put("success", false);
            result.put("message", "비활성화 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 단어 활성화
     */
    public Map<String, Object> activateWord(Integer id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(id);
            if (wordOpt.isPresent()) {
                PzWord word = wordOpt.get();
                word.setIsActive(true);
                pzWordRepository.save(word);
                
                result.put("success", true);
                result.put("message", "단어가 활성화되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "단어를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            logger.error("단어 활성화 중 오류 발생: {}", id, e);
            result.put("success", false);
            result.put("message", "활성화 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 단어 정제
     */
    public Map<String, Object> refineWord(Integer id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(id);
            if (wordOpt.isPresent()) {
                PzWord word = wordOpt.get();
                
                // 정제 확정 처리
                word.setConfYn("Y");
                pzWordRepository.save(word);
                
                result.put("success", true);
                result.put("message", "단어가 정제 확정되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "단어를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            logger.error("단어 정제 중 오류 발생: {}", id, e);
            result.put("success", false);
            result.put("message", "정제 중 오류가 발생했습니다.");
        }
        
        return result;
    }

    /**
     * 일괄 난이도 변경
     */
    public int updateDifficultyBatch(List<Integer> wordIds, Integer newDifficulty) {
        int updatedCount = 0;
        
        try {
            for (Integer wordId : wordIds) {
                Optional<PzWord> wordOpt = pzWordRepository.findById(wordId);
                if (wordOpt.isPresent()) {
                    PzWord word = wordOpt.get();
                    word.setDifficulty(newDifficulty);
                    pzWordRepository.save(word);
                    updatedCount++;
                }
            }
            
            logger.info("일괄 난이도 변경 완료: {} 개 단어", updatedCount);
        } catch (Exception e) {
            logger.error("일괄 난이도 변경 중 오류 발생", e);
            throw new RuntimeException("일괄 난이도 변경 중 오류가 발생했습니다.");
        }
        
        return updatedCount;
    }

    /**
     * 일괄 활성화 상태 변경
     */
    public int updateActiveStatusBatch(List<Integer> wordIds, Boolean isActive) {
        int updatedCount = 0;
        
        try {
            for (Integer wordId : wordIds) {
                Optional<PzWord> wordOpt = pzWordRepository.findById(wordId);
                if (wordOpt.isPresent()) {
                    PzWord word = wordOpt.get();
                    word.setIsActive(isActive);
                    pzWordRepository.save(word);
                    updatedCount++;
                }
            }
            
            logger.info("일괄 활성화 상태 변경 완료: {} 개 단어", updatedCount);
        } catch (Exception e) {
            logger.error("일괄 활성화 상태 변경 중 오류 발생", e);
            throw new RuntimeException("일괄 활성화 상태 변경 중 오류가 발생했습니다.");
        }
        
        return updatedCount;
    }

    /**
     * 단어 정제 (8081과 동일한 방식)
     */
    public boolean refineWordWithData(Integer wordId, Integer difficulty, List<Map<String, Object>> hints) {
        try {
            Optional<PzWord> wordOpt = pzWordRepository.findById(wordId);
            if (!wordOpt.isPresent()) {
                logger.error("단어를 찾을 수 없습니다: {}", wordId);
                return false;
            }
            
            PzWord word = wordOpt.get();
            
            // 난이도 업데이트
            word.setDifficulty(difficulty);
            
            // 정제 확정 처리
            word.setConfYn("Y");
            
            // 기존 힌트 삭제
            List<PzHint> existingHints = pzHintRepository.findByWordIdOrderById(wordId);
            pzHintRepository.deleteAll(existingHints);
            
            // 새 힌트 생성
            if (hints != null && !hints.isEmpty()) {
                for (Map<String, Object> hintData : hints) {
                    PzHint hint = new PzHint();
                    hint.setWord(word);
                    // 키 이름 호환 처리: hintText | hint_text, isPrimary | is_primary
                    Object textObj = hintData.get("hintText");
                    if (textObj == null) textObj = hintData.get("hint_text");
                    hint.setHintText(textObj != null ? textObj.toString() : null);

                    Object diffObj = hintData.get("difficulty");
                    Integer diffVal = null;
                    if (diffObj instanceof Integer) diffVal = (Integer) diffObj;
                    else if (diffObj instanceof Number) diffVal = ((Number) diffObj).intValue();
                    else if (diffObj != null) {
                        try { diffVal = Integer.parseInt(diffObj.toString()); } catch (Exception ignore) {}
                    }
                    hint.setDifficulty(diffVal != null ? diffVal : 2);

                    Object primaryObj = hintData.get("isPrimary");
                    if (primaryObj == null) primaryObj = hintData.get("is_primary");
                    Boolean isPrimary = false;
                    if (primaryObj instanceof Boolean) isPrimary = (Boolean) primaryObj;
                    else if (primaryObj instanceof String) isPrimary = Boolean.parseBoolean((String) primaryObj);
                    hint.setIsPrimary(isPrimary != null ? isPrimary : false);
                    // 기본값 보정: hint_type, language_code
                    hint.setHintType("TEXT");
                    hint.setLanguageCode("ko");
                    hint.setCreatedAt(LocalDateTime.now());
                    hint.setUpdatedAt(LocalDateTime.now());
                    pzHintRepository.save(hint);
                }
            }
            
            // 단어 저장
            pzWordRepository.save(word);
            
            logger.info("단어 정제 완료: wordId={}, difficulty={}, hints={}", wordId, difficulty, hints != null ? hints.size() : 0);
            return true;
            
        } catch (Exception e) {
            logger.error("단어 정제 중 오류 발생: wordId={}", wordId, e);
            return false;
        }
    }
}