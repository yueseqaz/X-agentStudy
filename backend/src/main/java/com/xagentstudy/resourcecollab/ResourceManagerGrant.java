package com.xagentstudy.resourcecollab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "resource_manager_grants")
public class ResourceManagerGrant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long grantedBy;

    @Column(columnDefinition = "text")
    private String note;

    @Column(nullable = false)
    private Boolean active;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected ResourceManagerGrant() {
    }

    public ResourceManagerGrant(Long userId, Long grantedBy, String note) {
        this.userId = userId;
        this.grantedBy = grantedBy;
        this.note = note;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getGrantedBy() {
        return grantedBy;
    }

    public String getNote() {
        return note;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
