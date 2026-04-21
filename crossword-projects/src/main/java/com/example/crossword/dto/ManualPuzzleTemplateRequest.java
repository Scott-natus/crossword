package com.example.crossword.dto;

import com.example.crossword.entity.PzGridTemplate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualPuzzleTemplateRequest {

    private Integer levelId;
    private String templateName;
    private Integer gridWidth;
    private Integer gridHeight;
    private Integer wordCount;
    private Integer intersectionCount;
    private Integer difficultyRating;
    private String description;
    private String category;
    private String gridPattern;
    private String wordPositions;

    public PzGridTemplate toEntity() {
        PzGridTemplate entity = new PzGridTemplate();
        entity.setLevelId(levelId);
        entity.setTemplateName(templateName);
        entity.setGridWidth(gridWidth);
        entity.setGridHeight(gridHeight);
        entity.setWordCount(wordCount);
        entity.setIntersectionCount(intersectionCount);
        entity.setDifficultyRating(difficultyRating);
        entity.setDescription(description);
        entity.setCategory(category != null ? category : "manual-puzzle");
        entity.setGridPattern(gridPattern);
        entity.setWordPositions(wordPositions);
        return entity;
    }
}
