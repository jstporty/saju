package com.company.saju.saju.adapter.out.engine;

import com.company.saju.common.exception.BusinessException;
import com.company.saju.common.exception.ErrorCode;
import com.company.saju.saju.application.dto.*;
import com.company.saju.saju.application.port.out.SajuEnginePort;
import com.nlf.calendar.Solar;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.EightChar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * lunar-java 기반 사주 엔진 어댑터 (ADR-13).
 * 한국식 보정: KST +30분 LMT (KoreanLmtAdjuster), 자시/야자시 (HourPillarAdjuster).
 */
@Slf4j
@Component
public class LunarJavaSajuEngine implements SajuEnginePort {

    private static final LocalDate MIN_BIRTH_DATE = LocalDate.of(1900, 1, 1);
    private static final int MIN_AGE = 13;

    private final KoreanLmtAdjuster lmtAdjuster = new KoreanLmtAdjuster();
    private final HourPillarAdjuster hourAdjuster = new HourPillarAdjuster();
    private final ElementAnalyzer elementAnalyzer = new ElementAnalyzer();
    private final TenGodsService tenGodsService = new TenGodsService();
    private final DailyStemBranchProvider dailyProvider = new DailyStemBranchProvider();

    @Override
    public SajuAnalysis analyze(BirthInfo birthInfo) {
        validate(birthInfo);

        List<String> warnings = new ArrayList<>();

        if (birthInfo.birthTimeUnknown()) {
            warnings.add("BIRTH_TIME_UNKNOWN");
        }

        LocalDateTime adjusted = lmtAdjuster.adjust(birthInfo);

        Solar solar = buildSolar(birthInfo, adjusted);
        Lunar lunar = solar.getLunar();
        EightChar bazi = lunar.getEightChar();

        FourPillarsDto raw = mapToFourPillars(bazi, birthInfo.birthTimeUnknown());
        FourPillarsDto pillars = hourAdjuster.adjust(raw, birthInfo);

        ElementsScore elementsScore = elementAnalyzer.analyze(pillars);
        String dominantElement = elementAnalyzer.dominant(elementsScore);
        TenGodsCount tenGodsCount = tenGodsService.count(pillars);
        String dayStem = pillars.day().stem();

        return new SajuAnalysis(pillars, elementsScore, dominantElement, tenGodsCount, dayStem, warnings);
    }

    @Override
    public DayPillar getDayPillar(LocalDate date) {
        return dailyProvider.of(date);
    }

    private void validate(BirthInfo birthInfo) {
        if (birthInfo.birthDate().isBefore(MIN_BIRTH_DATE)) {
            throw new BusinessException(ErrorCode.BIRTH_DATE_OUT_OF_RANGE);
        }
        if (birthInfo.birthDate().isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.BIRTH_DATE_OUT_OF_RANGE);
        }
        int age = Period.between(birthInfo.birthDate(), LocalDate.now()).getYears();
        if (age < MIN_AGE) {
            throw new BusinessException(ErrorCode.MINOR_BLOCKED);
        }
        if (!birthInfo.birthTimeUnknown() && birthInfo.birthTime() != null) {
            LocalTime t = birthInfo.birthTime();
            if (!t.isBefore(LocalTime.of(23, 0)) && (birthInfo.jasiPolicy() == null || birthInfo.jasiPolicy().isBlank())) {
                throw new BusinessException(ErrorCode.HOUR_AMBIGUOUS);
            }
        }
    }

    private Solar buildSolar(BirthInfo info, LocalDateTime adjusted) {
        int hour = adjusted.getHour();
        int minute = adjusted.getMinute();
        // lunar-java는 분 단위 미지원, 시 단위로 전달
        if ("LUNAR".equals(info.calendarType())) {
            // lunar-java Lunar.fromYmd() takes 3 args; leap month is set separately
            Lunar lunar = Lunar.fromYmd(
                    adjusted.getYear(), adjusted.getMonthValue(), adjusted.getDayOfMonth());
            return lunar.getSolar();
        }
        return Solar.fromYmdHms(
                adjusted.getYear(), adjusted.getMonthValue(), adjusted.getDayOfMonth(),
                hour, minute, 0);
    }

    private FourPillarsDto mapToFourPillars(EightChar bazi, boolean hourUnknown) {
        FourPillarsDto.PillarDto year = toPillarDto(bazi.getYear(), bazi.getYearNaYin());
        FourPillarsDto.PillarDto month = toPillarDto(bazi.getMonth(), bazi.getMonthNaYin());
        FourPillarsDto.PillarDto day = toPillarDto(bazi.getDay(), bazi.getDayNaYin());
        FourPillarsDto.PillarDto hour = hourUnknown ? null : toPillarDto(bazi.getTime(), bazi.getTimeNaYin());
        return new FourPillarsDto(year, month, day, hour);
    }

    private FourPillarsDto.PillarDto toPillarDto(String ganZhi, String naYin) {
        if (ganZhi == null || ganZhi.length() < 2) {
            return new FourPillarsDto.PillarDto("?", "?", "?", "?", "EARTH", "EARTH");
        }
        String stem = String.valueOf(ganZhi.charAt(0));
        String branch = String.valueOf(ganZhi.charAt(1));
        return new FourPillarsDto.PillarDto(
                stem, stemKo(stem),
                branch, branchKo(branch),
                stemElement(stem), branchElement(branch));
    }

    // ── 한글 이름 / 오행 매핑 ──────────────────────────────────────────

    private static final java.util.Map<String, String> STEM_KO = java.util.Map.of(
            "甲", "갑", "乙", "을", "丙", "병", "丁", "정",
            "戊", "무", "己", "기", "庚", "경", "辛", "신", "壬", "임", "癸", "계");
    private static final java.util.Map<String, String> BRANCH_KO = java.util.Map.ofEntries(
            java.util.Map.entry("子", "자"), java.util.Map.entry("丑", "축"),
            java.util.Map.entry("寅", "인"), java.util.Map.entry("卯", "묘"),
            java.util.Map.entry("辰", "진"), java.util.Map.entry("巳", "사"),
            java.util.Map.entry("午", "오"), java.util.Map.entry("未", "미"),
            java.util.Map.entry("申", "신"), java.util.Map.entry("酉", "유"),
            java.util.Map.entry("戌", "술"), java.util.Map.entry("亥", "해"));
    private static final java.util.Map<String, String> STEM_ELEM = java.util.Map.of(
            "甲", "WOOD", "乙", "WOOD", "丙", "FIRE", "丁", "FIRE",
            "戊", "EARTH", "己", "EARTH", "庚", "METAL", "辛", "METAL", "壬", "WATER", "癸", "WATER");
    private static final java.util.Map<String, String> BRANCH_ELEM = java.util.Map.ofEntries(
            java.util.Map.entry("子", "WATER"), java.util.Map.entry("丑", "EARTH"),
            java.util.Map.entry("寅", "WOOD"), java.util.Map.entry("卯", "WOOD"),
            java.util.Map.entry("辰", "EARTH"), java.util.Map.entry("巳", "FIRE"),
            java.util.Map.entry("午", "FIRE"), java.util.Map.entry("未", "EARTH"),
            java.util.Map.entry("申", "METAL"), java.util.Map.entry("酉", "METAL"),
            java.util.Map.entry("戌", "EARTH"), java.util.Map.entry("亥", "WATER"));

    private String stemKo(String s) { return STEM_KO.getOrDefault(s, s); }
    private String branchKo(String b) { return BRANCH_KO.getOrDefault(b, b); }
    private String stemElement(String s) { return STEM_ELEM.getOrDefault(s, "EARTH"); }
    private String branchElement(String b) { return BRANCH_ELEM.getOrDefault(b, "EARTH"); }
}
