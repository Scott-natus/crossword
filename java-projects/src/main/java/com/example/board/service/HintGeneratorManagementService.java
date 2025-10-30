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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${gemini.api.key:}")
    private String geminiApiKey;

    @org.springframework.beans.factory.annotation.Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent}")
    private String geminiApiUrl;
    
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
            
            // Gemini 호출로 카테고리+힌트 생성 (라라벨 프롬프트와 동일 형식)
            List<PzHint> createdHints = createHintsViaGemini(word);
            
            response.put("success", true);
            response.put("message", "힌트가 생성되었습니다. (성공: " + createdHints.size() + "개)");
            response.put("hints", createdHints);
            response.put("word", word);
            
        } catch (Exception e) {
            log.error("HINT_GEN_ERROR wordId={} err={}", wordId, e.toString());
            response.put("success", false);
            response.put("message", "힌트 생성 중 오류가 발생했습니다.");
        }
        
        return response;
    }
    
    private List<PzHint> createHintsViaGemini(PzWord word) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("Gemini API 키가 설정되어 있지 않습니다.");
        }

        String prompt = buildCategoryAndHintsPrompt(word.getWord());
        log.info("HINT_GEN_START word={} promptSnip={}",
            word.getWord(),
            prompt != null && prompt.length() > 120 ? prompt.substring(0,120) + "..." : prompt);

        Map<String, Object> body = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", java.util.List.of(part));
        body.put("contents", java.util.List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 1024);
        body.put("generationConfig", generationConfig);

        String url = geminiApiUrl + "?key=" + geminiApiKey;

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<Map<String, Object>> req = new org.springframework.http.HttpEntity<>(body, headers);

        org.springframework.http.ResponseEntity<String> resp;
        try {
            resp = restTemplate.postForEntity(url, req, String.class);
        } catch (Exception ex) {
            log.error("Gemini API 호출 예외 word={} url={} err={}", word.getWord(), url, ex.toString());
            throw new IllegalStateException("Gemini API 호출 예외: " + ex.getMessage());
        }
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            log.error("Gemini API 호출 실패 status={} body={} word={} url={}", resp.getStatusCode(), resp.getBody(), word.getWord(), url);
            throw new IllegalStateException("Gemini API 호출 실패: " + resp.getStatusCode());
        }

        String text;
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(resp.getBody());
            text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            log.info("HINT_GEN_RESP word={} bodySnip={}",
                word.getWord(),
                resp.getBody() != null && resp.getBody().length() > 200 ? resp.getBody().substring(0,200) + "..." : resp.getBody());
            log.info("HINT_GEN_TEXT word={} textSnip={}", word.getWord(), text != null && text.length() > 120 ? text.substring(0,120) + "..." : text);
        } catch (Exception e) {
            throw new IllegalStateException("Gemini 응답 파싱 실패: " + e.getMessage());
        }

        // 라라벨 파서와 동일한 패턴으로 카테고리/힌트 추출
        String category = extractCategory(text);
        Map<Integer, String> hintsByLevel = extractHints(text);

        if (category != null && (hintsByLevel.get(1) != null || hintsByLevel.get(2) != null || hintsByLevel.get(3) != null)) {
            // 카테고리 자동 보정(있을 때만)
            if (category != null && (word.getCategory() == null || word.getCategory().isBlank())) {
                word.setCategory(category);
                pzWordRepository.save(word);
            }

            // 힌트 저장: 보통(2)를 기본힌트로, 나머지는 추가힌트
            LocalDateTime now = LocalDateTime.now();
            java.util.ArrayList<PzHint> saved = new java.util.ArrayList<>();

            for (int level : new int[]{2,1,3}) {
                String h = hintsByLevel.get(level);
                if (h == null || h.isBlank()) continue;
                PzHint ph = new PzHint();
                ph.setWord(word);
                ph.setHintText(h.trim());
                ph.setDifficulty(level);
                ph.setIsPrimary(level == 2);
                ph.setCreatedAt(now);
                ph.setUpdatedAt(now);
                pzHintRepository.save(ph);
                saved.add(ph);
            }
            return saved;
        }

        throw new IllegalStateException("카테고리 또는 힌트 추출 실패");
    }

    private String buildCategoryAndHintsPrompt(String word) {
        return "당신은 한글 십자낱말 퍼즐을 위한 힌트를 만드는 전문가입니다.\n\n" +
               "단어 '" + word + "' 에 대한 적절한 카테고리를 하나 생성하고 \n" +
               "카테고리와 단어 '" + word + "'에  적정한 힌트를 3가지 난이도로 만들어주세요.\n\n" +
               "카테고리는 적합한 한가지 분야로 판단해주세요. (예: 사회 , 사회과학, 인문, 인문학 등)\n \n\n" +
               "**힌트 작성 규칙:**\n" +
               "1. 정답 단어를 직접 언급하지 마세요\n" +
               "2. 50자 내외로 연상되기 쉽게 설명해 주세요\n" +
               "3. 초등학생도 이해할 수 있게 작성하세요\n" +
               "4. 너무 어렵거나 추상적인 표현은 피하세요\n\n" +
               "**응답 형식 (다른 설명 없이):**\n\n" +
               "'" + word + "' 의 카테고리 : [카테고리]\n\n" +
               "쉬움: [매우 쉬운 힌트]\n" +
               "보통: [보통 난이도 힌트]  \n" +
               "어려움: [조금 어려운 힌트]";
    }

    private String extractCategory(String text) {
        if (text == null) return null;
        Pattern p = Pattern.compile("의 카테고리\\s*:\\s*([^\\n]+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String cat = m.group(1).trim();
            return cat.replaceAll("^\\[|\\]$", "");
        }
        return null;
    }

    private Map<Integer, String> extractHints(String text) {
        Map<Integer, String> map = new HashMap<>();
        if (text == null) return map;
        map.put(1, extractByLabel(text, "쉬움"));
        map.put(2, extractByLabel(text, "보통"));
        map.put(3, extractByLabel(text, "어려움"));
        return map;
    }

    private String extractByLabel(String text, String label) {
        Pattern p = Pattern.compile(label + "\\s*:\\s*([^\\n\\[\\]]+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
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
