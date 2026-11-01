# RentSpace: Automated CI/CD Deployment and Live Monitoring on AWS EC2

Cloud Computing on Linux project by Isha Kadam (UID 2025510028).

RentSpace is a Spring Boot property rental management application. This project
automates its build and deployment with **Jenkins, Docker and Ansible** on an AWS
EC2 Ubuntu server, and monitors the server, containers and application live with
**Prometheus and Grafana**. A load test (ApacheBench and `stress`) shows how CPU,
memory and request metrics react under load.

## Workflow

```
Developer -> GitHub -> Jenkins (build + Docker image) -> Ansible playbook -> Docker container on AWS EC2

Node Exporter (host) + cAdvisor (containers) + Spring Boot Actuator (app)
        -> Prometheus (scrape) -> Grafana (dashboards)
```

A push to GitHub triggers Jenkins, which builds and packages RentSpace. Ansible then
deploys the container to EC2, while Prometheus continuously scrapes metrics that
Grafana visualizes.

## Technologies

| Technology | Role |
|---|---|
| Spring Boot + MySQL 8 | The RentSpace application |
| Docker / Docker Compose | Containerizes the app, database and monitoring stack |
| Jenkins | CI/CD pipeline, triggered by a GitHub push |
| Ansible | Deploys the container to the EC2 server |
| Prometheus | Collects metrics |
| Grafana | Dashboards |
| Node Exporter | Host CPU, memory, disk and network metrics |
| cAdvisor | Per-container CPU, memory and network metrics |
| Spring Boot Actuator | Request rate and JVM metrics at `/actuator/prometheus` |

## Live demo links (running on AWS EC2)

These work only while the EC2 instance is running. If a link does not open, the
server is probably switched off or has a new IP address.

| Service | URL |
|---|---|
| RentSpace app | http://13.232.17.39:8080/login |
| App metrics endpoint | http://13.232.17.39:8080/actuator/prometheus |
| Grafana dashboards | http://13.232.17.39:3000 |
| Prometheus targets (all should be UP) | http://13.232.17.39:9090/targets |
| cAdvisor | http://13.232.17.39:8085 |
| Jenkins (login required) | http://13.232.17.39:8081 |

Source code: https://github.com/ishacodes23/RentSpace/tree/feature/docker-ansible

Logins are not stored in this repository. Ask the project owner for the Grafana and
Jenkins credentials.

## Run it on your own machine

You need Git and Docker (Docker Desktop on Windows or macOS, or Docker Engine on
Linux).

```bash
git clone -b feature/docker-ansible https://github.com/ishacodes23/RentSpace.git
cd RentSpace

# 1. Start the application and database
docker compose up -d --build

# 2. Start the monitoring stack
cd monitoring
docker compose -f docker-compose-monitoring.yml up -d
```

Start the application first. The monitoring file joins the `rentspace-network`
network that the application stack creates. If Docker reports that the network is
missing, run `docker network ls` and check the network name in both compose files.

Then open:

| Service | Local URL |
|---|---|
| App | http://localhost:8080/login |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090/targets |
| cAdvisor | http://localhost:8085 |

To stop everything: `docker compose down` in each folder.

### Import the Grafana dashboard

If the dashboard is not already provisioned, import the exported JSON:
Grafana -> Dashboards -> New -> Import -> upload `monitoring/grafana/dashboard.json`,
then choose Prometheus as the data source.

### Limits when running locally

- On Docker Desktop (Windows/macOS), Node Exporter and cAdvisor see Docker's
  virtual machine, not your laptop, so some host panels show different values.
- The full push-to-deploy pipeline needs a Linux server with Jenkins, Docker and
  Ansible installed (see below). Locally you can run the app and the monitoring
  stack.

## CI/CD pipeline

The Jenkins job `RentSpace-Pipeline` reads the `Jenkinsfile` from the
`feature/docker-ansible` branch. Stages:

1. **Checkout Source Code**: pulls the branch from GitHub.
2. **Build & Unit Test**: `./mvnw clean package`.
3. **Build Docker Image**: builds `rentspace-app:<build number>` and `:latest`.
4. **Deploy with Ansible**: runs `ansible/deploy.yml` to replace the running container.
5. **Smoke Test & Observability Check**: checks `/actuator/health`, the Prometheus
   metrics, Prometheus and Grafana.

The pipeline is triggered by a GitHub webhook (`/github-webhook/` on the Jenkins
port) with a 2-minute SCM polling fallback.

### Setting up Jenkins on a fresh EC2 Ubuntu server

```bash
sudo apt update
sudo apt install -y fontconfig openjdk-17-jre git ansible
# Install Jenkins from the official repository (https://www.jenkins.io/doc/book/installing/linux/)
sudo usermod -aG docker jenkins
echo "jenkins ALL=(ALL) NOPASSWD:ALL" | sudo tee /etc/sudoers.d/jenkins
```

Jenkins runs on port 8081 here (8080 is used by the application). Open that port
in the EC2 security group, then create a Pipeline job:

- Definition: Pipeline script from SCM
- SCM: Git, repository `https://github.com/ishacodes23/RentSpace.git`
- Branch: `*/feature/docker-ansible`
- Script path: `Jenkinsfile`
- Build trigger: GitHub hook trigger for GITScm polling

Add a GitHub webhook pointing to `http://<EC2-IP>:8081/github-webhook/`
(content type `application/json`, push events).

The `jenkins-local/` folder holds a separate Docker-based Jenkins setup for a
laptop. Do not copy its pipeline over the `Jenkinsfile` used on the server: it
expects a mounted key and a remote host, and it fails on the EC2 server.

## Load test

Install the tools on the server:

```bash
sudo apt-get install -y apache2-utils stress
```

Open Grafana first (RentSpace dashboard, last 15 minutes, 5 s refresh), then:

```bash
# Request load
ab -n 20000 -c 100 http://localhost:8080/login

# CPU and memory stress for 120 seconds (do not interrupt)
stress --cpu 2 --vm 1 --vm-bytes 512M --timeout 120
```

Run `stress` in one terminal and `ab` in a second terminal about 15 seconds later
to see both loads together.

### Results

| Measurement | Result |
|---|---|
| ApacheBench, idle server (20,000 requests, 100 concurrent) | 1098.6 requests/s, 0 failed, mean 89 ms, 99th percentile 393 ms |
| ApacheBench, run after the stress test | mean 43 ms, 99th percentile 139 ms, slowest 311 ms |
| Host CPU during `stress` | about 100% for the 120 s run, back to near 0% afterwards |
| Host RAM during `stress` | about 49% to about 58%, then back down |
| Request rate in Grafana during `ab` | up to about 730 req/s (1-minute average) |
| App container CPU during `ab` | about 29% of one core |
| MySQL container CPU | below 1% |

The Grafana request rate is a 1-minute average, so it reads lower than the peak
that `ab` reports. `stress` runs on the host, outside the containers, so it shows in
the host panels (Node Exporter) but not in the container panels (cAdvisor).

## Troubleshooting notes

- **cAdvisor panel shows "No data":** Docker 29 rejects clients older than API 1.44.
  cAdvisor v0.49.1 fails with `client version 1.41 is too old`, and container names
  are missing from the metrics. Use cAdvisor `ghcr.io/google/cadvisor:0.55.1` or newer.
- **Jenkins stage "Ship Image to EC2" fails with `cannot stat '/keys/rentspace-key.pem'`:**
  the local-Docker Jenkinsfile was used on the server. Restore the EC2 `Jenkinsfile`.
- **Build fails with `docker: permission denied`:**
  `sudo usermod -aG docker jenkins`, then `sudo systemctl restart jenkins`.
- **Disk getting full on the server:** `docker image prune -a -f` removes images no
  running container uses. Do not use `--volumes`, which would delete the database.
- **Webhook stops working after an EC2 restart:** the public IP changed. Attach an
  Elastic IP, or update the webhook URL.

## Repository layout

```
ansible/          deployment playbook and inventory
docs/             setup notes
jenkins-local/    Docker-based Jenkins for a local machine
monitoring/       Prometheus, Grafana, Node Exporter and cAdvisor setup
scripts/          helper scripts
src/              Spring Boot application source
Dockerfile        application image
docker-compose.yml  application + database
Jenkinsfile       pipeline used by the EC2 Jenkins job
```
## CI/CD
Jenkins pipeline: GitHub push -> Jenkins build -> Docker image -> Ansible deploy -> AWS EC2 (monitored by Prometheus and Grafana).
