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
                    // Use Maven wrapper to build the package
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
                       "adventuretube-microservice-eureka-server-1": "eureka-server",
                       "adventuretube-microservice-config-service-1": "config-service",
                       "adventuretube-microservice-gateway-service-1": "gateway-service",
                       "adventuretube-microservice-auth-service-1": "auth-service",
                       "adventuretube-microservice-member-service-1": "member-service",
                       "adventuretube-microservice-geospatial-service-1": "geospatial-service"
                    ]

                    // Loop through each service and restart it
                    serviceImageMap.each { serviceName, imageName ->
                     // Extract the base service name (e.g., config-service)
                      def baseServiceName = serviceName.split('-')[1..-2].join('-') // Get the second part to second-to-last part and join them with '-'
                        sh """
                            echo "Updating ${serviceName}..."
                            docker stop ${serviceName} || true
                            docker rm ${serviceName} || true
                            docker run -d --name ${serviceName} ${imageName}:latest
                        """
                    }
                }
            }
        }
    }
}
