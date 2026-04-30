package com.xagentstudy.audit;

import com.xagentstudy.auth.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {
    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String targetType, Long targetId, String action, String detail) {
        Long actorUserId;
        try {
            actorUserId = AuthContext.currentUserId();
        } catch (Exception ignored) {
            actorUserId = null;
        }
        repository.save(new AuditLog(actorUserId, targetType, targetId, action, detail));
    }
}
