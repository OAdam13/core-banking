# ============================================================
# STAGE 1: Build
# Uses a full JDK image with Maven to compile and package the app.
# This stage is NOT included in the final image — it's thrown away.
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and POM first (for dependency caching)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (cached unless pom.xml changes)
RUN chmod +x mvnw && ./mvnw dependency:resolve -B

# Copy source code
COPY src src

# Package the application (skip tests — CI runs them separately)
RUN ./mvnw package -DskipTests -B

# ============================================================
# STAGE 2: Runtime
# Uses a minimal JRE-only image. No compiler, no Maven, no source code.
# The final image is much smaller (~200MB vs ~800MB).
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy ONLY the compiled JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Document the port (doesn't actually expose it — that's docker-compose's job)
EXPOSE 8080

# Run the application with the Docker profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
