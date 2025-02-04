# AdventureTube Microservice

This repository contains the microservices that power the **AdventureTube** platform. The project follows a **Spring Boot microservices architecture** with multiple independent services communicating via REST and Kafka.

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
