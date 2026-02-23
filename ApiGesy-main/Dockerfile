# =========================
# 1) BUILD STAGE
# =========================
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Limite mémoire Maven pour éviter les OOM en CI / cloud
ENV MAVEN_OPTS="-Xmx512m -XX:+UseSerialGC"

# Copier uniquement le pom pour profiter du cache Docker
COPY pom.xml .

# Télécharger les dépendances à l'avance (cache)
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Build du JAR Spring Boot
RUN mvn clean package -DskipTests


# =========================
# 2) RUNTIME STAGE
# =========================
# Image stable (glibc) → évite les crashs Alpine
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copier le JAR généré
COPY --from=build /app/target/gesy-0.0.1-SNAPSHOT.jar app.jar

# Port Spring Boot
EXPOSE 8080

# Option JVM recommandée pour container
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

