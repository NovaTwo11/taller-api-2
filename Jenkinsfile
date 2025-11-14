pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-17'
            args '-v $HOME/.m2:/root/.m2'
        }
    }

    stages {
        stage('Checkout') {
            steps {
                ansiColor('xterm') {
                    checkout scm
                }
            }
        }

        stage('Compile') {
            steps {
                ansiColor('xterm') {
                    sh 'mvn compile'
                }
            }
        }

        stage('Test') {
            steps {
                ansiColor('xterm') {
                    sh 'mvn test -Dskip.integration.tests=true'
                }
            }
        }

        stage('Package') {
            steps {
                ansiColor('xterm') {
                    sh 'mvn package -DskipTests'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                ansiColor('xterm') {
                    script {
                        def appImage = docker.build("taller-api-2:${env.BUILD_NUMBER}")
                    }
                }
            }
        }
    }

    post {
        always {
            ansiColor('xterm') {
                cleanWs()
                junit 'target/surefire-reports/*.xml'
            }
        }
    }

}
