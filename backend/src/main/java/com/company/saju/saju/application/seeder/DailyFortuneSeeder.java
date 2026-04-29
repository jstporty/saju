package com.company.saju.saju.application.seeder;

import com.company.saju.ai.application.HuggingFaceService;
import com.company.saju.ai.application.PromptBuilder;
import com.company.saju.saju.adapter.out.persistence.entity.DailyFortuneTemplateEntity;
import com.company.saju.saju.adapter.out.persistence.repository.DailyFortuneTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * daily_fortune_template 시드.
 * 60 ganzhi daily pillars × 10 day stems = 600 rows.
 * 60간지: 10간 × 12지 중 유효한 조합 (천간·지지 음양이 같은 60가지)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyFortuneSeeder {

    // 60간지 (일주 순환) — 천간 홀짝과 지지 홀짝이 일치하는 유효 조합
    private static final List<String> STEMS   = List.of("甲","乙","丙","丁","戊","己","庚","辛","壬","癸");
    private static final List<String> BRANCHES = List.of("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥");

    private final DailyFortuneTemplateRepository repository;
    private final HuggingFaceService llm;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Transactional
    public int seed(Integer limit) {
        int count = 0;
        List<String[]> ganzhi60 = build60Ganzhi();
        for (String dayStem : KeyMessageSeeder.DAY_STEMS) {
            for (String[] gz : ganzhi60) {
                if (limit != null && count >= limit) return count;
                String dailyStem = gz[0];
                String dailyBranch = gz[1];
                if (repository.findByDayStemAndDailyStemAndDailyBranch(dayStem, dailyStem, dailyBranch).isPresent()) {
                    continue;
                }
                String raw = llm.generateText(promptBuilder.buildDailyFortunePrompt(dayStem, dailyStem, dailyBranch));
                if (raw == null || raw.isBlank()) {
                    log.warn("LLM empty for daily: dayStem={} gz={}{}", dayStem, dailyStem, dailyBranch);
                    continue;
                }
                DailyFortuneTemplateEntity entity = parseOrFallback(dayStem, dailyStem, dailyBranch, raw);
                repository.save(entity);
                count++;
                log.info("Seeded daily_fortune: dayStem={} daily={}{}", dayStem, dailyStem, dailyBranch);
            }
        }
        return count;
    }

    private DailyFortuneTemplateEntity parseOrFallback(String dayStem, String dailyStem, String dailyBranch, String raw) {
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start != -1 && end > start) {
                JsonNode node = objectMapper.readTree(raw.substring(start, end + 1));
                return DailyFortuneTemplateEntity.builder()
                        .dayStem(dayStem)
                        .dailyStem(dailyStem)
                        .dailyBranch(dailyBranch)
                        .message(node.path("message").asText(""))
                        .luckyColor(node.path("luckyColor").asText(""))
                        .luckyHour(node.path("luckyHour").asText(""))
                        .caution(node.path("caution").asText(""))
                        .build();
            }
        } catch (Exception e) {
            log.warn("JSON parse failed for daily fortune, storing raw text. dayStem={}", dayStem);
        }
        return DailyFortuneTemplateEntity.builder()
                .dayStem(dayStem)
                .dailyStem(dailyStem)
                .dailyBranch(dailyBranch)
                .message(raw.length() > 500 ? raw.substring(0, 500) : raw)
                .luckyColor("")
                .luckyHour("")
                .caution("")
                .build();
    }

    /**
     * 유효한 60간지 조합 생성 (천간 인덱스 % 2 == 지지 인덱스 % 2).
     */
    private List<String[]> build60Ganzhi() {
        List<String[]> result = new java.util.ArrayList<>();
        for (int i = 0; i < STEMS.size(); i++) {
            for (int j = 0; j < BRANCHES.size(); j++) {
                if (i % 2 == j % 2) {
                    result.add(new String[]{STEMS.get(i), BRANCHES.get(j)});
                }
            }
        }
        return result;
    }
}
