package com.example.board.repository;

import com.example.board.entity.Board;
import com.example.board.entity.BoardType;
import com.example.board.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    
    Page<Board> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);
    
    List<Board> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType);
    
    List<Board> findByParentIdIsNullAndBoardTypeOrderByCreatedAtDesc(BoardType boardType);
    
    List<Board> findByParentIdOrderByCreatedAtAsc(Long parentId);
    
    @Query("SELECT b FROM Board b WHERE b.boardType = :boardType AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.user.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY b.createdAt DESC")
    Page<Board> findByBoardTypeAndSearch(@Param("boardType") BoardType boardType, 
                                        @Param("search") String search, 
                                        Pageable pageable);
    
    @Query("SELECT b FROM Board b WHERE b.boardType = :boardType AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.content) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.user.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY b.createdAt DESC")
    List<Board> findByBoardTypeAndSearch(@Param("boardType") BoardType boardType, 
                                        @Param("search") String search);
    
    List<Board> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT b FROM Board b WHERE b.parentId IS NULL AND b.boardType = :boardType ORDER BY b.createdAt DESC")
    List<Board> findRootBoardsByBoardType(@Param("boardType") BoardType boardType);
    
    // 이전 글 찾기 (현재보다 ID가 작은 가장 최근 글)
    Board findFirstByBoardTypeAndIdLessThanOrderByIdDesc(BoardType boardType, Long id);
    
    // 다음 글 찾기 (현재보다 ID가 큰 가장 먼저 작성된 글)
    Board findFirstByBoardTypeAndIdGreaterThanOrderByIdAsc(BoardType boardType, Long id);
}

