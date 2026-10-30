# Test Phase 4 on your laptop BEFORE pushing to GitHub
Needs: Docker Desktop (running) + Git. Windows: use a WSL2 Ubuntu terminal with Docker Desktop's WSL integration ON.

1. Put these files in your local RentSpace repo on branch `feature/docker-ansible` and COMMIT (do not push):
   git add . && git commit -m "feat: Phase 4 CI/CD"
2. Start Jenkins:
   cd jenkins-local && docker compose up -d --build        # first build takes ~5-10 min
3. Open http://localhost:8081  (admin / admin) -> job "RentSpace-Pipeline" already exists -> Build Now.
4. Check: http://localhost:8080/login , http://localhost:9090/targets , http://localhost:3000
5. Prove the trigger: change a file, `git commit -am "test"` -> Jenkins starts a build by itself within ~2 min (it polls your local repo).
6. Happy? `git push origin feature/docker-ansible`, then follow docs/PHASE4_JENKINS_SETUP.md for the EC2 version.

Notes: cAdvisor may show DOWN on Docker Desktop (needs Linux host paths) - fine locally, works on EC2.
Other branch? REPO_BRANCH=mybranch docker compose up -d --build
Stop: docker compose down        Logs: docker logs -f jenkins-local
