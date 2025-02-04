# AdventureTube Microservice

This repository contains the microservices that power the **AdventureTube** platform. The project follows a **Spring Boot microservices architecture** with multiple independent services communicating via REST and Kafka.

## Introduction

Hi guys, this is Java iOS, and I just created a new channel named Java iOS. This is for those interested in both Java and iOS development, and I would like to understand what exactly happens between user devices and the backend system.

At this moment, I'm trying to show all the screens simultaneously. However, there's no way to put all these screens on a single monitor for a screenshot. This is my temporary choice, but from next time, I might focus on one screen at a time instead.

On the left side is IntelliJ IDEA, where I introduce my Spring Boot microservices setup. The middle screen is Xcode, where I am developing the user interface for my iOS application. The right screen shows Jenkins, managing the CI/CD pipeline that automatically updates the source code from GitHub and deploys it to my local server, which is a Raspberry Pi.

I believe the Raspberry Pi is one of the best tools for engineers and developers to build a home server with a minimal budget and low power consumption. I have failed numerous times in this process, but after several attempts, I managed to achieve build success.

This project includes Java, Spring Boot microservices, React, Spring Cloud, and an iOS application using Swift and Combine. It also utilizes Docker and Docker Compose extensively. Docker is incredibly beneficial for scaling applications in production environments, such as AWS, but it can slow down the development process.

Jenkins is crucial for CI/CD, even for a solo developer, as it automates the build and deployment process. I spend around 30% of my time working on DevOps-related tasks to ensure a seamless development workflow. The pipeline includes two major steps:
1. Deploying the iOS application to my device.
2. Updating the Spring Boot source code, triggering the Jenkins agent to pull the latest code from GitHub, build a new package, and reload the Docker image on my Raspberry Pi production server.

Let's walk through the process:
- The iOS application (AdventureTube iOS) is built and installed on my device.
- The Spring Boot application’s configuration is updated in the `application.yml` file.
- A Git commit and push trigger the Jenkins pipeline.
- Jenkins pulls the latest code, builds a new JAR file, and creates a fresh Docker image.
- The existing Docker container is stopped, removed, and replaced with the new version.
- The system checks the container’s health every five seconds up to ten times.
- Once healthy, the application is fully deployed and ready to communicate with the backend.

Moving forward, I will demonstrate actual communication between the iOS application and the Java backend. I plan to upload videos 2-3 times per week, covering both technical details and project progress.

In the first week, I will focus on the technical aspects and setup. In the second week, I will discuss the actual project and its functionalities. I hope you enjoy my journey, and I look forward to your feedback. Please leave your thoughts in the comments, and I will respond as soon as possible. Thank you!

## Microservices Structure

### 1. Core Components
- **common-domain** - Shared domain models and utilities used across services.
- **eureka-server** - Service registry using Netflix Eureka.
- **gateway-service** - API gateway using Spring Cloud Gateway.
- **config-service** - Centralized configuration management using Spring Cloud Config.

### 2. Service Modules
- **auth-service** - Authentication and authorization service.
- **member-service** - User management and profile handling.
- **geospatial-service** - Handles location-based data processing.
- **web-service** - Manages front-end API requests.

### 3. Storage Services
- **PostgreSQL & MongoDB** - Data storage solutions used across services.

### 4. Kafka Event Streaming
- **Kafka** - Enables event-driven architecture for microservices communication.

## Setup and Deployment

### Prerequisites
- Java 17+
- Docker & Docker Compose
- PostgreSQL & MongoDB
- Kafka (for event-driven services)

### Running Locally

1. Clone the repository:
   ```sh
   git clone https://github.com/strider73/adventuretube-microservice.git
   cd adventuretube-microservice
   ```

2. Start dependent services using Docker Compose:
   ```sh
   docker-compose up -d
   ```

3. Start individual services:
   ```sh
   cd config-service && ./mvnw spring-boot:run
   ```
   Repeat for each service as needed.

### Environment Configuration

- Environment variables are managed in `.env` files (e.g., `env.mac`, `env.pi`, `env.prod`).
- Set `SPRING_PROFILES_ACTIVE` appropriately (e.g., `pi`, `mac`, `prod`).
- Additional environment-specific configurations are stored in `env.XXX` files, where `XXX` represents different environments such as `dev`, `staging`, and `prod`.
- **All environment values have been centralized inside `env.XXX` files**, removing redundancy across submodules.

### Deployment

For cloud or production deployment:
```sh
./adventuretube-cloud-redeploy.sh
```

To redeploy the server:
```sh
./adventuretube-service-redeploy.sh
```

## Centralized Dockerfile Management
- **Dockerfile for all submodules has been centralized in a root-level `ssp` directory**, meaning no individual module contains a separate Dockerfile.
- This centralization simplifies build and deployment processes across microservices.

## Technologies Used
- **Spring Boot & Spring Cloud**
- **Netflix Eureka** for service discovery
- **Spring Cloud Gateway** for routing
- **PostgreSQL & MongoDB** for data storage
- **Kafka** for event-driven architecture
- **Docker & Docker Compose** for containerization

## License
This project is licensed under the MIT License.

