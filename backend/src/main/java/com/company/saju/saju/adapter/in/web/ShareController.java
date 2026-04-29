package com.company.saju.saju.adapter.in.web;

import com.company.saju.common.dto.ApiResponse;
import com.company.saju.common.exception.BusinessException;
import com.company.saju.common.exception.ErrorCode;
import com.company.saju.saju.adapter.in.web.dto.PublicChartDto;
import com.company.saju.saju.adapter.in.web.dto.ShareCreatedResponse;
import com.company.saju.saju.application.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Shares", description = "공유 링크 생성 및 공개 조회")
@RestController
@RequestMapping("/api/v1/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @Operation(summary = "공유 링크 생성 (8자 slug, 1년 만료)")
    @PostMapping
    public ResponseEntity<ApiResponse<ShareCreatedResponse>> createShare(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String userId = resolveUserId(request);
        String chartId = body.get("chartId");
        if (chartId == null || chartId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        ShareCreatedResponse response = shareService.createShare(chartId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "공개 차트 조회 (비로그인 허용, PII 마스킹)")
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<PublicChartDto>> getPublicChart(@PathVariable String token) {
        PublicChartDto dto = shareService.getPublicChart(token);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    private String resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        String userId = (String) session.getAttribute("userId");
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return userId;
    }
}
