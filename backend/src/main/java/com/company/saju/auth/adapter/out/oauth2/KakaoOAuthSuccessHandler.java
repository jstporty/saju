package com.company.saju.auth.adapter.out.oauth2;

import com.company.saju.auth.adapter.out.persistence.entity.OAuthUserEntity;
import com.company.saju.auth.adapter.out.persistence.repository.OAuthUserRepository;
import com.company.saju.common.util.IdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuthUserRepository userRepository;

    @Value("${app.frontend-url:https://saju.app}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String providerId = String.valueOf(oAuth2User.getAttribute("id"));
        String email = extractEmail(oAuth2User);
        String name = extractName(oAuth2User);
        String profileImage = extractProfileImage(oAuth2User);

        OAuthUserEntity user = userRepository.findByProviderAndProviderId("kakao", providerId)
                .map(existing -> {
                    existing.updateProfile(name, profileImage);
                    existing.updateLastLogin();
                    return existing;
                })
                .orElseGet(() -> createUser("kakao", providerId, email, name, profileImage));

        // Spring Security session is already created at this point.
        // Store userId in session for use by AuthController /me endpoint.
        request.getSession().setAttribute("userId", user.getId());
        request.getSession().setAttribute("userEmail", user.getEmail());

        log.info("Kakao OAuth login success - userId: {}", user.getId());

        // Redirect: no chart yet → onboarding, existing user → last chart
        // Frontend reads session via GET /api/v1/auth/me after redirect.
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/auth/kakao/callback");
    }

    private OAuthUserEntity createUser(String provider, String providerId, String email,
                                       String name, String profileImage) {
        OAuthUserEntity newUser = OAuthUserEntity.builder()
                .id(IdGenerator.generateId())
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .name(name)
                .profileImage(profileImage)
                .build();
        newUser.updateLastLogin();
        return userRepository.save(newUser);
    }

    @SuppressWarnings("unchecked")
    private String extractEmail(OAuth2User user) {
        Map<String, Object> kakaoAccount = user.getAttribute("kakao_account");
        if (kakaoAccount != null) {
            String email = (String) kakaoAccount.get("email");
            if (email != null && !email.isBlank()) return email;
        }
        return "kakao_" + user.getAttribute("id") + "@temp.example.com";
    }

    @SuppressWarnings("unchecked")
    private String extractName(OAuth2User user) {
        Map<String, Object> kakaoAccount = user.getAttribute("kakao_account");
        if (kakaoAccount != null) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                String nickname = (String) profile.get("nickname");
                if (nickname != null && !nickname.isBlank()) return nickname;
            }
        }
        return "user_" + user.getAttribute("id");
    }

    @SuppressWarnings("unchecked")
    private String extractProfileImage(OAuth2User user) {
        Map<String, Object> kakaoAccount = user.getAttribute("kakao_account");
        if (kakaoAccount != null) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) return (String) profile.get("profile_image_url");
        }
        return null;
    }
}
