# ===== 1단계(build stage): 소스를 jar로 컴파일 =====
# eclipse-temurin: Temurin(옛 AdoptOpenJDK) 팀이 배포하는 공식 OpenJDK 이미지
# 17-jdk: JDK 17 (컴파일 도구 javac 포함. build.gradle의 toolchain 17과 버전을 맞춤)
# "AS build": 이 단계에 build라는 이름을 붙여서, 2단계에서 --from=build로 꺼내 쓸 수 있게 함
FROM eclipse-temurin:17-jdk AS build

# 이후 명령어가 실행될 기준 디렉토리. 컨테이너 안에 없으면 자동 생성됨
WORKDIR /app

# Gradle 실행 스크립트(gradlew)만 먼저 복사
COPY gradlew .
# Windows에서 체크아웃하면 gradlew의 실행 권한(unix permission bit)이 날아가는 경우가 많아서,
# 리눅스 컨테이너 안에서 "Permission denied"로 실패하는 걸 막기 위해 명시적으로 실행 권한 부여
RUN chmod +x gradlew

# Gradle 래퍼가 실제로 쓰는 jar·설정 파일(버전 고정용, 없으면 ./gradlew 자체가 안 돌아감)
COPY gradle gradle
# 의존성 목록이 담긴 파일만 먼저 복사 (아직 src는 복사 안 함 — 바로 아래 캐싱 전략의 핵심)
COPY build.gradle settings.gradle ./

# 의존성만 미리 받아서 이 시점까지의 레이어를 Docker 캐시에 저장
# → 나중에 src만 바뀌고 build.gradle은 그대로면, 이 단계는 다시 안 돌고 캐시를 그대로 재사용해서 빌드가 빨라짐
# "|| true": 지금은 src가 없는 상태라 dependencies 태스크가 완전히는 성공 못 할 수도 있는데,
#            목적이 "의존성 다운로드"뿐이라 태스크 성공 여부 자체는 무시해도 됨
RUN ./gradlew dependencies --no-daemon || true

# 실제 소스 코드 복사. 여기부터는 소스가 바뀔 때마다 이 레이어부터 다시 빌드됨 (위 의존성 캐싱은 그대로 유지)
COPY src src
# 실행 가능한 jar(의존성 전부 포함된 fat jar) 생성 → build/libs/halfway-0.0.1-SNAPSHOT.jar
# --no-daemon: 컨테이너는 한 번 빌드하고 버려지는 일회성 환경이라 백그라운드 데몬을 유지할 필요 없음
RUN ./gradlew bootJar --no-daemon

# ===== 2단계(runtime stage): 빌드된 jar만 가져와서 실행 =====
# 컴파일 도구 없이 "실행"만 가능한 JRE 이미지 사용 → 1단계보다 훨씬 가볍고 불필요한 도구가 없어 안전함
FROM eclipse-temurin:17-jre
WORKDIR /app

# 1단계(build) 컨테이너의 build/libs 안에 있는 jar를 지금 이 이미지로 복사
# "*.jar" 와일드카드를 써도 되는 이유: 이 프로젝트는 bootJar만 실행하면 jar가 정확히 1개만 나오는 걸 확인함
# (plain jar까지 같이 만들어지는 설정이었다면 2개가 나와서 app.jar가 디렉토리로 취급되는 문제가 생길 수 있음)
COPY --from=build /app/build/libs/*.jar app.jar

# 이 컨테이너가 8080 포트를 쓴다는 걸 문서화하는 용도 (실제로 포트를 열어주는 명령은 아니고,
# docker-compose나 다른 사람이 이 이미지를 볼 때 참고하라고 남기는 메타데이터)
EXPOSE 8080

# 컨테이너 시작 시 실행되는 명령
# -Dfile.encoding=UTF-8: 베이스 이미지의 기본 로케일에 따라 로그의 한글이 깨질 수 있어서 명시적으로 고정
#   (이 프로젝트 로그 컨벤션이 전부 한글이라 특히 중요 — backend/CLAUDE.md 로깅 규칙 참고)
# -Duser.timezone=Asia/Seoul: 컨테이너 기본 타임존이 UTC일 수 있어서, 막차·약속 당일 계산이
#   엉뚱한 시각 기준으로 돌지 않게 JVM 타임존을 명시적으로 고정 (CLAUDE.md: 시간은 Asia/Seoul 기준)
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
