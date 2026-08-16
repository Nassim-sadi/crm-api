# Build stage: JDK 26 + the Maven wrapper (which downloads Maven 3.9.16 itself)
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
COPY src src

RUN chmod +x mvnw && ./mvnw -q package -DskipTests

# Runtime stage: slim JRE image, app jar only
FROM eclipse-temurin:26-jre
WORKDIR /app

COPY --from=build /app/target/crm-api-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
