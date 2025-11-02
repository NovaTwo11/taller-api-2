pipeline {
  agent any
  environment {
    JAVA_HOME = tool name: 'JDK17', type: 'jdk'
    MAVEN_HOME = tool name: 'Maven3', type: 'maven'
    M2_HOME = "${MAVEN_HOME}"
    PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${env.PATH}"
    SPRING_PROFILES_ACTIVE = 'test'
    SONAR_HOST_URL = 'http://localhost:9000' // ajusta a sonarqube:9000 si aplica
    // Si tu red usa proxy, descomenta:
    // HTTP_PROXY = 'http://proxy.host.local:8080'
    // HTTPS_PROXY = 'http://proxy.host.local:8080'
    // NO_PROXY = 'localhost,127.0.0.1,sonarqube,jenkins'
    MAVEN_OPTS = '-Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.pool=false -Dhttps.protocols=TLSv1.2,TLSv1.3 -Dhttp.keepAlive=false'
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
    stage('Update CA certs (once)') {
      steps {
        sh '''
          set -e
          if command -v apt-get >/dev/null 2>&1; then
            apt-get update -y || true
            DEBIAN_FRONTEND=noninteractive apt-get install -y ca-certificates curl || true
            update-ca-certificates || true
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
    stage('Build & Unit Tests') {
      steps {
        sh '''
          set -e
          mvn -B -U -s ci/settings.xml -DskipTests=false clean verify \
             -Dmaven.resolver.transport=wagon \
             -Dorg.eclipse.aether.connector.connectTimeout=60000 \
             -Dorg.eclipse.aether.connector.requestTimeout=60000
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
          archiveArtifacts artifacts: 'target/**', fingerprint: true, onlyIfSuccessful: false
        }
      }
    }
    stage('SonarQube Analysis') {
      environment { SONAR_TOKEN = credentials('sonar-token') }
      steps {
        withSonarQubeEnv('SonarQube') {
          sh """
            mvn -B -s ci/settings.xml \
              org.sonarsource.scanner.maven:sonar-maven-plugin:3.10.0.2594:sonar \
              -Dsonar.projectKey=taller-api-2 \
              -Dsonar.host.url=${SONAR_HOST_URL} \
              -Dsonar.login=${SONAR_TOKEN} \
              -Dmaven.resolver.transport=wagon \
              -Dorg.eclipse.aether.connector.connectTimeout=60000 \
              -Dorg.eclipse.aether.connector.requestTimeout=60000
          """
        }
      }
    }
    stage('Quality Gate') {
      steps {
        timeout(time: 5, unit: 'MINUTES') {
          script {
            def qg = waitForQualityGate()
            if (qg.status != 'OK') { error "Quality Gate failed: ${qg.status}" }
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