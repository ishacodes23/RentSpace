# Local Jenkins for testing the RentSpace pipeline on your laptop before pushing to GitHub.
FROM jenkins/jenkins:lts-jdk17
USER root

RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl gnupg rsync git python3 python3-pip \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && . /etc/os-release \
 && echo "deb [signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $VERSION_CODENAME stable" > /etc/apt/sources.list.d/docker.list \
 && apt-get update && apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin \
 && pip3 install --no-cache-dir --break-system-packages ansible docker requests \
 && git config --system --add safe.directory '*' \
 && rm -rf /var/lib/apt/lists/*

RUN jenkins-plugin-cli --plugins git workflow-aggregator github pipeline-stage-view timestamps configuration-as-code job-dsl

COPY casc.yaml /casc/casc.yaml
ENV CASC_JENKINS_CONFIG=/casc/casc.yaml
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
