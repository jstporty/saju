package com.company.saju.auth.application.service;

import com.company.saju.auth.adapter.out.persistence.entity.OAuthUserEntity;
import com.company.saju.auth.adapter.out.persistence.repository.OAuthUserRepository;
import com.company.saju.auth.application.dto.UserMeResponse;
import com.company.saju.common.exception.BusinessException;
import com.company.saju.common.exception.ErrorCode;
import com.company.saju.saju.adapter.out.persistence.repository.SajuChartRepository;
import com.company.saju.saju.adapter.out.persistence.repository.SajuShareLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final OAuthUserRepository userRepository;
    private final SajuChartRepository chartRepository;
    private final SajuShareLinkRepository shareLinkRepository;

    public UserMeResponse getCurrentUser(String userId) {
        OAuthUserEntity user = findUserOrThrow(userId);
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImage(user.getProfileImage())
                .provider(user.getProvider())
                .roles(List.of(user.getRoles().split(",")))
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private OAuthUserEntity findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 회원 탈퇴 — oauth_user → saju_chart → saju_share_link 연쇄 소프트 삭제.
     * 카카오 정책상 30일 유예 기간 내 복구 가능하도록 물리 삭제는 하지 않음.
     */
    @Transactional
    public void deleteAccount(String userId) {
        OAuthUserEntity user = findUserOrThrow(userId);

        // 1. 해당 유저의 공유 링크 전부 폐기
        shareLinkRepository.revokeAllByUserId(userId, LocalDateTime.now());

        // 2. 해당 유저의 차트 전부 소프트 삭제
        chartRepository.findAllByUserId(userId)
                .forEach(chart -> chart.softDelete());

        // 3. 유저 소프트 삭제
        user.softDelete();

        log.info("Account soft-deleted: userId={}", userId);
    }
}
