package com.example.board.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 단어 수집 프롬프트 관리 서비스
 * 다양한 분야의 단어를 수집하기 위한 프롬프트 템플릿을 관리
 */
@Service
@Slf4j
public class WordCollectionPromptService {
    
    // 프롬프트 템플릿 저장소
    private final Map<String, String> promptTemplates = new HashMap<>();
    
    // 현재 사용 중인 프롬프트 타입
    private String currentPromptType = "general";
    
    public WordCollectionPromptService() {
        initializePromptTemplates();
    }
    
    /**
     * 프롬프트 템플릿 초기화
     */
    private void initializePromptTemplates() {
        // 기본 일반 단어 프롬프트
        promptTemplates.put("general", """
            한글 '%s' 로 시작하는 단어를 추천해줘

            2,3,4,5,6 음절의 단어를 세개이상 다양하게 추천해줘

            명사,대명사, 외래어를 포함해줘

            설명없이 한줄에 한단어만
            """);
        
        // 과학/기술 분야 프롬프트
        promptTemplates.put("science", """
            한글 '%s' 로 시작하는 과학, 기술, 의학 관련 단어를 추천해줘

            2,3,4,5,6 음절의 단어를 세개이상 다양하게 추천해줘

            과학 용어, 기술 용어, 의학 용어, 발명품, 자연현상 등을 포함해줘

            설명없이 한줄에 한단어만
            """);
        
        // 음식/요리 분야 프롬프트
        promptTemplates.put("food", """
            한글 '%s' 로 시작하는 음식, 요리, 식재료 관련 단어를 추천해줘

            2,3,4,5,6 음절의 단어를 세개이상 다양하게 추천해줘

            음식명, 요리법, 식재료, 조미료, 디저트 등을 포함해줘

            설명없이 한줄에 한단어만
            """);
        
        // 스포츠/운동 분야 프롬프트
        promptTemplates.put("sports", """
            한글 '%s' 로 시작하는 스포츠, 운동, 게임 관련 단어를 추천해줘

            2,3,4,5,6 음절의 단어를 세개이상 다양하게 추천해줘

            스포츠 종목, 운동 기구, 게임 용어, 경기 규칙 등을 포함해줘

            설명없이 한줄에 한단어만
            """);
        
        // 예술/문화 분야 프롬프트
        promptTemplates.put("art", """
            한글 '%s' 로 시작하는 예술, 문화, 문학 관련 단어를 추천해줘

            2,3,4,5,6 음절의 단어를 세개이상 다양하게 추천해줘

            예술 작품, 문화 용어, 문학 용어, 전통 문화 등을 포함해줘

            설명없이 한줄에 한단어만
            """);
        
        // 동물/자연 분야 프롬프트
        promptTemplates.put("nature", """
            한글 '%s' 로 시작하는 동물, 식물, 자연 관련 단어를 추천해줘

            2,3,4,5,6 음절의 단어를 세개이상 다양하게 추천해줘

            동물명, 식물명, 자연현상, 지리 용어 등을 포함해줘

            설명없이 한줄에 한단어만
            """);
        
        // 직업/업무 분야 프롬프트
        promptTemplates.put("profession", """
            한글 '%s' 로 시작하는 직업, 업무, 직장 관련 단어를 추천해줘

            2,3,4,5,6 음절의 단어를 세개이상 다양하게 추천해줘

            직업명, 업무 용어, 직장 용어, 전문 분야 등을 포함해줘

            설명없이 한줄에 한단어만
            """);
        
        // 여행/지리 분야 프롬프트
        promptTemplates.put("travel", """
            한글 '%s' 로 시작하는 여행, 지리, 관광 관련 단어를 추천해줘

            2,3,4,5,6 음절의 단어를 세개이상 다양하게 추천해줘

            지명, 관광지, 여행 용어, 교통 수단 등을 포함해줘

            설명없이 한줄에 한단어만
            """);
        
        // 패션/뷰티 분야 프롬프트
        promptTemplates.put("fashion", """
            한글 '%s' 로 시작하는 패션, 뷰티, 스타일 관련 단어를 추천해줘

            2,3,4,5,6 음절의 단어를 세개이상 다양하게 추천해줘

            의류명, 액세서리, 화장품, 스타일 용어 등을 포함해줘

            설명없이 한줄에 한단어만
            """);
        
        // 게임/엔터테인먼트 분야 프롬프트
        promptTemplates.put("entertainment", """
            한글 '%s' 로 시작하는 게임, 엔터테인먼트, 미디어 관련 단어를 추천해줘

            2,3,4,5,6 음절의 단어를 세개이상 다양하게 추천해줘

            게임 용어, 영화/드라마 제목, 미디어 용어, 엔터테인먼트 용어 등을 포함해줘

            설명없이 한줄에 한단어만
            """);
    }
    
    /**
     * 현재 프롬프트 타입 조회
     */
    public String getCurrentPromptType() {
        return currentPromptType;
    }
    
    /**
     * 프롬프트 타입 설정
     */
    public void setPromptType(String promptType) {
        if (promptTemplates.containsKey(promptType)) {
            this.currentPromptType = promptType;
            log.info("프롬프트 타입 변경: {} -> {}", currentPromptType, promptType);
        } else {
            log.warn("존재하지 않는 프롬프트 타입: {}", promptType);
        }
    }
    
    /**
     * 사용 가능한 프롬프트 타입 목록 조회
     */
    public Map<String, String> getAvailablePromptTypes() {
        Map<String, String> types = new HashMap<>();
        types.put("general", "일반 단어");
        types.put("science", "과학/기술");
        types.put("food", "음식/요리");
        types.put("sports", "스포츠/운동");
        types.put("art", "예술/문화");
        types.put("nature", "동물/자연");
        types.put("profession", "직업/업무");
        types.put("travel", "여행/지리");
        types.put("fashion", "패션/뷰티");
        types.put("entertainment", "게임/엔터테인먼트");
        return types;
    }
    
    /**
     * 프롬프트 생성
     */
    public String generatePrompt(String combination) {
        String template = promptTemplates.get(currentPromptType);
        if (template == null) {
            template = promptTemplates.get("general"); // 기본값 사용
        }
        return String.format(template, combination);
    }
    
    /**
     * 커스텀 프롬프트 추가
     */
    public void addCustomPrompt(String promptType, String promptTemplate) {
        promptTemplates.put(promptType, promptTemplate);
        log.info("커스텀 프롬프트 추가: {}", promptType);
    }
    
    /**
     * 프롬프트 템플릿 조회
     */
    public String getPromptTemplate(String promptType) {
        return promptTemplates.get(promptType);
    }
    
    /**
     * 모든 프롬프트 템플릿 조회
     */
    public Map<String, String> getAllPromptTemplates() {
        return new HashMap<>(promptTemplates);
    }
}
