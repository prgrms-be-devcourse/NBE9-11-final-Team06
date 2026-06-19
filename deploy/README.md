# Backend & Frontend EC2 Deploy

## 배포 구조

프론트엔드와 백엔드는 Docker 이미지로 빌드되며, GitHub Actions를 통해 EC2에 자동 배포됩니다.

현재 배포 환경은 단일 EC2 인스턴스에서 Docker Compose 기반으로 구성되어 있습니다.
```
dev 브랜치 merge
→ GitHub Actions 실행
→ Backend / Frontend Docker 이미지 build
→ GHCR push
→ EC2 SSH 접속
→ 최신 이미지 pull
→ Docker Compose 재실행
→ Health Check
```
현재 서비스는 HTTPS가 적용되어 있으며, 외부 요청은 Nginx Proxy Manager를 통해 내부 Nginx로 전달됩니다.
```
Client
→ https://gotoday.site
→ gotoday-npm
→ gotoday-nginx
→ frontend:3000 / backend:8080
```
## 구성 요소

| 구성 요소 | 역할 |
|---|---|
| Dockerfile | Backend / Frontend Docker 이미지 빌드 |
| GHCR | Docker 이미지 저장소 |
| EC2 | NPM, Nginx, Frontend, Backend, MySQL 컨테이너 실행 서버 |
| Docker Compose | EC2 컨테이너 실행 및 의존성 관리 |
| Nginx Proxy Manager | HTTPS 인증서 관리, HTTP → HTTPS 리다이렉트, 외부 Reverse Proxy |
| Nginx | Docker 내부에서 Frontend / Backend 요청 라우팅 |
| Frontend | Next.js 프론트엔드 컨테이너 |
| Backend | Spring Boot 백엔드 컨테이너 |
| MySQL | 배포 환경 데이터베이스 |
| GitHub Actions | Docker 이미지 빌드 및 EC2 자동 배포 |

## 배포 파일 구조

Git 기준 배포 파일은 아래 경로에서 관리합니다.
```
deploy/
├── docker-compose.ec2.yml
├── nginx/
│   └── default.conf
└── README.md
```
EC2 서버에서는 아래 구조로 배포 파일을 관리합니다.
```
~/gotoday/
├── docker-compose.yml
├── .env
└── nginx/
    └── default.conf
```
현재 자동 배포는 EC2의 ~/gotoday/docker-compose.yml을 기준으로 실행됩니다.

GitHub Actions 배포 과정에서 아래 파일이 EC2로 복사됩니다.
```
deploy/docker-compose.ec2.yml → ~/gotoday/docker-compose.yml
deploy/nginx/default.conf     → ~/gotoday/nginx/default.conf
```
따라서 EC2에서 직접 docker-compose.yml을 수정하더라도, 이후 GitHub Actions 배포가 다시 실행되면 deploy/docker-compose.ec2.yml 내용으로 덮어씌워질 수 있습니다.

영구 반영이 필요한 배포 설정은 반드시 Git의 deploy/docker-compose.ec2.yml에 반영해야 합니다.

## 배포 이미지

EC2 서버는 아래 GHCR 이미지를 pull하여 컨테이너를 실행합니다.
```
ghcr.io/prgrms-be-devcourse/gotoday-backend:latest
ghcr.io/prgrms-be-devcourse/gotoday-frontend:latest
```
## 현재 컨테이너 구조

현재 EC2에서는 다음 컨테이너가 실행됩니다.

컨테이너	역할	외부 노출
| 컨테이너 | 역할 | 외부 노출 |
|---|---|---|
| gotoday-npm | Nginx Proxy Manager | 80, 443 |
| gotoday-nginx | 내부 Reverse Proxy | 외부 노출 없음 |
| gotoday-frontend | Next.js Frontend | 외부 노출 없음 |
| gotoday-backend | Spring Boot Backend | 외부 노출 없음 |
| gotoday-mysql | MySQL | 외부 노출 없음 |

gotoday-npm은 외부 80, 443 포트를 담당합니다.
```
ports:
  - "80:80"
  - "443:443"
  - "127.0.0.1:81:81"
```
81번 포트는 Nginx Proxy Manager 관리자 콘솔 포트입니다.

보안상 외부에 직접 노출하지 않고 127.0.0.1에만 바인딩합니다.

gotoday-nginx는 외부 포트를 직접 노출하지 않고 Docker 내부 네트워크에서만 80번 포트를 사용합니다.
```
expose:
  - "80"
```
### 수동 배포 명령어

자동 배포가 실패하거나 수동 재배포가 필요한 경우 EC2에서 아래 명령어를 실행합니다.
```
cd ~/gotoday
docker pull ghcr.io/prgrms-be-devcourse/gotoday-backend:latest
docker pull ghcr.io/prgrms-be-devcourse/gotoday-frontend:latest
docker compose down
docker compose up -d
```
MySQL 데이터와 NPM 설정 데이터를 유지해야 하므로 일반 재배포 시 아래 명령어는 사용하지 않습니다.
```
docker compose down -v
```
-v 옵션은 Docker volume까지 삭제하므로 MySQL 데이터, NPM 설정, Let’s Encrypt 인증서 데이터가 함께 삭제될 수 있습니다.

### Health Check

NPM 적용 전에는 EC2 호스트에서 다음 방식으로 헬스체크를 수행했습니다.
```
curl -f http://localhost/actuator/health
```
하지만 NPM 적용 이후 EC2 호스트의 localhost:80 요청은 gotoday-nginx가 아니라 gotoday-npm으로 전달됩니다.

NPM에 localhost 도메인에 대한 Proxy Host가 없으면 404가 발생할 수 있습니다.

따라서 GitHub Actions 배포 헬스체크는 gotoday-nginx 컨테이너 내부에서 수행합니다.
```
docker exec gotoday-nginx wget -qO- http://localhost/actuator/health
```
정상 응답 예시는 다음과 같습니다.
```
{"status":"UP"}
```
외부에서는 다음 주소로 확인할 수 있습니다.
```
https://gotoday.site/actuator/health
```
### Secret 관리

민감 정보는 Git에 포함하지 않고 EC2의 .env 파일에서 관리합니다.
```
~/gotoday/.env
```
예시:
```
JWT_SECRET=실제_JWT_SECRET
SEOUL_API_KEY=실제_서울시_API_KEY
MYSQL_DATABASE=gotoday
MYSQL_USER=gotoday_user
MYSQL_PASSWORD=실제_DB_비밀번호
MYSQL_ROOT_PASSWORD=실제_DB_ROOT_비밀번호
FRONTEND_ORIGIN=https://gotoday.site
OAUTH2_SUCCESS_REDIRECT_URL=https://gotoday.site/oauth/callback
OAUTH2_FAILURE_REDIRECT_URL=https://gotoday.site/login?error=oauth
```
아래 값들은 GitHub에 커밋하지 않습니다.
```
.env
.pem
GitHub PAT
AWS Access Key
DB 비밀번호
JWT Secret
OAuth Client Secret
실제 API Key
```
배포 로그나 문서에 secret 값이 노출되지 않도록 주의합니다.

## MySQL 구성

EC2 배포 환경에서는 Docker Compose로 MySQL 컨테이너를 함께 실행합니다.

백엔드 컨테이너는 Docker Compose 내부 네트워크에서 MySQL에 접근합니다.
```
backend → mysql:3306
```
MySQL 3306 포트는 외부에 공개하지 않습니다.

MySQL 데이터는 Docker volume으로 유지합니다.
```
volumes:
  mysql_data:
```
MySQL이 실제로 연결 가능한 상태가 된 후 백엔드가 실행되도록 healthcheck와 depends_on.condition을 사용합니다.
```
depends_on:
  mysql:
    condition: service_healthy
healthcheck:
  test: ["CMD-SHELL", "mysqladmin ping -h localhost -u root -p$$MYSQL_ROOT_PASSWORD"]
  interval: 10s
  timeout: 5s
  retries: 10
```
## JPA ddl-auto 설정

현재 EC2 배포 환경에서는 초기 스키마 생성을 위해 아래 설정을 임시 적용합니다.
```
SPRING_JPA_HIBERNATE_DDL_AUTO: update
```
ddl-auto=update는 엔티티 기준으로 DB 스키마를 자동 변경할 수 있으므로 운영 환경에서는 위험할 수 있습니다.

현재는 프로젝트 개발 및 초기 배포 편의를 위한 설정이며, 추후 Flyway 도입 또는 초기 스키마 생성 후 validate 전환을 검토합니다.

## Nginx Proxy Manager 구성

HTTPS 적용 이후 외부 요청 흐름은 다음과 같습니다.
```
Client
→ gotoday.site:443
→ gotoday-npm
→ gotoday-nginx:80
→ frontend:3000 또는 backend:8080
```
Nginx Proxy Manager의 역할은 다음과 같습니다.

 - 외부 HTTP/HTTPS 요청 수신
 - Let’s Encrypt SSL 인증서 발급 및 갱신
 - HTTP 요청을 HTTPS로 리다이렉트
 - 도메인 기반 프록시 관리
 - 관리자 콘솔 제공

NPM Proxy Host 설정은 다음과 같습니다.
```
Domain Names:
gotoday.site
www.gotoday.site
Scheme:
http
Forward Hostname / IP:
gotoday-nginx
Forward Port:
80
```
옵션은 다음과 같이 설정합니다.
```
Block Common Exploits: ON
Websockets Support: ON
Cache Assets: OFF
```
SSL 설정은 다음과 같이 적용합니다.
```
SSL Certificate: Request a new SSL Certificate
Force SSL: ON
HTTP/2 Support: ON
HSTS: OFF
```
HSTS는 HTTPS 설정이 완전히 안정화된 이후 적용을 검토합니다.

## NPM 관리자 콘솔 접속

NPM 관리자 콘솔 포트 81은 외부에 직접 공개하지 않습니다.

따라서 SSH 터널링을 통해 접속합니다.

로컬 PC에서 다음 명령어를 실행합니다.
```
ssh -i <키파일.pem> -L 8181:127.0.0.1:81 ec2-user@54.180.73.189
```
터널링 연결을 유지한 상태에서 브라우저로 접속합니다.
```
http://localhost:8181
```

최초 접속 후 관리자 이메일과 비밀번호를 반드시 변경합니다.

## Nginx 내부 Reverse Proxy 구성

gotoday-nginx는 Docker 내부 네트워크에서 프론트엔드와 백엔드 요청을 라우팅합니다.

Nginx 설정 파일은 아래 경로에서 관리합니다.
```
deploy/nginx/default.conf
```
EC2 서버에서는 아래 위치에 반영됩니다.
```
~/gotoday/nginx/default.conf
```
Nginx는 Docker Compose 내부 네트워크에서 컨테이너 이름으로 접근합니다.
```
frontend → http://frontend:3000
backend  → http://backend:8080
```
주요 라우팅 구조는 다음과 같습니다.
```
/                  → frontend:3000
/api/**            → backend:8080
/oauth2/**         → backend:8080
/login/oauth2/**   → backend:8080
/actuator/health   → backend:8080
```
백엔드 컨테이너는 외부 포트를 직접 노출하지 않고, Docker 내부 네트워크에서만 접근할 수 있도록 구성합니다.

외부 요청은 NPM과 내부 Nginx를 통해서만 백엔드로 전달됩니다.

## HTTPS 적용 후 OAuth 설정

HTTPS 적용 이후 OAuth Redirect URI도 HTTPS 기준으로 추가해야 합니다.

Kakao Redirect URI
```
https://gotoday.site/login/oauth2/code/kakao
```
Google Redirect URI
```
https://gotoday.site/login/oauth2/code/google
```
기존 HTTP Redirect URI는 테스트 안정화를 위해 당장 삭제하지 않고 유지할 수 있습니다.

최종 운영 단계에서는 HTTPS 기준 URI만 남기는 것을 검토합니다.

## 네이버 지도 도메인 설정

네이버 지도 콘솔에서는 서비스 도메인을 등록해야 합니다.

네이버 콘솔 안내상 http와 https는 구분하지 않으며, www는 제외하고 대표 도메인명을 입력합니다.

따라서 등록 도메인은 다음과 같이 관리합니다.
```
gotoday.site
```
또는 콘솔 입력 형식에 따라 URL 형태가 필요한 경우 다음과 같이 등록합니다.
```
http://gotoday.site
```
실제 서비스 접속 주소는 HTTPS 기준으로 통일합니다.
```
https://gotoday.site
```
www.gotoday.site는 인증 이슈를 줄이기 위해 추후 gotoday.site로 리다이렉트하는 것을 검토합니다.

## 로컬 Compose 설정 검증

로컬에서 deploy/docker-compose.ec2.yml 문법을 검증할 수 있습니다.
```
MYSQL_DATABASE=gotoday \
MYSQL_USER=gotoday_user \
MYSQL_PASSWORD=dummy \
MYSQL_ROOT_PASSWORD=dummy \
JWT_SECRET=dummy \
SEOUL_API_KEY=dummy \
docker compose -f deploy/docker-compose.ec2.yml config
```
또는 로컬 임시 .env 파일을 사용해 검증할 수 있습니다.
```
docker compose --env-file .env -f deploy/docker-compose.ec2.yml config
```
단, 로컬 검증용 .env 파일은 Git에 커밋하지 않습니다.

## HTTPS 최종 테스트 체크리스트

HTTPS 적용 이후 다음 항목을 확인합니다.

- [ ] https://gotoday.site 접속 시 메인 페이지가 정상 표시된다.
- [ ] http://gotoday.site 접속 시 https://gotoday.site로 자동 리다이렉트된다.
- [ ] https://gotoday.site/actuator/health 요청이 정상 응답한다.
- [ ] https://gotoday.site/api/categories 요청이 정상 응답한다.
- [ ] 네이버 지도가 HTTPS 환경에서 정상 출력된다.
- [ ] 일반 로그인 요청이 403 없이 성공한다.
- [ ] 로그아웃 요청이 403 없이 정상 처리된다.
- [ ] 카카오 소셜 로그인이 정상 동작한다.
- [ ] 구글 소셜 로그인이 정상 동작한다.
- [ ] 신규 소셜 회원의 선호 정보 저장이 정상 동작한다.
- [ ] 새로고침 후 로그인 상태가 유지된다.
- [ ] gotoday-npm이 외부 80/443 포트를 담당한다.
- [ ] gotoday-nginx는 외부 포트를 직접 노출하지 않는다.
- [ ] GitHub Actions 배포 Health Check가 정상 통과한다.

## 장애 대응 기록

백엔드 환경변수 누락으로 인한 502

초기 배포 과정에서 백엔드 필수 환경변수인 SEOUL_CROWD_AREA_NAMES가 컨테이너에 주입되지 않아 백엔드가 정상 부팅되지 않았습니다.

이로 인해 Nginx가 백엔드 upstream에 연결하지 못해 502가 발생했습니다.

해결 방법:
```
1. EC2 .env 값 확인
2. docker-compose.yml의 env_file 설정 확인
3. 백엔드 컨테이너 재생성
4. docker inspect로 환경변수 주입 여부 확인
```
### frontend 서비스 누락으로 인한 compose 오류

Nginx 서비스가 frontend에 의존하고 있었지만, EC2 배포용 compose 파일에 frontend 서비스 정의가 누락되어 오류가 발생했습니다.

해결 방법:
```
1. frontend 서비스를 EC2 배포용 compose 파일에 추가
2. deploy/docker-compose.ec2.yml에 영구 반영
```
### NPM 도입 후 GitHub Actions Health Check 실패

NPM 도입 후 외부 80번 포트의 진입점이 gotoday-nginx에서 gotoday-npm으로 변경되었습니다.

기존 헬스체크는 다음 방식이었습니다.
```
curl -f http://localhost/actuator/health
```
하지만 이 요청이 NPM으로 전달되면서 404가 발생했습니다.

해결 방법:
```
docker exec gotoday-nginx wget -qO- http://localhost/actuator/health
```
gotoday-nginx 컨테이너 내부에서 백엔드 헬스체크를 수행하도록 변경하여 문제를 해결했습니다.

### HTTPS 전환 후 POST 요청 403 발생

HTTPS 전환 이후 로그인, 로그아웃, 선호 정보 저장 등 POST 요청에서 403이 발생했습니다.

원인은 백엔드의 FRONTEND_ORIGIN이 기존 http://gotoday.site로 설정되어 있었고, 브라우저 요청 Origin은 https://gotoday.site로 변경되었기 때문입니다.

해결 방법:
```
FRONTEND_ORIGIN=https://gotoday.site
OAUTH2_SUCCESS_REDIRECT_URL=https://gotoday.site/oauth/callback
OAUTH2_FAILURE_REDIRECT_URL=https://gotoday.site/login?error=oauth
```
환경변수 수정 후 백엔드 컨테이너를 재생성하여 문제를 해결했습니다.

### 향후 확장 방향

현재는 단일 EC2 인스턴스 기반 Docker Compose 구조로 운영합니다.

초기에는 단일 EC2 인스턴스로 운영하되, 트래픽 증가 시 로드밸런서를 통해 여러 Spring Boot 서버로 확장할 수 있습니다.

이를 위해 서버는 JWT 기반의 Stateless 구조로 설계하고, 파일 저장은 S3, 캐시는 Redis, DB는 RDS로 분리하는 구조를 고려합니다.

확장 구조는 다음과 같습니다.
```
Client
→ ALB
→ Multiple Spring Boot Servers
→ RDS
→ Redis
→ S3
```
확장 시 고려 사항은 다음과 같습니다.

- JWT 기반 Stateless 인증 구조 유지
- Refresh Token 저장소를 모든 서버가 공유 가능한 DB 또는 Redis로 분리
- 파일 저장은 EC2 로컬 디스크가 아니라 S3 사용
- DB는 Docker MySQL에서 RDS로 분리
- 캐시, 토큰 블랙리스트, 조회수 등 공유 상태는 Redis 사용
- 서버 인스턴스 증설 시 ALB 기반 로드밸런싱 적용
