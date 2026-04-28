# Asset — k6 CI Integration

> Ready-to-paste CI workflows for common platforms. Copy the one that matches the target project's CI.

---

## Decision: which scenarios run in which job

| CI trigger | Scenarios | Duration | Gate |
|------------|-----------|----------|------|
| `pull_request` | load (short, 3-5m) | ~5m | Blocks merge |
| `push` to main | load (full) + stress | ~25m | Blocks deploy to staging |
| `schedule` (nightly) | soak (2h) + spike | ~2h 10m | Alerts on failure |
| `workflow_dispatch` | user picks | varies | Manual |

The PR gate must be FAST — full load at 10m is acceptable; stress and soak are too slow for PRs.

---

## GitHub Actions

### File: `.github/workflows/performance.yml`

```yaml
name: Performance

on:
  pull_request:
    paths:
      - 'src/**'
      - 'tests/performance/**'
      - 'pom.xml'
      - 'package.json'
  push:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'          # 2 AM UTC nightly
  workflow_dispatch:
    inputs:
      scenario:
        description: 'Scenario to run'
        required: true
        default: 'load'
        type: choice
        options: [load, stress, spike, soak, all]

jobs:
  performance:
    runs-on: ubuntu-latest
    timeout-minutes: 150          # enough for soak + buffer
    steps:
      - uses: actions/checkout@v4

      - name: Install k6
        run: |
          sudo gpg -k
          sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
            --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
          echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
            | sudo tee /etc/apt/sources.list.d/k6.list
          sudo apt-get update
          sudo apt-get install k6

      - name: Start backend (docker compose)
        run: |
          docker compose up -d --wait
          # Adjust to your healthcheck endpoint
          timeout 60 bash -c 'until curl -sf http://localhost:8080/health; do sleep 2; done'

      - name: Seed DB
        run: |
          # Adjust to your seed script
          if [ -f tests/performance/scripts/seed.sql ]; then
            docker compose exec -T postgres psql -U app -d appdb < tests/performance/scripts/seed.sql
          fi

      - name: Determine scenario
        id: scenario
        run: |
          case "${{ github.event_name }}" in
            pull_request)        echo "name=load"  >> $GITHUB_OUTPUT ;;
            push)                echo "name=load"  >> $GITHUB_OUTPUT ;;
            schedule)            echo "name=soak"  >> $GITHUB_OUTPUT ;;
            workflow_dispatch)   echo "name=${{ inputs.scenario }}" >> $GITHUB_OUTPUT ;;
            *)                   echo "name=load"  >> $GITHUB_OUTPUT ;;
          esac

      - name: Run k6
        env:
          BASE_URL: http://localhost:8080
          K6_USER: ${{ secrets.PERF_USER }}
          K6_PASS: ${{ secrets.PERF_PASS }}
        run: |
          SCENARIO=${{ steps.scenario.outputs.name }}
          # Short load for PR speed
          if [ "${{ github.event_name }}" = "pull_request" ] && [ "$SCENARIO" = "load" ]; then
            k6 run tests/performance/script.js \
              --env BASE_URL=$BASE_URL \
              --env SCENARIO=load \
              --env LOAD_DURATION=5m
          else
            k6 run tests/performance/script.js \
              --env BASE_URL=$BASE_URL \
              --env SCENARIO=$SCENARIO
          fi

      - name: Upload k6 summary
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: k6-summary-${{ steps.scenario.outputs.name }}-${{ github.run_number }}
          path: tests/performance/reports/
          retention-days: 30

      - name: Backend logs on failure
        if: failure()
        run: docker compose logs --tail=500

      - name: Tear down
        if: always()
        run: docker compose down -v
```

### Notes

- `timeout-minutes: 150` allows the soak scenario (~2h) to run without CI killing the job
- `if: always()` on artifact upload and teardown so failures still produce diagnostics
- Secrets `PERF_USER` / `PERF_PASS` → set in Repo Settings > Secrets
- Adjust `paths:` filter to match the project layout

---

## GitLab CI

### File: `.gitlab-ci.yml` additions

```yaml
stages:
  - build
  - test
  - performance
  - deploy

performance:load:
  stage: performance
  image: grafana/k6:latest
  services:
    - name: postgres:16-alpine
      alias: postgres
      variables:
        POSTGRES_DB: appdb
        POSTGRES_USER: app
        POSTGRES_PASSWORD: app
  variables:
    BASE_URL: http://backend:8080
  script:
    - k6 run tests/performance/script.js
        --env BASE_URL=$BASE_URL
        --env SCENARIO=load
        --env LOAD_DURATION=5m
  artifacts:
    when: always
    paths: [tests/performance/reports/]
    expire_in: 30 days
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
    - if: $CI_COMMIT_BRANCH == "main"

performance:stress:
  extends: performance:load
  script:
    - k6 run tests/performance/script.js --env BASE_URL=$BASE_URL --env SCENARIO=stress
  rules:
    - if: $CI_COMMIT_BRANCH == "main"                    # stress only on main
    - if: $CI_PIPELINE_SOURCE == "schedule"

performance:soak:
  extends: performance:load
  timeout: 3 hours
  script:
    - k6 run tests/performance/script.js --env BASE_URL=$BASE_URL --env SCENARIO=soak
  rules:
    - if: $CI_PIPELINE_SOURCE == "schedule"             # soak only on schedule
```

Set up the schedule in GitLab UI → CI/CD → Schedules. Nightly cron: `0 2 * * *`.

---

## Jenkins

### `Jenkinsfile` (declarative)

```groovy
pipeline {
  agent any
  triggers {
    cron('H 2 * * *')   // nightly for soak
  }
  parameters {
    choice(name: 'SCENARIO', choices: ['load', 'stress', 'spike', 'soak', 'all'], description: 'k6 scenario')
  }
  environment {
    BASE_URL = 'http://localhost:8080'
    K6_USER = credentials('perf-user')
    K6_PASS = credentials('perf-pass')
  }
  stages {
    stage('Install k6') {
      steps { sh 'which k6 || sudo apt-get update && sudo apt-get install -y k6' }
    }
    stage('Start backend') {
      steps {
        sh 'docker compose up -d --wait'
        sh 'timeout 60 bash -c "until curl -sf $BASE_URL/health; do sleep 2; done"'
      }
    }
    stage('Run k6') {
      steps {
        sh "k6 run tests/performance/script.js --env BASE_URL=$BASE_URL --env SCENARIO=${params.SCENARIO}"
      }
    }
  }
  post {
    always {
      archiveArtifacts artifacts: 'tests/performance/reports/**', allowEmptyArchive: true
      sh 'docker compose down -v'
    }
    failure {
      slackSend(channel: '#alerts', message: "Performance test failed: ${env.BUILD_URL}")
    }
  }
}
```

---

## CircleCI

### `.circleci/config.yml` additions

```yaml
version: 2.1

jobs:
  performance:
    docker:
      - image: cimg/base:2024.01
      - image: postgres:16-alpine
        environment:
          POSTGRES_DB: appdb
          POSTGRES_USER: app
          POSTGRES_PASSWORD: app
    parameters:
      scenario:
        type: enum
        enum: [load, stress, spike, soak]
        default: load
    steps:
      - checkout
      - run:
          name: Install k6
          command: |
            sudo apt-get update
            sudo apt-get install -y gnupg ca-certificates
            sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
              --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
            echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
              | sudo tee /etc/apt/sources.list.d/k6.list
            sudo apt-get update && sudo apt-get install -y k6
      - run:
          name: Start backend
          command: |
            docker compose up -d
            timeout 60 bash -c 'until curl -sf http://localhost:8080/health; do sleep 2; done'
      - run:
          name: Run k6 — << parameters.scenario >>
          command: |
            k6 run tests/performance/script.js \
              --env BASE_URL=http://localhost:8080 \
              --env SCENARIO=<< parameters.scenario >>
      - store_artifacts:
          path: tests/performance/reports/

workflows:
  pr:
    jobs:
      - performance:
          scenario: load
          filters:
            branches: { ignore: main }
  main:
    jobs:
      - performance:
          scenario: load
          filters: { branches: { only: main } }
      - performance:
          scenario: stress
          filters: { branches: { only: main } }
  nightly:
    triggers:
      - schedule:
          cron: '0 2 * * *'
          filters: { branches: { only: main } }
    jobs:
      - performance:
          scenario: soak
```

---

## Docker Compose helper

Most CI workflows assume `docker compose up -d --wait` works. If the target project lacks a `docker-compose.yml` at the root, the skill MUST flag it in the final report and suggest a template:

```yaml
# docker-compose.yml (for local dev + CI performance tests)
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: appdb
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    ports: ["5432:5432"]
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "app"]
      interval: 5s
      timeout: 5s
      retries: 10

  backend:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
    ports: ["8080:8080"]
    environment:
      DATABASE_URL: postgres://app:app@postgres:5432/appdb
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://localhost:8080/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 6
      start_period: 30s
```

---

## Slack / email alerts

For soak failures (nightly), alert the team:

```yaml
# GitHub Actions — add step AFTER the k6 run
- name: Notify Slack on failure
  if: failure() && github.event_name == 'schedule'
  uses: slackapi/slack-github-action@v1.27.0
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK }}
    payload: |
      {
        "text": ":warning: Nightly soak test FAILED on main",
        "attachments": [{
          "color": "danger",
          "fields": [
            { "title": "Run", "value": "${{ github.run_id }}", "short": true },
            { "title": "Commit", "value": "${{ github.sha }}", "short": true }
          ]
        }]
      }
```

---

## Exit code reference

k6's behavior is PREDICTABLE — know this:

| Exit code | Meaning | CI action |
|-----------|---------|-----------|
| 0 | All good | Pass |
| 99 | At least one threshold violated | Fail |
| 103 | Invalid CLI args / script | Fail |
| 107 | Script runtime error (uncaught exception) | Fail |
| 108 | Context cancelled (e.g., abortOnFail triggered) | Fail |

Always let CI treat non-zero as failure. Never add `|| true` or trap exit codes — that defeats the gate.
