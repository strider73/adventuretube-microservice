pipeline {
    agent {label 'jenkins-agent'}
    stages {
        stage('Source code checkout from Git Repository') {
            steps {
                script {
                    if (fileExists('adventuretube-microservice')) {
                        dir('adventuretube-microservice') {
                            sh 'git reset --hard'
                            sh 'git clean -fd'
                            sh 'git fetch --all'
                            sh 'git checkout main'
                            sh 'git pull'
                        }
                    } else {
                        git branch: 'main', url: 'git@github.com:strider73/adventuretube-microservice.git'
                    }
                }
            }
        }
        stage('Build Package') {
            steps {
                script {
                    sh './mvnw clean package -DskipTests -pl common-domain,gateway-service,eureka-server,config-service'
                }
            }
        }
        stage('Build Docker Images') {
            steps {
                script {
                    sh 'docker compose -f docker-compose-cloud.yml build'
                }
            }
        }
        stage('Run New Docker Images') {
            steps {
                script {
                    sh '''
                    docker compose -f docker-compose-cloud.yml down || true
                    docker compose -f docker-compose-cloud.yml up -d
                    '''

                    def services = ["adventuretube-microservice-gateway-service-1",
                                    "adventuretube-microservice-eureka-server-1",
                                    "adventuretube-microservice-config-service-1"]

                    services.each { service ->
                        def serviceReady = false
                        for (int i = 0; i < 10; i++) {
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

                        if (!serviceReady) {
                            error("${service} did not become ready in the expected time")
                        }
                    }
                }
            }
        }
    }
}
