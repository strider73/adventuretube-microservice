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
                    // Use Maven wrapper to build the specified modules (member-service, auth-service, web-service, geospatial-service)
                    sh './mvnw clean package -DskipTests -pl common-domain,member-service,auth-service,web-service,geospatial-service'
                }
            }
        }
        stage('Build Docker Images') {
            steps {
                script {
                    // Build new Docker images with the latest code
                    sh 'docker compose -f docker-compose-adventuretubes.yml build'
                }
            }
        }
        stage('Run New Docker Image') {
            steps {
                script {
                    // Stop and start Docker Compose in detached mode
                    sh '''
                    docker compose -f docker-compose-adventuretubes.yml down || true
                    docker compose -f docker-compose-adventuretubes.yml up -d
                    '''

                    // Define list of service container names to check
                    def services = ["adventuretube-microservice-web-service-1",
                                    "adventuretube-microservice-auth-service-1",
                                    "adventuretube-microservice-geospatial-service-1",
                                    "adventuretube-microservice-member-service-1"]



                    // Loop through each service to check if it's healthy
                    services.each { service ->
                        def serviceReady = false
                        for (int i = 0; i < 10; i++) { // Retry loop
                            def status = sh(
                                script: "docker inspect --format='{{.State.Health.Status}}' ${service} || echo 'unhealthy'",
                                returnStdout: true
                            ).trim()

                            if (status == 'healthy') {
                                serviceReady = true
                                echo "${service} is healthy."
                                break
                            }

                            echo "${service} is not ready yet. Retrying in 5 seconds..."
                            sleep(5)
                        }

                        // If the service did not become healthy in the given time, throw an error
                        if (!serviceReady) {
                            error("${service} did not become ready in the expected time")
                        }
                    }
                }
            }
        }
    }
}
