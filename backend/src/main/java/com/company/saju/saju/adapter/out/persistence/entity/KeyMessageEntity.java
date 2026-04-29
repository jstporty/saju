package com.company.saju.saju.adapter.out.persistence.entity;

import com.company.saju.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "key_message",
        uniqueConstraints = @UniqueConstraint(name = "uq_key_message_lookup",
                columnNames = {"day_stem", "dominant_element", "category"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KeyMessageEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_stem", columnDefinition = "CHAR(2)", nullable = false)
    private String dayStem;

    @Column(name = "dominant_element", length = 10, nullable = false)
    private String dominantElement;

    @Column(name = "category", length = 20, nullable = false)
    private String category;   // OVERALL/WEALTH/LOVE/HEALTH/CAREER/FAMILY

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Builder
    public KeyMessageEntity(String dayStem, String dominantElement, String category, String message) {
        this.dayStem = dayStem;
        this.dominantElement = dominantElement;
        this.category = category;
        this.message = message;
    }
}
