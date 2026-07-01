# 오늘 어디가? 🗺️

> 날짜, 위치, 동행 유형, 선호 카테고리와 혼잡도 정보를 바탕으로  
> 행사·관광지·식당·카페를 포함한 하루 코스를 추천하는 서비스

**오늘 어디가?**는 사용자가 원하는 날짜와 출발 위치, 동행 유형, 관심 카테고리를 입력하면 행사·관광지·장소·실시간 혼잡도 정보를 종합하여 방문 후보와 이동 동선을 추천하는 서비스입니다.

단순히 장소 목록을 나열하는 것이 아니라, 사용자의 조건에 맞는 장소를 선별하고 이동 거리, 혼잡도, 선호도 등을 반영하여 실제로 방문 가능한 코스를 구성하는 것을 목표로 합니다.

---

## 프로젝트 소개

외출이나 여행 계획을 세울 때는 단순히 유명한 장소를 찾는 것만으로 충분하지 않습니다.

- 방문 날짜에 행사가 진행 중인지
- 현재 또는 예상 혼잡도가 어떤지
- 동행 유형에 적합한지
- 이동 동선이 과도하지 않은지
- 식당과 카페를 함께 포함할 수 있는지
- 행사 기간과 방문 날짜가 맞는지

오늘 어디가는 행사·관광지·장소·혼잡도 데이터를 결합하여, 사용자가 바로 활용할 수 있는 하루 코스를 추천합니다.

---

## 주요 기능

### 인증

- 로그인
- 로그아웃
- Access Token 재발급
- 쿠키 기반 인증 처리

### 사용자 선호 정보

- 선호 정보 등록
- 선호 정보 조회
- 선호 정보 수정
- 선호 정보 삭제
- 동행 유형, 선호 카테고리, 이동 강도, 혼잡도 회피 조건 관리

### 장소

- 장소 생성
- 장소 단건 조회
- 장소 목록 조회
- 키워드 기반 장소 검색
- 좌표 기반 지역 조회
- 장소 삭제
- 카카오 장소 검색 API 연동

### 행사 및 혼잡도

- 행사 단건 조회
- 행사 목록 조회
- 지역 실시간 혼잡도 조회
- 혼잡도 상위 지역 조회
- 서울시 도시데이터 기반 혼잡도 활용

### 추천

- 날짜, 위치, 동행 유형, 카테고리 조건 기반 추천 후보 생성
- 행사·관광지·식당·카페 후보 통합
- 거리, 혼잡도, 선호도, 운영 시간 등을 반영한 후보 점수 계산
- 추천 사유 제공
- Beam Search 기반 방문 순서 및 경로 탐색

### 코스

- 식당·카페 후보 미리보기
- 추천 결과 기반 코스 생성
- 코스 단건 조회
- 코스 목록 조회
- 코스 삭제
- 코스 북마크 등록 및 해제
- 저장한 코스 목록 조회
- 방문 장소 순서와 이동 시간 정보 관리

### 리뷰

- 리뷰 작성
- 내 리뷰 조회
- 코스 리뷰 목록 조회
- 리뷰 수정
- 리뷰 삭제

### 관리자

- 관리자 장소 목록 조회
- 관리자 장소 등록, 수정, 삭제
- 회원 목록 조회 및 탈퇴 처리
- 관리자 행사 목록 조회
- 행사 데이터 동기화
- 지역 혼잡도 갱신
- 서울 관광지 데이터 동기화

### 결제 및 구독

- 요금제 조회
- 토스 빌링 인증 기반 카드 등록
- 등록된 빌링키 조회 및 삭제
- 구독 신청
- 구독 정보 조회 및 해지
- 결제 내역 조회 및 결제 취소
- 토스 결제 웹훅 수신
- `Idempotency-Key` 기반 중복 결제 요청 방지

---

## 추천 코스 생성 흐름

```text
사용자 조건 입력
→ 행사·관광지·식당·카페 후보 수집
→ 날짜·거리·혼잡도·선호도 기준 후보 필터링
→ 후보 점수 계산
→ Beam Search 기반 경로 탐색
→ 최적 방문 순서 선정
→ 식당·카페 포함 코스 미리보기
→ 사용자 선택 후 코스 저장
```

추천 점수에는 다음 요소를 반영합니다.

- 사용자 선호 카테고리
- 동행 유형
- 출발 위치와 장소 간 거리
- 실시간 또는 예측 혼잡도
- 행사 날짜와 운영 시간
- 이동 시간 및 전체 동선

---

## 기술 스택

### Backend

| 구분 | 기술 |
|---|---|
| Language | Java |
| Framework | Spring Boot |
| ORM | Spring Data JPA |
| Security | Spring Security, JWT |
| Database | MySQL |
| Build Tool | Gradle |
| Test | JUnit 5, JaCoCo |
| API Test | Postman |
| Load Test | k6 |
| Monitoring | Prometheus, Grafana |
| External API | Naver Map API, Naver Local Search API, Kakao Local API, 서울시 도시데이터 API, 관광지·행사 공공데이터 API, Toss Payments |

### Frontend

| 구분 | 기술 |
|---|---|
| Language | TypeScript |
| Framework | Next.js |
| Styling | Tailwind CSS |
| Map | Naver Map API |

---

## 주요 외부 API

| 구분 | 활용 목적 |
|---|---|
| Naver Map API | 추천 장소 마커, 코스 경로 및 지도 화면 표시 |
| Naver Local Search API | 장소명·주소 기반 장소 검색 및 장소 정보 조회 |
| Kakao Local API | 식당·카페 후보 검색 및 좌표 기반 장소 정보 조회 |
| 서울시 실시간 도시데이터 API | 지역별 실시간 혼잡도 조회 |
| 관광지 공공데이터 API | 서울 관광지 데이터 동기화 |
| 행사 공공데이터 API | 행사 정보 동기화 및 추천 후보 생성 |
| Toss Payments | 빌링 인증, 구독 결제, 결제 취소, 웹훅 처리 |

---

## 프로젝트 구조

```text
NBE9-11-final-Team06
├── backend
│   ├── src
│   │   ├── main
│   │   │   ├── java
│   │   │   │   └── come.back.gotoday
│   │   │   │       ├── admin
│   │   │   │       ├── auth
│   │   │   │       ├── category
│   │   │   │       ├── course
│   │   │   │       ├── crowd
│   │   │   │       ├── event
│   │   │   │       ├── external
│   │   │   │       ├── global
│   │   │   │       ├── member
│   │   │   │       ├── payment
│   │   │   │       ├── place
│   │   │   │       ├── preference
│   │   │   │       ├── recommend
│   │   │   │       ├── review
│   │   │   │       └── tour
│   │   │   └── resources
│   │   │       ├── application.yml
│   │   │       ├── application-local.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       └── application-test.yml
│   │   └── test
│   └── build.gradle.kts
│
├── frontend
│   ├── app
│   ├── components
│   ├── hooks
│   ├── lib
│   └── public
│
├── performance
│   └── k6
│
├── docs
├── docker-compose.yml
└── .github
    └── workflows
```

---

## 실행 방법

### 1. Repository Clone

```bash
git clone <repository-url>
cd NBE9-11-final-Team06
```

### 2. Backend 실행

```bash
cd backend
./gradlew bootRun
```

Windows 환경에서는 아래 명령어를 사용합니다.

```bash
gradlew.bat bootRun
```

로컬 프로필을 명시해서 실행할 경우:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 3. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

기본적으로 프론트는 아래 주소에서 실행됩니다.

```text
http://localhost:3000
```

---

## 환경 변수

민감 정보는 코드에 직접 작성하지 않고 환경 변수 또는 프로필별 설정 파일로 관리합니다.

```bash
DB_URL=
DB_USERNAME=
DB_PASSWORD=

JWT_SECRET=

KAKAO_REST_API_KEY=
SEOUL_API_KEY=
TOUR_API_KEY=
EVENT_API_KEY=

TOSS_CLIENT_KEY=
TOSS_SECRET_KEY=
```

환경별 설정 파일은 Git에 포함하지 않거나, 민감 값을 제거한 예시 파일만 관리합니다.

---

## 테스트

### 전체 테스트 실행

```bash
cd backend
./gradlew test
```

### JaCoCo 커버리지 리포트 생성

```bash
cd backend
./gradlew clean test jacocoTestReport
```

리포트 생성 후 아래 경로에서 HTML 결과를 확인할 수 있습니다.

```text
backend/build/reports/jacoco/test/html/index.html
```

### 커버리지 결과

| 구분 | Instruction Coverage | Branch Coverage |
|---|---:|---:|
| 전체 | 70% | 52% |
| 추천 서비스 | 87% | 64% |
| 코스 서비스 | 82% | 63% |
| 행사 서비스 | 80% | 66% |

추천, 코스 생성, 행사 처리처럼 핵심 사용자 흐름과 조건 분기를 중심으로 테스트를 보강했습니다.

---

## 성능 테스트

코스 미리보기 API를 대상으로 k6 기반 부하 테스트를 진행했습니다.

| 항목 | 결과 |
|---|---:|
| 대상 API | `POST /api/courses/preview` |
| 동시 사용자 수 | 최대 5 VU |
| 테스트 시간 | 약 3분 |
| 총 요청 수 | 56건 |
| 성공 요청 | 56건 |
| HTTP 실패율 | 0% |
| 체크 성공률 | 100% |
| 평균 응답 시간 | 약 10.08초 |
| p95 응답 시간 | 약 10.11초 |

외부 장소 API 응답 지연 상황을 WireMock으로 구성하고, Prometheus와 Grafana를 통해 HikariCP 커넥션 풀과 Tomcat 스레드 상태를 함께 모니터링했습니다.

---

## API 문서

주요 API는 다음 기능 단위로 구성됩니다.

```text
인증
선호 정보
카테고리
장소
행사
혼잡도
추천
코스
리뷰
관리자
관광지 동기화
결제 및 구독
토스 웹훅
```

Postman 컬렉션을 통해 주요 API 흐름을 시연할 수 있도록 구성했습니다.

---

## 코드 작성 원칙

### Backend

- Controller는 요청과 응답 처리만 담당합니다.
- 비즈니스 로직은 Service 계층에서 처리합니다.
- 데이터베이스 접근은 Repository에서 담당합니다.
- Entity를 API 응답으로 직접 반환하지 않습니다.
- Request DTO와 Response DTO를 분리합니다.
- 예외는 공통 예외 처리 구조를 통해 관리합니다.
- 핵심 비즈니스 로직에는 테스트 코드를 작성합니다.

### Entity

- Entity 생성은 정적 팩토리 메서드를 우선 사용합니다.
- 상태 변경은 도메인 메서드로 처리합니다.
- Setter 사용을 지양합니다.
- 생성자 접근 범위는 필요한 수준으로 제한합니다.

### 보안

- 비밀번호는 암호화하여 저장합니다.
- JWT Secret과 외부 API Key는 환경 변수로 관리합니다.
- 민감 정보는 GitHub에 커밋하지 않습니다.
- 관리자 API는 관리자 권한을 가진 사용자만 접근할 수 있습니다.
- 결제 요청에는 멱등성 키를 적용해 중복 결제를 방지합니다.

---

## 프로젝트 강점

- 행사, 관광지, 장소, 혼잡도 데이터를 통합해 추천에 활용합니다.
- 단순 장소 추천이 아닌 방문 순서와 이동 동선을 포함한 코스를 구성합니다.
- 날짜, 거리, 혼잡도, 선호도, 동행 유형을 추천 조건에 반영합니다.
- 추천 사유를 제공해 사용자에게 추천 결과의 근거를 전달합니다.
- 식당과 카페를 포함한 코스 미리보기와 최종 저장 흐름을 제공합니다.
- 관리자 기능을 통해 장소, 행사, 회원, 혼잡도 데이터를 운영할 수 있습니다.
- 토스 빌링 인증 기반 구독 결제와 결제 이력 관리 기능을 제공합니다.
- JaCoCo와 k6를 활용해 테스트 범위와 성능을 점검했습니다.

---

## 향후 개선 방향

- 날씨와 미세먼지 데이터를 추천 점수에 반영
- 예약 가능 여부 및 대기 시간 정보 연동
- 주차장, 대중교통, 따릉이 정보 연동
- 추천 결과 개인화 고도화
- 관리자 통계 및 운영 기능 확장
- 외부 API 장애 상황에 대한 캐시 및 재시도 전략 보강

---

## License

본 프로젝트는 학습 및 포트폴리오 목적으로 제작되었습니다.

