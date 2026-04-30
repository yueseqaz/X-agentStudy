package com.xagentstudy.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long planId;

    private String sourceScope;

    @Column(length = 64)
    private String quizBatchId;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 32)
    private String difficulty;

    @Column(nullable = false, columnDefinition = "text")
    private String stem;

    @Column(columnDefinition = "json")
    private String options;

    @Column(nullable = false, columnDefinition = "json")
    private String standardAnswer;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(columnDefinition = "json")
    private String knowledgePoints;

    private OffsetDateTime createdAt;

    protected Question() {
    }

    public Question(
            Long planId,
            String sourceScope,
            String type,
            String difficulty,
            String stem,
            String options,
            String standardAnswer,
            String explanation,
            String knowledgePoints
    ) {
        this(planId, sourceScope, null, type, difficulty, stem, options, standardAnswer, explanation, knowledgePoints);
    }

    public Question(
            Long planId,
            String sourceScope,
            String quizBatchId,
            String type,
            String difficulty,
            String stem,
            String options,
            String standardAnswer,
            String explanation,
            String knowledgePoints
    ) {
        this.planId = planId;
        this.sourceScope = sourceScope;
        this.quizBatchId = quizBatchId;
        this.type = type;
        this.difficulty = difficulty;
        this.stem = stem;
        this.options = options;
        this.standardAnswer = standardAnswer;
        this.explanation = explanation;
        this.knowledgePoints = knowledgePoints;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getSourceScope() {
        return sourceScope;
    }

    public String getQuizBatchId() {
        return quizBatchId;
    }

    public String getType() {
        return type;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getStem() {
        return stem;
    }

    public String getOptions() {
        return options;
    }

    public String getStandardAnswer() {
        return standardAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getKnowledgePoints() {
        return knowledgePoints;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
