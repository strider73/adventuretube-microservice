# AdventureTube - Microservices Backend

## 1. Project Overview

[AdventureTube](https://adventuretube.net/) project is a personal project designed to showcase a comprehensive mobile system that can be used in many cases.  
"AdventureTube - Microservice" is a Java Spring framework to support [AdventureTube iOS](https://github.com/strider73/AdventureTube) as the backbone system.

The AdventureTube project will include both a user-facing frontend and a robust backend system, serving as a foundational framework for various commercial applications in the mobile ecosystem.

### AdventureTube Microservices

This backend system has been developed using the **Java Spring Framework**, complemented by **Spring Cloud**, and utilizes a **dual-database system**, incorporating **MongoDB** and **PostgreSQL**.

- **MongoDB** is employed for managing JSON-styled and geographical data, providing excellent support for geolocation queries essential for pinpointing specific locations on user interfaces.

- **PostgreSQL** handles traditional relational database management tasks.

Additionally, the project includes a **Kafka messaging system** to ensure reliable message delivery, enhancing the system's resilience and safeguarding the integrity of user data.

Finally, the infrastructure is orchestrated using **Docker Compose**.

---

## 2. Microservices Architecture

The microservices architecture is organized into six submodules within three different Docker Compose configurations, enhancing maintainability and simplifying deployment. 
This structured separation allows for easier management and debugging, preventing the complexities that would arise if all submodules were contained within a single Docker configuration.
This systematic division helps streamline development and ensures each part can be independently managed and scaled as needed.

### 1. Configuration and Messaging System

The first layer involves the foundational setup using:

- **Spring Config** - Manages centralized configuration.
- **Spring Eureka** - Handles service discovery and registry.
- **Spring Gateway** - Acts as the API Gateway to route requests efficiently.

This layer must be initialized first to facilitate the setup and coordination of subsequent processes.

### 2. Service Layer

The second layer comprises several distinct services:

- **Authentication and Authorization Service**: Handles user authentication and role-based access control. It secures endpoints and ensures only authorized users can access certain functionalities.
- **Member Service**: Manages user profile data, registration, and role assignments. It interacts with both PostgreSQL for relational data and MongoDB for structured metadata.
- **Geospatial Services**: Processes and stores geolocation data, enabling users to search and navigate location-based content efficiently.
- **Web Services**: Provides API endpoints and integrates with the frontend applications. This service facilitates communication between the mobile app, web interface, and backend services.

### 3. Database Layer

This layer manages data persistence and consists of database systems that support the functionality of the above services.

---


## 3. Technology Stack

The AdventureTube microservices backend is a content management system that processes external requests to access user content through robust authentication and authorization mechanisms.

### Security Infrastructure

- **Spring Security**: Facilitates authentication and authorization processes.
- **JWT (JSON Web Tokens)**: Ensures secure communication without maintaining live sessions.

### Data Management

- **Spring Data JPA**: Enables easy interaction with relational databases.
- **MapStruct**: Provides efficient object mapping between DTOs and entities.

### Database Technologies

- **PostgreSQL**: Used for handling relational database tasks.
- **MongoDB**: Supports JSON-based and geospatial data storage.

### Messaging System

- **Apache Kafka**: Manages event-driven communication and ensures reliable data processing.

---

## 4. Infrastructure & Deployment

- **[Docker & Docker Compose](https://github.com/strider73/jenkins-docker-compose)**: Simplifies containerized deployments and service orchestration.
- **[Spring Cloud](https://github.com/strider73/jenkins-docker-compose)**: Enhances microservice scalability with features like service discovery and centralized configuration.
- **[Jenkins](https://github.com/strider73/jenkins-docker-compose)**: Automates the CI/CD pipeline for continuous integration and deployment.

---

## 5. Deployment - CI/CD Process

For a detailed breakdown of the **CI/CD process**, including source code management, testing, quality assurance, integration, and deployment, refer to the **Jenkins Deployment Repository**:

➡️ **[Jenkins-Docker-Compose on GitHub](https://github.com/strider73/jenkins-docker-compose)**

This repository provides a complete guide to the development and deployment lifecycle, covering:

- Continuous Integration & Continuous Deployment (CI/CD) setup.
- Automated builds and testing mechanisms.
- Image creation and deployment strategies.
- Infrastructure setup for streamlined microservices management.

---

### Related Repositories

For more details on different components of this project, refer to the following repositories:

- **[Frontend iOS Project](https://github.com/strider73/AdventureTube)**
- **[Kafka Deployment](https://github.com/strider73/kafka-docker)**
- **[Grafana Monitoring for Kafka](https://github.com/strider73/grafana-prometheus-for-kafka)**

