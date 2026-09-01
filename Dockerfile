FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S nexamarket && adduser -S nexamarket -G nexamarket
WORKDIR /app
COPY --from=build /workspace/target/nexamarket-0.0.1-SNAPSHOT.jar app.jar
USER nexamarket
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=12 \
  CMD wget -q -O /dev/null http://localhost:8080/v3/api-docs || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
