FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 lab
WORKDIR /app
COPY --from=build /src/target/recargalab.jar app.jar
USER lab
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
