package com.example.board.service;

import com.example.board.entity.PzWord;
import com.example.board.repository.PzWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledHintGenerationService {

    private final PzWordRepository pzWordRepository;
    private final HintGeneratorManagementService hintGeneratorManagementService;

    /**
     * 10초마다 힌트가 없는 활성 단어 하나를 찾아 힌트를 생성한다.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void generateHintForMissingWord() {
        try {
            Optional<PzWord> targetOpt = pzWordRepository.findOneActiveWithoutHints();
            if (targetOpt.isEmpty()) {
                log.debug("SCHED_HINT: 생성 대상 없음 (무힌트 단어 없음)");
                return;
            }
            PzWord target = targetOpt.get();
            log.info("SCHED_HINT_START wordId={} word={}", target.getId(), target.getWord());
            var result = hintGeneratorManagementService.generateForWord(target.getId(), true);
            log.info("SCHED_HINT_DONE wordId={} success={}", target.getId(), result.getOrDefault("success", false));
        } catch (Exception e) {
            log.error("SCHED_HINT_ERROR err={}", e.toString());
        }
    }
}


