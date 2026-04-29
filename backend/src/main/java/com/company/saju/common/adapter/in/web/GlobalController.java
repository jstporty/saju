package com.company.saju.common.adapter.in.web;

import com.company.saju.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GlobalController {

    /**
     * 루트 경로 헬스 체크 (Whitelabel Error Page 방지용)
     */
    @GetMapping("/")
    public ResponseEntity<ApiResponse<String>> root() {
        return ResponseEntity.ok(ApiResponse.success("사주 백엔드 API 서버가 정상 작동 중입니다. (v1.0.0)"));
    }
}
