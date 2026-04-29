package com.company.saju.saju.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ChartSummaryDto {
    private String id;
    private String subjectName;
    private String subjectKind;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    private String keyMessagePreview;   // null or truncated 30자
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
}
