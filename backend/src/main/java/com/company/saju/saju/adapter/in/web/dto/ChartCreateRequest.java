package com.company.saju.saju.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class ChartCreateRequest {

    @NotBlank
    private String subjectName;

    @NotNull
    private String subjectKind;   // SELF | OTHER

    @NotNull
    @Past
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime birthTime;

    private boolean birthTimeUnknown;

    private String calendarType = "SOLAR";   // SOLAR | LUNAR

    private boolean isLeapMonth;

    @NotBlank
    private String gender;   // MALE | FEMALE

    private String jasiPolicy;   // NEXT_DAY | YAJASI

    private double birthLongitude = 127.5;
}
