FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN chmod +x mvnw
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/target/beer-catalogue-0.0.1-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=default

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
