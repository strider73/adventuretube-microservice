FROM openjdk:17
EXPOSE 8761
VOLUME /tmp
COPY target/*.jar   eureka-server.jar
# Make port 5005 available to the world outside this container
CMD ["java","-jar","/eureka-server.jar"]