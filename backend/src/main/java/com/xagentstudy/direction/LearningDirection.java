package com.xagentstudy.direction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "learning_directions")
public class LearningDirection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String category;

    @Column(columnDefinition = "text")
    private String description;

    private OffsetDateTime lastActiveAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

    protected LearningDirection() {
    }

    public LearningDirection(Long userId, String name, String category, String description) {
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
        this.lastActiveAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void update(String name, String category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.updatedAt = OffsetDateTime.now();
    }

    public void archive() {
        this.deletedAt = OffsetDateTime.now();
        this.updatedAt = this.deletedAt;
    }
}
