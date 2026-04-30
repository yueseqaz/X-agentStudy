package com.xagentstudy.admin;

import com.xagentstudy.audit.AuditLog;

import java.time.OffsetDateTime;

public record AdminAuditLogResponse(
        Long id,
        Long actorUserId,
        String targetType,
        Long targetId,
        String action,
        String detail,
        OffsetDateTime createdAt
) {
    public static AdminAuditLogResponse from(AuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getActorUserId(),
                log.getTargetType(),
                log.getTargetId(),
                log.getAction(),
                log.getDetail(),
                log.getCreatedAt()
        );
    }
}
