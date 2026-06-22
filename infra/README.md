# Gotoday Terraform Infrastructure

`오늘 어디가` 서비스의 AWS 인프라를 Terraform으로 재현하기 위한 코드입니다.

## 목적

현재 운영 중인 `team06-web` 인스턴스를 직접 변경하는 것이 아니라, 현재 운영 서버 구성을 참고하여 신규 VPC 기반 인프라를 별도로 생성하고 검증하기 위한 Terraform 코드입니다.

멘토님 조언에 따라, 운영 서버를 바로 Terraform으로 전환하지 않고 별도 환경에 인프라를 생성하여 정상 동작 여부를 확인하는 방식으로 진행했습니다.

## 구성 범위

Terraform으로 생성하는 리소스는 다음과 같습니다.

- VPC
- Public Subnet
- Internet Gateway
- Route Table
- Route Table Association
- Security Group
- EC2 Instance

EC2 생성 시 `user_data`를 통해 다음 초기 설정을 수행합니다.

- Docker 설치
- Git 설치
- Docker 서비스 활성화
- Swap 4GB 설정
- `/home/ec2-user/gotoday/nginx` 디렉토리 생성

## 현재 운영 서버 참고값

현재 운영 서버인 `team06-web`의 설정을 참고하여 작성했습니다.

- Region: `ap-northeast-2`
- Availability Zone: `ap-northeast-2b`
- AMI: Amazon Linux 2023
- Instance Type: `t3.small`
- Root Volume: `gp3`, 20GB
- Open Ports:
    - SSH: 22
    - HTTP: 80
    - HTTPS: 443

## Terraform에서 제외한 항목

이번 Terraform 작업은 인프라 생성 검증을 목적으로 하므로, 아래 항목은 범위에서 제외했습니다.

- IAM Role / Instance Profile
- SSM 설정
- EIP 연결
- 도메인 A 레코드 변경
- NPM SSL 설정
- Docker Compose 실행
- Backend / Frontend 배포
- Blue-Green 배포
- MySQL 데이터 이전

애플리케이션 배포와 무중단 배포는 기존 GitHub Actions에서 담당합니다.

## 역할 분리

### Terraform

Terraform은 인프라 생성과 서버 초기 환경 구성을 담당합니다.

- 네트워크 구성
- 보안 그룹 구성
- EC2 생성
- 서버 기본 패키지 설치
- Swap 설정
- 배포 디렉토리 생성

### GitHub Actions

GitHub Actions는 애플리케이션 배포를 담당합니다.

- Backend Docker image build/push
- Frontend Docker image build/push
- Docker Compose 실행
- Backend Blue-Green 배포
- Nginx upstream 전환
- Health check

## 검증 결과

아래 명령어로 Terraform 설정을 검증했습니다.

```bash
terraform init
terraform fmt
terraform validate
terraform plan
```

`terraform plan` 결과는 다음과 같습니다.

```text
Plan: 7 to add, 0 to change, 0 to destroy.
```

매니저님 확인 후 `terraform apply`를 실행하여 테스트 인프라 생성을 검증했습니다.

생성된 테스트 인프라:

```text
VPC: vpc-0665214e3fc35c9a6
Subnet: subnet-011d9bb38ebfb1ad3
Security Group: sg-0724a4f0841a254aa
EC2: i-054f9559762df5a8d
Public IP: 3.38.163.212
```

생성된 EC2에 SSH 접속 후 아래 항목을 확인했습니다.

```bash
docker --version
git --version
free -h
ls -al /home/ec2-user/gotoday
docker ps
```

확인 결과:

- Docker 설치 확인
- Git 설치 확인
- Swap 4GB 설정 확인
- `/home/ec2-user/gotoday/nginx` 디렉토리 생성 확인
- Docker 명령 실행 확인

테스트 완료 후 아래 명령어로 생성 리소스를 정리했습니다.

```bash
terraform destroy
```

## 주의 사항

`terraform apply`를 실행하면 현재 운영 중인 `team06-web` 인스턴스가 변경되는 것이 아니라, 신규 VPC와 신규 EC2 인스턴스가 추가로 생성됩니다.

따라서 실제 apply 전에는 서버 담당자 또는 매니저에게 테스트용 리소스 생성 가능 여부를 확인해야 합니다.

테스트가 끝난 뒤에는 비용 방지를 위해 반드시 아래 명령어로 리소스를 정리합니다.

```bash
terraform destroy
```

## 사용 명령어

```bash
cd infra

terraform init
terraform fmt
terraform validate
terraform plan
terraform apply
terraform destroy
```