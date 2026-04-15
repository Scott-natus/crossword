package com.example.crossword.service;

import com.example.crossword.entity.PzHint;
import com.example.crossword.entity.PzWord;
import com.example.crossword.repository.PzHintRepository;
import com.example.crossword.repository.PzWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HintGenerationScheduler {

    private final PzWordRepository wordRepository;
    private final PzHintRepository hintRepository;
    private final GeminiService geminiService;

    /**
     * 10분마다(서울 시각 기준 시계 정렬) 힌트가 없는 단어들을 자동으로 생성 (라라벨 스케줄러 이식)
     */
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void generateHints() {
        log.info("🚀 힌트 생성 스케줄러 시작 - " + LocalDateTime.now());

        int limit = 50; // 한 번에 처리할 단어 수
        Page<PzWord> wordsWithoutHints = wordRepository.findActiveWordsWithoutHints(PageRequest.of(0, limit));

        if (wordsWithoutHints.isEmpty()) {
            log.info("✅ 모든 단어에 힌트가 생성되어 있습니다!");
            return;
        }

        log.info("📝 힌트가 없는 단어 {}개 처리 중...", wordsWithoutHints.getContent().size());

        int successCount = 0;
        int errorCount = 0;

        for (PzWord word : wordsWithoutHints.getContent()) {
            try {
                Map<String, Object> result = geminiService.generateHint(word.getWord(), word.getCategory());

                if ((boolean) result.get("success")) {
                    saveHints(word, (Map<Integer, Map<String, Object>>) result.get("hints"));
                    successCount++;
                    log.debug("힌트 생성 성공 - 단어: {}", word.getWord());
                } else {
                    errorCount++;
                    log.warn("힌트 생성 실패 - 단어: {}, 오류: {}", word.getWord(), result.get("error"));
                }
                
                // API 속도 제한 준수 (라라벨과 동일하게 3초 대기)
                Thread.sleep(3000);
                
            } catch (Exception e) {
                errorCount++;
                log.error("단어 '{}' 처리 중 오류 발생: {}", word.getWord(), e.getMessage());
            }
        }

        log.info("📊 힌트 생성 완료! 성공: {}개, 실패: {}개", successCount, errorCount);
    }

    @Transactional
    protected void saveHints(PzWord word, Map<Integer, Map<String, Object>> hintsData) {
        // 기존 힌트 삭제
        hintRepository.deleteByWordId(word.getId());

        // 세 가지 난이도의 힌트 저장
        for (Map.Entry<Integer, Map<String, Object>> entry : hintsData.entrySet()) {
            Integer difficulty = entry.getKey();
            Map<String, Object> hintInfo = entry.getValue();

            if ((boolean) hintInfo.get("success")) {
                PzHint hint = new PzHint();
                hint.setWord(word);
                hint.setHintText((String) hintInfo.get("hint"));
                hint.setHintType("TEXT");
                hint.setDifficulty(difficulty);
                // 단어의 기본 난이도와 일치하면 isPrimary = true
                hint.setIsPrimary(difficulty.equals(word.getDifficulty()));
                hint.setCreatedAt(LocalDateTime.now());
                hint.setUpdatedAt(LocalDateTime.now());
                
                hintRepository.save(hint);
            }
        }
    }
}
