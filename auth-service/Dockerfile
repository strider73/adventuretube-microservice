# Stage 1: Build
FROM openjdk:17-jdk-alpine as builder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

# Stage 2: Runtime
FROM openjdk:17-jdk-alpine
# Add curl for health checks
RUN apk --no-cache add curl
# Add a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
# Expose application port and debug port
ENV APP_PORT=8010 DEBUG_PORT=5005
EXPOSE ${APP_PORT} ${DEBUG_PORT}
# Copy layers from the builder
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./
# Entry point for Spring Boot application
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "org.springframework.boot.loader.launch.JarLauncher"]
