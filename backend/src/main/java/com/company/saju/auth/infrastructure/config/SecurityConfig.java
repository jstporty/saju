package com.company.saju.auth.infrastructure.config;

import com.company.saju.auth.adapter.out.oauth2.KakaoOAuthSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KakaoOAuthSuccessHandler kakaoOAuthSuccessHandler;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOriginsRaw;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())   // MVP: SameSite=None+Secure+Origin 검증으로 보강
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/oauth2/**", "/login/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/shares/**").permitAll()
                .requestMatchers("/actuator/health/**", "/error", "/").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Admin seeder: 카카오 로그인 세션 보유자만 접근 가능. 운영은 IP 제한 추가 권장.
                .requestMatchers("/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh.authenticationEntryPoint(apiAwareEntryPoint()))
            .oauth2Login(oauth2 -> oauth2
                .successHandler(kakaoOAuthSuccessHandler)
                .failureUrl("https://saju.app/?error=oauth")
            )
            .logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .deleteCookies("SESSION")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204))
            );

        return http.build();
    }

    /**
     * /api/** 요청은 401 JSON, 그 외(/oauth2/**, 정적/페이지 등)는 OAuth 로그인으로 redirect.
     */
    private AuthenticationEntryPoint apiAwareEntryPoint() {
        LinkedHashMap<org.springframework.security.web.util.matcher.RequestMatcher, AuthenticationEntryPoint> map = new LinkedHashMap<>();
        map.put(new AntPathRequestMatcher("/api/**"), new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        map.put(new AntPathRequestMatcher("/admin/**"), new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        DelegatingAuthenticationEntryPoint delegating = new DelegatingAuthenticationEntryPoint(map);
        delegating.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/kakao"));
        return delegating;
    }

    @Bean
    public RequestCache requestCache() {
        return new NullRequestCache();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOriginsRaw.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Idempotency-Key"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
