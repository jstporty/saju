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
 * 6개 카테고리 key_message 시드 (10 stems × 5 elements × 6 categories = 300 rows)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryFortuneSeeder {

    private static final List<String> CATEGORIES = List.of("WEALTH", "LOVE", "HEALTH", "CAREER", "FAMILY");

    private final KeyMessageRepository repository;
    private final HuggingFaceService llm;
    private final PromptBuilder promptBuilder;

    @Transactional
    public int seed(Integer limit) {
        int count = 0;
        for (String stem : KeyMessageSeeder.DAY_STEMS) {
            for (String element : KeyMessageSeeder.ELEMENTS) {
                for (String category : CATEGORIES) {
                    if (limit != null && count >= limit) return count;
                    if (repository.findByDayStemAndDominantElementAndCategory(stem, element, category).isPresent()) {
                        log.debug("Skip existing: stem={} element={} category={}", stem, element, category);
                        continue;
                    }
                    String message = llm.generateText(promptBuilder.buildKeyMessagePrompt(stem, element, category));
                    if (message == null || message.isBlank()) {
                        log.warn("LLM returned empty for category: stem={} element={} category={}", stem, element, category);
                        continue;
                    }
                    repository.save(KeyMessageEntity.builder()
                            .dayStem(stem)
                            .dominantElement(element)
                            .category(category)
                            .message(message)
                            .build());
                    count++;
                    log.info("Seeded key_message {}: stem={} element={}", category, stem, element);
                }
            }
        }
        return count;
    }
}
