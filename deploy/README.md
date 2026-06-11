# EC2 Backend Docker Compose Deploy

## 실행 환경

- EC2: Amazon Linux 2023
- Docker: 25.x
- Docker Compose: v2.29.7
- Image: ghcr.io/prgrms-be-devcourse/gotoday-backend:latest

## 실행 명령어

```bash
docker login ghcr.io
docker pull ghcr.io/prgrms-be-devcourse/gotoday-backend:latest
docker compose -f docker-compose.ec2.yml up -d
```

## 팀원 EC2 접속 및 확인 방법

- EC2 접속이 필요한 팀원은 AWS 담당자에게 .pem 키를 개인적으로 요청하면 됩니다.  
- .pem 키는 GitHub, Notion, Slack 공개 채널 등에 업로드하지 않습니다.
````
bash chmod 400 team06-key.pem 
ssh -i team06-key.pem ec2-user@EC2_PUBLIC_IP 
````
접속 후 배포 디렉터리로 이동합니다.
```
bash cd ~/gotoday 
```
컨테이너 상태를 확인합니다.
```
bash docker ps docker compose ps 
```
Health Check를 확인합니다.
```
bash curl http://localhost:8081/actuator/health 
```
정상 응답 예시:
````
json {"groups":["liveness","readiness"],"status":"UP"} 
````
- 현재 테스트 편의를 위해 SSH 22번 포트는 임시로 전체 허용 상태입니다.  
- 실제 운영 또는 주요 secret 적용 이후에는 필요한 IP만 허용하도록 변경할 예정입니다.