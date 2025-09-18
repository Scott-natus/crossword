package com.example.board.repository;

import com.example.board.entity.Board;
import com.example.board.entity.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {
    
    List<BoardComment> findByBoardAndParentIdIsNullOrderByCreatedAtAsc(Board board);
    
    List<BoardComment> findByParentIdOrderByCreatedAtAsc(Long parentId);
    
    @Query("SELECT c FROM BoardComment c WHERE c.board = :board AND c.parentId IS NULL ORDER BY c.createdAt ASC")
    List<BoardComment> findTopLevelCommentsByBoard(@Param("board") Board board);
    
    @Query("SELECT c FROM BoardComment c WHERE c.parentId = :parentId ORDER BY c.createdAt ASC")
    List<BoardComment> findChildCommentsByParentId(@Param("parentId") Long parentId);
}

