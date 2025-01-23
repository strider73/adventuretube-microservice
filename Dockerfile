# Stage 1: Build
# Use platform-specific base images
ARG BASE_IMAGE=arm64v8/openjdk:17
FROM ${BASE_IMAGE} as builder


# Specify the JAR file location dynamically
ARG JAR_FILE=target/*.jar
# Copy the application JAR file into the build image
COPY ${JAR_FILE} application.jar
# Use Spring Boot's layertools to extract layers for optimized builds
RUN java -Djarmode=layertools -jar application.jar extract

# Stage 2: Runtime
FROM ${BASE_IMAGE}
# Install required tools for health checks using apt-get for Debian-based images
RUN apt-get update && apt-get install -y --no-install-recommends curl && apt-get clean && rm -rf /var/lib/apt/lists/*

# Add a jenkins user for enhanced security
RUN groupadd --system jenkins && \
    useradd --system --gid jenkins --home-dir /home/jenkins --create-home jenkins
#    mkdir -p /home/jenkins/.config/jgit && \
#    chown -R jenkins:jenkins /home/jenkins && \
#    chmod -R 755 /home/jenkins
USER jenkins

# Expose application and debug ports
EXPOSE ${APP_PORT} ${DEBUG_PORT}

# Copy the extracted layers from the builder stage
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./

# Define the entry point to run the Spring Boot application
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
