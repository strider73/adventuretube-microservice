pipeline {
    agent any
    stages {
        stage('Clone Repository') {
            steps {
                // Pull the latest code from GitHub
                git branch: 'add-kafka', url: 'git@github.com:strider73/adventuretube-microservice.git'
            }
        }
        stage('Build Package') {
            steps {
                script {
                    // Use Maven wrapper to build the package, skipping tests
                    sh './mvnw package -DskipTests'
                }
            }
        }
        stage('Build Docker Images') {
            steps {
                script {
                    // Build new Docker images with the latest code
                    sh 'docker compose -f docker-compose-pi.yml build'
                }
            }
        }
        stage('Deploy New Images') {
            steps {
                script {
                    // List of services to restart after rebuilding
                    def serviceImageMap = [
                        "adventuretube-microservice-eureka-server-1": ["eureka-server", "8761"],
                        "adventuretube-microservice-config-service-1": ["config-service", "9297"],
                        "adventuretube-microservice-gateway-service-1": ["gateway-service", "8030"],
                        "adventuretube-microservice-auth-service-1": ["auth-service", "8010"],
                        "adventuretube-microservice-member-service-1": ["member-service", "8070"],
                        "adventuretube-microservice-geospatial-service-1": ["geospatial-service", "8060"]
                    ]

                    // Loop through each service and restart it
                    serviceImageMap.each { serviceName, details ->
                        def imageName = details[0]
                        def servicePort = details[1]
                        def healthCheckUrl = "http://${imageName}:${servicePort}/actuator/health" // Construct the health check URL

                        // Check if the service is healthy
                        def isHealthy = sh(script: "curl -s -o /dev/null -w '%{http_code}' ${healthCheckUrl}", returnStdout: true).trim() == "200"

                        // Extract the base service name (e.g., config-service)
                        def baseServiceName = serviceName.split('-')[1..-2].join('-') // Get the second part to second-to-last part and join them with '-'

                        // Deploy the service if it is healthy
                        if (isHealthy) {
                            sh """
                                echo "Updating ${serviceName}..."
                                docker stop ${serviceName} || true
                                docker rm ${serviceName} || true
                                docker run -d --name ${serviceName} ${imageName}:latest
                            """
                        } else {
                            echo "${serviceName} is not healthy. Skipping update."
                        }
                    }
                }
            }
        }
    }
}
