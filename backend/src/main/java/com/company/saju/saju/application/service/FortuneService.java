package com.company.saju.saju.application.service;

import com.company.saju.common.exception.BusinessException;
import com.company.saju.common.exception.ErrorCode;
import com.company.saju.saju.adapter.in.web.dto.CategoryFortunesResponse;
import com.company.saju.saju.adapter.in.web.dto.CategoryFortunesResponse.CategoryEntry;
import com.company.saju.saju.adapter.in.web.dto.TodayFortuneResponse;
import com.company.saju.saju.adapter.out.persistence.entity.SajuChartEntity;
import com.company.saju.saju.adapter.out.persistence.repository.DailyFortuneTemplateRepository;
import com.company.saju.saju.adapter.out.persistence.repository.KeyMessageRepository;
import com.company.saju.saju.adapter.out.persistence.repository.SajuChartRepository;
import com.company.saju.saju.application.dto.DayPillar;
import com.company.saju.saju.application.port.out.SajuEnginePort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FortuneService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)");

    private static final List<Map.Entry<String, String>> CATEGORY_META = List.of(
            Map.entry("OVERALL", "총운"),
            Map.entry("WEALTH",  "금전운"),
            Map.entry("LOVE",    "연애운"),
            Map.entry("HEALTH",  "건강운"),
            Map.entry("CAREER",  "직업·학업운"),
            Map.entry("FAMILY",  "가족·인간관계운")
    );

    private static final Map<String, String> CATEGORY_ICON = Map.of(
            "OVERALL", "✨", "WEALTH", "💰", "LOVE", "💕",
            "HEALTH", "🩺", "CAREER", "🎯", "FAMILY", "👨‍👩‍👧");

    private final SajuChartRepository chartRepository;
    private final KeyMessageRepository keyMessageRepository;
    private final DailyFortuneTemplateRepository dailyRepository;
    private final SajuEnginePort engine;

    @Cacheable(value = "keyMessage", key = "#chartId")
    public CategoryFortunesResponse getCategories(String chartId, String userId) {
        SajuChartEntity chart = requireOwned(chartId, userId);

        List<CategoryEntry> entries = CATEGORY_META.stream()
                .map(entry -> {
                    String msg = keyMessageRepository
                            .findByDayStemAndDominantElementAndCategory(
                                    chart.getDayStem(), chart.getDominantElement(), entry.getKey())
                            .map(km -> km.getMessage())
                            .orElse(null);
                    return CategoryEntry.builder()
                            .category(entry.getKey())
                            .label(entry.getValue())
                            .icon(CATEGORY_ICON.getOrDefault(entry.getKey(), ""))
                            .message(msg)
                            .build();
                })
                .collect(Collectors.toList());

        return CategoryFortunesResponse.builder().categories(entries).build();
    }

    @Cacheable(value = "todayFortune", key = "#chartId + ':' + T(java.time.LocalDate).now(T(java.time.ZoneId).of('Asia/Seoul'))")
    public TodayFortuneResponse getToday(String chartId, String userId) {
        SajuChartEntity chart = requireOwned(chartId, userId);

        if (!"SELF".equals(chart.getSubjectKind())) {
            throw new BusinessException(ErrorCode.TODAY_NOT_ALLOWED_FOR_OTHER);
        }

        LocalDate today = LocalDate.now(KST);
        DayPillar todayPillar = engine.getDayPillar(today);

        return dailyRepository
                .findByDayStemAndDailyStemAndDailyBranch(
                        chart.getDayStem(), todayPillar.stem(), todayPillar.branch())
                .map(t -> TodayFortuneResponse.builder()
                        .date(today)
                        .dayLabel(today.format(DAY_LABEL))
                        .message(t.getMessage())
                        .luckyColor(t.getLuckyColor())
                        .luckyHour(t.getLuckyHour())
                        .caution(t.getCaution())
                        .build())
                .orElse(TodayFortuneResponse.builder()
                        .date(today)
                        .dayLabel(today.format(DAY_LABEL))
                        .message(null)  // 시드 전 null 허용
                        .build());
    }

    private SajuChartEntity requireOwned(String chartId, String userId) {
        SajuChartEntity chart = chartRepository.findById(chartId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHART_NOT_FOUND));
        if (!chart.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHART_NOT_FOUND);
        }
        return chart;
    }
}
