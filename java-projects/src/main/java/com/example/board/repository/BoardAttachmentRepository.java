package com.example.board.repository;

import com.example.board.entity.Board;
import com.example.board.entity.BoardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardAttachmentRepository extends JpaRepository<BoardAttachment, Long> {
    List<BoardAttachment> findByBoard(Board board);
}
