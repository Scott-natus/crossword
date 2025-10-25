package com.example.board.repository;

import com.example.board.entity.Hint;
import com.example.board.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 힌트 정보를 관리하는 Repository
 * Laravel의 Hint 모델과 동일한 기능을 제공
 */
@Repository
public interface HintRepository extends JpaRepository<Hint, Integer> {
    
    /**
     * 특정 단어의 모든 힌트 조회
     */
    List<Hint> findByWord(Word word);
    
    /**
     * 특정 단어의 모든 힌트를 페이징으로 조회
     */
    Page<Hint> findByWord(Word word, Pageable pageable);
    
    /**
     * 특정 단어 ID의 모든 힌트 조회
     */
    List<Hint> findByWordId(Integer wordId);
    
    /**
     * 특정 단어 ID의 모든 힌트를 페이징으로 조회
     */
    Page<Hint> findByWordId(Integer wordId, Pageable pageable);
    
    /**
     * 특정 단어의 주 힌트 조회
     */
    Optional<Hint> findByWordAndIsPrimaryTrue(Word word);
    
    /**
     * 특정 단어 ID의 주 힌트 조회
     */
    Optional<Hint> findByWordIdAndIsPrimaryTrue(Integer wordId);
    
    /**
     * 특정 힌트 타입의 힌트 조회
     */
    List<Hint> findByHintType(String hintType);
    
    /**
     * 특정 힌트 타입의 힌트를 페이징으로 조회
     */
    Page<Hint> findByHintType(String hintType, Pageable pageable);
    
    /**
     * 특정 단어의 특정 힌트 타입 조회
     */
    List<Hint> findByWordAndHintType(Word word, String hintType);
    
    /**
     * 특정 단어 ID의 특정 힌트 타입 조회
     */
    List<Hint> findByWordIdAndHintType(Integer wordId, String hintType);
    
    /**
     * 특정 난이도의 힌트 조회
     */
    List<Hint> findByDifficulty(Integer difficulty);
    
    /**
     * 특정 난이도의 힌트를 페이징으로 조회
     */
    Page<Hint> findByDifficulty(Integer difficulty, Pageable pageable);
    
    /**
     * 특정 단어의 특정 난이도 힌트 조회
     */
    List<Hint> findByWordAndDifficulty(Word word, Integer difficulty);
    
    /**
     * 특정 단어 ID의 특정 난이도 힌트 조회
     */
    List<Hint> findByWordIdAndDifficulty(Integer wordId, Integer difficulty);
    
    /**
     * 수정된 힌트 조회
     */
    List<Hint> findByCorrectionStatus(String correctionStatus);
    
    /**
     * 수정된 힌트를 페이징으로 조회
     */
    Page<Hint> findByCorrectionStatus(String correctionStatus, Pageable pageable);
    
    /**
     * 특정 단어의 수정된 힌트 조회
     */
    List<Hint> findByWordAndCorrectionStatus(Word word, String correctionStatus);
    
    /**
     * 특정 단어 ID의 수정된 힌트 조회
     */
    List<Hint> findByWordIdAndCorrectionStatus(Integer wordId, String correctionStatus);
    
    /**
     * 힌트 텍스트에 특정 문자열이 포함된 힌트 조회
     */
    List<Hint> findByHintTextContainingIgnoreCase(String hintText);
    
    /**
     * 힌트 텍스트에 특정 문자열이 포함된 힌트를 페이징으로 조회
     */
    Page<Hint> findByHintTextContainingIgnoreCase(String hintText, Pageable pageable);
    
    /**
     * 특정 단어의 힌트 텍스트에 특정 문자열이 포함된 힌트 조회
     */
    List<Hint> findByWordAndHintTextContainingIgnoreCase(Word word, String hintText);
    
    /**
     * 특정 단어 ID의 힌트 텍스트에 특정 문자열이 포함된 힌트 조회
     */
    List<Hint> findByWordIdAndHintTextContainingIgnoreCase(Integer wordId, String hintText);
    
    /**
     * 이미지 URL이 있는 힌트 조회
     */
    @Query("SELECT h FROM Hint h WHERE h.imageUrl IS NOT NULL AND h.imageUrl != ''")
    List<Hint> findHintsWithImage();
    
    /**
     * 이미지 URL이 있는 힌트를 페이징으로 조회
     */
    @Query("SELECT h FROM Hint h WHERE h.imageUrl IS NOT NULL AND h.imageUrl != ''")
    Page<Hint> findHintsWithImage(Pageable pageable);
    
    /**
     * 오디오 URL이 있는 힌트 조회
     */
    @Query("SELECT h FROM Hint h WHERE h.audioUrl IS NOT NULL AND h.audioUrl != ''")
    List<Hint> findHintsWithAudio();
    
    /**
     * 오디오 URL이 있는 힌트를 페이징으로 조회
     */
    @Query("SELECT h FROM Hint h WHERE h.audioUrl IS NOT NULL AND h.audioUrl != ''")
    Page<Hint> findHintsWithAudio(Pageable pageable);
    
    /**
     * 특정 단어의 랜덤 힌트 조회
     */
    @Query(value = "SELECT * FROM pz_hints WHERE word_id = :wordId ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Hint> findRandomHintByWordId(@Param("wordId") Integer wordId);
    
    /**
     * 특정 단어의 특정 힌트 타입 랜덤 힌트 조회
     */
    @Query(value = "SELECT * FROM pz_hints WHERE word_id = :wordId AND hint_type = :hintType ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Hint> findRandomHintByWordIdAndType(@Param("wordId") Integer wordId, @Param("hintType") String hintType);
    
    /**
     * 특정 단어의 특정 난이도 랜덤 힌트 조회
     */
    @Query(value = "SELECT * FROM pz_hints WHERE word_id = :wordId AND difficulty = :difficulty ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Hint> findRandomHintByWordIdAndDifficulty(@Param("wordId") Integer wordId, @Param("difficulty") Integer difficulty);
    
    /**
     * 힌트 타입별 힌트 개수 조회
     */
    @Query("SELECT h.hintType, COUNT(h) FROM Hint h GROUP BY h.hintType ORDER BY h.hintType")
    List<Object[]> countHintsByType();
    
    /**
     * 난이도별 힌트 개수 조회
     */
    @Query("SELECT h.difficulty, COUNT(h) FROM Hint h WHERE h.difficulty IS NOT NULL GROUP BY h.difficulty ORDER BY h.difficulty")
    List<Object[]> countHintsByDifficulty();
    
    /**
     * 수정 상태별 힌트 개수 조회
     */
    @Query("SELECT h.correctionStatus, COUNT(h) FROM Hint h GROUP BY h.correctionStatus ORDER BY h.correctionStatus")
    List<Object[]> countHintsByCorrectionStatus();
    
    /**
     * 특정 단어의 힌트 개수 조회
     */
    long countByWord(Word word);
    
    /**
     * 특정 단어 ID의 힌트 개수 조회
     */
    long countByWordId(Integer wordId);
    
    /**
     * 특정 단어의 특정 힌트 타입 개수 조회
     */
    long countByWordAndHintType(Word word, String hintType);
    
    /**
     * 특정 단어 ID의 특정 힌트 타입 개수 조회
     */
    long countByWordIdAndHintType(Integer wordId, String hintType);
    
    /**
     * 특정 단어의 주 힌트 개수 조회
     */
    long countByWordAndIsPrimaryTrue(Word word);
    
    /**
     * 특정 단어 ID의 주 힌트 개수 조회
     */
    long countByWordIdAndIsPrimaryTrue(Integer wordId);
    
    /**
     * 특정 단어의 수정된 힌트 개수 조회
     */
    long countByWordAndCorrectionStatus(Word word, String correctionStatus);
    
    /**
     * 특정 단어 ID의 수정된 힌트 개수 조회
     */
    long countByWordIdAndCorrectionStatus(Integer wordId, String correctionStatus);
}
