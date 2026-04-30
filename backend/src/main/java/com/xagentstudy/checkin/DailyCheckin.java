package com.xagentstudy.checkin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "daily_checkins")
public class DailyCheckin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate checkinDate;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(length = 32)
    private String mood;

    @Column(nullable = false)
    private Integer studyMinutes;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected DailyCheckin() {
    }

    public DailyCheckin(Long userId, LocalDate checkinDate) {
        this.userId = userId;
        this.checkinDate = checkinDate;
        this.studyMinutes = 0;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String summary, String mood, Integer studyMinutes) {
        this.summary = summary;
        this.mood = mood;
        this.studyMinutes = Math.max(0, studyMinutes == null ? 0 : studyMinutes);
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getCheckinDate() {
        return checkinDate;
    }

    public String getSummary() {
        return summary;
    }

    public String getMood() {
        return mood;
    }

    public Integer getStudyMinutes() {
        return studyMinutes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
