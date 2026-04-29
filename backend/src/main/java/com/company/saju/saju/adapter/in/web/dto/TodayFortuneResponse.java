package com.company.saju.saju.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TodayFortuneResponse {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private String dayLabel;
    private String message;
    private String luckyColor;
    private String luckyHour;
    private String caution;
}
