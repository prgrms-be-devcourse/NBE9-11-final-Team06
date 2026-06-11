# 첫 번째 스테이지: 빌드 스테이지
# Gradle과 JDK 25가 포함된 이미지를 사용하여 Spring Boot 애플리케이션을 빌드한다.
FROM gradle:jdk25-graal AS builder

# 컨테이너 내부 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper와 Gradle 설정 파일 복사
# 프로젝트에서 사용하는 Gradle Wrapper를 그대로 사용하여 로컬과 Docker의 빌드 환경 차이를 줄인다.
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Gradle Wrapper 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 미리 다운로드
# Docker 캐시를 활용하기 위해 소스 코드 복사 전에 의존성을 먼저 받는다.
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
# 현재 테스트 코드가 완전히 준비되지 않았을 수 있으므로 Docker 이미지 빌드에서는 테스트를 제외한다.
RUN ./gradlew clean build --no-daemon -x test

# plain jar 제거
# Spring Boot 실행 가능한 jar만 남기기 위해 plain jar를 삭제한다.
RUN rm -rf /app/build/libs/*-plain.jar

# 두 번째 스테이지: 실행 스테이지
# GraalVM JDK 25 이미지를 사용하여 빌드된 JAR 파일을 실행한다.
FROM container-registry.oracle.com/graalvm/jdk:25

# 컨테이너 내부 작업 디렉토리 설정
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일을 실행 스테이지로 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring Boot 애플리케이션 실행
# 현재 Docker 실행 검증 단계에서는 H2 기반 dev profile로 실행한다.
# 추후 EC2/RDS 운영 배포 시에는 prod profile과 환경변수를 사용하도록 변경할 수 있다.
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "app.jar"]