# Backend EC2 Deploy

## 배포 구조

현재 백엔드는 Docker 기반으로 빌드하고, GitHub Actions를 통해 EC2에 자동 배포합니다.

전체 흐름은 다음과 같습니다.
```text
dev 브랜치 merge
→ GitHub Actions 실행
→ 백엔드 Docker 이미지 build
→ GHCR에 latest 및 commit SHA 태그로 push
→ GitHub Actions가 EC2에 SSH 접속
→ EC2에서 최신 이미지 pull
→ Docker Compose로 백엔드 컨테이너 재실행
→ /actuator/health Health Check
```

## 구성 요소

| 구성 요소 | 역할 |
|---|---|
| Dockerfile | Spring Boot 백엔드 애플리케이션을 Docker 이미지로 빌드 |
| GHCR | GitHub Actions에서 빌드한 백엔드 Docker 이미지 저장 |
| EC2 | 백엔드 컨테이너 실행 서버 |
| Docker Compose | EC2에서 백엔드 컨테이너 실행 관리 |
| GitHub Actions | Docker 이미지 빌드, GHCR push, EC2 자동 배포 수행 |

## 배포 이미지

EC2 서버는 아래 GHCR 이미지를 pull하여 실행합니다.
```text
ghcr.io/prgrms-be-devcourse/gotoday-backend:latest 
```

## EC2 수동 배포 명령어

자동 배포가 실패하거나 수동으로 재배포가 필요한 경우 EC2에서 아래 명령어를 실행합니다.
```bash
cd ~/gotoday
docker pull ghcr.io/prgrms-be-devcourse/gotoday-backend:latest
docker compose down
docker compose up -d 
```

## Health Check

배포 후 백엔드 상태는 아래 명령어로 확인합니다.
```bash
curl http://localhost:8081/actuator/health
```

정상 응답 예시는 다음과 같습니다.

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

외부에서는 아래 주소로 확인할 수 있습니다.
```text
http://EC2_PUBLIC_IP:8081/actuator/health
```

## 자동 배포 확인 방법

dev 브랜치에 PR이 merge되면 GitHub Actions의 Backend Docker Build and Push workflow가 실행됩니다.

정상적으로 자동 배포가 완료되면 아래 job이 모두 성공해야 합니다.

```text
docker
deploy
```

deploy job에서는 다음 작업을 수행합니다.

```text
EC2 SSH 접속
→ 최신 Docker 이미지 pull
→ Docker Compose 재실행
→ /actuator/health 재시도 확인
```

## 팀원 EC2 접속 및 확인 방법

EC2 접속이 필요한 팀원은 AWS 담당자에게 .pem 키를 개인적으로 요청하면 됩니다.

.pem 키는 GitHub, Notion, Slack 공개 채널 등에 업로드하지 않습니다.

```bash
chmod 400 team06-key.pem
ssh -i team06-test-key.pem ec2-user@EC2_PUBLIC_IP
```

접속 후 배포 디렉터리로 이동합니다.

```bash
cd ~/gotoday
```

컨테이너 상태를 확인합니다.

```bash
docker ps
docker compose ps
```

Health Check를 확인합니다.

```bash
curl http://localhost:8081/actuator/health
```

## Secret 관리

백엔드 실행에 필요한 민감 정보는 Git에 포함하지 않습니다.

현재 EC2에서는 아래 파일을 통해 환경변수를 관리합니다.

```text
~/gotoday/.env
```

예시:

```env
JWT_SECRET=실제_JWT_SECRET SEOUL_API_KEY=dummy
```

주의사항:

```text
.env
.pem
GitHub PAT
AWS Access Key
DB 비밀번호
실제 API Key
```

위 값들은 GitHub에 커밋하지 않습니다.

## 현재 보안 설정 참고

현재 테스트 편의를 위해 SSH 22번 포트는 임시로 전체 허용 상태입니다.

실제 운영 환경 또는 주요 secret 적용 이후에는 필요한 IP만 허용하도록 변경할 예정입니다.
