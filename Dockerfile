FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Cachea las dependencias en su propia capa: solo se re-descargan si pom.xml cambia.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S seritracker && adduser -S seritracker -G seritracker
USER seritracker:seritracker

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
