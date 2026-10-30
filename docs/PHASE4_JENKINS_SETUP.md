# Phase 4 – CI/CD Pipeline (Jenkins + Ansible, triggered on GitHub push)

Everything runs on the existing EC2 (65.0.6.71). Jenkins uses **port 8081** because RentSpace owns 8080.

## 1. Security Group (AWS Console → EC2 → Security Groups → Inbound)
Add: TCP **8081** from your IP (Jenkins UI) and TCP **8081** from `0.0.0.0/0` *or* GitHub webhook IPs (needed for the webhook).

## 2. Install Jenkins + Ansible (SSH into EC2)
```bash
cd ~/RentSpace && git fetch origin && git checkout feature/docker-ansible && git pull
bash scripts/setup-jenkins-ec2.sh      # prints the Jenkins URL + unlock password
```

## 3. Jenkins first-time setup  (http://65.0.6.71:8081)
1. Paste the unlock password → **Install suggested plugins** → create admin user.
2. Manage Jenkins → Plugins → confirm **Git**, **GitHub**, **Pipeline** are installed.
3. Manage Jenkins → System → **Jenkins URL** = `http://65.0.6.71:8081/`

## 4. Create the pipeline job
1. New Item → name `RentSpace-Pipeline` → **Pipeline** → OK.
2. Build Triggers → tick **GitHub hook trigger for GITScm polling**.
3. Pipeline → Definition: **Pipeline script from SCM** → SCM: **Git**
   - Repo URL: `https://github.com/ishacodes23/RentSpace.git`
   - Branch: `*/feature/docker-ansible`  (use `*/main` after merging the PR)
   - Script Path: `Jenkinsfile`
4. Save → **Build Now** (first run, confirm all 5 stages are green).

## 5. GitHub webhook (GitHub repo → Settings → Webhooks → Add)
- Payload URL: `http://65.0.6.71:8081/github-webhook/`
- Content type: `application/json`, event: **Just the push event** → Add.
- A green tick should appear under *Recent Deliveries*.
(Needs repo admin rights. If you can't, ask `ishacodes23`; the `pollSCM` fallback in the Jenkinsfile still triggers a build within ~2 minutes.)

## 6. Prove the trigger works (screenshots for evidence)
```bash
# edit something visible, e.g. a line in README.md, then:
git commit -am "test: trigger Jenkins" && git push origin feature/docker-ansible
```
Capture: Jenkins stage view (all green) • Console Output ending `Finished: SUCCESS` • Ansible `PLAY RECAP failed=0` •
GitHub webhook delivery (200) • app at :8080/login • Prometheus :9090/targets all UP.

## Troubleshooting
| Symptom | Fix |
|---|---|
| `permission denied ... docker.sock` | `sudo usermod -aG docker jenkins && sudo systemctl restart jenkins` |
| `sudo: a password is required` | check `/etc/sudoers.d/jenkins` exists (script creates it) |
| `couldn't resolve module community.docker...` | `sudo ansible-galaxy collection install community.docker -p /usr/share/ansible/collections` |
| Build hangs / OOM | check `free -h`; swap is added by the script; consider t3.small+ |
| Webhook delivery fails | open port 8081 in the Security Group; Jenkins URL must be the public IP |
| "container name already in use" | `docker rm -f rentspace-app rentspace-mysql` once, then rebuild |
