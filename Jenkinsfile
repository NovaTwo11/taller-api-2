pipeline {
    agent any

    tools {
        maven 'Maven3'
        jdk 'JDK17'
    }

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        MAVEN_OPTS = '-Dmaven.resolver.transport=wagon -Dorg.eclipse.aether.connector.connectTimeout=60000 -Dorg.eclipse.aether.connector.requestTimeout=60000'
    }

    stages {
        stage('Update CA certs (once)') {
            steps {
                sh '''
                    set -e
                    if command -v apt-get &> /dev/null; then
                        apt-get update -y
                        DEBIAN_FRONTEND=noninteractive apt-get install -y ca-certificates curl
                        update-ca-certificates
                    fi
                    java -version
                    mvn -v
                    echo "Testing connectivity..."
                    curl -I --max-time 20 https://repo1.maven.org/maven2/ || true
                    curl -I --max-time 20 https://maven-central.storage-download.googleapis.com/maven2/ || true
                    curl -I --max-time 20 http://repo1.maven.org/maven2/ || true
                '''
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Tests') {
            steps {
                sh '''
                    set -e
                    mvn -B -U -s ci/settings.xml \
                        -DskipTests=false \
                        clean verify \
                        ${MAVEN_OPTS}
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') { // Debe coincidir con el Name configurado en Jenkins
                    sh '''
        chmod +x mvnw || true
        ./mvnw -B clean verify sonar:sonar \
          -Dsonar.projectKey=taller-api-2 \
          -Dsonar.projectName="Taller API 2" \
          -Dsonar.java.binaries=target/classes
      '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Generate Allure Report') {
            steps {
                script {
                    allure([
                        includeProperties: false,
                        jdk: '',
                        results: [[path: 'target/allure-results']]
                    ])
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            cleanWs()
        }
        success {
            echo '✅ Pipeline ejecutado exitosamente'
        }
        failure {
            echo '❌ Pipeline falló'
        }
    }
}