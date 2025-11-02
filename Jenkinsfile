pipeline {
  agent any

  environment {
    JAVA_HOME = tool name: 'JDK17', type: 'jdk'
    MAVEN_HOME = tool name: 'Maven3', type: 'maven'
    M2_HOME = "${MAVEN_HOME}"
    PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${env.PATH}"

    SPRING_PROFILES_ACTIVE = 'test'

    // Ajusta SONAR_HOST_URL según dónde corra Jenkins:
    // - Si Jenkins está en el mismo docker-compose que Sonar: http://sonarqube:9000
    // - Si Jenkins está fuera (localhost):                  http://localhost:9000
    SONAR_HOST_URL = 'http://localhost:9000'
  }

  options {
    // Si no tienes instalados estos plugins, puedes comentar estas 2 líneas:
    timestamps()
    ansiColor('xterm')
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    // Opcional, pero útil para contenedores que requieren refrescar CA certs
    stage('Update CA certs (optional)') {
      when { expression { return true } }
      steps {
        sh '''
          set -e
          if command -v apt-get >/dev/null 2>&1; then
            apt-get update -y || true
            DEBIAN_FRONTEND=noninteractive apt-get install -y ca-certificates || true
            update-ca-certificates || true
          else
            echo "apt-get no disponible, saltando actualización de certificados"
          fi
        '''
      }
    }

    stage('Build & Unit Tests') {
      steps {
        // Usamos settings.xml con mirrors para evitar fallas TLS de Maven Central
        sh 'mvn -B -U -s ci/settings.xml -DskipTests=false clean verify'
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
        SONAR_TOKEN = credentials('sonar-token')
      }
      steps {
        withSonarQubeEnv('SonarQube') {
          sh """
            mvn -B -s ci/settings.xml \
              org.sonarsource.scanner.maven:sonar-maven-plugin:3.10.0.2594:sonar \
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

  post {
    success { echo 'Build OK ✅' }
    failure { echo 'Build fallido ❌' }
  }
}