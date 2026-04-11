# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/MoneyManager-0.0.1-SNAPSHOT.jar moneymanager-v1.0.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "moneymanager-v1.0.jar"]