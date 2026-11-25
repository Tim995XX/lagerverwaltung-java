# --- Stufe 1: bauen (JDK + Maven Wrapper) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Erst nur pom.xml und Wrapper kopieren: Die Abhängigkeiten landen in einer eigenen
# Schicht und werden nur neu geladen, wenn sich die pom.xml ändert.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline

COPY src src
# Tests laufen lokal bzw. in CI mit ./mvnw verify, hier nur das Paket bauen
RUN ./mvnw -q package -DskipTests

# --- Stufe 2: ausführen (nur JRE, kein Maven, kein Quellcode) ---
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home lager
COPY --from=build /build/target/lagerverwaltung-*.jar app.jar

USER lager
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
