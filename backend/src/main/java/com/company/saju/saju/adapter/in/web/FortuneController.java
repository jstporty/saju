package com.company.saju.saju.adapter.in.web;

import com.company.saju.common.dto.ApiResponse;
import com.company.saju.common.exception.BusinessException;
import com.company.saju.common.exception.ErrorCode;
import com.company.saju.saju.adapter.in.web.dto.CategoryFortunesResponse;
import com.company.saju.saju.adapter.in.web.dto.TodayFortuneResponse;
import com.company.saju.saju.application.service.FortuneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Fortunes", description = "카테고리 운세 + 오늘의 운세")
@RestController
@RequestMapping("/api/v1/charts")
@RequiredArgsConstructor
public class FortuneController {

    private final FortuneService fortuneService;

    @Operation(summary = "6개 카테고리 운세 (Caffeine 캐시)")
    @GetMapping("/{id}/categories")
    public ResponseEntity<ApiResponse<CategoryFortunesResponse>> getCategories(
            @PathVariable String id,
            HttpServletRequest request) {

        String userId = resolveUserId(request);
        return ResponseEntity.ok(ApiResponse.success(fortuneService.getCategories(id, userId)));
    }

    @Operation(summary = "오늘의 운세 (SELF 차트 전용, 24h 캐시)")
    @GetMapping("/{id}/today")
    public ResponseEntity<ApiResponse<TodayFortuneResponse>> getToday(
            @PathVariable String id,
            HttpServletRequest request) {

        String userId = resolveUserId(request);
        return ResponseEntity.ok(ApiResponse.success(fortuneService.getToday(id, userId)));
    }

    private String resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        String userId = (String) session.getAttribute("userId");
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return userId;
    }
}
