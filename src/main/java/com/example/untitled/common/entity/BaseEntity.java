package com.example.untitled.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // @CreatedBy (まだ認証系が無いので使わない)
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // @LastModifiedBy (まだ認証系が無いので使わない)
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @PrePersist
    protected void setDefaultAuditFields() {
        if (this.createdBy == null) {
            this.createdBy = "guest";
        }
        if (this.updatedBy == null) {
            this.updatedBy = "guest";
        }
    }

    @PreUpdate
    protected void updateAuditFields() {
        if (this.updatedBy == null) {
            this.updatedBy = "guest";
        }
    }
}
