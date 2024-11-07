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
    

        stage('Run New Docker Image') {
            steps {
               script {
            // Start containers in detached mode
            sh '''
            docker compose -f docker-compose-pi.yml down || true
            sleep 10  # Wait for a few seconds to ensure containers are fully stopped
            docker compose -f dockcder-compose-pi.yml up -d
            '''

            // // Wait for the service to be ready by checking container status
            def serviceReady = false
            for (int i = 0; i < 10; i++) { // Adjust the loop count and sleep interval as needed
                def status = sh(
                    script: "docker inspect --format='{{.State.Health.Status}}' container_name || echo 'unhealthy'",
                    returnStdout: true
                ).trim()

                if (status == 'healthy') {
                    serviceReady = true
                    break
                }

                echo "Service is not ready yet. Retrying in 5 seconds..."
                sleep(5)
            }

            if (!serviceReady) {
                error("Service did not become ready in the expected time")
            }
        }
    }
}

//         stage('Push Docker Image') {
//             steps {
//                 script {
//                     // Optionally push the new Docker image to a Docker registry
//                     // Add an explicit closure here
//                     sh {
//                         '''
//                         docker tag adventuretube:latest your-docker-repo/adventuretube:latest
//                         docker push your-docker-repo/adventuretube:latest
//                         '''
//                     }
//                 }
//             }
//         }
        // stage('Deploy New Image') {
        //     steps {
        //         script {
        //             // Deploy the new Docker image on Raspberry Pi
        //             sshagent(['strider_jenkins_key']) {
        //                 // SSH into the Raspberry Pi and update the Docker container
        //                 sh '''
        //                 ssh -o StrictHostKeyChecking=no strider@strider.freeddns.org <<EOF
        //                 docker stop adventuretube-microservice
        //                 docker rm adventuretube-microservice
        //                 docker run -d --name adventuretube-microservice adventuretube:latest
        //                 EOF
        //                 '''
        //             }
        //         }
        //     }
        // }
    }
}
