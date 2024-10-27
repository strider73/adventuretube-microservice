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
                    def servicesToRestart = [
                        "adventuretube-microservice-eureka-server-1",
                        "adventuretube-microservice-config-service-1",
                        "adventuretube-microservice-gateway-service-1",
                        "adventuretube-microservice-auth-service-1",
                        "adventuretube-microservice-member-service-1",
                        "adventuretube-microservice-geospatial-service-1"
                    ]

                    // Loop through each service and restart it
                    servicesToRestart.each { serviceName ->
                        sh """
                            echo "Updating ${serviceName}..."
                            docker stop ${serviceName} || true
                            docker rm ${serviceName} || true
                            docker run -d --name ${serviceName} ${serviceName}:latest
                        """
                    }
                }
            }
        }
    }
}
