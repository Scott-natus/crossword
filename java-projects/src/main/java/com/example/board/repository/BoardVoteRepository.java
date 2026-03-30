package com.example.board.repository;

import com.example.board.entity.Board;
import com.example.board.entity.BoardVote;
import com.example.board.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardVoteRepository extends JpaRepository<BoardVote, Long> {
    
    Optional<BoardVote> findByBoardAndUser(Board board, User user);
    
    List<BoardVote> findByBoardAndIsAgreeTrue(Board board);
    
    List<BoardVote> findByBoardAndIsAgreeFalse(Board board);
    
    @Query("SELECT COUNT(v) FROM BoardVote v WHERE v.board = :board AND v.isAgree = true")
    long countAgreeVotesByBoard(@Param("board") Board board);
    
    @Query("SELECT COUNT(v) FROM BoardVote v WHERE v.board = :board AND v.isAgree = false")
    long countDisagreeVotesByBoard(@Param("board") Board board);
    
    boolean existsByBoardAndUser(Board board, User user);
}

