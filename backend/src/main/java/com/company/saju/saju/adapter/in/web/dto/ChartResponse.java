package com.company.saju.saju.adapter.in.web.dto;

import com.company.saju.saju.adapter.out.persistence.entity.SajuChartEntity;
import com.company.saju.saju.application.dto.ElementsScore;
import com.company.saju.saju.application.dto.FourPillarsDto;
import com.company.saju.saju.application.dto.TenGodsCount;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
public class ChartResponse {

    private String id;
    private String subjectName;
    private String subjectKind;

    private BirthDto birth;
    private FourPillarsDto fourPillars;
    private ElementsScore elementsScore;
    private List<TenGodsCount.TenGodEntry> tenGodsCount;
    private String keyMessage;   // null if seed not yet populated
    private List<String> warnings;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class BirthDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate birthDate;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime birthTime;
        private boolean birthTimeUnknown;
        private String calendarType;
        private boolean isLeapMonth;
        private String gender;
    }
}
