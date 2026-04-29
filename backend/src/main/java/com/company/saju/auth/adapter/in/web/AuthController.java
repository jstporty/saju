package com.company.saju.auth.adapter.in.web;

import com.company.saju.auth.application.dto.UserMeResponse;
import com.company.saju.auth.application.service.AuthService;
import com.company.saju.common.dto.ApiResponse;
import com.company.saju.common.exception.BusinessException;
import com.company.saju.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "세션 인증")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "현재 세션 사용자 정보")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        UserMeResponse userResponse = authService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    @Operation(summary = "회원 탈퇴 — 연쇄 소프트 삭제 후 세션 무효화 (카카오 30일 유예)")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        authService.deleteAccount(userId);
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
