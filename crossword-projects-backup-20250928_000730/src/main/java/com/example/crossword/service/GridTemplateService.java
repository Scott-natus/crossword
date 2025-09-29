package com.example.crossword.service;

import com.example.crossword.entity.PzGridTemplate;
import com.example.crossword.repository.PzGridTemplateRepository;
import lombok.RequiredArgsConstructor;
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

/**
 * 그리드 템플릿 관리 서비스
 * 
 * @author Crossword Team
 * @version 1.0.0
 * @since 2025-09-23
 */
@Service
@RequiredArgsConstructor
public class GridTemplateService {

    private final PzGridTemplateRepository pzGridTemplateRepository;

    /**
     * 모든 템플릿 조회 (페이징 및 검색 조건 포함)
     */
    public Page<PzGridTemplate> getAllTemplates(int page, int size, String sortField, String sortDir,
                                                Integer levelId, String templateName, Integer minWordCount, 
                                                Integer maxWordCount, Integer minIntersectionCount,
                                                Integer maxIntersectionCount) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // 활성 템플릿만 조회
        return pzGridTemplateRepository.findByIsActiveTrue(pageable);
    }

    /**
     * 단일 템플릿 조회
     */
    public Optional<PzGridTemplate> getTemplateById(Long id) {
        return pzGridTemplateRepository.findById(id);
    }

    /**
     * 템플릿 생성
     */
    @Transactional
    public PzGridTemplate createTemplate(PzGridTemplate template) {
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setIsActive(true);
        return pzGridTemplateRepository.save(template);
    }

    /**
     * 템플릿 수정
     */
    @Transactional
    public Optional<PzGridTemplate> updateTemplate(Long id, PzGridTemplate updatedTemplate) {
        return pzGridTemplateRepository.findById(id).map(template -> {
            template.setLevelId(updatedTemplate.getLevelId());
            template.setTemplateName(updatedTemplate.getTemplateName());
            template.setGridWidth(updatedTemplate.getGridWidth());
            template.setGridHeight(updatedTemplate.getGridHeight());
            template.setWordCount(updatedTemplate.getWordCount());
            template.setIntersectionCount(updatedTemplate.getIntersectionCount());
            template.setGridPattern(updatedTemplate.getGridPattern());
            template.setWordPositions(updatedTemplate.getWordPositions());
            template.setDifficultyRating(updatedTemplate.getDifficultyRating());
            template.setDescription(updatedTemplate.getDescription());
            template.setIsActive(updatedTemplate.getIsActive());
            template.setUpdatedAt(LocalDateTime.now());
            return pzGridTemplateRepository.save(template);
        });
    }

    /**
     * 템플릿 삭제 (논리 삭제)
     */
    @Transactional
    public boolean deleteTemplate(Long id) {
        return pzGridTemplateRepository.findById(id).map(template -> {
            template.setIsActive(false);
            template.setUpdatedAt(LocalDateTime.now());
            pzGridTemplateRepository.save(template);
            return true;
        }).orElse(false);
    }

    /**
     * 템플릿 물리 삭제
     */
    @Transactional
    public boolean hardDeleteTemplate(Long id) {
        if (pzGridTemplateRepository.existsById(id)) {
            pzGridTemplateRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * 통계 정보 조회
     */
    public Map<String, Object> getTemplateStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTemplates", pzGridTemplateRepository.countActiveTemplates());
        stats.put("averageWordCount", pzGridTemplateRepository.findAverageWordCount());
        stats.put("maxWordCount", pzGridTemplateRepository.findMaxWordCount());
        stats.put("minWordCount", pzGridTemplateRepository.findMinWordCount());
        stats.put("maxIntersectionCount", pzGridTemplateRepository.findMaxIntersectionCount());
        stats.put("minIntersectionCount", pzGridTemplateRepository.findMinIntersectionCount());

        // 레벨별 통계
        Map<Integer, Long> levelStats = new HashMap<>();
        pzGridTemplateRepository.countTemplatesByLevel().forEach(obj ->
                levelStats.put((Integer) obj[0], (Long) obj[1])
        );
        stats.put("levelStats", levelStats);

        // 그리드 크기별 통계
        Map<String, Long> gridSizeStats = new HashMap<>();
        pzGridTemplateRepository.countTemplatesByGridSize().forEach(obj ->
                gridSizeStats.put((String) obj[0], (Long) obj[1])
        );
        stats.put("gridSizeStats", gridSizeStats);

        // 카테고리별 통계는 제거됨
        Map<String, Long> categoryStats = new HashMap<>();
        stats.put("categoryStats", categoryStats);

        return stats;
    }

    /**
     * 레벨별 샘플 템플릿 조회
     */
    public List<PzGridTemplate> getSampleTemplatesByLevel() {
        // 활성화된 모든 템플릿들을 레벨별로 가져오기 (제한 없음)
        return pzGridTemplateRepository.findByIsActiveTrueOrderByLevelIdAscCreatedAtDesc();
    }

    /**
     * 특정 레벨의 샘플 템플릿 조회
     */
    public List<PzGridTemplate> getSampleTemplatesBySpecificLevel(Integer levelId) {
        return pzGridTemplateRepository.findSampleTemplatesBySpecificLevel(levelId);
    }

    /**
     * 복잡한 검색 조건으로 템플릿 조회
     */
    public List<PzGridTemplate> searchTemplatesWithFilters(Integer levelId, String templateName,
                                                           Integer minWordCount, Integer maxWordCount,
                                                           Integer minIntersectionCount, Integer maxIntersectionCount,
                                                           String category) {
        return pzGridTemplateRepository.findTemplatesWithFilters(
                levelId, templateName, minWordCount, maxWordCount,
                minIntersectionCount, maxIntersectionCount
        );
    }

    /**
     * 템플릿 복사
     */
    @Transactional
    public Optional<PzGridTemplate> copyTemplate(Long id, String newTemplateName) {
        return pzGridTemplateRepository.findById(id).map(originalTemplate -> {
            PzGridTemplate newTemplate = new PzGridTemplate();
            newTemplate.setLevelId(originalTemplate.getLevelId());
            newTemplate.setTemplateName(newTemplateName);
            newTemplate.setGridWidth(originalTemplate.getGridWidth());
            newTemplate.setGridHeight(originalTemplate.getGridHeight());
            newTemplate.setWordCount(originalTemplate.getWordCount());
            newTemplate.setIntersectionCount(originalTemplate.getIntersectionCount());
            newTemplate.setGridPattern(originalTemplate.getGridPattern());
            newTemplate.setWordPositions(originalTemplate.getWordPositions());
            newTemplate.setDifficultyRating(originalTemplate.getDifficultyRating());
            newTemplate.setDescription(originalTemplate.getDescription());
            newTemplate.setIsActive(true);
            newTemplate.setCreatedAt(LocalDateTime.now());
            newTemplate.setUpdatedAt(LocalDateTime.now());
            
            return pzGridTemplateRepository.save(newTemplate);
        });
    }

    /**
     * 선택된 템플릿들 일괄 삭제
     */
    @Transactional
    public int bulkDeleteTemplates(List<Long> ids) {
        int deletedCount = 0;
        for (Long id : ids) {
            if (deleteTemplate(id)) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    /**
     * 선택된 템플릿들 일괄 수정
     */
    @Transactional
    public int bulkUpdateTemplates(List<Long> templateIds, Map<String, Object> updateData) {
        int updatedCount = 0;
        for (Long templateId : templateIds) {
            Optional<PzGridTemplate> templateOpt = pzGridTemplateRepository.findById(templateId);
            if (templateOpt.isPresent()) {
                PzGridTemplate template = templateOpt.get();
                
                // 업데이트할 필드들 적용
                if (updateData.containsKey("levelId")) {
                    template.setLevelId((Integer) updateData.get("levelId"));
                }
                if (updateData.containsKey("difficultyRating")) {
                    template.setDifficultyRating((Integer) updateData.get("difficultyRating"));
                }
                if (updateData.containsKey("description")) {
                    template.setDescription((String) updateData.get("description"));
                }
                
                template.setUpdatedAt(LocalDateTime.now());
                pzGridTemplateRepository.save(template);
                updatedCount++;
            }
        }
        return updatedCount;
    }

    /**
     * 샘플 템플릿 조회 (모든 레벨)
     */
    public List<PzGridTemplate> getSampleTemplates() {
        return pzGridTemplateRepository.findByIsActiveTrueOrderByLevelIdAscCreatedAtDesc();
    }

    /**
     * 특정 레벨의 샘플 템플릿 조회
     */
    public List<PzGridTemplate> getSampleTemplatesByLevel(Long levelId) {
        return pzGridTemplateRepository.findByLevelIdAndIsActiveTrueOrderByCreatedAtDesc(levelId.intValue());
    }


}
