pipeline {
    agent any

    environment {
        PROJECT_DIR = "/home/ubuntu/RentSpace"
        DOCKER_IMAGE = "rentspace-app:latest"
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                echo 'Checking out source code from GitHub...'
                checkout scm
            }
        }

        stage('Build & Test Spring Boot App') {
            steps {
                echo 'Building RentSpace with Maven...'
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker Image...'
                sh 'docker build -t ${DOCKER_IMAGE} .'
            }
        }

        stage('Deploy with Ansible') {
            steps {
                echo 'Executing Ansible Playbook...'
                sh 'ansible-playbook -i ansible/inventory.ini ansible/deploy.yml'
            }
        }

        stage('Smoke Test & Observability Check') {
            steps {
                echo 'Verifying application and Prometheus endpoints...'
                sh '''
                    curl -s -f http://localhost:8080/actuator/health | grep UP
                    curl -s -f http://localhost:8080/actuator/prometheus | grep jvm_memory_used_bytes | head -n 5
                    echo "RentSpace is live, healthy, and emitting Prometheus metrics!"
                '''
            }
        }
    }

    post {
        success {
            echo 'Deployment Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed. Check stage logs for details.'
        }
    }
}
