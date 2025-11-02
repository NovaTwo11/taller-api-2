pipeline {
  agent any

  environment {
    JAVA_HOME = tool name: 'JDK17', type: 'jdk'
    MAVEN_HOME = tool name: 'Maven3', type: 'maven'
    M2_HOME = "${MAVEN_HOME}"
    PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${env.PATH}"
    SPRING_PROFILES_ACTIVE = 'test'
  }

  options {
    timestamps()
    ansiColor('xterm')
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Unit Tests') {
      steps {
        sh 'mvn -B -U -DskipTests=false clean verify'
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
          archiveArtifacts artifacts: 'target/**', fingerprint: true, onlyIfSuccessful: false
        }
      }
    }

    stage('SonarQube Analysis') {
      environment {
        SONAR_HOST_URL = 'http://localhost:9000' // o http://sonarqube:9000 si Jenkins está dentro del compose
        SONAR_TOKEN = credentials('sonar-token')
      }
      steps {
        withSonarQubeEnv('SonarQube') {
          sh """
            mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:3.10.0.2594:sonar \
              -Dsonar.projectKey=taller-api-2 \
              -Dsonar.host.url=${SONAR_HOST_URL} \
              -Dsonar.login=${SONAR_TOKEN}
          """
        }
      }
    }

    stage('Quality Gate') {
      steps {
        timeout(time: 5, unit: 'MINUTES') {
          script {
            def qg = waitForQualityGate()
            if (qg.status != 'OK') {
              error "Quality Gate failed: ${qg.status}"
            }
          }
        }
      }
    }
  }
}