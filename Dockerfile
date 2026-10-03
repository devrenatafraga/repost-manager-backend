# Build the Ktor distribution, then run it on a JRE sized for the Koyeb free instance (~512 MB).
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src

COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY server server
COPY admin-api admin-api
COPY public-api public-api
COPY persistence persistence

RUN chmod +x gradlew && ./gradlew :server:installDist --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /src/server/build/install/server /app

ENV PORT=8080
EXPOSE 8080

CMD ["./bin/server"]
