package com.example.crossword.service;

import com.example.crossword.repository.PzWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 매일 새벽 1시에 퍼즐 단어 정리 및 비활성화 (라라벨 CleanupPuzzleWords 이식)
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupPuzzleWords() {
        log.info("Starting puzzle words cleanup...");

        try {
            // 1. 쉼표와 숫자가 포함된 단어 정리
            int updatedCommaWords = jdbcTemplate.update(
                "UPDATE pz_words SET " +
                "word = SUBSTRING(word FROM 1 FOR POSITION(',' IN word) - 1), " +
                "length = CHAR_LENGTH(SUBSTRING(word FROM 1 FOR POSITION(',' IN word) - 1)) " +
                "WHERE word LIKE '%,%'"
            );
            log.info("Updated {} words with comma and numbers", updatedCommaWords);

            // 2. 영문이나 숫자가 포함된 단어 비활성화 (PostgreSQL 정규식 사용)
            int deactivatedWords = jdbcTemplate.update(
                "UPDATE pz_words SET is_active = false " +
                "WHERE is_active = true AND (word ~ '[a-zA-Z]' OR word ~ '[0-9]')"
            );
            log.info("Deactivated {} words with English letters or numbers", deactivatedWords);

            log.info("Puzzle words cleanup completed successfully!");

        } catch (Exception e) {
            log.error("Error during puzzle words cleanup: {}", e.getMessage());
        }
    }
}
