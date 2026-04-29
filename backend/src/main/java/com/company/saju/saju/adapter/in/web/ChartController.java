package com.company.saju.saju.adapter.in.web;

import com.company.saju.common.dto.ApiResponse;
import com.company.saju.common.exception.BusinessException;
import com.company.saju.common.exception.ErrorCode;
import com.company.saju.saju.adapter.in.web.dto.ChartCreateRequest;
import com.company.saju.saju.adapter.in.web.dto.ChartResponse;
import com.company.saju.saju.adapter.in.web.dto.ChartSummaryDto;
import com.company.saju.saju.application.port.in.CreateChartUseCase;
import com.company.saju.saju.application.port.in.GetChartUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Charts", description = "사주 차트 생성/조회/이력/삭제")
@RestController
@RequestMapping("/api/v1/charts")
@RequiredArgsConstructor
public class ChartController {

    private final CreateChartUseCase createChartUseCase;
    private final GetChartUseCase getChartUseCase;

    @Operation(summary = "사주 차트 생성 (중복 입력 시 기존 차트 반환)")
    @PostMapping
    public ResponseEntity<ApiResponse<ChartResponse>> createChart(
            @Valid @RequestBody ChartCreateRequest request,
            HttpServletRequest httpRequest) {

        String userId = resolveUserId(httpRequest);
        ChartResponse response = createChartUseCase.createChart(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "차트 상세 조회 (매 GET 시 엔진 재계산)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChartResponse>> getChart(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        String userId = resolveUserId(httpRequest);
        ChartResponse response = getChartUseCase.getChart(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 차트 이력 목록 (offset 페이지네이션)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChartSummaryDto>>> listCharts(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor,
            HttpServletRequest httpRequest) {

        String userId = resolveUserId(httpRequest);
        List<ChartSummaryDto> charts = getChartUseCase.listCharts(userId, size, cursor);
        return ResponseEntity.ok(ApiResponse.success(charts));
    }

    @Operation(summary = "차트 소프트 삭제 (공유 링크 동시 폐기)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChart(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        String userId = resolveUserId(httpRequest);
        getChartUseCase.deleteChart(id, userId);
        return ResponseEntity.noContent().build();
    }

    private String resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        String userId = (String) session.getAttribute("userId");
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return userId;
    }
}
