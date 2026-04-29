package com.company.saju.saju.application.port.out;

import com.company.saju.saju.application.dto.BirthInfo;
import com.company.saju.saju.application.dto.DayPillar;
import com.company.saju.saju.application.dto.SajuAnalysis;

import java.time.LocalDate;

/**
 * 사주 엔진 포트. LunarJavaSajuEngine이 구현.
 */
public interface SajuEnginePort {

    /**
     * 출생 정보로 사주 분석 결과 계산.
     * 한국식 보정(KST +30분 LMT, 자시/야자시)은 어댑터 내부에서 처리.
     */
    SajuAnalysis analyze(BirthInfo birthInfo);

    /**
     * 임의 날짜의 일진(천간/지지) 계산. 오늘의 운세 룩업 키로 사용.
     */
    DayPillar getDayPillar(LocalDate date);
}
