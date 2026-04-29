# Hugging Face 모델 가이드

## 추천 모델 (무료 Inference API)

### 1. meta-llama/Llama-3.1-8B-Instruct (현재 설정됨)
- **장점**: 높은 성능, 긴 컨텍스트 지원 (최대 128K 토큰)
- **단점**: 무료 티어에서 Rate Limit 가능성
- **용도**: 고품질 사주 해석 필요 시

### 2. mistralai/Mistral-7B-Instruct-v0.3 (대안 추천)
- **장점**: 빠른 응답 속도, 무료 티어 안정적
- **단점**: 컨텍스트 길이 제한 (32K 토큰)
- **용도**: 빠르고 안정적인 서비스

### 3. google/flan-t5-xxl (경량 대안)
- **장점**: 무료 티어에서 매우 안정적, 빠름
- **단점**: 성능이 상대적으로 낮음
- **용도**: 개발/테스트 단계

## 모델 변경 방법

### 환경 변수로 변경
```bash
export HF_MODEL_ID="mistralai/Mistral-7B-Instruct-v0.3"
./start-server.sh
```

### application.yml에서 직접 변경
```yaml
huggingface:
  api:
    model-id: mistralai/Mistral-7B-Instruct-v0.3
```

## Rate Limit 대응

무료 티어에서 Rate Limit 발생 시:
1. 모델을 Mistral이나 Flan-T5로 변경
2. `temperature`를 낮춰서 일관된 결과 유도 (0.5~0.6)
3. `max-tokens`를 줄여서 응답 속도 향상 (512~768)

## 성능 최적화

```yaml
huggingface:
  api:
    max-tokens: 768      # 짧은 응답 (512~1024)
    temperature: 0.6     # 안정적 (0.5~0.7)
```

## 토큰 확인

현재 설정된 토큰: 환경변수 `HF_TOKEN`으로 주입 (`.env` 또는 AWS Secrets Manager)
- Hugging Face 대시보드: https://huggingface.co/settings/tokens
- Rate Limit 확인: https://huggingface.co/pricing#inference-endpoints
