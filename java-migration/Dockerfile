# Build multi-stage: compila con Maven dentro de la imagen (no hace falta
# tener Maven ni JDK instalados en la maquina del usuario) y corre el jar
# resultante sobre un JRE liviano.

FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app

# Cachear las dependencias en una capa aparte: mientras no cambie el
# pom.xml, "docker compose up --build" no vuelve a descargarlas.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
