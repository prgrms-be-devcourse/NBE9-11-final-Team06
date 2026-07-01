# 오늘 어디가?

> 날짜, 지역, 동행 유형, 선호 카테고리, 혼잡도 정보를 기반으로 사용자의 하루 코스를 추천하는 여행·나들이 추천 서비스

**오늘 어디가?**는 사용자가 원하는 날짜와 지역을 선택하면 문화 행사, 장소, 실시간 혼잡도 정보를 종합해 맞춤형 장소와 이동 코스를 추천하는 서비스입니다.
단순히 장소 목록을 보여주는 것이 아니라, 사용자의 상황과 선호도를 반영해 “왜 이 장소를 추천하는지”까지 설명하는 것을 목표로 합니다.

---

## 프로젝트 소개

기존 장소 추천 서비스는 맛집이나 명소를 단순 나열하는 경우가 많습니다.
하지만 실제 외출 계획을 세울 때는 다음과 같은 요소가 중요합니다.

* 오늘 운영 중인지
* 현재 사람이 많은지
* 동행 유형에 적합한지
* 이동 동선이 무리 없는지
* 예산이나 활동 강도가 적절한지
* 행사 기간과 방문 시간이 맞는지

오늘 어디가?는 공공 문화행사 데이터, 장소 정보, 실시간 도시 혼잡도 데이터, 지도 API를 활용해 사용자가 바로 실행할 수 있는 실용적인 코스를 제공합니다.

---

## 주요 기능

### 회원 기능

* 회원가입
* 로그인
* 로그아웃
* 내 정보 조회
* 내 정보 수정
* 회원 탈퇴

### 사용자 선호 기능

* 선호 지역 설정
* 동행 유형 설정
* 선호 카테고리 설정
* 이동 강도 설정
* 혼잡도 회피 여부 설정

### 추천 기능

* 날짜 기반 장소 및 행사 추천
* 지역 기반 추천
* 동행 유형 기반 추천
* 카테고리 기반 추천
* 혼잡도 기반 추천 점수 반영
* 추천 사유 제공

### 코스 기능

* 하루 코스 생성
* 여러 장소를 방문 순서대로 구성
* 장소별 예상 체류 시간 관리
* 장소 간 이동 시간 및 거리 반영
* 추천 코스 저장
* 저장한 코스 목록 조회
* 저장한 코스 상세 조회
* 저장한 코스 삭제

### 지도 기능

* 추천 장소 지도 마커 표시
* 코스 이동 경로 표시

---

## 서비스 대상

| 사용자 유형 | 설명                        |
| ------ | ------------------------- |
| 커플     | 데이트 코스, 감성 장소, 전시·행사 추천   |
| 친구     | 활동적인 코스, 맛집, 축제, 인기 장소 추천 |
| 가족     | 이동 부담이 적고 함께 즐기기 좋은 장소 추천 |
| 혼자     | 전시, 산책, 카페, 문화생활 중심 추천    |
| 부모님 동반 | 혼잡도가 낮고 이동 강도가 낮은 장소 추천   |

---

## 기술 스택

### Backend

| 구분         | 기술                            |
| ---------- | ----------------------------- |
| Language   | Java                          |
| Framework  | Spring Boot                   |
| ORM        | Spring Data JPA               |
| Security   | Spring Security, JWT          |
| Database   | MySQL                         |
| Build Tool | Gradle                        |
| Test       | JUnit 5                       |
| Infra      | AWS EC2, RDS, S3              |
| Deployment | Docker, Nginx, GitHub Actions |

### Frontend

| 구분           | 기술            |
| ------------ | ------------- |
| Language     | TypeScript    |
| Framework    | React         |
| Styling      | Tailwind CSS  |
| Server State | React Query   |
| Client State | Zustand       |
| Map          | Kakao Map API |

---

## 프로젝트 구조

```bash
today-where
├── backend
│   ├── src
│   │   ├── main
│   │   │   ├── java
│   │   │   │   └── come.back.gotoday
│   │   │   │       ├── global
│   │   │   │       │   ├── config
│   │   │   │       │   ├── exception
│   │   │   │       │   ├── response
│   │   │   │       │   └── security
│   │   │   │       ├── member
│   │   │   │       ├── auth
│   │   │   │       ├── preference
│   │   │   │       ├── place
│   │   │   │       ├── event
│   │   │   │       ├── crowd
│   │   │   │       ├── recommend
│   │   │   │       ├── course
│   │   │   │       ├── admin
│   │   │   │       └── external
│   │   │   └── resources
│   │   │       ├── application.yml
│   │   │       ├── application-local.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       └── application-test.yml
│   │   └── test
│   └── build.gradle
│
├── frontend
│   ├── src
│   │   ├── api
│   │   ├── components
│   │   ├── pages
│   │   ├── hooks
│   │   ├── stores
│   │   ├── types
│   │   ├── utils
│   │   └── styles
│   └── package.json
│
├── docs
├── docker-compose.yml
├── README.md
└── .github
    └── workflows
---

## 실행 방법

### 1. Repository Clone
```

```bash
git clone <repository-url>
cd today-where
```

---

### 2. Backend 실행

```bash
cd backend
./gradlew bootRun
```

Windows 환경에서는 다음 명령어를 사용합니다.

```bash
gradlew.bat bootRun
```

---

### 3. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

---

## 환경 변수

Backend 실행을 위해 다음 환경 변수가 필요합니다.

```bash
DB_URL=jdbc:mysql://localhost:3306/today_where
DB_USERNAME=root
DB_PASSWORD=password

JWT_SECRET=your-jwt-secret-key

KAKAO_API_KEY=your-kakao-api-key
SEOUL_API_KEY=your-seoul-api-key
WEATHER_API_KEY=your-weather-api-key
```

운영 환경의 민감 정보는 GitHub에 직접 커밋하지 않고, GitHub Secrets 또는 서버 환경 변수로 관리합니다.

---

## application profile

프로젝트는 환경별 설정 파일을 분리합니다.

| Profile | 설명       |
| ------- | -------- |
| local   | 로컬 개발 환경 |
| dev     | 개발 서버 환경 |
| prod    | 운영 서버 환경 |
| test    | 테스트 환경   |

실행 예시:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## Docker 실행

```bash
docker-compose up -d
```

Docker Compose를 통해 MySQL, Redis 등 로컬 개발에 필요한 인프라를 실행할 수 있습니다.

---

## 테스트

### Backend 테스트

```bash
cd backend
./gradlew test
```

### Backend 빌드

```bash
cd backend
./gradlew clean build
```

---

## 개발 규칙

### Branch 규칙

```bash
feature/기능명
fix/수정내용
refactor/개선내용
docs/문서내용
```

예시:

```bash
feature/member-signup
feature/auth-login
refactor/member-service
docs/api-spec
```

---

## Commit Convention

| Type     | 설명              |
| -------- | --------------- |
| feat     | 새로운 기능 추가       |
| fix      | 버그 수정           |
| refactor | 기능 변화 없는 코드 개선  |
| docs     | 문서 수정           |
| test     | 테스트 코드 추가 또는 수정 |
| chore    | 설정, 빌드, 기타 작업   |
| style    | 코드 포맷팅          |
| rename   | 파일 또는 패키지명 변경   |
| remove   | 파일 또는 코드 삭제     |

예시:

```bash
feat: 회원가입 기능 구현
fix: 로그인 실패 시 예외 처리 수정
refactor: 회원 수정 로직 도메인 메서드로 분리
docs: README 실행 방법 추가
test: 회원가입 서비스 테스트 추가
```
---

## 코드 작성 규칙

### 공통 규칙

* 역할이 드러나는 이름을 사용합니다.
* 불필요한 주석은 작성하지 않습니다.
* 중복 코드를 제거합니다.
* 하나의 메서드는 하나의 책임만 갖도록 작성합니다.
* 기능 구현 후 직접 테스트합니다.
* API 변경 사항은 문서와 프론트엔드 담당자에게 공유합니다.

### Backend 규칙

* Controller는 요청과 응답만 담당합니다.
* 비즈니스 로직은 Service에서 처리합니다.
* Repository는 데이터베이스 접근만 담당합니다.
* Entity를 API 응답으로 직접 반환하지 않습니다.
* Request DTO와 Response DTO를 분리합니다.
* Request DTO에는 Validation 어노테이션을 적용합니다.
* 예외는 공통 `BusinessException`과 `ErrorCode`로 처리합니다.
* 반복적인 try-catch를 Controller에 작성하지 않습니다.
* 핵심 비즈니스 로직에는 테스트 코드를 작성합니다.

### Entity 규칙

* Entity는 정적 팩토리 메서드를 통해 생성합니다.
* Entity 상태 변경은 도메인 메서드로 처리합니다.
* Setter 사용은 지양합니다.
* 생성자 접근 제어자는 protected로 제한합니다.

---

## 보안 규칙

* 비밀번호는 반드시 암호화하여 저장합니다.
* JWT Secret은 코드에 직접 작성하지 않습니다.
* API Key는 환경 변수로 관리합니다.
* 민감 정보는 GitHub에 커밋하지 않습니다.
* 인증이 필요한 API는 JWT 인증을 적용합니다.
* 관리자 API는 ADMIN 권한만 접근할 수 있습니다.
* 비밀번호, JWT, API Key, 개인정보는 로그에 남기지 않습니다.

---

## 로깅 규칙

* `System.out.println`을 사용하지 않습니다.
* Logger를 사용합니다.
* 정상적인 주요 흐름은 INFO 레벨로 기록합니다.
* 상세 디버깅 정보는 DEBUG 레벨로 기록합니다.
* 예외 상황은 WARN 또는 ERROR 레벨로 기록합니다.
* 운영 환경에서는 과도한 DEBUG 로그를 비활성화합니다.

---

## MVP 범위

MVP에는 다음 기능을 포함합니다.

* 회원가입 / 로그인 / 로그아웃
* 내 정보 조회 / 수정 / 탈퇴
* 사용자 선호 정보 관리
* 추천 조건 선택
* 장소 및 행사 추천
* 혼잡도 기반 추천 점수 반영
* 하루 코스 생성
* 코스 저장 / 조회 / 삭제
* 지도 마커 표시
* 추천 사유 제공

---

## 향후 확장 기능

* 날씨 기반 추천
* 미세먼지 기반 추천
* 예약 가능 장소 연동
* 주차 정보 제공
* 리뷰 및 이미지 업로드
* 북마크 기능
* 알림 기능
* 결제 및 구독 기능
* 프리미엄 추천 기능
* 관리자 페이지 고도화

---

## 프로젝트 차별점

* 공공 문화행사 데이터와 실시간 도시 데이터를 함께 활용합니다.
* 단순 장소 추천이 아니라 실제 이동 가능한 코스를 생성합니다.
* 혼잡도, 운영 시간, 행사 기간, 이동 거리 등을 추천 점수에 반영합니다.
* 사용자에게 추천 사유를 제공하여 결과를 납득할 수 있도록 합니다.
* 동행 유형에 따라 추천 전략을 다르게 적용할 수 있습니다.

---

## 팀 개발 원칙

* 직접 `dev` 브랜치에 push하지 않습니다.
* 모든 기능은 Pull Request를 통해 병합합니다.
* GitHub Actions 실패 시 병합하지 않습니다.
* API 변경 시 문서를 수정하고 팀원에게 공유합니다.
* 공통 코드 변경 시 팀원과 먼저 협의합니다.
* 민감 정보는 절대 커밋하지 않습니다.

---

## License

이 프로젝트는 학습 및 포트폴리오 목적으로 제작되었습니다.

