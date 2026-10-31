#!/usr/bin/env bash
# One-time setup of Jenkins + Ansible on the RentSpace EC2 (Ubuntu). Run as: bash setup-jenkins-ec2.sh
set -euo pipefail

echo "== 0. Swap (Spring Boot + MySQL + Prometheus + Grafana + Jenkins is tight on small instances)"
if ! swapon --show | grep -q swapfile; then
  sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
  sudo mkswap /swapfile && sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

echo "== 1. Java 17, git, rsync"
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jdk git rsync curl gnupg ca-certificates

echo "== 2. Jenkins (LTS)"
sudo install -d /etc/apt/keyrings
sudo curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key -o /etc/apt/keyrings/jenkins-keyring.asc
echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" \
  | sudo tee /etc/apt/sources.list.d/jenkins.list >/dev/null
sudo apt-get update -y && sudo apt-get install -y jenkins

echo "== 3. Move Jenkins to port 8081 (RentSpace already owns 8080)"
sudo mkdir -p /etc/systemd/system/jenkins.service.d
printf '[Service]\nEnvironment="JENKINS_PORT=8081"\n' | sudo tee /etc/systemd/system/jenkins.service.d/override.conf >/dev/null
sudo systemctl daemon-reload

echo "== 4. Ansible + Docker collection + Python docker SDK"
sudo apt-get install -y ansible python3-docker python3-requests
sudo ansible-galaxy collection install community.docker --upgrade -p /usr/share/ansible/collections

echo "== 5. Let the jenkins user run docker and sudo (needed by 'become: yes' in the playbook)"
sudo usermod -aG docker jenkins
echo 'jenkins ALL=(ALL) NOPASSWD:ALL' | sudo tee /etc/sudoers.d/jenkins >/dev/null
sudo chmod 440 /etc/sudoers.d/jenkins

echo "== 6. Start Jenkins"
sudo systemctl enable --now jenkins
sudo systemctl restart jenkins
sleep 20

echo
echo "Jenkins:  http://$(curl -s http://checkip.amazonaws.com):8081"
echo "Unlock password:"
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
