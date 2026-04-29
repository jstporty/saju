package com.company.saju.saju.application.seeder;

import com.company.saju.ai.application.HuggingFaceService;
import com.company.saju.ai.application.PromptBuilder;
import com.company.saju.saju.adapter.out.persistence.entity.KeyMessageEntity;
import com.company.saju.saju.adapter.out.persistence.repository.KeyMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * OVERALL 카테고리 key_message 시드 (10 stems × 5 elements = 50 rows)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeyMessageSeeder {

    static final List<String> DAY_STEMS = List.of("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸");
    static final List<String> ELEMENTS  = List.of("WOOD", "FIRE", "EARTH", "METAL", "WATER");
    private static final String CATEGORY = "OVERALL";

    private final KeyMessageRepository repository;
    private final HuggingFaceService llm;
    private final PromptBuilder promptBuilder;

    @Transactional
    public int seed(Integer limit) {
        int count = 0;
        for (String stem : DAY_STEMS) {
            for (String element : ELEMENTS) {
                if (limit != null && count >= limit) return count;
                if (repository.findByDayStemAndDominantElementAndCategory(stem, element, CATEGORY).isPresent()) {
                    log.debug("Skip existing key_message: stem={} element={} category={}", stem, element, CATEGORY);
                    continue;
                }
                String message = llm.generateText(promptBuilder.buildKeyMessagePrompt(stem, element, CATEGORY));
                if (message == null || message.isBlank()) {
                    log.warn("LLM returned empty for key_message: stem={} element={}", stem, element);
                    continue;
                }
                repository.save(KeyMessageEntity.builder()
                        .dayStem(stem)
                        .dominantElement(element)
                        .category(CATEGORY)
                        .message(message)
                        .build());
                count++;
                log.info("Seeded key_message OVERALL: stem={} element={}", stem, element);
            }
        }
        return count;
    }
}
