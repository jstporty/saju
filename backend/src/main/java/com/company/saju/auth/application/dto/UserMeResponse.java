package com.company.saju.auth.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserMeResponse {

    private String id;
    private String email;
    private String name;
    private String profileImage;
    private String provider;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
