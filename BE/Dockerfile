FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src ./src

RUN ./mvnw -q -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/target/BE-0.0.1-SNAPSHOT.jar ./app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
