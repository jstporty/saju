package com.company.saju.user.adapter.out.persistence.entity;

import com.company.saju.common.domain.BaseEntity;
import com.company.saju.user.domain.model.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.company.saju.user.domain.model.Gender;
import com.company.saju.user.domain.model.CalendarType;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "birth_time")
    private LocalTime birthTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10, nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_type", length = 10, nullable = false)
    private CalendarType calendarType;

    @Column(name = "birth_longitude")
    private Double birthLongitude;

    @Column(name = "birth_latitude")
    private Double birthLatitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status;

    @Column(name = "login_failed_count", nullable = false)
    private Integer loginFailedCount = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Builder
    private UserJpaEntity(String id, String email, String passwordHash, String name,
                          LocalDate birthDate, LocalTime birthTime, Gender gender, CalendarType calendarType,
                          Double birthLongitude, Double birthLatitude) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.birthDate = birthDate;
        this.birthTime = birthTime;
        this.gender = gender;
        this.calendarType = calendarType;
        this.birthLongitude = birthLongitude;
        this.birthLatitude = birthLatitude;
        this.status = UserStatus.ACTIVE;
        this.loginFailedCount = 0;
    }
}
