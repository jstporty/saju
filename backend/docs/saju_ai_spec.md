# 사주 분석 시스템 (Saju AI System) - 상세 설계 명세서

## 1. 시스템 개요

### 1.1 목적
사용자의 생년월일시 및 성별 정보를 입력받아, 전통 명리학 원리에 기반한 정밀 사주 원국(四柱原局)을 계산하고, Hugging Face LLM을 활용하여 전문가 수준의 성향/운세/조언을 제공하는 백엔드 시스템.

### 1.2 핵심 특징
- **정밀 시각 보정**: 한국 표준시(KST, 135°E 기준)를 실제 출생 경도 기반 LMT(Local Mean Time)로 변환하여 시주(時柱) 정확도 향상.
- **절기 기반 월주 교체**: 단순 음력 월이 아닌, 24절기의 정확한 입절(入節) 시각을 기준으로 월주(月柱) 산출.
- **AI 해석 연동**: 계산된 사주 원국 데이터를 구조화된 프롬프트로 LLM에 전달하여, 인간 명리학자처럼 자연스러운 해석 생성.

---

## 2. 기술 스택

| Layer | Technology | Purpose |
|:---|:---|:---|
| **Application** | Spring Boot 3.2.2 + Java 21 | RESTful API 서버 |
| **Database** | MySQL 8.0 + JPA/Hibernate | 사용자 정보 및 결과 캐싱 |
| **LLM Integration** | Hugging Face Inference API | 사주 해석 자동 생성 |
| **HTTP Client** | RestClient (Spring 6.1+) | HF API 통신 |
| **Build Tool** | Gradle 8.x | 의존성 관리 및 빌드 |

---

## 3. 데이터베이스 설계

### 3.1 Entity 구조

#### Users (사용자 정보)
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,              -- 생년월일
    birth_time TIME,                       -- 태어난 시간 (HH:mm)
    gender ENUM('MALE', 'FEMALE') NOT NULL,
    calendar_type ENUM('SOLAR', 'LUNAR', 'LEAP_LUNAR') NOT NULL,
    birth_longitude DECIMAL(9,6),          -- 출생지 경도 (예: 126.9780)
    birth_latitude DECIMAL(9,6),           -- 출생지 위도 (선택, 추후 확장용)
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### SajuResults (사주 분석 결과 캐싱)
```sql
CREATE TABLE saju_results (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    calculation_key VARCHAR(100) UNIQUE NOT NULL, -- MD5(birth_date+time+gender+longitude)
    raw_pillars JSON NOT NULL,            -- { year: {gan, ji}, month: {...}, day: {...}, time: {...} }
    elements_score JSON NOT NULL,         -- { wood: 2, fire: 3, earth: 1, metal: 1, water: 1 }
    ten_gods JSON,                        -- 십신 분포: { bijeon: 2,겁재: 1, ... }
    special_stars JSON,                   -- 신살: ['도화살', '역마살']
    ai_interpretation TEXT,               -- LLM 생성 해석 전문
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_calculation_key (calculation_key)
);
```

#### SolarTerms (24절기 데이터)
```sql
CREATE TABLE solar_terms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    year INT NOT NULL,
    term_name VARCHAR(20) NOT NULL,       -- '입춘', '경칩', ...
    term_datetime TIMESTAMP NOT NULL,     -- 절기 정확한 입절 시각 (KST)
    INDEX idx_year_term (year, term_name)
);
```

---

## 4. 핵심 알고리즘 설계

### 4.1 경도 보정 (LMT 계산)

한국 표준시(KST)는 동경 135°를 기준으로 하지만, 실제 한반도는 약 124°~131° 범위에 위치.  
출생 시각이 자시(子時, 23:00~01:00) 경계에 있을 경우, 경도 차이로 인해 일주(日柱)가 바뀔 수 있으므로 LMT 보정이 필수.

#### 공식
```
실제 태양시(LMT) = KST + (실제 경도 - 135°) × 4분
```

**예시**:
- 서울 (경도 126.978°): `LMT = KST + (126.978 - 135) × 4 = KST - 32.088분`
- 즉, KST 00:10에 태어났다면 실제 태양시는 전날 23:38이 되어 일주가 하루 전으로 바뀜.

#### 구현 지점
- `TimeService.adjustToLMT(LocalDateTime kstTime, double longitude)` 메서드에서 처리.

---

### 4.2 절기 기반 월주 교체 로직

전통 명리학에서 월주(月柱)는 **음력 월이 아니라 24절기의 입절 시각을 기준**으로 교체됨.

#### 월주 교체 기준 절기
| 월주 | 절기 시작 | 간지 예시 |
|:---|:---|:---|
| 1월 | 입춘(立春) | 寅월 (인월) |
| 2월 | 경칩(驚蟄) | 卯월 (묘월) |
| 3월 | 청명(淸明) | 辰월 (진월) |
| ... | ... | ... |
| 12월 | 소한(小寒) | 丑월 (축월) |

**핵심 규칙**:
- 2월 3일생이더라도, **입춘 입절 시각 이전**에 태어났다면 전년도 12월주(丑월)로 계산.
- 절기 데이터는 천문력 API(한국천문연구원 또는 기상청 오픈API)를 통해 수집하거나, 미리 계산된 데이터를 `solar_terms` 테이블에 저장.

#### 구현 지점
- `SolarTermService.getMonthPillarBySolarTerm(LocalDateTime adjustedTime)` 메서드.

---

### 4.3 육십갑자 변환 알고리즘

**천간(天干)**: 甲乙丙丁戊己庚辛壬癸 (10개)  
**지지(地支)**: 子丑寅卯辰巳午未申酉戌亥 (12개)  
→ 조합하여 60갑자(甲子, 乙丑, ..., 癸亥) 순환.

#### 연주(年柱) 계산
```java
// 입춘 기준 연도 조정 필요
int ganIndex = (year - 4) % 10;  // 甲이 0
int jiIndex = (year - 4) % 12;   // 子가 0
```

#### 일주(日柱) 계산
기원일(예: 1900년 1월 1일 = 甲戌일)부터 경과 일수를 계산하여 60으로 나눈 나머지.

```java
long daysSinceEpoch = ChronoUnit.DAYS.between(EPOCH_DATE, targetDate);
int ganjiIndex = (int)((daysSinceEpoch + EPOCH_OFFSET) % 60);
```

#### 시주(時柱) 계산
일간(日干)과 출생 시간대(子시=23-01시, 丑시=01-03시 등)를 조합하여 산출.

---

### 4.4 십신(Ten Gods) 계산

**십신(十神)**은 일간(日干)을 중심으로 나머지 7개 글자와의 관계를 분석하는 핵심 개념.

#### 산출 규칙
- **비견(比肩)**: 일간과 동일한 오행 + 음양 같음
- **겁재(劫財)**: 일간과 동일한 오행 + 음양 다름
- **식신(食神)**: 일간이 생(生)하는 오행 + 음양 같음
- **상관(傷官)**: 일간이 생하는 오행 + 음양 다름
- **편재(偏財)**: 일간이 극(剋)하는 오행 + 음양 같음
- **정재(正財)**: 일간이 극하는 오행 + 음양 다름
- **편관(偏官/七殺)**: 일간을 극하는 오행 + 음양 같음
- **정관(正官)**: 일간을 극하는 오행 + 음양 다름
- **편인(偏印)**: 일간을 생하는 오행 + 음양 같음
- **정인(正印)**: 일간을 생하는 오행 + 음양 다름

#### 구현 지점
- `TenGodsService.calculate(String dayGan, List<String> allGans)` 메서드.
- 결과: `{ "bijeon": 2, "겁재": 1, "식신": 1, ... }`

---

### 4.5 신살(神煞) 판별

주요 신살 목록:
- **도화살(桃花煞)**: 연애/인기운. 일지 기준으로 판단 (子일생 → 酉가 있으면 도화살).
- **역마살(驛馬煞)**: 이동/변화운. 연지 기준.
- **화개살(華蓋煞)**: 예술/종교 성향.
- **천을귀인(天乙貴人)**: 귀인의 도움.

#### 구현 지점
- `SpecialStarService.detect(FourPillars pillars)` 메서드.

---

## 5. LLM 연동 설계

### 5.1 Hugging Face Inference API 사용

**추천 모델**: `meta-llama/Llama-3.1-8B-Instruct` (무료 티어)

#### API 엔드포인트
```
POST https://api-inference.huggingface.co/models/{model_id}
Authorization: Bearer {HF_API_TOKEN}
Content-Type: application/json

{
  "inputs": "프롬프트 텍스트",
  "parameters": {
    "max_new_tokens": 1024,
    "temperature": 0.7
  }
}
```

### 5.2 프롬프트 엔지니어링

#### 시스템 프롬프트 템플릿
```
너는 30년 경력의 명리학 전문가다. 아래 사주 원국 데이터를 분석하여 다음 항목을 JSON 형식으로 작성해줘:

[사주 원국]
- 연주: {year_gan}{year_ji} (오행: {year_element})
- 월주: {month_gan}{month_ji} (오행: {month_element})
- 일주: {day_gan}{day_ji} (오행: {day_element})
- 시주: {time_gan}{time_ji} (오행: {time_element})

[오행 분포]
- 목: {wood}, 화: {fire}, 토: {earth}, 금: {metal}, 수: {water}

[십신 분포]
{ten_gods_summary}

[신살]
{special_stars}

위 정보를 바탕으로 다음 항목을 한국어로 작성:
1. personality: { "traits": ["성격 특징 3가지"], "strengths": "장점", "weaknesses": "단점" }
2. fortunes: { "wealth": "재물운 해석", "career": "직업운 해석", "love": "연애운 해석", "health": "건강운 해석" }
3. advice: ["오늘의 행동 조언 3가지"]

반드시 JSON 형식으로만 응답해.
```

### 5.3 응답 파싱
LLM이 JSON 형식으로 답변하지 않을 경우를 대비하여:
1. 정규식으로 JSON 블록 추출 (`\{[\s\S]*\}`).
2. 파싱 실패 시 기본 템플릿 응답 반환.

---

## 6. API 설계

### 6.1 사주 분석 요청

**Endpoint**: `POST /api/v1/saju/analyze`

**Request Body**:
```json
{
  "birthDate": "1990-05-15",
  "birthTime": "14:30",
  "gender": "MALE",
  "calendarType": "SOLAR",
  "birthLongitude": 126.9780,
  "birthLatitude": 37.5665
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "rawData": {
      "fourPillars": {
        "year": { "gan": "庚", "ji": "午", "element": "金", "ganji": "庚午" },
        "month": { "gan": "辛", "ji": "巳", "element": "金", "ganji": "辛巳" },
        "day": { "gan": "甲", "ji": "寅", "element": "木", "ganji": "甲寅" },
        "time": { "gan": "辛", "ji": "未", "element": "金", "ganji": "辛未" }
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
        "겁재": 0,
        "식신": 1,
        "상관": 1,
        "편재": 2,
        "정재": 1,
        "편관": 1,
        "정관": 1,
        "편인": 0,
        "정인": 0
      },
      "specialStars": ["도화살", "천을귀인"]
    },
    "aiInterpretation": {
      "personality": {
        "traits": ["리더십이 강함", "추진력이 뛰어남", "고집이 셈"],
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

### 6.2 기존 결과 조회

**Endpoint**: `GET /api/v1/saju/results/{userId}`

캐싱된 분석 결과를 반환하여 LLM API 호출 비용 절감.

---

## 7. 모듈 구조

```
src/main/java/com/company/saju/
├── SajuBackendApplication.java
├── common/                         # 공통 유틸리티
│   ├── domain/BaseEntity.java
│   ├── dto/ApiResponse.java
│   ├── exception/
│   └── util/IdGenerator.java
├── auth/                           # 카카오 로그인 (기존 유지)
│   ├── adapter/
│   ├── application/
│   └── infrastructure/
├── user/                           # 사용자 관리
│   ├── domain/model/
│   ├── adapter/
│   │   ├── in/web/UserController.java
│   │   └── out/persistence/UserRepository.java
│   └── application/service/UserService.java
├── saju/                           # 사주 분석 핵심 모듈
│   ├── domain/
│   │   ├── model/
│   │   │   ├── FourPillars.java       # 사주 원국 VO
│   │   │   ├── Pillar.java            # 개별 기둥 (연/월/일/시)
│   │   │   ├── GanJi.java             # 간지 VO
│   │   │   └── Element.java           # 오행 enum
│   │   └── SajuResult.java            # Entity
│   ├── adapter/
│   │   ├── in/web/
│   │   │   ├── SajuController.java
│   │   │   └── dto/
│   │   │       ├── SajuAnalyzeRequest.java
│   │   │       └── SajuAnalyzeResponse.java
│   │   └── out/
│   │       ├── persistence/SajuResultRepository.java
│   │       └── ai/HuggingFaceClient.java
│   └── application/
│       ├── service/
│       │   ├── SajuAnalysisService.java  # 통합 파이프라인
│       │   └── SajuCacheService.java
│       └── port/
│           └── out/LLMPort.java
├── engine/                         # 명리학 계산 엔진
│   ├── time/
│   │   ├── TimeService.java           # LMT 보정
│   │   └── SolarTermService.java      # 절기 처리
│   ├── ganji/
│   │   ├── GanjiCalculator.java       # 육십갑자 변환
│   │   └── GanjiConstants.java
│   ├── element/
│   │   └── ElementAnalyzer.java       # 오행 점수 계산
│   ├── tengods/
│   │   └── TenGodsService.java        # 십신 산출
│   └── star/
│       └── SpecialStarService.java    # 신살 판별
└── ai/                             # AI 연동 모듈
    ├── application/
    │   ├── HuggingFaceService.java
    │   └── PromptBuilder.java
    ├── adapter/
    │   └── HuggingFaceAdapter.java
    └── dto/
        ├── LLMRequest.java
        └── LLMResponse.java
```

---

## 8. 환경 설정

### 8.1 application.yml 추가 항목
```yaml
huggingface:
  api:
    base-url: https://api-inference.huggingface.co
    token: ${HF_API_TOKEN:hf_xxxxxxxxxxxxxxxxxxxxx}
    model-id: ${HF_MODEL_ID:meta-llama/Llama-3.1-8B-Instruct}
    timeout: 30000
    max-tokens: 1024
    temperature: 0.7

saju:
  cache:
    enabled: true
    ttl-days: 365  # 결과 캐싱 기간
```

### 8.2 환경 변수
```bash
HF_API_TOKEN=hf_xxxxxxxxxxxxxxx
DB_USERNAME=saju
DB_PASSWORD=saju1234
JWT_SECRET=your-secret-key
```

---

## 9. 구현 순서

1. ✅ **Entity 및 Repository 생성** (`User`, `SajuResult`, `SolarTerm`)
2. ✅ **TimeService**: LMT 보정 로직 구현
3. ✅ **SolarTermService**: 절기 데이터 로드 및 월주 판별
4. ✅ **GanjiCalculator**: 육십갑자 변환
5. ✅ **ElementAnalyzer**: 오행 점수 계산
6. ✅ **TenGodsService**: 십신 산출
7. ✅ **SpecialStarService**: 신살 판별
8. ✅ **HuggingFaceClient**: LLM API 연동
9. ✅ **PromptBuilder**: 프롬프트 생성
10. ✅ **SajuAnalysisService**: 전체 파이프라인 통합
11. ✅ **SajuController**: REST API 엔드포인트
12. ✅ **테스트 및 검증**

---

## 10. 확장 가능성

### 10.1 추가 기능 아이디어
- **궁합 분석**: 두 사용자의 사주를 비교하여 오행 균형 및 합/충 관계 점수화.
- **택일(擇日)**: 결혼, 이사, 개업 등 중요한 날짜 추천.
- **대운/세운 분석**: 10년 단위 대운과 매년 세운 흐름 제공.
- **일일 운세 푸시**: Notification 서비스와 연동하여 매일 아침 운세 알림.

### 10.2 성능 최적화
- **캐싱 전략**: Redis를 도입하여 자주 조회되는 사주 결과를 메모리에 캐싱.
- **비동기 처리**: LLM API 호출을 비동기로 처리하여 응답 속도 향상.
- **배치 처리**: 절기 데이터를 매년 초 배치 작업으로 미리 계산하여 DB에 저장.

---

## 11. 주요 참고 자료

- [한국천문연구원 - 24절기 데이터](https://astro.kasi.re.kr/)
- [Hugging Face Inference API 문서](https://huggingface.co/docs/api-inference/)
- [명리학 기초 이론](https://ko.wikipedia.org/wiki/사주)

---

**문서 작성일**: 2024-02-20  
**작성자**: 사주 백엔드 아키텍트  
**버전**: 1.0.0
