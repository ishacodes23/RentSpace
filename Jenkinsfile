pipeline {
    agent any

    // Jenkins (on the laptop) polls the public GitHub repo, so a push starts this pipeline within ~2 minutes.
    triggers { pollSCM('H/2 * * * *') }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        IMAGE   = "rentspace-app"
        EC2_HOST = "${env.EC2_HOST}"
    }

    stages {
        stage('Checkout Source Code') {
            steps { checkout scm }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building the image (Maven build happens inside the Dockerfile)...'
                sh 'docker build -t ${IMAGE}:latest -t ${IMAGE}:${BUILD_NUMBER} .'
            }
        }

        stage('Ship Image to EC2') {
            steps {
                echo "Sending image to ${EC2_HOST} over SSH..."
                sh '''
                    install -m 600 /keys/rentspace-key.pem /tmp/rentspace-key.pem
                    docker save ${IMAGE}:latest | gzip | \
                      ssh -i /tmp/rentspace-key.pem -o StrictHostKeyChecking=no ubuntu@${EC2_HOST} "gunzip | docker load"
                '''
            }
        }

        stage('Deploy with Ansible') {
            steps {
                sh '''
                    ansible-playbook -i "${EC2_HOST}," -u ubuntu --private-key /tmp/rentspace-key.pem \
                      -e ansible_ssh_common_args="-o StrictHostKeyChecking=no" \
                      -e source_dir=${WORKSPACE} ansible/deploy-remote.yml
                '''
            }
        }

        stage('Smoke Test & Observability Check') {
            steps {
                sh '''
                    curl -sf http://${EC2_HOST}:8080/actuator/health | grep UP
                    curl -sf http://${EC2_HOST}:9090/-/healthy
                    curl -sf http://${EC2_HOST}:3000/api/health
                    echo "RentSpace is live on EC2 and healthy!"
                '''
            }
        }
    }

    post {
        success { echo "Build #${env.BUILD_NUMBER}: deployed to EC2 ${env.EC2_HOST}" }
        failure { echo 'Pipeline failed. Check stage logs for details.' }
        always  { sh 'rm -f /tmp/rentspace-key.pem; docker image prune -f || true' }
    }
}
