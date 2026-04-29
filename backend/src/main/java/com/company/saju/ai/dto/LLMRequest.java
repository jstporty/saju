package com.company.saju.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMRequest {
    private String inputs;
    private Parameters parameters;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Parameters {
        private Integer maxNewTokens;
        private Double temperature;
        private Double topP;
        private Boolean returnFullText;
    }
}
