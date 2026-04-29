package com.company.saju.saju.adapter.in.web.dto;

import com.company.saju.saju.application.dto.ElementsScore;
import com.company.saju.saju.application.dto.FourPillarsDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PublicChartDto {
    private String subjectNameMasked;
    private FourPillarsDto fourPillars;
    private ElementsScore elementsScore;
    private String keyMessage;
    private List<CategoryFortunesResponse.CategoryEntry> categories;
    // tenGodsCount excluded — 성격 카드는 본인 로그인 화면 전용
    // birthDate/birthTime/birthLongitude excluded — PII
}
