# 사주 백엔드 (Saju Backend)

**전통 명리학 기반 정밀 사주 분석 시스템**

Hugging Face LLM을 활용하여 사용자의 생년월일시를 기반으로 사주 원국을 계산하고, 전문가 수준의 운세 해석을 제공하는 Spring Boot 백엔드 서비스입니다.

---

## 🚀 주요 기능

- **정밀 만세력 계산**: 경도 보정(LMT), 24절기 기준 월주 교체
- **사주 원국 산출**: 연주/월주/일주/시주(60갑자)
- **오행 분석**: 목/화/토/금/수 분포 및 균형 판단
- **십신 계산**: 일간 기준 비견/겁재/식신/상관 등 10신 산출
- **신살 판별**: 도화살/역마살/화개살/천을귀인 자동 탐지
- **AI 해석**: Hugging Face LLM을 통한 자연스러운 운세 해석 생성
- **카카오 로그인**: OAuth2 기반 소셜 로그인 연동
- **결과 캐싱**: 동일 사주 재조회 시 즉시 응답

---

## 📋 기술 스택

- **Backend**: Java 21, Spring Boot 3.2.2
- **Database**: MySQL 8.0, JPA/Hibernate
- **LLM**: Hugging Face Inference API (meta-llama/Llama-3.1-8B-Instruct)
- **Build**: Gradle 8.x
- **Authentication**: JWT, OAuth2 (Kakao)

---

## 🛠️ 설치 및 실행

### 1. 필수 요구사항

- JDK 21+
- MySQL 8.0+
- Hugging Face API Token (무료 티어 가능)

### 2. 데이터베이스 초기화

```bash
# MySQL 실행 후 초기화 스크립트 실행
mysql -u root -p < infra/db/init-saju-db.sql
```

### 3. 환경 변수 설정

**✅ 현재 설정 상태:**
- Hugging Face Token: **설정 완료** (`hf_PqawZ...BIhVbx`)
- 모델: **meta-llama/Llama-3.1-8B-Instruct**
- DB: **MySQL (saju_db)**

**방법 1: 그대로 실행 (권장)**

토큰이 이미 `start-server.sh`에 설정되어 있으므로 바로 실행 가능합니다!

```bash
./start-server.sh
```

**방법 2: .env 파일 사용**

```bash
cp .env.example .env
# 필요시 .env 파일 편집
```

**방법 3: 직접 export (토큰 변경 시)**

```bash
export HF_API_TOKEN="your_new_token"
export HF_MODEL_ID="mistralai/Mistral-7B-Instruct-v0.3"  # 모델 변경 시
```

### 4. 빌드 및 실행

```bash
# 빌드
./gradlew clean build

# 실행
./gradlew bootRun

# 또는 JAR 파일 직접 실행
java -jar build/libs/saju-backend-1.0.0-SNAPSHOT.jar
```

서버는 `http://localhost:8080` 에서 실행됩니다.

---

## 📡 API 사용 예시

### 1. 헬스 체크

```bash
curl http://localhost:8080/api/v1/saju/health
```

### 2. 사주 분석 요청

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
        "year": { "gan": "庚", "ji": "午", "element": "금", "ganji": "庚午" },
        "month": { "gan": "辛", "ji": "巳", "element": "금", "ganji": "辛巳" },
        "day": { "gan": "甲", "ji": "寅", "element": "목", "ganji": "甲寅" },
        "time": { "gan": "辛", "ji": "未", "element": "금", "ganji": "辛未" }
      },
      "elementsScore": {
        "wood": 2,
        "fire": 2,
        "earth": 1,
        "metal": 3,
        "water": 0
      },
      "tenGods": {
        "비견": 1,
        "편재": 2,
        "정관": 1,
        "식신": 1
      },
      "specialStars": ["도화살", "천을귀인"]
    },
    "aiInterpretation": {
      "personality": {
        "traits": ["리더십이 강함", "추진력이 뛰어남", "독립적 성향"],
        "strengths": "목표를 향한 추진력과 결단력이 탁월합니다.",
        "weaknesses": "타인의 의견을 수용하는 데 어려움이 있을 수 있습니다."
      },
      "fortunes": {
        "wealth": "재물운이 강하며, 특히 사업을 통한 수입이 좋습니다.",
        "career": "리더십을 발휘할 수 있는 직종이 적합합니다.",
        "love": "적극적인 성향으로 인해 연애에서 주도권을 잡습니다.",
        "health": "금기운이 강하여 호흡기 계통에 주의가 필요합니다."
      },
      "advice": [
        "오늘은 중요한 결정을 내리기 좋은 날입니다.",
        "타인과의 협력을 통해 더 큰 성과를 얻을 수 있습니다.",
        "감정을 절제하고 이성적으로 판단하세요."
      ]
    },
    "generatedAt": "2024-02-20T10:30:00Z"
  }
}
```

---

## 📖 프로젝트 구조

```
saju-backend/
├── docs/
│   └── saju_ai_spec.md          # 시스템 설계 명세서
├── infra/
│   └── db/
│       └── init-saju-db.sql     # DB 초기화 스크립트
├── src/main/java/com/company/saju/
│   ├── SajuBackendApplication.java
│   ├── common/                   # 공통 유틸리티
│   ├── auth/                     # 카카오 로그인
│   ├── user/                     # 사용자 관리
│   ├── saju/                     # 사주 분석 핵심
│   │   ├── domain/               # 도메인 모델
│   │   ├── adapter/              # Controller & Repository
│   │   └── application/          # 서비스 로직
│   ├── engine/                   # 명리 계산 엔진
│   │   ├── ganji/                # 간지 변환
│   │   ├── time/                 # 시각 보정
│   │   ├── solarterm/            # 절기 처리
│   │   ├── element/              # 오행 분석
│   │   ├── tengods/              # 십신 계산
│   │   └── star/                 # 신살 판별
│   └── ai/                       # LLM 연동
│       ├── application/          # HuggingFaceService
│       └── adapter/              # HuggingFaceClient
└── src/main/resources/
    └── application.yml           # 설정 파일
```

---

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests GanjiCalculatorTest
```

---

## 📚 참고 자료

- [시스템 설계 명세서](docs/saju_ai_spec.md)
- [Hugging Face Inference API](https://huggingface.co/docs/api-inference/)
- [한국천문연구원 - 24절기 데이터](https://astro.kasi.re.kr/)

---

## 🤝 기여

이 프로젝트는 명리학 전문가와 개발자의 협업으로 만들어졌습니다.  
이슈 제보 및 기능 제안은 GitHub Issues를 활용해주세요.

---

## 📄 라이선스

이 프로젝트는 개인/상업적 용도로 자유롭게 사용 가능합니다.

---

**Powered by Spring Boot, MySQL, and Hugging Face LLM**
