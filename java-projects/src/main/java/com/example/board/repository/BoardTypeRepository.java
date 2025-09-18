package com.example.board.repository;

import com.example.board.entity.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardTypeRepository extends JpaRepository<BoardType, Long> {
    
    Optional<BoardType> findBySlug(String slug);
    
    List<BoardType> findByIsActiveTrueOrderBySortOrderAscNameAsc();
    
    @Query("SELECT bt FROM BoardType bt WHERE bt.isActive = true ORDER BY bt.sortOrder, bt.name")
    List<BoardType> findActiveBoardTypesOrdered();
}

