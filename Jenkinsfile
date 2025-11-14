pipeline {
    agent {
        // Usamos un agente de Docker que tenga Maven y JDK 17
        docker {
            image 'maven:3.9-eclipse-temurin-17'
            args '-v $HOME/.m2:/root/.m2' // Cachear dependencias de Maven
        }
    }
    stages {
        stage('Checkout') {
            steps {
                // Clona el repositorio (asumiendo que Jenkins está configurado para esto)
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
                // Omitimos tests de integración (@Test) y corremos solo unitarios
                sh 'mvn test -Dskip.integration.tests=true'
            }
        }
        stage('Package') {
            steps {
                // Genera el .jar
                sh 'mvn package -DskipTests'
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    // Usamos el Dockerfile que está en este mismo directorio
                    def appImage = docker.build("taller-api-2:${env.BUILD_NUMBER}")
                    // Opcional: Si tuvieras un registry (Docker Hub, ECR, etc.)
                    // docker.withRegistry('https://docker.my-registry.com', 'my-registry-credentials') {
                    //    appImage.push()
                    // }
                }
            }
        }
    }
    post {
        always {
            // Limpiar el workspace
            cleanWs()
            // Recoger reportes de tests (si se generan)
            junit 'target/surefire-reports/*.xml'
        }
    }
}