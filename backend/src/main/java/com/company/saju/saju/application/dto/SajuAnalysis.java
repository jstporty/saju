package com.company.saju.saju.application.dto;

import java.util.List;

/**
 * 사주 엔진 분석 결과.
 * SajuEnginePort.analyze() 반환값.
 */
public record SajuAnalysis(
        FourPillarsDto fourPillars,
        ElementsScore elementsScore,
        String dominantElement,         // WOOD/FIRE/EARTH/METAL/WATER — max elementsScore, 동점=목>화>토>금>수
        TenGodsCount tenGodsCount,      // 6장 (count 내림차순 5 + 일주 고정)
        String dayStem,                 // 일간 천간 한자 (key_message 룩업 키)
        List<String> warnings
) {}
