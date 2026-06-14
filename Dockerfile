# syntax=docker/dockerfile:1
# ============================================================================
#  Dockerfile multi-stage — build + image runtime du Business Core
# ============================================================================
#  Contexte de build = racine du dépôt (le projet Maven vit dans business-core/).
#  La compilation se fait DANS l'image : un échec de build bloque le déploiement.
#  Tests NON exécutés ici (-DskipTests) : ils nécessitent Docker/Testcontainers et
#  tournent en local / via mvn verify, pas pendant le build d'image.
# ============================================================================

# ── Étape 1 : build (Maven + JDK 21) ─────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY business-core/pom.xml ./pom.xml
COPY business-core/src ./src
# Cache des dépendances .m2 entre builds (BuildKit).
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

# ── Étape 2 : runtime (JRE 21 seul) ──────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
# Le jar repackagé par spring-boot-maven-plugin (le .jar.original est exclu par le glob).
COPY --from=build /app/target/business-core-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
