package com.xagentstudy.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "answer_records")
public class AnswerRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long questionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "json")
    private String userAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean correct;
    private Integer score;
    @Column(columnDefinition = "text")
    private String feedback;
    private Long redoOfQuestionId;

    private OffsetDateTime answeredAt;

    protected AnswerRecord() {
    }

    public AnswerRecord(Long questionId, Long userId, String userAnswer, Boolean correct) {
        this(questionId, userId, userAnswer, correct, null, null, null);
    }

    public AnswerRecord(Long questionId, Long userId, String userAnswer, Boolean correct, Integer score, String feedback, Long redoOfQuestionId) {
        this.questionId = questionId;
        this.userId = userId;
        this.userAnswer = userAnswer;
        this.correct = correct;
        this.score = score;
        this.feedback = feedback;
        this.redoOfQuestionId = redoOfQuestionId;
        this.answeredAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public OffsetDateTime getAnsweredAt() {
        return answeredAt;
    }

    public Integer getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }
}
