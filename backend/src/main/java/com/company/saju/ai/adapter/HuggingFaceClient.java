package com.company.saju.ai.adapter;

import com.company.saju.ai.dto.LLMRequest;
import com.company.saju.ai.dto.LLMResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HuggingFaceClient {

    private final RestClient restClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    
    @Value("${huggingface.api.base-url}")
    private String baseUrl;
    
    @Value("${huggingface.api.token}")
    private String apiToken;
    
    @Value("${huggingface.api.model-id}")
    private String modelId;

    public HuggingFaceClient(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Hugging Face Inference API 호출 (OpenAI 호환)
     */
    public String generate(String prompt, int maxTokens, double temperature) {
        try {
            String url = baseUrl + "/v1/chat/completions";
            
            // OpenAI 형식의 요청 생성
            var requestBody = Map.of(
                "model", modelId,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", maxTokens,
                "temperature", temperature
            );

            log.debug("HF API 호출: {}", url);
            
            String response = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .body(requestBody)
                .retrieve()
                .body(String.class);

            log.debug("HF API 응답 길이: {} characters", response != null ? response.length() : 0);
            if (response != null) {
                log.debug("HF API 원본 응답: {}", response);
            }
            
            // OpenAI 형식 응답에서 content 추출
            return extractContentFromOpenAIResponse(response);

        } catch (Exception e) {
            log.error("HF API 호출 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * OpenAI 형식 응답에서 content 추출
     */
    private String extractContentFromOpenAIResponse(String response) {
        if (response == null) return null;
        
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                com.fasterxml.jackson.databind.JsonNode message = root.get("choices").get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
            return response;
        } catch (Exception e) {
            log.warn("Content 추출 실패, 원본 반환", e);
            return response;
        }
    }

    /**
     * JSON 추출 (LLM이 불필요한 텍스트를 포함할 경우 대비)
     */
    public String extractJson(String response) {
        if (response == null) return null;

        // JSON 블록 추출 시도
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }

        return response;
    }
}
