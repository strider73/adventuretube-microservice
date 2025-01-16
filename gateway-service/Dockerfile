# Stage 1: Build
FROM openjdk:17-jdk-alpine as builder
# Specify the JAR file location
ARG JAR_FILE=target/*.jar
# Copy the JAR file into the image
COPY ${JAR_FILE} application.jar
# Use Spring Boot's layertools to extract layers for efficient caching
RUN java -Djarmode=layertools -jar application.jar extract

# Stage 2: Runtime
FROM openjdk:17-jdk-alpine
# Install curl for health checks
RUN apk --no-cache add curl

# Add a non-root user for better security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Expose the application port
EXPOSE 8030

# Copy application layers from the builder stage
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./

# Define the entry point to run the application
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
