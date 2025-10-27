package com.example.board.config;

import com.example.board.entity.BoardType;
import com.example.board.repository.BoardTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 게시판 타입 초기화 클래스
 * 애플리케이션 시작 시 기본 게시판 타입들을 생성
 */
@Component
public class BoardTypeInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(BoardTypeInitializer.class);
    
    @Autowired
    private BoardTypeRepository boardTypeRepository;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("=== 게시판 타입 초기화 시작 ===");
        
        // 기본 게시판 타입들 생성
        createBoardTypeIfNotExists("project", "프로젝트 게시판", "프로젝트 관련 소통과 정보 공유", false);
        createBoardTypeIfNotExists("notice", "공지사항", "중요한 공지사항과 업데이트 소식", false);
        createBoardTypeIfNotExists("free", "자유 게시판", "자유로운 주제로 소통하는 공간", false);
        createBoardTypeIfNotExists("qna", "Q&A", "질문과 답변을 위한 게시판", false);
        createBoardTypeIfNotExists("tech", "기술 게시판", "기술 관련 정보와 토론", true);
        createBoardTypeIfNotExists("review", "리뷰 게시판", "다양한 리뷰와 후기를 공유", false);
        
        logger.info("=== 게시판 타입 초기화 완료 ===");
    }
    
    /**
     * 게시판 타입이 존재하지 않으면 생성
     */
    private void createBoardTypeIfNotExists(String slug, String name, String description, boolean requiresAuth) {
        if (!boardTypeRepository.existsBySlug(slug)) {
            BoardType boardType = new BoardType();
            boardType.setSlug(slug);
            boardType.setName(name);
            boardType.setDescription(description);
            boardType.setRequiresAuth(requiresAuth);
            boardType.setIsActive(true);
            
            boardTypeRepository.save(boardType);
            logger.info("게시판 타입 생성: {} ({})", name, slug);
        } else {
            logger.debug("게시판 타입 이미 존재: {} ({})", name, slug);
        }
    }
}
