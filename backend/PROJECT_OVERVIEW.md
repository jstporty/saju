# 사주 백엔드 (Saju Backend)

**🔮 전통 명리학 기반 정밀 사주 분석 시스템**

생년월일시를 기반으로 정밀한 사주 원국을 계산하고, Hugging Face LLM을 활용하여 전문가 수준의 운세 해석을 제공하는 Spring Boot 백엔드 서비스입니다.

---

## 📂 프로젝트 구조

```
saju-backend/
├── docs/
│   └── saju_ai_spec.md          # 시스템 설계 명세서
├── infra/
│   ├── db/
│   │   └── init-saju-db.sql     # DB 초기화 + 절기 데이터
│   ├── podman-compose.yml        # MySQL 컨테이너 설정
│   ├── start-infra.sh            # 인프라 시작 스크립트
│   └── stop-infra.sh             # 인프라 중단 스크립트
├── src/main/
│   ├── java/com/company/saju/
│   │   ├── SajuBackendApplication.java
│   │   ├── common/               # 공통 유틸리티
│   │   ├── auth/                 # 카카오 OAuth2 로그인
│   │   ├── user/                 # 사용자 관리
│   │   ├── saju/                 # 사주 분석 핵심 모듈
│   │   │   ├── domain/           # 도메인 모델 (FourPillars, GanJi 등)
│   │   │   ├── adapter/          # Controller & Repository
│   │   │   └── application/      # 서비스 로직
│   │   ├── engine/               # 명리학 계산 엔진
│   │   │   ├── ganji/            # 간지 변환 (60갑자)
│   │   │   ├── time/             # LMT 경도 보정
│   │   │   ├── solarterm/        # 24절기 처리
│   │   │   ├── element/          # 오행 분석
│   │   │   ├── tengods/          # 십신 계산
│   │   │   └── star/             # 신살 판별
│   │   └── ai/                   # Hugging Face LLM 연동
│   │       ├── application/      # HuggingFaceService
│   │       └── adapter/          # HuggingFaceClient
│   └── resources/
│       └── application.yml       # 설정 파일
├── build.gradle                  # Gradle 빌드 설정
├── settings.gradle
├── start-server.sh               # 서버 시작 스크립트
└── stop-server.sh                # 서버 중단 스크립트
```

---

## 🚀 빠른 시작

### 1. 사전 준비

- **JDK 21+** 설치
- **MySQL 8.0+** 또는 **Podman/Docker** (컨테이너 사용 시)
- **Hugging Face API Token** 발급 (https://huggingface.co/settings/tokens)

### 2. 인프라 실행 (MySQL)

```bash
cd infra
./start-infra.sh
# MySQL이 localhost:3306에서 실행됩니다
```

### 3. 환경 변수 설정

```bash
export HF_API_TOKEN="hf_xxxxxxxxxxxxxxxxxxxxx"
export KAKAO_CLIENT_ID="your_kakao_client_id"
export KAKAO_CLIENT_SECRET="your_kakao_client_secret"
export DB_PASSWORD="saju1234"
export JWT_SECRET="your-256-bit-secret-key-at-least-32-characters"
```

### 4. 서버 실행

```bash
./start-server.sh
```

서버는 `http://localhost:8080` 에서 실행됩니다.

---

## 📡 API 사용 예시

### 사주 분석 요청

```bash
curl -X POST http://localhost:8080/api/v1/saju/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "birthDate": "1990-05-15",
    "birthTime": "14:30:00",
    "calendarType": "SOLAR",
    "birthLongitude": 126.9780
  }'
```

**응답 예시**:

```json
{
  "success": true,
  "data": {
    "rawData": {
      "fourPillars": {
        "year": { "gan": "庚", "ji": "午", "ganji": "庚午" },
        "month": { "gan": "辛", "ji": "巳", "ganji": "辛巳" },
        "day": { "gan": "甲", "ji": "寅", "ganji": "甲寅" },
        "time": { "gan": "辛", "ji": "未", "ganji": "辛未" }
      },
      "elementsScore": {
        "wood": 2, "fire": 2, "earth": 1, "metal": 3, "water": 0
      },
      "tenGods": { "비견": 1, "편재": 2, "정관": 1 },
      "specialStars": ["도화살", "천을귀인"]
    },
    "aiInterpretation": {
      "personality": {
        "traits": ["리더십이 강함", "추진력이 뛰어남"],
        "strengths": "목표를 향한 추진력과 결단력이 탁월합니다.",
        "weaknesses": "타인의 의견을 수용하는 데 어려움이 있을 수 있습니다."
      },
      "fortunes": {
        "wealth": "재물운이 강하며, 특히 사업을 통한 수입이 좋습니다.",
        "career": "리더십을 발휘할 수 있는 직종이 적합합니다.",
        "love": "적극적인 성향으로 인해 연애에서 주도권을 잡습니다."
      },
      "advice": ["중요한 결정을 내리기 좋은 날입니다."]
    }
  }
}
```

---

## ⚙️ 주요 기능

| 기능 | 설명 |
|:---|:---|
| **정밀 시각 보정** | 경도 기반 LMT 변환으로 시주(時柱) 정확도 향상 |
| **절기 기반 월주** | 24절기 입절 시각을 기준으로 월주(月柱) 정확히 계산 |
| **60갑자 변환** | 연/월/일/시주를 천간지지 조합으로 정밀 산출 |
| **오행 분석** | 목/화/토/금/수 분포 및 균형 상태 판단 |
| **십신 계산** | 일간 기준 비견/겁재/식신/상관 등 10신 자동 분석 |
| **신살 판별** | 도화살/역마살/화개살/천을귀인 자동 탐지 |
| **AI 해석** | Hugging Face LLM을 통한 자연스러운 운세 해석 |
| **결과 캐싱** | MD5 해시 기반 중복 조회 방지 (빠른 응답) |

---

## 🛠️ 기술 스택

- **Backend**: Java 21, Spring Boot 3.2.2
- **Database**: MySQL 8.0 + JPA/Hibernate
- **LLM**: Hugging Face Inference API (Llama-3.1-8B-Instruct)
- **Build**: Gradle 8.x
- **Authentication**: JWT, OAuth2 (Kakao)
- **Container**: Podman/Docker

---

## 📖 문서

- [시스템 설계 명세서](docs/saju_ai_spec.md) - 전체 시스템 아키텍처 및 알고리즘 상세 설명

---

## 🧪 개발 환경

### 빌드

```bash
./gradlew clean build
```

### 테스트

```bash
./gradlew test
```

### 로그 확인

```bash
tail -f logs/saju-backend.log
```

### 서버 중단

```bash
./stop-server.sh
```

---

## 🔧 환경 설정 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/saju_db
    username: ${DB_USERNAME:saju}
    password: ${DB_PASSWORD:saju1234}

huggingface:
  api:
    token: ${HF_API_TOKEN}
    model-id: meta-llama/Llama-3.1-8B-Instruct

jwt:
  secret: ${JWT_SECRET}
```

---

## 🌟 향후 개발 계획

- [ ] **음력 변환**: 한국천문연구원 API 연동
- [ ] **대운/세운**: 10년 단위 대운 및 매년 세운 분석
- [ ] **궁합 분석**: 두 사용자 사주 비교 API
- [ ] **택일 서비스**: 결혼/이사/개업 길일 추천
- [ ] **일일 운세 푸시**: 매일 아침 알림 발송

---

## 📄 라이선스

이 프로젝트는 개인/상업적 용도로 자유롭게 사용 가능합니다.

---

**Powered by Spring Boot, MySQL, and Hugging Face LLM** 🚀




