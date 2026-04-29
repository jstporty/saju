package com.company.saju.saju.application.service;

import com.company.saju.common.exception.BusinessException;
import com.company.saju.common.exception.ErrorCode;
import com.company.saju.common.util.IdGenerator;
import com.company.saju.saju.adapter.in.web.dto.*;
import com.company.saju.saju.adapter.out.persistence.entity.SajuChartEntity;
import com.company.saju.saju.adapter.out.persistence.entity.SajuShareLinkEntity;
import com.company.saju.saju.adapter.out.persistence.repository.KeyMessageRepository;
import com.company.saju.saju.adapter.out.persistence.repository.SajuChartRepository;
import com.company.saju.saju.adapter.out.persistence.repository.SajuShareLinkRepository;
import com.company.saju.saju.application.dto.BirthInfo;
import com.company.saju.saju.application.dto.SajuAnalysis;
import com.company.saju.saju.application.port.out.SajuEnginePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShareService {

    private final SajuChartRepository chartRepository;
    private final SajuShareLinkRepository shareLinkRepository;
    private final KeyMessageRepository keyMessageRepository;
    private final SajuEnginePort engine;

    @Value("${app.frontend-url:https://saju.app}")
    private String frontendUrl;

    @Transactional
    public ShareCreatedResponse createShare(String chartId, String userId) {
        SajuChartEntity chart = chartRepository.findById(chartId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHART_NOT_FOUND));
        if (!chart.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHART_NOT_FOUND);
        }

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusYears(1);

        SajuShareLinkEntity link = SajuShareLinkEntity.builder()
                .id(IdGenerator.generateId())
                .chartId(chartId)
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        shareLinkRepository.save(link);

        return ShareCreatedResponse.builder()
                .token(token)
                .shareUrl(frontendUrl + "/s/" + token)
                .expiresAt(expiresAt)
                .build();
    }

    public PublicChartDto getPublicChart(String token) {
        SajuShareLinkEntity link = shareLinkRepository.findByToken(token)
                .filter(SajuShareLinkEntity::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARE_NOT_FOUND));

        SajuChartEntity chart = chartRepository.findById(link.getChartId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SHARE_NOT_FOUND));

        BirthInfo birthInfo = new BirthInfo(
                chart.getSubjectName(), chart.getSubjectKind(),
                chart.getBirthDate(), chart.getBirthTime(), chart.getBirthTime() == null,
                chart.getCalendarType(), chart.isLeapMonth(),
                chart.getGender(), null, chart.getBirthLongitude());

        SajuAnalysis analysis = engine.analyze(birthInfo);

        List<CategoryFortunesResponse.CategoryEntry> categories = keyMessageRepository
                .findAll6Categories(chart.getDayStem(), chart.getDominantElement())
                .stream()
                .map(km -> CategoryFortunesResponse.CategoryEntry.builder()
                        .category(km.getCategory())
                        .label(km.getCategory())
                        .message(km.getMessage())
                        .build())
                .collect(Collectors.toList());

        String keyMessage = keyMessageRepository
                .findByDayStemAndDominantElementAndCategory(
                        chart.getDayStem(), chart.getDominantElement(), "OVERALL")
                .map(km -> km.getMessage())
                .orElse(null);

        return PublicChartDto.builder()
                .subjectNameMasked(maskName(chart.getSubjectName()))
                .fourPillars(analysis.fourPillars())
                .elementsScore(analysis.elementsScore())
                .keyMessage(keyMessage)
                .categories(categories)
                .build();
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) return "○○○";
        if (name.length() == 1) return name + "○";
        return String.valueOf(name.charAt(0)) + "○".repeat(name.length() - 1);
    }

    private String generateToken() {
        // 8자 URL-safe slug
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
