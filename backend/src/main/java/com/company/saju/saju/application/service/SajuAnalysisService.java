package com.company.saju.saju.application.service;

import com.company.saju.common.exception.BusinessException;
import com.company.saju.common.exception.ErrorCode;
import com.company.saju.common.util.IdGenerator;
import com.company.saju.saju.adapter.in.web.dto.ChartCreateRequest;
import com.company.saju.saju.adapter.in.web.dto.ChartResponse;
import com.company.saju.saju.adapter.in.web.dto.ChartSummaryDto;
import com.company.saju.saju.adapter.out.persistence.entity.SajuChartEntity;
import com.company.saju.saju.adapter.out.persistence.repository.KeyMessageRepository;
import com.company.saju.saju.adapter.out.persistence.repository.SajuChartRepository;
import com.company.saju.saju.adapter.out.persistence.repository.SajuShareLinkRepository;
import com.company.saju.saju.application.dto.*;
import com.company.saju.saju.application.port.in.CreateChartUseCase;
import com.company.saju.saju.application.port.in.GetChartUseCase;
import com.company.saju.saju.application.port.out.SajuEnginePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SajuAnalysisService implements CreateChartUseCase, GetChartUseCase {

    private final SajuChartRepository chartRepository;
    private final SajuShareLinkRepository shareLinkRepository;
    private final KeyMessageRepository keyMessageRepository;
    private final SajuEnginePort engine;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ChartResponse createChart(ChartCreateRequest req, String userId) {
        BirthInfo birthInfo = toBirthInfo(req);
        String calculationKey = buildCalculationKey(req);

        // 중복 체크 — 동일 사용자 + 동일 입력 → 기존 차트 반환
        return chartRepository.findByUserIdAndCalculationKey(userId, calculationKey)
                .map(existing -> toChartResponse(existing))
                .orElseGet(() -> {
                    SajuAnalysis analysis = engine.analyze(birthInfo);
                    String rawPillarsJson = serializeRawPillars(analysis);
                    String warningsJson = serializeWarnings(analysis.warnings());

                    String keyMessage = keyMessageRepository
                            .findByDayStemAndDominantElementAndCategory(
                                    analysis.dayStem(), analysis.dominantElement(), "OVERALL")
                            .map(km -> km.getMessage())
                            .orElse(null);   // 시드 전 null 허용

                    SajuChartEntity entity = SajuChartEntity.builder()
                            .id(IdGenerator.generateId())
                            .userId(userId)
                            .subjectName(req.getSubjectName())
                            .subjectKind(req.getSubjectKind())
                            .birthDate(req.getBirthDate())
                            .birthTime(req.getBirthTime())
                            .calendarType(req.getCalendarType())
                            .leapMonth(req.isLeapMonth())
                            .gender(req.getGender())
                            .birthLongitude(req.getBirthLongitude())
                            .dominantElement(analysis.dominantElement())
                            .calculationKey(calculationKey)
                            .rawPillars(rawPillarsJson)
                            .warnings(warningsJson)
                            .build();

                    chartRepository.save(entity);
                    log.info("Chart created: id={} userId={} subjectKind={}", entity.getId(), userId, req.getSubjectKind());

                    return buildChartResponse(entity, analysis, keyMessage);
                });
    }

    @Override
    public ChartResponse getChart(String chartId, String userId) {
        SajuChartEntity entity = requireOwned(chartId, userId);
        SajuAnalysis analysis = engine.analyze(toBirthInfoFromEntity(entity));
        String keyMessage = keyMessageRepository
                .findByDayStemAndDominantElementAndCategory(entity.getDayStem(), entity.getDominantElement(), "OVERALL")
                .map(km -> km.getMessage())
                .orElse(null);
        return buildChartResponse(entity, analysis, keyMessage);
    }

    @Override
    public List<ChartSummaryDto> listCharts(String userId, int size, String cursor) {
        int effectiveSize = Math.min(size, 50);
        List<SajuChartEntity> charts = chartRepository.findByUserId(
                userId, PageRequest.of(0, effectiveSize));
        return charts.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteChart(String chartId, String userId) {
        SajuChartEntity entity = requireOwned(chartId, userId);
        entity.softDelete();
        shareLinkRepository.revokeAllByChartId(chartId, LocalDateTime.now());
        log.info("Chart soft-deleted: id={} userId={}", chartId, userId);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private SajuChartEntity requireOwned(String chartId, String userId) {
        SajuChartEntity entity = chartRepository.findById(chartId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHART_NOT_FOUND));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHART_NOT_FOUND);
        }
        return entity;
    }

    private ChartResponse toChartResponse(SajuChartEntity entity) {
        SajuAnalysis analysis = engine.analyze(toBirthInfoFromEntity(entity));
        String keyMessage = keyMessageRepository
                .findByDayStemAndDominantElementAndCategory(entity.getDayStem(), entity.getDominantElement(), "OVERALL")
                .map(km -> km.getMessage())
                .orElse(null);
        return buildChartResponse(entity, analysis, keyMessage);
    }

    private ChartResponse buildChartResponse(SajuChartEntity entity, SajuAnalysis analysis, String keyMessage) {
        return ChartResponse.builder()
                .id(entity.getId())
                .subjectName(entity.getSubjectName())
                .subjectKind(entity.getSubjectKind())
                .birth(ChartResponse.BirthDto.builder()
                        .birthDate(entity.getBirthDate())
                        .birthTime(entity.getBirthTime())
                        .birthTimeUnknown(entity.getBirthTime() == null)
                        .calendarType(entity.getCalendarType())
                        .isLeapMonth(entity.isLeapMonth())
                        .gender(entity.getGender())
                        .build())
                .fourPillars(analysis.fourPillars())
                .elementsScore(analysis.elementsScore())
                .tenGodsCount(analysis.tenGodsCount().entries())
                .keyMessage(keyMessage)
                .warnings(analysis.warnings())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ChartSummaryDto toSummaryDto(SajuChartEntity entity) {
        String preview = keyMessageRepository
                .findByDayStemAndDominantElementAndCategory(entity.getDayStem(), entity.getDominantElement(), "OVERALL")
                .map(km -> truncate(km.getMessage(), 30))
                .orElse(null);
        return ChartSummaryDto.builder()
                .id(entity.getId())
                .subjectName(entity.getSubjectName())
                .subjectKind(entity.getSubjectKind())
                .birthDate(entity.getBirthDate())
                .keyMessagePreview(preview)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private BirthInfo toBirthInfo(ChartCreateRequest req) {
        return new BirthInfo(
                req.getSubjectName(), req.getSubjectKind(),
                req.getBirthDate(), req.getBirthTime(), req.isBirthTimeUnknown(),
                req.getCalendarType(), req.isLeapMonth(),
                req.getGender(), req.getJasiPolicy(), req.getBirthLongitude());
    }

    private BirthInfo toBirthInfoFromEntity(SajuChartEntity e) {
        return new BirthInfo(
                e.getSubjectName(), e.getSubjectKind(),
                e.getBirthDate(), e.getBirthTime(), e.getBirthTime() == null,
                e.getCalendarType(), e.isLeapMonth(),
                e.getGender(), null, e.getBirthLongitude());
    }

    private String buildCalculationKey(ChartCreateRequest req) {
        String raw = req.getBirthDate()
                + "|" + (req.getBirthTime() != null ? req.getBirthTime() : "UNKNOWN")
                + "|" + req.getGender()
                + "|" + req.getCalendarType()
                + "|" + req.isLeapMonth()
                + "|" + req.getSubjectKind()
                + "|" + req.getSubjectName();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 64);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private String serializeRawPillars(SajuAnalysis analysis) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "fourPillars", analysis.fourPillars(),
                    "elementsScore", analysis.elementsScore(),
                    "tenGodsCount", analysis.tenGodsCount().entries()
            ));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private String serializeWarnings(List<String> warnings) {
        try {
            return objectMapper.writeValueAsString(warnings);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
