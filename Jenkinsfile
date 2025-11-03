pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 45, unit: 'MINUTES')
    }

    environment {
        // Nombre configurado en Manage Jenkins → System → SonarQube servers
        SONARQUBE_SERVER = 'SonarQube'
        // Ruta del Allure CLI preinstalado
        ALLURE_CLI = '/opt/allure/bin/allure'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Tests') {
            steps {
                sh '''
          chmod +x mvnw
          ./mvnw -q -DskipITs -DskipDeploy -Dmaven.test.failure.ignore=false clean verify
        '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${env.SONARQUBE_SERVER}") {
                    sh '''
            chmod +x mvnw
            ./mvnw -q \
              -DskipITs -DskipDeploy \
              -Dsonar.projectKey=taller-api-2 \
              -Dsonar.projectName=taller-api-2 \
              -Dsonar.java.binaries=target/classes \
              sonar:sonar
          '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Generate Allure Report') {
            when {
                expression { fileExists('target/allure-results') }
            }
            options { timeout(time: 10, unit: 'MINUTES') }
            steps {
                retry(2) {
                    allure includeProperties: false,
                            jdk: '',
                            commandline: "${env.ALLURE_CLI}",
                            results: [[path: 'target/allure-results']]
                }
            }
        }

        stage('Archive Artifacts') {
            when { expression { fileExists('target') } }
            steps {
                archiveArtifacts artifacts: 'target/*.jar, target/allure-report/**', allowEmptyArchive: true
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline OK'
            cleanWs()
        }
        unstable {
            echo '🟡 Pipeline UNSTABLE'
            cleanWs()
        }
        failure {
            echo '❌ Pipeline falló'
            cleanWs()
        }
    }
}