package com.company.saju.saju.adapter.out.persistence.entity;

import com.company.saju.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_fortune_template",
        uniqueConstraints = @UniqueConstraint(name = "uq_daily_fortune_lookup",
                columnNames = {"day_stem", "daily_stem", "daily_branch"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyFortuneTemplateEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_stem", columnDefinition = "CHAR(2)", nullable = false)
    private String dayStem;

    @Column(name = "daily_stem", columnDefinition = "CHAR(2)", nullable = false)
    private String dailyStem;

    @Column(name = "daily_branch", columnDefinition = "CHAR(2)", nullable = false)
    private String dailyBranch;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "lucky_color", length = 50, nullable = false)
    private String luckyColor;

    @Column(name = "lucky_hour", length = 50, nullable = false)
    private String luckyHour;

    @Column(name = "caution", length = 100, nullable = false)
    private String caution;

    @Builder
    public DailyFortuneTemplateEntity(String dayStem, String dailyStem, String dailyBranch,
                                      String message, String luckyColor, String luckyHour, String caution) {
        this.dayStem = dayStem;
        this.dailyStem = dailyStem;
        this.dailyBranch = dailyBranch;
        this.message = message;
        this.luckyColor = luckyColor != null ? luckyColor : "";
        this.luckyHour = luckyHour != null ? luckyHour : "";
        this.caution = caution != null ? caution : "";
    }
}
