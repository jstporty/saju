package com.company.saju.saju.adapter.out.persistence.entity;

import com.company.saju.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saju_share_link")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SajuShareLinkEntity extends BaseTimeEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "chart_id", length = 36, nullable = false)
    private String chartId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "token", length = 16, nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Builder
    public SajuShareLinkEntity(String id, String chartId, String userId, String token, LocalDateTime expiresAt) {
        this.id = id;
        this.chartId = chartId;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }
}
