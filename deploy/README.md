# Backend EC2 Deploy

## 배포 구조

백엔드는 Docker 이미지로 빌드되며, GitHub Actions를 통해 EC2에 자동 배포됩니다.

```text
dev 브랜치 merge
→ GitHub Actions 실행
→ Docker 이미지 build
→ GHCR push
→ EC2 SSH 접속
→ 최신 이미지 pull
→ Docker Compose 재실행
→ Health Check
```

## 구성 요소

| 구성 요소 | 역할 |
|---|---|
| Dockerfile | Spring Boot 백엔드 Docker 이미지 빌드 |
| GHCR | Docker 이미지 저장소 |
| EC2 | Nginx, Backend, MySQL 컨테이너 실행 서버 |
| Docker Compose | EC2 컨테이너 실행 및 의존성 관리 |
| MySQL | 배포 환경 데이터베이스 |
| Nginx | 외부 요청을 백엔드 컨테이너로 전달하는 Reverse Proxy |
| GitHub Actions | Docker 이미지 빌드 및 EC2 자동 배포 |

## 배포 파일 구조

Git 기준 배포 파일은 아래 경로에서 관리합니다.

```text
deploy/
├── docker-compose.ec2.yml
├── nginx/
│   └── default.conf
└── README.md
```

EC2 서버에서는 아래 구조로 배포 파일을 관리합니다.

```text
~/gotoday/
├── docker-compose.yml
├── .env
└── nginx/
    └── default.conf
```

현재 자동 배포는 EC2의 `~/gotoday/docker-compose.yml`을 기준으로 실행됩니다.

따라서 `deploy/docker-compose.ec2.yml`, `deploy/nginx/default.conf`를 수정한 경우 EC2 서버에도 동일하게 반영해야 합니다.

## 배포 이미지

EC2 서버는 아래 GHCR 이미지를 pull하여 백엔드 컨테이너를 실행합니다.

```text
ghcr.io/prgrms-be-devcourse/gotoday-backend:latest
```

## 수동 배포 명령어

자동 배포가 실패하거나 수동 재배포가 필요한 경우 EC2에서 아래 명령어를 실행합니다.

```bash
cd ~/gotoday
docker pull ghcr.io/prgrms-be-devcourse/gotoday-backend:latest
docker compose down
docker compose up -d
```

MySQL 데이터를 유지해야 하므로 일반 재배포 시 아래 명령어는 사용하지 않습니다.

```bash
docker compose down -v
```

`-v` 옵션은 Docker volume까지 삭제하므로 MySQL 데이터가 함께 삭제될 수 있습니다.

## Health Check

EC2 내부에서 확인:

```bash
curl http://localhost/actuator/health
```

백엔드 직접 접근 확인:

```bash
curl http://localhost:8081/actuator/health
```

외부에서 확인:

```text
http://EC2_PUBLIC_IP/actuator/health
```

정상 응답 예시:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

## Secret 관리

민감 정보는 Git에 포함하지 않고 EC2의 `.env` 파일에서 관리합니다.

```text
~/gotoday/.env
```

예시:

```env
JWT_SECRET=실제_JWT_SECRET
SEOUL_API_KEY=dummy

MYSQL_DATABASE=gotoday
MYSQL_USER=gotoday_user
MYSQL_PASSWORD=실제_DB_비밀번호
MYSQL_ROOT_PASSWORD=실제_DB_ROOT_비밀번호
```

아래 값들은 GitHub에 커밋하지 않습니다.

```text
.env
.pem
GitHub PAT
AWS Access Key
DB 비밀번호
실제 API Key
```

## MySQL 구성

EC2 배포 환경에서는 Docker Compose로 MySQL 컨테이너를 함께 실행합니다.

백엔드 컨테이너는 Docker Compose 내부 네트워크에서 MySQL에 접근합니다.

```text
backend → mysql:3306
```

MySQL 3306 포트는 외부에 공개하지 않습니다.

MySQL 데이터는 Docker volume으로 유지합니다.

```yaml
volumes:
  mysql_data:
```

MySQL이 실제로 연결 가능한 상태가 된 후 백엔드가 실행되도록 healthcheck와 `depends_on.condition`을 사용합니다.

```yaml
depends_on:
  mysql:
    condition: service_healthy
```

```yaml
healthcheck:
  test: ["CMD-SHELL", "mysqladmin ping -h localhost -u root -p$$MYSQL_ROOT_PASSWORD"]
  interval: 10s
  timeout: 5s
  retries: 10
```

## JPA ddl-auto 설정

현재 EC2 배포 환경에서는 초기 스키마 생성을 위해 아래 설정을 임시 적용합니다.

```yaml
SPRING_JPA_HIBERNATE_DDL_AUTO: update
```

`ddl-auto=update`는 엔티티 기준으로 DB 스키마를 자동 변경할 수 있으므로 운영 환경에서는 위험할 수 있습니다.

현재는 중간 제출 전 초기 테이블 생성을 위한 임시 설정이며, 추후 Flyway 도입 또는 초기 스키마 생성 후 `validate`로 전환할 예정입니다.

## Nginx Reverse Proxy 구성

EC2 배포 환경에서는 Nginx가 외부 요청을 백엔드 컨테이너로 전달합니다.

```text
Client
→ EC2 80번 포트
→ Nginx 컨테이너
→ Backend 컨테이너 8080 포트
```

Nginx 설정 파일:

```text
deploy/nginx/default.conf
```

EC2 서버 설정 파일:

```text
~/gotoday/nginx/default.conf
```

Nginx는 Docker Compose 내부 네트워크에서 백엔드를 아래 주소로 호출합니다.

```text
http://backend:8080
```

백엔드 컨테이너는 외부 포트를 직접 노출하지 않고, Docker 내부 네트워크에서만 접근할 수 있도록 구성합니다.
외부 요청은 Nginx 80번 포트를 통해서만 백엔드로 전달됩니다.

## 로컬 Compose 설정 검증

로컬에서 `deploy/docker-compose.ec2.yml` 문법을 검증할 수 있습니다.

```bash
MYSQL_DATABASE=gotoday \
MYSQL_USER=gotoday_user \
MYSQL_PASSWORD=dummy \
MYSQL_ROOT_PASSWORD=dummy \
JWT_SECRET=dummy \
SEOUL_API_KEY=dummy \
docker compose -f deploy/docker-compose.ec2.yml config
```

또는 로컬 임시 `.env` 파일을 사용해 검증할 수 있습니다.

```bash
docker compose --env-file .env -f deploy/docker-compose.ec2.yml config
```

단, 로컬 검증용 `.env` 파일은 Git에 커밋하지 않습니다.