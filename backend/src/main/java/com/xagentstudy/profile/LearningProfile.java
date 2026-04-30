package com.xagentstudy.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "learning_profiles")
public class LearningProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long directionId;

    @Column(columnDefinition = "text")
    private String goal;

    @Column(length = 64)
    private String currentLevel;

    @Column(length = 128)
    private String timeBudget;

    @Column(columnDefinition = "text")
    private String preference;

    @Column(columnDefinition = "json")
    private String risks;

    @Column(columnDefinition = "text")
    private String strategy;

    @Column(columnDefinition = "json")
    private String rawConversation;

    private OffsetDateTime createdAt;

    protected LearningProfile() {
    }

    public LearningProfile(
            Long directionId,
            String goal,
            String currentLevel,
            String timeBudget,
            String preference,
            String risks,
            String strategy,
            String rawConversation
    ) {
        this.directionId = directionId;
        this.goal = goal;
        this.currentLevel = currentLevel;
        this.timeBudget = timeBudget;
        this.preference = preference;
        this.risks = risks;
        this.strategy = strategy;
        this.rawConversation = rawConversation;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getDirectionId() {
        return directionId;
    }

    public String getGoal() {
        return goal;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public String getTimeBudget() {
        return timeBudget;
    }

    public String getPreference() {
        return preference;
    }

    public String getRisks() {
        return risks;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getRawConversation() {
        return rawConversation;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
