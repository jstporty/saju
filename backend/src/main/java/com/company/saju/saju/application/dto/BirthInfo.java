package com.company.saju.saju.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 사주 계산 요청 입력 값. Controller → Service → EnginePort.
 */
public record BirthInfo(
        String subjectName,
        String subjectKind,         // SELF | OTHER
        LocalDate birthDate,
        LocalTime birthTime,        // null when birthTimeUnknown=true
        boolean birthTimeUnknown,
        String calendarType,        // SOLAR | LUNAR
        boolean isLeapMonth,
        String gender,              // MALE | FEMALE
        String jasiPolicy,          // NEXT_DAY | YAJASI | null
        double birthLongitude       // default 127.5
) {}
