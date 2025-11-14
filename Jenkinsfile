// Jenkinsfile para taller-api-2
pipeline {
    agent {
        // Usa un agente Docker con Maven y JDK 17 preinstalados
        docker {
            image 'maven:3.9-eclipse-temurin-17'
            // Monta el socket de Docker para construir imágenes
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    environment {
        // Define la URL de SonarQube
        SONAR_HOST_URL = "http://sonarqube:9000" // Asumiendo que Jenkins y SonarQube están en la misma red Docker
        // Define tu clave de proyecto de Sonar
        SONAR_PROJECT_KEY = "taller-api-2"
    }

    stages {
        stage('Checkout') {
            steps {
                echo "📥 Clonando repositorio..."
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                echo "📦 Compilando..."
                // Usa el settings.xml para mejorar la descarga de dependencias
                sh 'mvn compile -s ci/settings.xml'
            }
        }

        stage('Test') {
            steps {
                echo "🧪 Ejecutando tests unitarios..."
                // El pom.xml ya está configurado para JaCoCo
                sh 'mvn test -s ci/settings.xml'
            }
        }

        stage('Package') {
            steps {
                echo "🎁 Empaquetando JAR..."
                sh 'mvn package -s ci/settings.xml -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo "📊 Analizando con SonarQube..."
                script {
                    // Asume que tienes un 'SonarQube' configurado en Manage Jenkins > Configure System
                    // Si no, usa 'withCredentials' para el token
                    sh """
                    mvn sonar:sonar \
                        -s ci/settings.xml \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                        -Dsonar.login=${env.SONAR_AUTH_TOKEN} 
                    """
                    // Necesitarás definir SONAR_AUTH_TOKEN como credencial en Jenkins
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "🐳 Construyendo imagen Docker..."
                // El Dockerfile está en la raíz de 'taller-api-2'
                sh 'docker build -t taller-api-2:${env.BUILD_NUMBER} .'
            }
        }

        stage('Push Docker Image') {
            // Opcional: Descomenta si quieres pushear a un registro
            when { expression { return false } }
            steps {
                echo "🚀 Publicando imagen en DockerHub..."
                // Asume que 'dockerhub-credentials' es un ID de credencial en Jenkins
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh 'docker login -u ${DOCKER_USER} -p ${DOCKER_PASS}'
                    sh 'docker tag taller-api-2:${env.BUILD_NUMBER} tu-usuario/taller-api-2:${env.BUILD_NUMBER}'
                    sh 'docker push tu-usuario/taller-api-2:${env.BUILD_NUMBER}'
                }
            }
        }
    }

    post {
        always {
            echo "🧹 Limpiando workspace..."
            // Archiva los reportes de tests
            junit 'target/surefire-reports/*.xml'
            // Archiva los reportes de cobertura
            jacoco(execPattern: 'target/jacoco.exec')
            cleanWs()
        }
    }
}