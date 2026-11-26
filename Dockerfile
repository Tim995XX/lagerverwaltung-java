# --- Stufe 1: Oberfläche bauen (React + TypeScript → statische Dateien) ---
FROM node:26-alpine AS oberflaeche
WORKDIR /oberflaeche

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY frontend/ ./
RUN npm run build

# --- Stufe 2: Anwendung bauen (JDK + Maven Wrapper) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Erst nur pom.xml und Wrapper kopieren: Die Abhängigkeiten landen in einer eigenen
# Schicht und werden nur neu geladen, wenn sich die pom.xml ändert.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline

COPY src src
# Spring Boot liefert alles unter static/ aus: Oberfläche und API kommen vom selben Server (kein CORS)
COPY --from=oberflaeche /oberflaeche/dist src/main/resources/static
# Tests laufen lokal bzw. in CI mit ./mvnw verify, hier nur das Paket bauen
RUN ./mvnw -q package -DskipTests

# --- Stufe 3: ausführen (nur JRE, kein Maven, kein Quellcode) ---
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home lager
COPY --from=build /build/target/lagerverwaltung-*.jar app.jar

USER lager
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
