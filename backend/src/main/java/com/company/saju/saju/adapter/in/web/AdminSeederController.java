package com.company.saju.saju.adapter.in.web;

import com.company.saju.saju.application.seeder.CategoryFortuneSeeder;
import com.company.saju.saju.application.seeder.DailyFortuneSeeder;
import com.company.saju.saju.application.seeder.KeyMessageSeeder;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 오픈 전 LLM 콘텐츠 시드용 관리자 엔드포인트.
 * SecurityConfig에서 /admin/** 은 카카오 로그인 세션 보유자만 접근 가능.
 * 운영 시 ALB/IP 제한으로 추가 보호 권장.
 */
@Slf4j
@RestController
@RequestMapping("/admin/seed")
@RequiredArgsConstructor
public class AdminSeederController {

    private final KeyMessageSeeder keyMessageSeeder;
    private final CategoryFortuneSeeder categoryFortuneSeeder;
    private final DailyFortuneSeeder dailyFortuneSeeder;

    /**
     * key_message 시드 — OVERALL(50) + 5 categories × 50 = 300 → 총 350행
     * @param limit null이면 전체 시드, 값이 있으면 OVERALL/카테고리 각각 limit개까지만 시드 (테스트용)
     */
    @Operation(summary = "key_message 시드 (LLM 사전 생성, idempotent)")
    @PostMapping("/key-messages")
    public ResponseEntity<Map<String, Object>> seedKeyMessages(
            @RequestParam(required = false) Integer limit) {
        log.info("[Admin] Starting key_message seed (limit={})", limit);
        int overall    = keyMessageSeeder.seed(limit);
        int categories = categoryFortuneSeeder.seed(limit);
        int total      = overall + categories;
        log.info("[Admin] key_message seed complete: overall={} categories={} total={}", overall, categories, total);
        return ResponseEntity.ok(Map.of(
                "seeded_overall", overall,
                "seeded_categories", categories,
                "seeded_total", total
        ));
    }

    /**
     * daily_fortune_template 시드 — 10 day stems × 60 ganzhi = 600행
     * @param limit null이면 전체 시드, 값이 있으면 limit개까지만 시드 (테스트용)
     */
    @Operation(summary = "daily_fortune_template 시드 (LLM 사전 생성, idempotent)")
    @PostMapping("/daily-fortunes")
    public ResponseEntity<Map<String, Object>> seedDailyFortunes(
            @RequestParam(required = false) Integer limit) {
        log.info("[Admin] Starting daily_fortune_template seed (limit={})", limit);
        int count = dailyFortuneSeeder.seed(limit);
        log.info("[Admin] daily_fortune_template seed complete: count={}", count);
        return ResponseEntity.ok(Map.of("seeded_total", count));
    }
}
