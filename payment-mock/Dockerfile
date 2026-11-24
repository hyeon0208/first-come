# 빌드 문맥은 저장소 루트다(루트 Gradle 빌드가 두 모듈을 함께 가진다).
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY backend/build.gradle.kts backend/build.gradle.kts
COPY payment-mock/build.gradle.kts payment-mock/build.gradle.kts
RUN ./gradlew :payment-mock:dependencies --configuration runtimeClasspath --no-daemon > /dev/null
COPY payment-mock/src payment-mock/src
RUN ./gradlew :payment-mock:bootJar --no-daemon && mv payment-mock/build/libs/*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
# root가 아닌 사용자로 실행한다.
USER 1000:1000
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
