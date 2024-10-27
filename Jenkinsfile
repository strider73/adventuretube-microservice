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
                        "adventuretube-microservice-geospatial-service",
                        "adventuretube-microservice-member-service",
                        "adventuretube-microservice-auth-service",
                        "adventuretube-microservice-gateway-service",
                        "adventuretube-microservice-config-service",
                        "adventuretube-microservice-eureka-server"
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
