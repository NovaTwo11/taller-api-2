pipeline {
    agent any  // <-- Importante: NO usamos agent docker

    tools {
        maven 'Maven3'   // nombre configurado en Jenkins (Manage Jenkins > Global Tools)
        jdk 'JDK17'      // igual
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test -Dskip.integration.tests=true'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    dockerImage = docker.build("taller-api-2:${env.BUILD_NUMBER}")
                }
            }
        }

        stage('Run Container (optional)') {
            when { expression { return false } } // activa si quieres probarlo
            steps {
                sh """
                    docker stop taller-api-2 || true
                    docker rm taller-api-2 || true
                    docker run -d --name taller-api-2 -p 8080:8080 taller-api-2:${env.BUILD_NUMBER}
                """
            }
        }
    }

    post {
        always {
            junit 'target/surefire-reports/*.xml'
            cleanWs()
        }
    }
}
