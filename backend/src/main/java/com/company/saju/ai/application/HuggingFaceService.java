package com.company.saju.ai.application;

import com.company.saju.ai.adapter.HuggingFaceClient;
import com.company.saju.saju.domain.model.FourPillars;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HuggingFaceService {

    private final HuggingFaceClient client;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Value("${huggingface.api.max-tokens:1024}")
    private int maxTokens;

    @Value("${huggingface.api.temperature:0.7}")
    private double temperature;

    /**
     * 단순 텍스트 생성 (Seeder용)
     */
    public String generateText(String prompt) {
        String raw = client.generate(prompt, maxTokens, temperature);
        return raw != null ? raw.strip() : null;
    }

    /**
     * 사주 데이터를 LLM에게 전달하여 해석 생성
     */
    public String generateInterpretation(String name,
                                       String nameHanja,
                                       String gender,
                                       FourPillars fourPillars,
                                       Map<String, Integer> elements,
                                       Map<String, Integer> tenGods,
                                       List<String> specialStars) {
        
        // 1. 프롬프트 생성
        String prompt = promptBuilder.buildSajuAnalysisPrompt(name, nameHanja, gender, fourPillars, elements, tenGods, specialStars);

        // 2. LLM API 호출
        String rawResponse = client.generate(prompt, maxTokens, temperature);

        if (rawResponse == null) {
            log.error("LLM 응답 없음. 기본 응답 반환");
            return generateFallbackResponse();
        }

        // 3. JSON 추출
        String jsonResponse = client.extractJson(rawResponse);

        // 4. JSON 검증
        if (!isValidJson(jsonResponse)) {
            log.warn("LLM 응답이 올바른 JSON 형식이 아님. 기본 응답 사용. 응답 내용: {}", jsonResponse);
            return generateFallbackResponse();
        }

        log.info("LLM 해석 생성 완료");
        return jsonResponse;
    }

    /**
     * JSON 유효성 검사
     */
    private boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * LLM 실패 시 기본 응답 생성
     */
    private String generateFallbackResponse() {
        return """
            {
              "personality": {
                "traits": ["분석 중", "데이터 처리 중", "잠시 후 재시도"],
                "strengths": "현재 AI 해석을 생성할 수 없습니다.",
                "weaknesses": "나중에 다시 시도해주세요."
              },
              "fortunes": {
                "wealth": {
                    "summary": "재물운 분석 대기 중",
                    "detail": "현재 AI 서비스가 일시적으로 응답하지 않고 있습니다. 잠시 후 다시 시도해주세요."
                },
                "career": {
                    "summary": "직업운 분석 대기 중",
                    "detail": "현재 AI 서비스가 일시적으로 응답하지 않고 있습니다. 잠시 후 다시 시도해주세요."
                },
                "love": {
                    "summary": "연애운 분석 대기 중",
                    "detail": "현재 AI 서비스가 일시적으로 응답하지 않고 있습니다. 잠시 후 다시 시도해주세요."
                },
                "health": {
                    "summary": "건강운 분석 대기 중",
                    "detail": "현재 AI 서비스가 일시적으로 응답하지 않고 있습니다. 잠시 후 다시 시도해주세요."
                }
              },
              "advice": ["잠시 후 다시 시도해주세요"]
            }
            """;
    }
}
