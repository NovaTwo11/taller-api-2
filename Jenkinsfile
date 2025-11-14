pipeline {
    agent {
        docker {
            image 'maven:3.9.6-eclipse-temurin-17'
            // Conecta el agente a la red de docker-compose
            args '--network app-network'
        }
    }

    environment {
        // Define las URLs de los servicios DENTRO de la red de Docker
        // Usamos los nombres de los servicios del docker-compose.yml [cite: 950, 951, 955]
        SONAR_HOST_URL    = 'http://sonarqube:9000'
        DB_URL            = 'jdbc:postgresql://postgres:5432/tallerdb'
        RABBITMQ_HOST     = 'rabbitmq'
        KEYCLOAK_URL      = 'http://keycloak:8080'
        // Credenciales de Sonar (ajusta si es necesario)
        SONAR_TOKEN       = credentials('sonar-token')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Clonando repositorio...'
                checkout scm
            }
        }

        stage('Compile & Test') {
            steps {
                echo 'Ejecutando pruebas unitarias...'
                sh '''
                    # 💡 RUTA FINAL CORRECTA: Apunta al archivo en el workspace
                    mvn clean install -s ci/settings.xml \
                        -Dspring.datasource.url=${DB_URL} \
                        -Dspring.rabbitmq.host=${RABBITMQ_HOST} \
                        -Dspring.security.oauth2.resourceserver.jwt.issuer-uri=${KEYCLOAK_URL}/realms/taller \
                        -Dkeycloak.admin.url=${KEYCLOAK_URL} \
                        -Dskip.integration.tests=true 
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'Analizando con SonarQube...'
                // Usa las propiedades de sonar-project.properties [cite: 880]
                sh '''
                    mvn sonar:sonar \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_TOKEN} \
                        -Dsonar.projectKey=taller-api-2 \
                        -Dsonar.projectName="Taller API 2 (Java)"
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Construyendo imagen Docker...'
                script {
                    def appImage = docker.build("taller-api-2:${env.BUILD_NUMBER}")
                    echo "Imagen construida: ${appImage.id}"
                }
            }
        }
    }

    post {
        always {
            echo 'Limpiando workspace...'
            junit 'target/surefire-reports/*.xml' // Publica resultados de tests [cite: 839]
            cleanWs() // [cite: 840]
        }
    }
}