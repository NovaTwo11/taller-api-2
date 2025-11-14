pipeline {
    agent {
        docker {
            image 'my-ci/maven-git-docker:latest'
            // Monta socket Docker y conéctalo a la red de tus servicios
            args '--network app-network -v /var/run/docker.sock:/var/run/docker.sock -u root'
        }
    }
    options { skipDefaultCheckout() }

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
                echo 'Checkout dentro del contenedor...'
                checkout scm
            }
        }

        stage('Compile & Test') {
            steps {
                sh '''
          mvn clean install -s ci/settings.xml \
            -Dspring.rabbitmq.host=${RABBITMQ_HOST} \
            -Dspring.security.oauth2.resourceserver.jwt.issuer-uri=${KEYCLOAK_URL}/realms/taller \
            -Dkeycloak.admin.url=${KEYCLOAK_URL} \
            -Dskip.integration.tests=true
        '''
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    // Debug: confirma que docker existe y que el socket está montado
                    sh 'which docker || true'
                    sh 'docker --version || true'
                    sh 'ls -l /var/run/docker.sock || true'

                    def appImage = docker.build("taller-api-2:${env.BUILD_NUMBER}")
                    echo "Imagen construida: ${appImage.id}"
                }
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

    }

    post {
        always {
            echo 'Limpiando workspace...'
            junit 'target/surefire-reports/*.xml' // Publica resultados de tests [cite: 839]
            cleanWs() // [cite: 840]
        }
    }
}