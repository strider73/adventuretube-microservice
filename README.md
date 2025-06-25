# AdventureTube Microservices Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🚀 Quick Start

**TL;DR**: Clone → Configure Environment → Build → Deploy Infrastructure → Start Services

```bash
# 1. Clone and navigate
git clone https://github.com/strider73/adventuretube-microservice.git
cd adventuretube-microservice

# 2. Configure environment (choose your platform)
cp env.mac .env          # macOS
cp env.pi .env           # Raspberry Pi
cp env.prod .env         # Production

# 3. Build all services
mvn clean install

# 4. Start infrastructure and services
docker-compose -f docker-compose-storages.yml --env-file .env up -d
docker-compose -f docker-compose-clouds.yml --env-file .env up -d  
docker-compose -f docker-compose-adventuretubes.yml --env-file .env up -d

# 5. Verify deployment
docker-compose ps
open http://localhost:8761  # Eureka Dashboard
```

## 🏗️ Project Overview

**AdventureTube** is a comprehensive microservices-based backend platform designed to support mobile and web applications with robust authentication, geospatial services, and content management capabilities. Built with modern Java Spring frameworks, it serves as the backbone for the [AdventureTube iOS application](https://github.com/strider73/AdventureTube) and demonstrates enterprise-grade microservices architecture patterns.

### 🎯 Key Features

- **🏛️ Microservices Architecture**: Modular, scalable, and maintainable service-oriented design
- **🗄️ Dual Database System**: PostgreSQL for relational data, MongoDB for geospatial and document storage
- **🔐 Advanced Security**: JWT-based authentication with OAuth2 integration (Google)
- **🔍 Service Discovery**: Eureka-based service registry and discovery
- **🚪 API Gateway**: Centralized routing, load balancing, and rate limiting
- **⚙️ Configuration Management**: Centralized configuration with Spring Cloud Config
- **📨 Event-Driven Architecture**: Kafka messaging for reliable inter-service communication
- **🐳 Containerized Deployment**: Docker Compose orchestration for easy deployment
- **🔄 CI/CD Pipeline**: Jenkins-based continuous integration and deployment

## 🏛️ Architecture Overview

The platform follows a layered microservices architecture organized into three main tiers:

```
┌─────────────────────────────────────────────────────────────┐
│                    🌐 Client Applications                   │
│                 (iOS App, Web Frontend)                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                🚪 API Gateway (Port 8030)                  │
│              Routing, Load Balancing, Security             │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                ☁️ Infrastructure Layer                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │ Eureka      │ │ Config      │ │ Gateway Service     │   │
│  │ Server      │ │ Service     │ │ (8030)              │   │
│  │ (8761)      │ │ (9297)      │ │                     │   │
│  └─────────────┘ └─────────────┘ └─────────────────────┘   │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                🎯 Business Logic Layer                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ Auth     │ │ Member   │ │ Geospatial│ │ Web Service  │   │
│  │ Service  │ │ Service  │ │ Service   │ │ (8040)       │   │
│  │ (8010)   │ │ (8070)   │ │ (8060)    │ │              │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                   💾 Data Layer                             │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │ PostgreSQL  │ │ MongoDB     │ │ Apache Kafka        │   │
│  │ (5432)      │ │ (27017)     │ │ Event Streaming     │   │
│  │             │ │             │ │                     │   │
│  └─────────────┘ └─────────────┘ └─────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Service Details

| Service | Purpose | Port | Debug | Technology Stack |
|---------|---------|------|-------|------------------|
| **🔍 Eureka Server** | Service Discovery & Registry | 8761 | - | Spring Cloud Netflix |
| **⚙️ Config Service** | Centralized Configuration | 9297 | - | Spring Cloud Config |
| **🚪 Gateway Service** | API Gateway & Load Balancer | 8030 | 5004 | Spring Cloud Gateway |
| **🔐 Auth Service** | Authentication & Authorization | 8010 | 5005 | Spring Security + JWT |
| **👤 Member Service** | User Profile Management | 8070 | 5006 | Spring Data JPA |
| **🗺️ Geospatial Service** | Location Services | 8060 | 5008 | Spring Data MongoDB |
| **🌐 Web Service** | Web API & Frontend Integration | 8040 | 5007 | Spring Web MVC |

## 🛠️ Technology Stack

### Core Framework
- **Java 17** - Modern Java with performance optimizations and latest features
- **Spring Boot 3.4.0** - Application framework with auto-configuration
- **Spring Cloud 2024.0.0** - Microservices tooling and distributed system patterns
- **Maven** - Dependency management and build automation

### Security & Authentication
- **Spring Security** - Comprehensive security framework
- **JWT (JSON Web Tokens)** - Stateless authentication mechanism
- **BCrypt** - Password hashing and encryption
- **OAuth2** - Third-party authentication integration (Google)

### Data Management
- **PostgreSQL** - Primary relational database for structured data
- **MongoDB 4.4.18** - Document database for geospatial data and flexible schemas
- **Spring Data JPA** - Data access layer for PostgreSQL
- **Spring Data MongoDB** - Data access layer for MongoDB
- **HikariCP** - High-performance JDBC connection pooling

### Development & Tools
- **MapStruct 1.6.3** - Type-safe object mapping between DTOs and entities
- **Lombok 1.18.36** - Boilerplate code reduction
- **SpringDoc OpenAPI** - API documentation and Swagger UI integration
- **Spring Boot Actuator** - Application monitoring and management endpoints

### Infrastructure & DevOps
- **Docker & Docker Compose** - Containerization and multi-service orchestration
- **Jenkins** - CI/CD pipeline automation with custom Jenkinsfiles
- **Apache Kafka** - Event streaming platform for asynchronous communication
- **Spring Cloud Config** - Externalized configuration management

### Database Administration
- **pgAdmin 4** - PostgreSQL database administration interface (Port 5050)
- **Mongo Express** - MongoDB web-based admin interface (Port 8081)

## 📋 Prerequisites

Ensure you have the following installed on your development machine:

| Tool | Version | Installation |
|------|---------|--------------|
| **Java** | 17+ | [Download OpenJDK](https://adoptium.net/) |
| **Maven** | 3.8+ | [Installation Guide](https://maven.apache.org/install.html) |
| **Docker** | Latest | [Get Docker](https://docs.docker.com/get-docker/) |
| **Docker Compose** | Latest | Included with Docker Desktop |
| **Git** | Latest | [Download Git](https://git-scm.com/downloads) |

### System Requirements
- **Memory**: 8GB RAM minimum (16GB recommended)
- **Storage**: 10GB free space
- **Network**: Internet connection for dependency downloads

## 🚀 Deployment Guide

### Environment Configuration

The platform supports multiple deployment environments through configuration files:

| Environment | File | Description | Use Case |
|-------------|------|-------------|----------|
| **Development** | `env.mac` | macOS local development | Development and testing |
| **Raspberry Pi** | `env.pi` | ARM-based deployment | Edge computing, IoT |
| **Production** | `env.prod` | Production configuration | Live deployment |

### Step-by-Step Deployment

#### 1. Repository Setup
```bash
git clone https://github.com/strider73/adventuretube-microservice.git
cd adventuretube-microservice
```

#### 2. Environment Configuration
```bash
# Choose your target environment
cp env.mac .env      # For macOS development
cp env.pi .env       # For Raspberry Pi
cp env.prod .env     # For production

# Customize environment variables as needed
nano .env
```

#### 3. Application Build
```bash
# Build all microservices
mvn clean install

# Or build specific services
mvn clean install -pl auth-service,member-service

# Skip tests for faster builds (not recommended for production)
mvn clean install -DskipTests
```

#### 4. Infrastructure Deployment

**Start Database and Storage Services:**
```bash
docker-compose -f docker-compose-storages.yml --env-file .env up -d
```
*This starts: PostgreSQL, MongoDB, pgAdmin, Mongo Express*

**Start Cloud Infrastructure:**
```bash
docker-compose -f docker-compose-clouds.yml --env-file .env up -d
```
*This starts: Eureka Server, Config Service, Gateway Service*

**Deploy Application Services:**
```bash
docker-compose -f docker-compose-adventuretubes.yml --env-file .env up -d
```
*This starts: Auth, Member, Geospatial, Web Services*

#### 5. Deployment Verification

**Check Service Status:**
```bash
# View running containers
docker-compose ps

# Check service logs
docker-compose logs -f auth-service
docker-compose logs -f gateway-service

# Monitor all services
docker-compose logs -f
```

**Access Service Dashboards:**
- **Eureka Dashboard**: http://localhost:8761
- **pgAdmin**: http://localhost:5050
- **Mongo Express**: http://localhost:8081
- **Gateway Health**: http://localhost:8030/actuator/health

### Production Deployment

For production environments, use the automated deployment script:

```bash
# Deploy to production with health checks
./adventuretube-service-redeploy.sh prod

# Monitor deployment progress
tail -f deployment.log
```

## 🔧 Configuration Management

### Environment Variables

Key configuration parameters in your `.env` file:

```bash
# Platform Configuration
SPRING_PROFILES_ACTIVE=mac
HOST_IP=192.168.1.112
CLOUD_IP_ADDRESS=192.168.1.105

# Security Configuration
JWT_SECRET=your-jwt-secret-key
ACCESS_TOKEN_EXPIRATION=120
REFRESH_TOKEN_EXPIRATION=86400

# Database Configuration
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-password
POSTGRES_DB=adventuretube
MONGO_USERNAME=your-mongo-user
MONGO_PASSWORD=your-mongo-password

# OAuth2 Configuration
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-secret
```

### Service Configuration

Each service can be configured through:
1. **Spring Cloud Config**: Centralized configuration server
2. **Environment Variables**: Runtime configuration
3. **Application YAML**: Service-specific settings

## 🔐 Security Configuration

### JWT Authentication Flow

```
1. User Login → Auth Service
2. Validate Credentials → Database
3. Generate JWT Token → Client
4. API Request + JWT → Gateway
5. Token Validation → Auth Service
6. Forward Request → Target Service
```

### Security Features

- **🔒 Password Encryption**: BCrypt hashing with salt rounds
- **🎫 Token Management**: Access tokens (2 min) + Refresh tokens (24 hours)
- **🛡️ Role-Based Access Control**: Admin and user roles with granular permissions
- **🌐 CORS Configuration**: Cross-origin request handling for web clients
- **🚦 Rate Limiting**: Request throttling configured in API Gateway
- **🔍 OAuth2 Integration**: Google OAuth for social authentication

### Security Configuration Example

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration:
    access-token: ${ACCESS_TOKEN_EXPIRATION:120}
    refresh-token: ${REFRESH_TOKEN_EXPIRATION:86400}

google:
  client-id: ${GOOGLE_CLIENT_ID}
  client-secret: ${GOOGLE_CLIENT_SECRET}
  redirect-uri: ${GOOGLE_REDIRECT_URI}
```

## 📊 Monitoring & Management

### Health Monitoring

All services include comprehensive health checks:

```bash
# Check individual service health
curl http://localhost:8010/actuator/health  # Auth Service
curl http://localhost:8070/actuator/health  # Member Service
curl http://localhost:8060/actuator/health  # Geospatial Service

# Check through API Gateway
curl http://localhost:8030/auth/actuator/health
```

### Service Discovery

Eureka provides real-time service registry:
- **Dashboard**: http://localhost:8761
- **Service Registration**: Automatic on startup
- **Health Monitoring**: Continuous heartbeat checks
- **Load Balancing**: Client-side load balancing

### Application Metrics

Access detailed metrics for each service:
- **Health**: `/actuator/health`
- **Info**: `/actuator/info`
- **Metrics**: `/actuator/metrics`
- **Environment**: `/actuator/env`

## 📚 API Documentation

### Interactive API Documentation

Each service provides Swagger UI for API exploration:

| Service | Swagger UI URL | Description |
|---------|----------------|-------------|
| **Auth Service** | http://localhost:8010/swagger-ui.html | Authentication endpoints |
| **Member Service** | http://localhost:8070/swagger-ui.html | User management APIs |
| **Geospatial Service** | http://localhost:8060/swagger-ui.html | Location-based services |
| **Web Service** | http://localhost:8040/swagger-ui.html | Web integration APIs |

### Core API Endpoints

#### Authentication API
```bash
# User Registration
POST /auth/register
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "securePassword",
  "firstName": "John",
  "lastName": "Doe"
}

# User Login  
POST /auth/login
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "securePassword"
}

# Token Refresh
POST /auth/refresh
Authorization: Bearer <refresh-token>

# Google OAuth Login
GET /auth/google/login
```

#### Member Management API
```bash
# Get User Profile
GET /members/profile
Authorization: Bearer <access-token>

# Update Profile
PUT /members/profile
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "bio": "Adventure enthusiast"
}

# Get User by ID
GET /members/{userId}
Authorization: Bearer <access-token>
```

#### Geospatial API
```bash
# Find Nearby Locations
GET /geospatial/nearby?lat=37.7749&lng=-122.4194&radius=5000
Authorization: Bearer <access-token>

# Create New Location
POST /geospatial/location
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "name": "Adventure Spot",
  "latitude": 37.7749,
  "longitude": -122.4194,
  "description": "Great hiking location"
}

# Search Locations
GET /geospatial/search?query=hiking&category=outdoor
Authorization: Bearer <access-token>
```

## 🧪 Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific service
mvn test -pl auth-service

# Run integration tests
mvn verify -Pintegration-test

# Generate test reports
mvn test jacoco:report
```

### Test Categories

- **Unit Tests**: Service logic and business rules
- **Integration Tests**: Database and external service integration
- **Security Tests**: Authentication and authorization
- **API Tests**: REST endpoint functionality

### Testing with Docker

```bash
# Start test environment
docker-compose -f docker-compose-storages.yml up -d
docker-compose -f docker-compose-clouds.yml up -d

# Run integration tests
mvn verify -Pintegration-test

# Cleanup test environment
docker-compose down
```

## 🚀 Development Workflow

### Local Development Setup

```bash
# 1. Start infrastructure services
docker-compose -f docker-compose-storages.yml up -d
docker-compose -f docker-compose-clouds.yml up -d

# 2. Run services locally for development
mvn spring-boot:run -pl auth-service &
mvn spring-boot:run -pl member-service &
mvn spring-boot:run -pl geospatial-service &

# 3. Enable debug mode
mvn spring-boot:run -pl auth-service -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### Hot Reloading

Enable automatic restart during development:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### Debugging

Debug ports are configured for each service:
- **Gateway Service**: 5004
- **Auth Service**: 5005  
- **Member Service**: 5006
- **Web Service**: 5007
- **Geospatial Service**: 5008

## 🔄 CI/CD Pipeline

### Jenkins Integration

The platform includes automated CI/CD pipelines:

```bash
# Cloud services pipeline
./Jenkinsfile-cloud

# Application services pipeline  
./Jenkinsfile-adventuretubes
```

### Pipeline Stages

1. **🔍 Code Checkout**: Pull latest code from repository
2. **🏗️ Build**: Compile and package applications
3. **🧪 Test**: Run unit and integration tests
4. **🔐 Security Scan**: Static code analysis and vulnerability checks  
5. **🐳 Docker Build**: Create container images
6. **📦 Deploy**: Deploy to target environment
7. **✅ Health Check**: Verify service health post-deployment

### Automated Deployment

```bash
# Deploy cloud services
./adventuretube-cloud-redeploy.sh

# Deploy application services
./adventuretube-service-redeploy.sh prod
```

## 🤝 Contributing

We welcome contributions to the AdventureTube platform! Please follow these guidelines:

### Development Process

1. **🍴 Fork the Repository**
   ```bash
   git clone https://github.com/your-username/adventuretube-microservice.git
   cd adventuretube-microservice
   ```

2. **🌿 Create Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **💻 Development Standards**
   - Follow Java coding conventions
   - Maintain test coverage above 80%
   - Update documentation for new features
   - Use conventional commit messages

4. **🧪 Testing Requirements**
   ```bash
   # Run tests before committing
   mvn clean test
   mvn verify -Pintegration-test
   ```

5. **📝 Pull Request**
   - Provide clear description of changes
   - Include test results and coverage reports
   - Reference related issues

### Code Style Guidelines

- **Java**: Follow Google Java Style Guide
- **Spring Boot**: Use standard Spring conventions  
- **REST APIs**: Follow RESTful design principles
- **Documentation**: Update README and API docs

## 🐛 Troubleshooting

### Common Issues

#### Service Discovery Issues
```bash
# Check Eureka server status
curl http://localhost:8761/actuator/health

# Verify service registration
curl http://localhost:8761/eureka/apps
```

#### Database Connection Issues
```bash
# Check PostgreSQL connection
docker exec -it postgres psql -U postgres -d adventuretube

# Check MongoDB connection  
docker exec -it mongodb mongo -u admin -p password
```

#### Port Conflicts
```bash
# Check port usage
lsof -i :8761  # Eureka
lsof -i :8030  # Gateway
lsof -i :5432  # PostgreSQL

# Kill processes using ports
kill -9 $(lsof -t -i:8761)
```

### Logging Configuration

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    com.adventuretube: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

### Performance Tuning

```bash
# JVM tuning for production
export JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"

# Database connection pool tuning
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

## 📝 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 🔗 Related Projects

- **[AdventureTube iOS App](https://github.com/strider73/AdventureTube)** - Native iOS mobile application
- **[Jenkins Docker Compose](https://github.com/strider73/jenkins-docker-compose)** - CI/CD infrastructure setup
- **[Kafka Docker Setup](https://github.com/strider73/kafka-docker)** - Event streaming configuration
- **[Grafana Monitoring](https://github.com/strider73/grafana-prometheus-for-kafka)** - Monitoring and alerting stack

## 📞 Support & Community

### Getting Help

- **🐛 Bug Reports**: [GitHub Issues](https://github.com/strider73/adventuretube-microservice/issues)
- **📖 Documentation**: [Project Wiki](https://github.com/strider73/adventuretube-microservice/wiki)  
- **💬 Discussions**: [GitHub Discussions](https://github.com/strider73/adventuretube-microservice/discussions)
- **📧 Contact**: strider.lee@gmail.com

### Community Guidelines

- Be respectful and inclusive
- Provide detailed information when reporting issues
- Help others in discussions and code reviews
- Follow the project's code of conduct

## 🏆 Acknowledgments

Special thanks to:
- **Spring Boot & Spring Cloud Communities** - For excellent frameworks and documentation
- **Docker Team** - For containerization technology
- **Open Source Contributors** - For the amazing tools and libraries
- **AdventureTube Community** - For feedback and contributions

---

<div align="center">

**🌟 Built with ❤️ by the AdventureTube Team**

*Empowering adventures through technology*

**Last Updated**: June 2025 | **Version**: 0.0.1-SNAPSHOT

[⬆️ Back to Top](#adventuretube-microservices-platform)

</div>