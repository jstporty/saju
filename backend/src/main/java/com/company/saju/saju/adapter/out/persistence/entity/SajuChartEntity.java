package com.company.saju.saju.adapter.out.persistence.entity;

import com.company.saju.common.domain.BaseTimeEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "saju_chart",
        uniqueConstraints = @UniqueConstraint(name = "uq_saju_chart_user_calc",
                columnNames = {"user_id", "calculation_key"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SajuChartEntity extends BaseTimeEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "subject_name", length = 100, nullable = false)
    private String subjectName;

    @Column(name = "subject_name_hanja", length = 100)
    private String subjectNameHanja;

    @Column(name = "subject_kind", length = 10, nullable = false)
    private String subjectKind;   // SELF | OTHER

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "birth_time")
    private LocalTime birthTime;

    @Column(name = "calendar_type", length = 10, nullable = false)
    private String calendarType;

    @Column(name = "is_leap_month", nullable = false)
    private boolean leapMonth;

    @Column(name = "gender", length = 10, nullable = false)
    private String gender;

    @Column(name = "birth_longitude", nullable = false)
    private double birthLongitude;

    @Column(name = "dominant_element", length = 10, nullable = false)
    private String dominantElement;

    @Column(name = "calculation_key", length = 64, nullable = false)
    private String calculationKey;

    @Column(name = "raw_pillars", columnDefinition = "JSON", nullable = false)
    private String rawPillars;   // JSON: FourPillarsDto + elementsScore + tenGodsCount 직렬화

    @Column(name = "warnings", columnDefinition = "JSON")
    private String warnings;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public SajuChartEntity(String id, String userId, String subjectName, String subjectKind,
                            LocalDate birthDate, LocalTime birthTime, String calendarType,
                            boolean leapMonth, String gender, double birthLongitude,
                            String dominantElement, String calculationKey,
                            String rawPillars, String warnings) {
        this.id = id;
        this.userId = userId;
        this.subjectName = subjectName;
        this.subjectKind = subjectKind;
        this.birthDate = birthDate;
        this.birthTime = birthTime;
        this.calendarType = calendarType;
        this.leapMonth = leapMonth;
        this.gender = gender;
        this.birthLongitude = birthLongitude;
        this.dominantElement = dominantElement;
        this.calculationKey = calculationKey;
        this.rawPillars = rawPillars;
        this.warnings = warnings;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /** 일간 천간 한자 추출 (raw_pillars JSON에서 day.stem 읽기). */
    @SuppressWarnings("unchecked")
    public String getDayStem() {
        try {
            Map<String, Object> raw = MAPPER.readValue(rawPillars, new TypeReference<>() {});
            Map<String, Object> fourPillars = (Map<String, Object>) raw.get("fourPillars");
            Map<String, Object> day = (Map<String, Object>) fourPillars.get("day");
            return (String) day.get("stem");
        } catch (Exception e) {
            return "甲"; // fallback
        }
    }
}
