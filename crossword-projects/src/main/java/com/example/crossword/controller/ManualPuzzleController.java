package com.example.crossword.controller;

import com.example.crossword.dto.ManualPuzzleTemplateRequest;
import com.example.crossword.entity.PzGridTemplate;
import com.example.crossword.entity.PzWord;
import com.example.crossword.entity.PzHint;
import com.example.crossword.entity.ThemeDailyPuzzle;
import com.example.crossword.repository.PzWordRepository;
import com.example.crossword.repository.PzHintRepository;
import com.example.crossword.repository.ThemeDailyPuzzleRepository;
import com.example.crossword.service.GeminiService;
import com.example.crossword.service.GridTemplateService;
import com.example.crossword.service.TemplateValidationService;
import com.example.crossword.service.WordExtractionService;
import com.example.crossword.util.GridRenderer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 수동 퍼즐 생성 화면 전용 API.
 * 일반 그리드 템플릿 관리({@link GridTemplateController})와 URL·요청 DTO를 분리해
 * 수동 퍼즐만의 역직렬화/검증 변경이 다른 관리 화면에 영향을 주지 않도록 한다.
 */
@Slf4j
@RestController
@RequestMapping("/admin/api/manual-puzzle")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ManualPuzzleController {

    private final GridTemplateService gridTemplateService;
    private final TemplateValidationService templateValidationService;
    private final WordExtractionService wordExtractionService;
    private final ObjectMapper objectMapper;
    private final GridRenderer gridRenderer;
    private final PzWordRepository pzWordRepository;
    private final PzHintRepository pzHintRepository;
    private final ThemeDailyPuzzleRepository themeDailyPuzzleRepository;
    private final GeminiService geminiService;
    private final com.example.crossword.repository.UserPuzzleCompletionRepository userPuzzleCompletionRepository;

    @GetMapping("/template/{id}")
    public ResponseEntity<Map<String, Object>> getTemplateById(@PathVariable Long id) {
        try {
            Optional<PzGridTemplate> templateOpt = gridTemplateService.getTemplateById(id);
            if (templateOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", templateOpt.get());
                return ResponseEntity.ok(response);
            }
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "템플릿을 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/template")
    public ResponseEntity<Map<String, Object>> createManualTemplate(@RequestBody ManualPuzzleTemplateRequest request) {
        try {
            PzGridTemplate template = request.toEntity();

            Map<String, Object> templateData = new HashMap<>();
            templateData.put("level_id", template.getLevelId());
            templateData.put("grid_size", template.getGridWidth());
            templateData.put("grid_pattern", template.getGridPattern());
            templateData.put("word_positions", template.getWordPositions());
            templateData.put("word_count", template.getWordCount());
            templateData.put("intersection_count", template.getIntersectionCount());
            if (template.getCategory() != null) {
                templateData.put("category", template.getCategory());
            }

            Map<String, Object> validationResult = templateValidationService.validateTemplateCreation(templateData);
            if (!(Boolean) validationResult.get("valid")) {
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) validationResult.get("errors");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", String.join("\n", errors));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 검증 단계에서 level_id가 puzzle_levels PK로 정규화됨 — 엔티티에 반영해야 DB에 올바른 FK가 저장된다.
            Object resolvedLevelId = templateData.get("level_id");
            if (resolvedLevelId instanceof Number n) {
                template.setLevelId(n.intValue());
            }

            PzGridTemplate created = gridTemplateService.createTemplate(template);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "템플릿이 성공적으로 생성되었습니다.");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/extract-words")
    public ResponseEntity<Map<String, Object>> extractWords(@RequestBody Map<String, Object> request) {
        try {
            Long templateId = Long.valueOf(request.get("template_id").toString());
            boolean force = Boolean.TRUE.equals(request.get("force_regenerate"));
            Map<String, Object> result = wordExtractionService.extractWords(templateId, force);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("수동 퍼즐 단어 추출 API 오류", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "단어 추출 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/template/{id}/extracted-words-snapshot")
    public ResponseEntity<Map<String, Object>> saveExtractedWordsSnapshot(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Object extracted = body.get("extracted_words");
            if (extracted == null) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "extracted_words 필드가 필요합니다.");
                return ResponseEntity.badRequest().body(err);
            }
            String json = objectMapper.writeValueAsString(extracted);
            Optional<PzGridTemplate> updated = gridTemplateService.saveExtractedWordsSnapshot(id, json);
            if (updated.isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "템플릿을 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
            }
            Map<String, Object> ok = new HashMap<>();
            ok.put("success", true);
            ok.put("message", "단어 스냅샷이 저장되었습니다.");
            ok.put("data", updated.get());
            return ResponseEntity.ok(ok);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * 수동 화면은 gridPattern/wordPositions를 문자열 또는 JSON 배열로 보낼 수 있음.
     */
    @PostMapping("/render-grid")
    public ResponseEntity<Map<String, Object>> renderGrid(@RequestBody Map<String, Object> request) {
        try {
            String gridPattern = jsonValueToString(request.get("gridPattern"));
            String wordPositions = jsonValueToString(request.get("wordPositions"));
            Integer cellSize = request.get("cellSize") != null
                    ? Integer.valueOf(request.get("cellSize").toString())
                    : 20;
            Boolean showNumbers = request.get("showNumbers") != null
                    ? Boolean.valueOf(request.get("showNumbers").toString())
                    : Boolean.TRUE;

            if (gridPattern == null || gridPattern.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "그리드 패턴이 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String html = gridRenderer.renderGrid(gridPattern, wordPositions, cellSize, showNumbers);
            int gridSize = gridRenderer.calculateGridSize(gridPattern);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("html", html);
            response.put("gridSize", gridSize);
            response.put("cellSize", cellSize);
            response.put("showNumbers", showNumbers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "그리드 렌더링 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 수동 퍼즐 게임 발행.
     * 추출·정제된 단어를 pz_words에 저장하고, theme_daily_puzzles에
     * pz_word_id 참조만 담은 puzzle_data를 생성한다.
     * 실제 단어 텍스트는 puzzle_data에 포함하지 않는다 (보안).
     */
    @PostMapping("/publish-game")
    @Transactional
    public ResponseEntity<Map<String, Object>> publishGame(@RequestBody Map<String, Object> body) {
        try {
            Long templateId = Long.valueOf(body.get("template_id").toString());
            String title = body.get("title") != null ? body.get("title").toString() : "수동 퍼즐";

            Optional<PzGridTemplate> tplOpt = gridTemplateService.getTemplateById(templateId);
            if (tplOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false, "message", "템플릿을 찾을 수 없습니다."));
            }
            PzGridTemplate tpl = tplOpt.get();

            String snapshot = tpl.getExtractedWordsSnapshot();
            if (snapshot == null || snapshot.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "추출된 단어 스냅샷이 없습니다. 먼저 단어를 배치해 주세요."));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> extractedWords = objectMapper.readValue(snapshot, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> wordOrder = (List<Map<String, Object>>) extractedWords.get("word_order");
            if (wordOrder == null || wordOrder.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "배치된 단어가 없습니다."));
            }

            List<Map<String, Object>> puzzleWordRefs = new ArrayList<>();
            List<Map<String, Object>> puzzleHints = new ArrayList<>();
            int autoSlot = 0;

            for (Map<String, Object> item : wordOrder) {
                autoSlot++;
                int slotNumber = item.get("display_num") != null
                        ? ((Number) item.get("display_num")).intValue()
                        : autoSlot;
                String wordText = item.get("extracted_word") != null ? item.get("extracted_word").toString() : null;
                String hintText = item.get("hint") != null ? item.get("hint").toString() : "힌트 없음";
                @SuppressWarnings("unchecked")
                Map<String, Object> position = (Map<String, Object>) item.get("position");

                if (wordText == null || wordText.isBlank()) continue;

                Optional<PzWord> existingOpt = pzWordRepository.findFirstByWordAndIsActiveTrue(wordText);
                PzWord pzWord;
                if (existingOpt.isPresent()) {
                    pzWord = existingOpt.get();
                } else {
                    pzWord = new PzWord();
                    pzWord.setWord(wordText);
                    pzWord.setLength(wordText.length());
                    pzWord.setCategory("manual-puzzle");
                    pzWord.setCat2("template:" + templateId);
                    pzWord.setDifficulty(tpl.getDifficultyRating() != null ? tpl.getDifficultyRating() : 1);
                    pzWord.setIsActive(true);
                    pzWord.setIsApproved(true);
                    pzWord.setConfYn("Y");
                    pzWord = pzWordRepository.save(pzWord);
                }

                Integer hintId = null;
                if (hintText != null && !"힌트 없음".equals(hintText)) {
                    List<PzHint> existingHints = pzHintRepository.findByWordIdOrderById(pzWord.getId());
                    boolean hintExists = existingHints.stream()
                            .anyMatch(h -> hintText.equals(h.getHintText()));
                    if (!hintExists) {
                        PzHint hint = new PzHint();
                        hint.setWord(pzWord);
                        hint.setHintText(hintText);
                        hint.setHintType("TEXT");
                        hint.setDifficulty(1);
                        hint.setIsPrimary(existingHints.isEmpty());
                        hint.setLanguageCode("ko");
                        hint.setCreatedAt(java.time.LocalDateTime.now());
                        hint.setUpdatedAt(java.time.LocalDateTime.now());
                        PzHint savedHint = pzHintRepository.save(hint);
                        hintId = savedHint.getId();
                    } else {
                        hintId = existingHints.stream()
                                .filter(h -> hintText.equals(h.getHintText()))
                                .findFirst().map(PzHint::getId).orElse(null);
                    }
                }

                String maskedWord = "○".repeat(wordText.length());

                Map<String, Object> ref = new HashMap<>();
                ref.put("word_id", slotNumber);
                ref.put("pz_word_id", pzWord.getId());
                ref.put("position", position);
                ref.put("extracted_word", maskedWord);
                ref.put("hint", hintText);
                ref.put("length", wordText.length());
                puzzleWordRefs.add(ref);

                Map<String, Object> hintRef = new HashMap<>();
                hintRef.put("wordId", pzWord.getId());
                hintRef.put("hintId", hintId);
                hintRef.put("hintText", hintText);
                hintRef.put("wordPositionId", slotNumber);
                puzzleHints.add(hintRef);
            }

            String gridPatternStr = tpl.getGridPattern();
            Object gridPattern;
            try {
                gridPattern = objectMapper.readValue(gridPatternStr, Object.class);
            } catch (Exception e) {
                gridPattern = gridPatternStr;
            }

            int newPuzzleId = (int) (System.currentTimeMillis() % 1_000_000);

            Map<String, Object> puzzleData = new HashMap<>();
            puzzleData.put("success", true);
            puzzleData.put("puzzleId", newPuzzleId);
            puzzleData.put("theme", "MANUAL-" + title);
            puzzleData.put("title", title);
            puzzleData.put("templateId", templateId);
            puzzleData.put("grid", gridPattern);
            puzzleData.put("words", puzzleWordRefs);
            puzzleData.put("hints", puzzleHints);
            puzzleData.put("generatedAt", new java.util.Date());

            ThemeDailyPuzzle dailyPuzzle = new ThemeDailyPuzzle();
            dailyPuzzle.setTheme("MANUAL-" + title);
            dailyPuzzle.setPuzzleDate(LocalDate.now());
            dailyPuzzle.setPuzzleId(newPuzzleId);
            dailyPuzzle.setPuzzleData(objectMapper.writeValueAsString(puzzleData));
            dailyPuzzle.setIsActive(true);
            themeDailyPuzzleRepository.save(dailyPuzzle);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "게임이 발행되었습니다.");
            response.put("puzzle_id", newPuzzleId);
            response.put("game_url", "/K-CrossWord/manual-game?puzzleId=" + newPuzzleId);
            response.put("word_count", puzzleWordRefs.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("수동 퍼즐 게임 발행 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false, "message", "게임 발행 중 오류: " + e.getMessage()));
        }
    }

    /**
     * puzzleId로 게임 데이터 조회 (프론트 게임 페이지에서 호출).
     * theme_daily_puzzles.puzzle_id 로만 검색한다.
     * 실제 단어 텍스트는 포함하지 않으며, pz_word_id 참조만 전달한다.
     */
    @GetMapping("/game-data/{puzzleId}")
    public ResponseEntity<Map<String, Object>> getGameData(@PathVariable Integer puzzleId) {
        try {
            Optional<ThemeDailyPuzzle> tdpOpt = themeDailyPuzzleRepository
                    .findByPuzzleIdAndIsActive(puzzleId, true);
            if (tdpOpt.isEmpty() || tdpOpt.get().getPuzzleData() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", "퍼즐을 찾을 수 없습니다. (puzzleId=" + puzzleId + ")"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> pd = objectMapper.readValue(tdpOpt.get().getPuzzleData(), Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> words = (List<Map<String, Object>>) pd.get("words");
            if (words != null) {
                for (Map<String, Object> w : words) {
                    w.remove("word");
                }
            }

            pd.put("puzzle_id", puzzleId);
            pd.put("success", true);
            return ResponseEntity.ok(pd);

        } catch (Exception e) {
            log.error("게임 데이터 조회 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false, "message", "게임 데이터 조회 오류: " + e.getMessage()));
        }
    }

    /**
     * 수동 퍼즐 정답 확인 (서버 사이드).
     * pz_words 테이블에서 pz_word_id로 실제 단어를 조회하여 비교한다.
     */
    @PostMapping("/check-answer")
    public ResponseEntity<Map<String, Object>> checkAnswer(@RequestBody Map<String, Object> body) {
        try {
            Object wordIdObj = body.get("word_id");
            String answer = (String) body.get("answer");
            if (wordIdObj == null || answer == null || answer.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "word_id와 answer가 필요합니다."));
            }

            Integer pzWordId = Integer.valueOf(wordIdObj.toString());
            Optional<PzWord> pzWordOpt = pzWordRepository.findById(pzWordId);
            if (pzWordOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "단어 정보를 찾을 수 없습니다."));
            }

            PzWord pzWord = pzWordOpt.get();
            boolean isCorrect = pzWord.getWord().equals(answer.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("is_correct", isCorrect);
            if (isCorrect) {
                response.put("correct_answer", pzWord.getWord());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("정답 확인 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false, "message", "정답 확인 오류: " + e.getMessage()));
        }
    }

    /**
     * 수동 퍼즐 힌트 조회.
     * pz_word_id로 pz_hints에서 힌트 목록을 가져온다.
     * is_additional_hint=true이면 base_hint_id 이후의 다음 힌트를 반환한다.
     */
    @GetMapping("/hints")
    public ResponseEntity<Map<String, Object>> getHints(
            @RequestParam Long word_id,
            @RequestParam(required = false) Long base_hint_id,
            @RequestParam(required = false) Boolean is_additional_hint) {
        try {
            List<PzHint> allHints = pzHintRepository.findByWordIdOrderById(word_id.intValue());
            if (allHints.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true, "hints", List.of()));
            }

            List<Map<String, Object>> result = new ArrayList<>();

            if (Boolean.TRUE.equals(is_additional_hint) && base_hint_id != null) {
                boolean found = false;
                for (PzHint h : allHints) {
                    if (found) {
                        result.add(hintToMap(h));
                        break;
                    }
                    if (h.getId().equals(base_hint_id.intValue())) {
                        found = true;
                    }
                }
            } else {
                PzHint primary = allHints.stream()
                        .filter(h -> Boolean.TRUE.equals(h.getIsPrimary()))
                        .findFirst().orElse(allHints.get(0));
                result.add(hintToMap(primary));
            }

            return ResponseEntity.ok(Map.of("success", true, "hints", result));
        } catch (Exception e) {
            log.error("수동 퍼즐 힌트 조회 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false, "message", "힌트 조회 오류: " + e.getMessage()));
        }
    }

    private Map<String, Object> hintToMap(PzHint h) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", h.getId());
        m.put("hint_text", h.getHintText());
        m.put("difficulty", h.getDifficulty());
        m.put("is_primary", h.getIsPrimary());
        return m;
    }

    /**
     * 단어 검색 API — 슬롯 길이 및 키워드로 pz_words를 조회한다.
     * 결과에는 pz_word_id, 단어, 힌트가 포함된다.
     */
    @GetMapping("/search-words")
    public ResponseEntity<Map<String, Object>> searchWords(
            @RequestParam(required = false) Integer length,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        try {
            List<PzWord> results;
            if (keyword != null && !keyword.isBlank()) {
                String likePattern = "%" + keyword.trim() + "%";
                if (length != null && length > 0) {
                    results = pzWordRepository.findByLengthAndKeyword(length, likePattern, limit);
                } else {
                    results = pzWordRepository.findByKeyword(likePattern, limit);
                }
            } else if (length != null && length > 0) {
                results = pzWordRepository.findByLengthForSearch(length, limit);
            } else {
                results = List.of();
            }

            List<Map<String, Object>> items = new ArrayList<>();
            for (PzWord w : results) {
                List<PzHint> hints = pzHintRepository.findByWordIdOrderById(w.getId());
                String hintText = hints.isEmpty() ? "" : hints.get(0).getHintText();

                Map<String, Object> item = new HashMap<>();
                item.put("pz_word_id", w.getId());
                item.put("word", w.getWord());
                item.put("length", w.getLength());
                item.put("hint", hintText);
                item.put("category", w.getCategory());
                item.put("difficulty", w.getDifficulty());
                items.add(item);
            }

            return ResponseEntity.ok(Map.of("success", true, "words", items, "total", items.size()));
        } catch (Exception e) {
            log.error("단어 검색 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false, "message", "단어 검색 오류: " + e.getMessage()));
        }
    }

    /**
     * AI 힌트 생성 — GeminiService를 호출하여 쉬움/보통/어려움 힌트를 한 번에 생성.
     */
    @PostMapping("/generate-ai-hints")
    public ResponseEntity<Map<String, Object>> generateAiHints(@RequestBody Map<String, Object> body) {
        try {
            String word = body.get("word") != null ? body.get("word").toString().trim() : "";
            String category = body.get("category") != null ? body.get("category").toString().trim() : "일반";
            if (word.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "단어를 입력해주세요."));
            }
            Map<String, Object> result = geminiService.generateHint(word, category);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("AI 힌트 생성 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "AI 힌트 생성 오류: " + e.getMessage()));
        }
    }

    /**
     * 수동 퍼즐용 단어+힌트 저장.
     * pz_words.cat2 에 "template:{templateId}" 를 넣어 PK 위반을 피하고,
     * 생성된 pz_word.id 를 반환한다.
     */
    @PostMapping("/save-manual-word")
    @Transactional
    public ResponseEntity<Map<String, Object>> saveManualWord(@RequestBody Map<String, Object> body) {
        try {
            String wordText = body.get("word") != null ? body.get("word").toString().trim() : "";
            Long templateId = body.get("template_id") != null ? Long.valueOf(body.get("template_id").toString()) : null;

            if (wordText.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "단어를 입력해주세요."));
            }

            String cat2Value = templateId != null ? "template:" + templateId : "manual";

            PzWord pzWord = new PzWord();
            pzWord.setWord(wordText);
            pzWord.setLength(wordText.length());
            pzWord.setCategory("manual-puzzle");
            pzWord.setCat2(cat2Value);
            pzWord.setDifficulty(1);
            pzWord.setIsActive(true);
            pzWord.setIsApproved(true);
            pzWord.setConfYn("Y");
            pzWord = pzWordRepository.save(pzWord);

            @SuppressWarnings("unchecked")
            Map<String, Object> hintsMap = body.get("hints") != null ? (Map<String, Object>) body.get("hints") : null;
            List<Map<String, Object>> savedHints = new ArrayList<>();

            if (hintsMap != null) {
                int[] difficulties = {1, 2, 3};
                String[] keys = {"easy", "normal", "hard"};
                for (int i = 0; i < keys.length; i++) {
                    String hintText = hintsMap.get(keys[i]) != null ? hintsMap.get(keys[i]).toString().trim() : "";
                    if (hintText.isBlank()) continue;
                    PzHint hint = new PzHint();
                    hint.setWord(pzWord);
                    hint.setHintText(hintText);
                    hint.setHintType("TEXT");
                    hint.setDifficulty(difficulties[i]);
                    hint.setIsPrimary(i == 0);
                    hint.setLanguageCode("ko");
                    hint.setCreatedAt(java.time.LocalDateTime.now());
                    hint.setUpdatedAt(java.time.LocalDateTime.now());
                    PzHint saved = pzHintRepository.save(hint);
                    savedHints.add(Map.of("id", saved.getId(), "difficulty", difficulties[i], "hint", hintText));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("pz_word_id", pzWord.getId());
            response.put("word", wordText);
            response.put("cat2", cat2Value);
            response.put("hints", savedHints);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("수동 단어 저장 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "단어 저장 오류: " + e.getMessage()));
        }
    }

    /**
     * 수동 퍼즐 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listManualPuzzles(
            @RequestParam(required = false, defaultValue = "all") String filter) {
        try {
            List<ThemeDailyPuzzle> puzzles = "active".equals(filter)
                    ? themeDailyPuzzleRepository.findActiveManualPuzzles()
                    : themeDailyPuzzleRepository.findAllManualPuzzles();

            List<Map<String, Object>> items = new ArrayList<>();
            for (ThemeDailyPuzzle p : puzzles) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("puzzleId", p.getPuzzleId());
                item.put("theme", p.getTheme());
                item.put("puzzleDate", p.getPuzzleDate().toString());
                item.put("isActive", p.getIsActive());
                item.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);

                String title = p.getTheme() != null ? p.getTheme().replace("MANUAL-", "") : "수동 퍼즐";
                int wordCount = 0;
                Long templateId = null;
                try {
                    if (p.getPuzzleData() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> pd = objectMapper.readValue(p.getPuzzleData(), Map.class);
                        if (pd.get("title") != null) title = pd.get("title").toString();
                        if (pd.get("words") instanceof List<?> wl) wordCount = wl.size();
                        if (pd.get("templateId") != null) templateId = Long.valueOf(pd.get("templateId").toString());
                    }
                } catch (Exception ignore) {}
                item.put("title", title);
                item.put("wordCount", wordCount);
                item.put("templateId", templateId);

                String theme = p.getTheme();
                LocalDate puzzleDate = p.getPuzzleDate();
                long participantCount = userPuzzleCompletionRepository.countByThemeAndPuzzleDate(theme, puzzleDate);
                Double avgTime = userPuzzleCompletionRepository.findAverageCompletionTimeByThemeAndDate(theme, puzzleDate);
                Integer bestTime = userPuzzleCompletionRepository.findBestCompletionTimeByThemeAndDate(theme, puzzleDate);
                item.put("participantCount", participantCount);
                item.put("avgCompletionTime", avgTime != null ? Math.round(avgTime) : null);
                item.put("bestTime", bestTime);

                items.add(item);
            }

            return ResponseEntity.ok(Map.of("success", true, "puzzles", items, "total", items.size()));
        } catch (Exception e) {
            log.error("수동 퍼즐 목록 조회 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "목록 조회 오류: " + e.getMessage()));
        }
    }

    /**
     * 수동 퍼즐 비활성화 (삭제)
     */
    @PostMapping("/{id}/deactivate")
    @Transactional
    public ResponseEntity<Map<String, Object>> deactivatePuzzle(@PathVariable Long id) {
        try {
            Optional<ThemeDailyPuzzle> opt = themeDailyPuzzleRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "퍼즐을 찾을 수 없습니다."));
            }
            ThemeDailyPuzzle puzzle = opt.get();
            puzzle.setIsActive(false);
            themeDailyPuzzleRepository.save(puzzle);
            return ResponseEntity.ok(Map.of("success", true, "message", "퍼즐이 비활성화되었습니다."));
        } catch (Exception e) {
            log.error("수동 퍼즐 비활성화 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "비활성화 오류: " + e.getMessage()));
        }
    }

    /**
     * 수동 퍼즐 활성화 (복원)
     */
    @PostMapping("/{id}/activate")
    @Transactional
    public ResponseEntity<Map<String, Object>> activatePuzzle(@PathVariable Long id) {
        try {
            Optional<ThemeDailyPuzzle> opt = themeDailyPuzzleRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "퍼즐을 찾을 수 없습니다."));
            }
            ThemeDailyPuzzle puzzle = opt.get();
            puzzle.setIsActive(true);
            themeDailyPuzzleRepository.save(puzzle);
            return ResponseEntity.ok(Map.of("success", true, "message", "퍼즐이 활성화되었습니다."));
        } catch (Exception e) {
            log.error("수동 퍼즐 활성화 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "활성화 오류: " + e.getMessage()));
        }
    }

    private String jsonValueToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("grid_pattern/word_positions JSON 변환 실패", e);
        }
    }
}
