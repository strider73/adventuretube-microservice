pipeline {
    agent any
    stages {
        stage('Clone Repository') {
            steps {
                // Pull the latest code from GitHub
                git 'git@github.com:strider73/adventuretube-microservice.git'
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    // Clean up unused images to save space
                    sh 'docker image prune -f'
                    // Build a new Docker image with the latest code
                    sh 'docker build -t adventuretube:latest .'
                }
            }
        }

         stage('Deploy New Image') {
                    steps {
                        script {
                            // Stop and remove any existing container and redeploy the new image
                            sh '''
                            docker stop adventuretube-microservice || true
                            docker rm adventuretube-microservice || true
                            docker run -d --name adventuretube-microservice adventuretube:latest
                            '''
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
//         stage('Deploy New Image') {
//             steps {
//                 script {
//                     // Deploy the new Docker image on Raspberry Pi
//                     sshagent(['strider_jenkins_key']) {
//                         // SSH into the Raspberry Pi and update the Docker container
//                         sh '''
//                         ssh -o StrictHostKeyChecking=no strider@strider.freeddns.org <<EOF
//                         docker stop adventuretube-microservice
//                         docker rm adventuretube-microservice
//                         docker run -d --name adventuretube-microservice adventuretube:latest
//                         EOF
//                         '''
//                     }
//                 }
//             }
//         }
    }
}
