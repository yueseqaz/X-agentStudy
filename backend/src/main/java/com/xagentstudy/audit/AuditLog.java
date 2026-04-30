package com.xagentstudy.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorUserId;

    @Column(nullable = false, length = 64)
    private String targetType;

    private Long targetId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(columnDefinition = "text")
    private String detail;

    private OffsetDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(Long actorUserId, String targetType, Long targetId, String action, String detail) {
        this.actorUserId = actorUserId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.detail = detail;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
