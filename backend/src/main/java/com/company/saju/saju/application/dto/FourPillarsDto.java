package com.company.saju.saju.application.dto;

/**
 * 사주 사주팔자 (년/월/일/시 사주).
 */
public record FourPillarsDto(
        PillarDto year,
        PillarDto month,
        PillarDto day,
        PillarDto hour    // null when birthTime is unknown
) {
    public record PillarDto(
            String stem,        // 천간 한자
            String stemKo,      // 천간 한글
            String branch,      // 지지 한자
            String branchKo,    // 지지 한글
            String stemElement, // WOOD/FIRE/EARTH/METAL/WATER
            String branchElement
    ) {}
}
