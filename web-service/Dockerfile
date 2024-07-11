# Use an official Maven image to build the application
FROM maven:3.8.6-openjdk-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and other necessary files to the working directory
COPY pom.xml .

# Download all dependencies. This is a separate step so that Docker can cache it
RUN mvn dependency:go-offline -B

# Copy the source code to the working directory
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Use an official OpenJDK image to run the application
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /app/target/web-service-0.0.1-SNAPSHOT.jar ./web-service.jar

# Expose the port that the application will run on
EXPOSE 8080
EXPOSE 5005
# Run the application
ENTRYPOINT ["java", "-jar", "web-service.jar"]
