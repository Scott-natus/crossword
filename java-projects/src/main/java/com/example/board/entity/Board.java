package com.example.board.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
@EntityListeners(AuditingEntityListener.class)
public class Board {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "comment_notify")
    private Boolean commentNotify = false;
    
    @Column(nullable = false)
    private Long views = 0L;
    
    @Column(name = "parent_id")
    private Long parentId;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_type_id", nullable = false)
    private BoardType boardType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Board parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Board> children = new ArrayList<>();
    
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BoardAttachment> attachments = new ArrayList<>();
    
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BoardComment> comments = new ArrayList<>();
    
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BoardVote> votes = new ArrayList<>();
    
    // Constructors
    public Board() {}
    
    public Board(String title, String content, String password, User user, BoardType boardType) {
        this.title = title;
        this.content = content;
        this.password = password;
        this.user = user;
        this.boardType = boardType;
    }
    
    // Helper methods
    public List<BoardVote> getAgreeVotes() {
        return votes.stream()
                .filter(vote -> vote.getIsAgree())
                .toList();
    }
    
    public List<BoardVote> getDisagreeVotes() {
        return votes.stream()
                .filter(vote -> !vote.getIsAgree())
                .toList();
    }
    
    public BoardVote getUserVote(User user) {
        return votes.stream()
                .filter(vote -> vote.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Boolean getCommentNotify() {
        return commentNotify;
    }
    
    public void setCommentNotify(Boolean commentNotify) {
        this.commentNotify = commentNotify;
    }
    
    public Long getViews() {
        return views;
    }
    
    public void setViews(Long views) {
        this.views = views;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public BoardType getBoardType() {
        return boardType;
    }
    
    public void setBoardType(BoardType boardType) {
        this.boardType = boardType;
    }
    
    public Board getParent() {
        return parent;
    }
    
    public void setParent(Board parent) {
        this.parent = parent;
    }
    
    public List<Board> getChildren() {
        return children;
    }
    
    public void setChildren(List<Board> children) {
        this.children = children;
    }
    
    public List<BoardAttachment> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<BoardAttachment> attachments) {
        this.attachments = attachments;
    }
    
    public List<BoardComment> getComments() {
        return comments;
    }
    
    public void setComments(List<BoardComment> comments) {
        this.comments = comments;
    }
    
    public List<BoardVote> getVotes() {
        return votes;
    }
    
    public void setVotes(List<BoardVote> votes) {
        this.votes = votes;
    }
}

