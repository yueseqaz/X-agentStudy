package com.xagentstudy.community;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "community_answers")
public class CommunityAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long questionId;

    private Long userId;

    @Column(nullable = false, length = 16)
    private String source;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    private OffsetDateTime createdAt;

    protected CommunityAnswer() {
    }

    public CommunityAnswer(Long questionId, Long userId, String source, String content) {
        this.questionId = questionId;
        this.userId = userId;
        this.source = source;
        this.content = content;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
