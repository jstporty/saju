package com.company.saju.saju.adapter.out.engine;

import com.company.saju.saju.application.dto.BirthInfo;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 동경 127.5° 기준 KST +30분 LMT 보정.
 * 1908년 이전 LMT는 MVP 스코프 외 (warnings에 추가).
 */
class KoreanLmtAdjuster {

    private static final double KOREA_LONGITUDE = 127.5;
    private static final double UTC_REFERENCE_LONGITUDE = 135.0; // KST 기준
    // LMT 오프셋: (127.5 - 135.0) / 15 * 60 = -30분
    private static final int LMT_OFFSET_MINUTES = -30;

    LocalDateTime adjust(BirthInfo birthInfo) {
        LocalTime time = birthInfo.birthTime() != null ? birthInfo.birthTime() : LocalTime.MIDNIGHT;
        LocalDateTime base = birthInfo.birthDate().atTime(time);
        return base.plusMinutes(LMT_OFFSET_MINUTES);
    }
}
