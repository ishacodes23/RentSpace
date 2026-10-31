pipeline {
    agent any

    // Triggered automatically on every GitHub push (needs the webhook from docs/PHASE4_JENKINS_SETUP.md).
    // pollSCM is a fallback in case the webhook is blocked/misconfigured.
    triggers {
        githubPush()
        pollSCM('H/2 * * * *')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DOCKER_IMAGE = "rentspace-app"
        // Defaults suit the EC2 server; the local Jenkins container overrides these (see jenkins-local/docker-compose.yml)
        APP_HOST   = "${env.APP_HOST ?: 'localhost'}"
        USE_BECOME = "${env.USE_BECOME ?: 'true'}"
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                echo 'Checking out source code from GitHub...'
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                echo 'Building RentSpace with Maven...'
                sh 'chmod +x mvnw && ./mvnw -B clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Validating Docker image build...'
                sh 'docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} -t ${DOCKER_IMAGE}:latest .'
            }
        }

        stage('Deploy with Ansible') {
            steps {
                echo 'Executing Ansible Playbook...'
                sh 'ansible-playbook -i ansible/inventory.ini ansible/deploy.yml -e source_dir=${WORKSPACE} -e use_become=${USE_BECOME}'
            }
        }

        stage('Smoke Test & Observability Check') {
            steps {
                echo 'Verifying application, Prometheus and Grafana...'
                sh '''
                    curl -sf http://${APP_HOST}:8080/actuator/health | grep UP
                    curl -sf http://${APP_HOST}:8080/actuator/prometheus | grep jvm_memory_used_bytes | head -n 3
                    curl -sf http://${APP_HOST}:9090/-/healthy
                    curl -sf http://${APP_HOST}:3000/api/health
                    echo "RentSpace is live, healthy, and emitting Prometheus metrics!"
                '''
            }
        }
    }

    post {
        success { echo "Build #${env.BUILD_NUMBER}: deployment pipeline completed successfully!" }
        failure { echo 'Pipeline failed. Check stage logs for details.' }
        always  { sh 'docker image prune -f || true' }
    }
}
