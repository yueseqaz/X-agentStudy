package com.xagentstudy.qa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "qa_records")
public class QARecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(nullable = false, columnDefinition = "text")
    private String answer;

    @Column(columnDefinition = "json")
    private String citations;

    @Column(columnDefinition = "json")
    private String relatedPoints;

    private OffsetDateTime createdAt;

    protected QARecord() {
    }

    public QARecord(Long planId, Long userId, String question, String answer, String citations, String relatedPoints) {
        this.planId = planId;
        this.userId = userId;
        this.question = question;
        this.answer = answer;
        this.citations = citations;
        this.relatedPoints = relatedPoints;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getCitations() {
        return citations;
    }

    public String getRelatedPoints() {
        return relatedPoints;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
