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
        stage('Build Docker Image') {
            steps {
                script {
                    // Build a new Docker image with the latest code
                    sh 'docker-compose -f docker-compose-pi.yml build'
                }
            }
        }
        stage('Deploy New Images') {
            steps {
                script {
                    // Define the services to restart after rebuilding
                    def servicesToRestart = [
                        "eureka-server",
                        "config-service",
                        "gateway-service",
                        "auth-service",
                        "member-service",
                        "geospatial-service"
                    ]

                    // SSH into the Raspberry Pi to stop, remove, and restart services
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
