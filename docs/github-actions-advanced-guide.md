# GitHub Actions — Advanced Guide

> **Prerequisites:** Read [github-actions-guide.md](github-actions-guide.md) first.  
> You should already know: triggers, `uses:`, `run:`, `checkout@v4`, `setup-java@v4`, and how to run a workflow manually.

This guide covers the next level — patterns you will reach for repeatedly once you move beyond a single basic workflow.

---

## 1. `concurrency` — Stop Wasting CI Minutes on Stale Runs

**The problem:** You push commit A. CI starts. Before it finishes, you fix a typo and push commit B.  
Now two CI runs are in progress — but commit A's result is irrelevant. You're burning CI minutes for nothing.

**The fix:**

```yaml
concurrency:
  group: ci-${{ github.ref }}   # one slot per branch
  cancel-in-progress: true       # cancel the old run when a new one starts
```

**What `${{ github.ref }}` actually looks like at runtime:**

| Event | `github.ref` value |
|---|---|
| Push to master | `refs/heads/master` |
| Push to a feature branch | `refs/heads/feature/my-feature` |
| Pull request | `refs/pull/42/merge` |

Using it as the group key means: *"only one run of this workflow per branch/PR at a time."*  
Different branches never cancel each other — each has its own slot.

**Where to put it:** at the top level of the workflow file (same level as `name:` and `on:`).

**What the cancelled run looks like in the GitHub UI:**  
The old run shows a **grey ◉ "Cancelled"** badge — not a red ❌ "Failed". This is important:  
it means your PR status checks won't be polluted with false failures from runs that were simply superseded.

> 💡 **Rule of thumb:**  
> - **Add** `concurrency` to workflows triggered by `push` or `pull_request` — you always want fresh results.  
> - **Skip it** for manual `workflow_dispatch` workflows — you usually want those to run fully and independently.

---

## 2. `permissions` — Principle of Least Privilege

Every workflow run gets an automatic token (`GITHUB_TOKEN`) that can interact with the GitHub API.  
By default this token has **broad write access** — it can push code, delete branches, create releases.

Restricting it means a malicious or compromised dependency in your `pom.xml` cannot abuse the token
even if it runs code during the Maven build:

```yaml
permissions:
  contents: read   # only allows reading the repo — nothing else
```

**Common permission combinations:**

| What you're doing | Permissions needed |
|---|---|
| Just running tests | `contents: read` |
| Posting a PR comment with test results | `contents: read` + `pull-requests: write` |
| Publishing a GitHub Release | `contents: write` |
| Publishing a package to GitHub Packages | `contents: read` + `packages: write` |

**Where to put it:** At the top level to apply to all jobs, or inside a `job:` block to scope it to one job only:

```yaml
# Top-level: applies to every job in this workflow
permissions:
  contents: read

jobs:
  release-job:
    # Override for just this job that needs to write
    permissions:
      contents: write
      packages: write
```

> 💡 If you're unsure what permissions a step needs, GitHub will tell you — the Actions log shows  
> a "Resource not accessible by integration" error if a permission is missing.

---

## 3. `env:` — Environment Variables at Three Levels

You can define environment variables at the **workflow**, **job**, or **step** level.  
Inner levels override outer levels — step wins over job, job wins over workflow.

```yaml
env:
  APP_ENV: ci               # available in EVERY job and step in this workflow

jobs:
  test:
    env:
      LOG_LEVEL: INFO       # available in every step of THIS job only

    steps:
      - name: Run tests
        env:
          REPORT_DIR: ./reports   # available ONLY inside this one step
        run: |
          # The pipe character "|" after "run:" means: treat everything indented
          # below as one multi-line shell script. Without it you can only write
          # a single command. With it, every indented line is a separate command
          # that runs in order — like a small shell script.
          echo "APP_ENV=$APP_ENV"       # from workflow level
          echo "LOG_LEVEL=$LOG_LEVEL"   # from job level
          echo "REPORT_DIR=$REPORT_DIR" # from step level
          mvn test --batch-mode
```

**Practical use in this project:**  
Pass a system property to Maven tests — equivalent to running `mvn test -DinParameter=staging` locally:

```yaml
- name: Run tests against staging config
  env:
    IN_PARAM: staging        # set the env var
  run: mvn test --batch-mode -DinParameter=$IN_PARAM   # pass it to Maven as a system property
```

> 💡 **Rule:** Use `env:` for non-sensitive configuration values (environment names, log levels, feature flags).  
> For passwords and tokens, always use **secrets** (next section) — never plain `env:`.

---

## 4. `secrets` — Safely Inject Sensitive Values

Never hardcode passwords, API keys, or tokens in your YAML. They would be visible to anyone who can read  
the repository. GitHub Secrets are encrypted at rest and **masked in all logs** (shown as `***`).

**Step 1 — Add a secret (one-time, done in the GitHub UI):**
```
Repository → Settings → Secrets and variables → Actions → "New repository secret"
Name: DEPLOY_TOKEN
Value: (paste your token here)
```

**Step 2 — Use it in your workflow:**

```yaml
- name: Deploy to staging
  env:
    # ${{ secrets.NAME }} injects the secret value as an env variable.
    # The value is never printed in logs — it appears as *** automatically.
    DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }}
    DB_PASSWORD:  ${{ secrets.DB_PASSWORD }}
  run: ./deploy.sh   # your script reads DEPLOY_TOKEN and DB_PASSWORD from env
```

**The automatic secret — `GITHUB_TOKEN`:**  
You never need to create this one. GitHub generates it automatically at the start of every run  
and revokes it when the run ends. Its scope is limited to this specific repository.

```yaml
# Example: use the GitHub CLI (pre-installed on GitHub runners) to comment on a PR
- name: Post test summary on PR
  # Only run this step when the event is a pull request
  if: github.event_name == 'pull_request'
  run: |
    gh pr comment ${{ github.event.pull_request.number }} \
      --body "✅ All tests passed!"
  env:
    GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}   # the automatic token, no setup needed
```

> ⚠️ **Important:** Secrets are **not** passed to workflows triggered from a fork of your repository.  
> This is a deliberate security feature — it prevents a malicious fork's PR from stealing your secrets.

---

## 5. `if:` — Conditional Steps and Jobs

Run a step (or entire job) only when a specific condition is true.  
Without `if:`, every step always runs. With `if:`, you control exactly when a step executes.

### Pattern A — Run a step only for a specific trigger

```yaml
steps:
  - name: Only notify on a direct push to master (not on PRs)
    if: github.event_name == 'push'
    run: echo "Code was merged to master"

  - name: Only run on pull requests
    if: github.event_name == 'pull_request'
    run: echo "This is a PR review build"
```

`github.event_name` contains the trigger name: `'push'`, `'pull_request'`, `'workflow_dispatch'`, `'schedule'`.

### Pattern B — Run a step regardless of whether previous steps failed

By default, if one step fails, all subsequent steps are skipped.  
Use `if: always()` to break that rule — useful for cleanup steps and report uploads:

```yaml
steps:
  - name: Run tests
    run: mvn test --batch-mode --no-transfer-progress
    # If this step fails, the job is marked "failed"
    # BUT steps with "if: always()" below will STILL run

  - name: Upload test report (run even on failure — that's when you need it most)
    uses: actions/upload-artifact@v4
    if: always()   # ← this step runs regardless of what happened above
    with:
      name: surefire-reports
      path: target/surefire-reports/
```

### The four status check functions

| `if:` expression | When this step runs |
|---|---|
| *(nothing — default)* | Only when ALL previous steps succeeded |
| `if: success()` | Same as default — explicit version |
| `if: failure()` | Only when at least one previous step failed |
| `if: always()` | Always — whether previous steps passed, failed, or the job was cancelled |
| `if: cancelled()` | Only when the job was manually cancelled |

### In this project — the if/else pattern in `workflow_dispatch_with_input.yml`

```yaml
# Only one of these two steps will run per execution:

- name: Run tests for a specific tag
  if: ${{ github.event.inputs.tags != '' }}     # condition: user typed something
  run: mvn test --batch-mode -Dgroups="${{ github.event.inputs.tags }}"

- name: Run ALL tests (no tag filter)
  if: ${{ github.event.inputs.tags == '' }}     # condition: user left the field blank
  run: mvn test --batch-mode
```

This is the **if/else pattern for workflow steps** — two steps with opposite conditions.

---

## 6. `needs:` — Sequential Jobs (Dependencies Between Jobs)

By default, jobs in the same workflow run **in parallel** — GitHub starts all of them at once.  
Use `needs:` to express "this job must wait for that one to succeed first."

```yaml
jobs:

  unit-tests:           # ← starts immediately, no dependencies
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        # The { } "flow style" is just compact YAML — identical to writing:
        #   with:
        #     java-version: '21'
        #     distribution: 'temurin'
        #     cache: maven
        # It's used here to keep the example shorter. Both styles are valid.
        with: { java-version: '21', distribution: 'temurin', cache: maven }
      - run: mvn test --batch-mode -Dgroups="ecomm-unit-tests"

  integration-tests:    # ← waits for unit-tests to finish and pass
    runs-on: ubuntu-latest
    needs: unit-tests             # single dependency — one job name as a string
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: maven }
      - run: mvn test --batch-mode -Dgroups="integration"

  deploy:               # ← waits for BOTH to finish and pass
    runs-on: ubuntu-latest
    needs: [unit-tests, integration-tests]   # multiple dependencies — a list
    steps:
      - run: echo "Both test stages passed — safe to deploy"
```

**Visual pipeline:**
```
unit-tests ────────┐
                   ├──► deploy
integration-tests ─┘
```

> 💡 If `unit-tests` fails, `integration-tests` is skipped and `deploy` never runs.  
> This is the core of a safe CI/CD pipeline: **never deploy when tests are red.**

**Important detail — each job gets a fresh machine:**  
Each job runs on its own brand-new virtual machine. Files created in `unit-tests` are NOT available  
in `integration-tests`. If you need to share files between jobs, use **artifacts** (Section 8).

---

## 7. `matrix` — Run Tests on Multiple Java Versions (in Parallel)

A matrix multiplies a job across a set of values — each combination runs as a **separate parallel job**.  
Most common use in Java projects: verify your code compiles and tests pass on multiple Java versions.

```yaml
jobs:
  test:
    runs-on: ubuntu-latest

    strategy:
      # Run this job TWICE: once with java-version=17, once with java-version=21
      matrix:
        java-version: [17, 21]

      # IMPORTANT: fail-fast defaults to true, which means if java-17 fails,
      # GitHub cancels the java-21 run mid-flight — you never see its results.
      # Set it to false so ALL matrix jobs always run to completion.
      fail-fast: false

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK ${{ matrix.java-version }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java-version }}   # ← the matrix value for this run
          distribution: 'temurin'
          cache: maven

      - name: Run tests on Java ${{ matrix.java-version }}
        run: mvn test --batch-mode --no-transfer-progress
```

**What you'll see in the GitHub UI:**  
Two parallel jobs appear: `test (17)` and `test (21)`. Both must pass for the overall workflow to succeed.

**Multi-dimensional matrix** — you can combine multiple variables:

```yaml
strategy:
  fail-fast: false
  matrix:
    java-version: [17, 21]
    os: [ubuntu-latest, windows-latest]
    # This creates 4 jobs: ubuntu+17, ubuntu+21, windows+17, windows+21

    exclude:
      # But skip Java 17 on Windows — we only care about LTS on Windows
      - java-version: 17
        os: windows-latest
    # Result: 3 jobs — ubuntu+17, ubuntu+21, windows+21
```

> 💡 **`fail-fast: false` is almost always what you want.**  
> With the default `true`, one flaky test on Java 17 hides whether Java 21 is green or red — you have to re-run everything to find out.

---

## 8. Artifacts — Save Test Reports for Download

Every runner is a **temporary virtual machine** — when the job ends, the machine is discarded and everything on it is gone.  
**Artifacts** let you save files from the runner before it disappears, so you can download them from the GitHub UI.

The most practical artifact in a Java/Maven project: the **Surefire test reports** that Maven writes to `target/surefire-reports/`.

```yaml
steps:

  - name: Run tests
    run: mvn test --batch-mode --no-transfer-progress
    # Do NOT add continue-on-error: true here.
    # If tests fail, the job should fail — that's the point of CI.
    # The upload step below handles running after failure via "if: always()"

  - name: Upload Surefire reports
    uses: actions/upload-artifact@v4
    if: always()     # ← runs whether tests passed OR failed.
                     # This is the RIGHT way to run a step after failure —
                     # NOT continue-on-error (which would hide the failure from CI).
    with:
      name: surefire-reports             # the name of the zip file in the GitHub UI
      path: target/surefire-reports/     # folder to upload (Maven writes XML reports here)
      retention-days: 7                  # auto-delete after 7 days to save storage
```

> ⚠️ **Common mistake — `continue-on-error: true`:**  
> Adding `continue-on-error: true` to the test step makes the whole job show ✅ green even when tests fail.  
> Your PR would look like it passed — silently hiding broken tests.  
> Always use `if: always()` on the upload/cleanup steps instead.

**How to download the report:**
```
GitHub.com → Your Repository → Actions tab
→ click the workflow run
→ scroll to the bottom of the run page
→ "Artifacts" section → click "surefire-reports" → download zip
→ open the XML files in any browser or test reporting tool
```

**Sharing files between jobs using artifacts:**  
Since each job runs on a fresh machine, use `upload-artifact` + `download-artifact` to pass files:

```yaml
jobs:
  build:
    steps:
      - run: mvn package --batch-mode
      - uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: target/myapp.jar

  deploy:
    needs: build
    steps:
      - uses: actions/download-artifact@v4   # restores the jar onto this new machine
        with:
          name: app-jar
      - run: ./deploy.sh myapp.jar
```

---

## 9. Scheduled Triggers (`schedule:`)

Run a workflow automatically on a time-based schedule — no human trigger needed.  
Uses standard **cron syntax**: five space-separated fields.

```
┌─────────── minute       (0–59)
│  ┌──────── hour         (0–23, UTC)
│  │  ┌───── day of month (1–31)
│  │  │  ┌── month        (1–12)
│  │  │  │  ┌─ day of week (0=Sunday, 1=Monday … 6=Saturday)
│  │  │  │  │
0  1  *  *  1-5    →  1:00 AM UTC, Monday to Friday
```

**Common cron expressions:**

| Expression | When it runs |
|---|---|
| `0 1 * * 1-5` | 1:00 AM every weekday (Mon–Fri) |
| `0 0 * * 0` | Midnight every Sunday |
| `0 8 * * *` | 8:00 AM every day |
| `0 8,17 * * *` | 8:00 AM AND 5:00 PM every day |
| `*/30 * * * *` | Every 30 minutes |

> 💡 All times are **UTC**. If your team is in BST (UTC+1), `0 1 * * *` runs at 2:00 AM local time.  
> Use a tool like [crontab.guru](https://crontab.guru) to verify your expression before committing it.

**Real-world pattern — fast tests on push, full suite nightly:**

```yaml
on:
  push:
    branches: [ "master" ]     # ← fast unit tests on every push
  schedule:
    - cron: '0 1 * * 1-5'      # ← full regression suite every weekday night

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: maven }

      # Run only fast unit tests when triggered by a push
      - name: Unit tests (push trigger)
        if: github.event_name == 'push'
        run: mvn test --batch-mode -Dgroups="ecomm-unit-tests"

      # Run the full suite when triggered by the nightly schedule
      - name: Full regression suite (nightly schedule)
        if: github.event_name == 'schedule'
        run: mvn test --batch-mode
```

> ⚠️ GitHub does not guarantee exact timing — scheduled workflows may be delayed by a few minutes  
> during high-traffic periods. They also don't run at all if the repository has had no activity  
> for 60 days (GitHub pauses them to save resources).

---

## 10. Reusable Workflows (`workflow_call`)

Look at the four workflows in this project — they all repeat the same three steps:  
checkout → setup Java → run Maven. If you upgrade to Java 25, you have to edit four files.

**Reusable workflows** solve this: extract the shared steps into one file and call it from others.  
Change the Java version in one place → all workflows are updated automatically.

**Step 1 — Create the reusable workflow:**

```yaml
# .github/workflows/reusable-maven-test.yml

name: Reusable — Maven Test Runner

on:
  workflow_call:       # ← This trigger is what makes a workflow reusable.
                       #   It cannot be triggered manually or by push — only by "uses:" in another workflow.
    inputs:
      groups:
        description: 'JUnit 5 tag filter (leave empty to run all tests)'
        type: string
        required: false
        default: ''    # empty string = no filter = run everything

permissions:
  contents: read

jobs:
  run-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'      # ← change Java version here once for all callers
          distribution: 'temurin'
          cache: maven

      - name: Run tests
        # Shell if/else: run with tag filter if provided, without it if not.
        # "run: |" starts a multi-line shell script (the | means multi-line).
        # "if [ -n "..." ]" is bash syntax meaning "if the string is NOT empty".
        #   -n = "non-zero length string" (opposite: -z = "zero length / empty")
        # fi closes the if block (bash if blocks always end with fi).
        run: |
          if [ -n "${{ inputs.groups }}" ]; then
            echo "Running tests with tag filter: ${{ inputs.groups }}"
            mvn test --batch-mode --no-transfer-progress -Dgroups="${{ inputs.groups }}"
          else
            echo "No tag filter — running all tests"
            mvn test --batch-mode --no-transfer-progress
          fi
```

**Step 2 — Call it from any workflow using `uses:` at the job level:**

```yaml
# .github/workflows/ecomm_unit_tests.yml

name: Ecommerce — Unit Tests
on:
  workflow_dispatch:

jobs:
  unit-tests:
    # "uses:" at the job level (not step level) calls a reusable workflow
    uses: ./.github/workflows/reusable-maven-test.yml
    with:
      groups: "ecomm-unit-tests"   # passes the tag to the reusable workflow's input
```

```yaml
# .github/workflows/ecomm_sanity_tests.yml  — same pattern, different tag

jobs:
  sanity-tests:
    uses: ./.github/workflows/reusable-maven-test.yml
    with:
      groups: "ecomm-sanity"
```

> 💡 **`uses:` at job level vs step level:**  
> - At **step level** → calls a marketplace action (e.g., `uses: actions/checkout@v4`)  
> - At **job level** → calls a reusable workflow (e.g., `uses: ./.github/workflows/reusable-maven-test.yml`)  
> These look similar but do different things depending on indentation.

---

## 11. Step Outputs — Passing Data Between Steps

Steps in the same job can pass data to each other using **outputs**.  
A step writes a value to a special file; subsequent steps read it via `${{ steps.<id>.outputs.<name> }}`.

```yaml
steps:

  - name: Get the current date
    id: date          # ← give this step an ID so other steps can reference it
    run: echo "today=$(date +'%Y-%m-%d')" >> $GITHUB_OUTPUT
    # Breaking this command down:
    #   date +'%Y-%m-%d'        → shell command that prints today's date, e.g. "2026-07-20"
    #   today=$(...)            → captures the output into a variable named "today"
    #   echo "today=2026-07-20" → prints the key=value pair
    #   >> $GITHUB_OUTPUT       → ">>" means APPEND to the file (not overwrite).
    #                             $GITHUB_OUTPUT is a special file path that GitHub
    #                             provides — writing "key=value" lines to it registers
    #                             them as outputs of this step, readable by later steps.

  - name: Use the date in an artifact name
    uses: actions/upload-artifact@v4
    with:
      name: test-report-${{ steps.date.outputs.today }}   # e.g. test-report-2026-07-19
      path: target/surefire-reports/
```

**Practical use:** Tag your artifact names with the date or commit SHA so old reports don't overwrite new ones.

```yaml
  - name: Get short commit SHA
    id: sha
    run: echo "short=$(git rev-parse --short HEAD)" >> $GITHUB_OUTPUT

  - name: Upload report with commit reference
    uses: actions/upload-artifact@v4
    with:
      name: surefire-${{ steps.sha.outputs.short }}   # e.g. surefire-a3f9d12
      path: target/surefire-reports/
```

---

## Putting It All Together — A Production-Ready CI Workflow

This is what a mature CI workflow looks like combining everything from this guide:

```yaml
name: CI — Production Ready

on:
  push:
    branches: [ "master" ]
  pull_request:
    branches: [ "master" ]
  schedule:
    - cron: '0 1 * * 1-5'     # nightly full run, Mon–Fri at 1 AM UTC

# Cancel stale runs when a new push arrives on the same branch
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

# Minimum permissions — never give more than needed
permissions:
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest

    strategy:
      fail-fast: false           # let all matrix jobs finish even if one fails
      matrix:
        java-version: [17, 21]   # verify compatibility on two Java LTS versions

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK ${{ matrix.java-version }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java-version }}
          distribution: 'temurin'
          cache: maven

      # On push: run only fast unit tests
      - name: Unit tests (push)
        if: github.event_name == 'push'
        run: mvn test --batch-mode --no-transfer-progress -Dgroups="ecomm-unit-tests"

      # On PR: run unit + sanity tests together
      - name: Unit + sanity tests (pull request)
        if: github.event_name == 'pull_request'
        run: mvn test --batch-mode --no-transfer-progress -Dgroups="ecomm-unit-tests | ecomm-sanity"

      # Nightly: run everything
      - name: Full regression suite (nightly)
        if: github.event_name == 'schedule'
        run: mvn test --batch-mode --no-transfer-progress

      # Upload reports regardless of outcome — if: always() is the right tool here,
      # NOT continue-on-error (which would hide test failures from CI)
      - name: Upload Surefire reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: surefire-reports-java-${{ matrix.java-version }}
          path: target/surefire-reports/
          retention-days: 7
```

---

## Quick Reference — Advanced Concepts

```
Concept                   When to reach for it
──────────────────────────────────────────────────────────────────────────────
concurrency               Push/PR workflows — cancel stale runs automatically
permissions               Every workflow — always restrict to minimum needed
env:                      Non-sensitive config: environment name, log level, flags
secrets                   Passwords, tokens, API keys — never use plain env: for these
if: failure()             Alert steps, diagnostics — run only when something went wrong
if: always()              Upload reports, cleanup — run regardless of outcome
if: cancelled()           Emergency cleanup when a run is manually stopped
needs: [a, b]             Sequential pipeline: unit tests → integration → deploy
matrix + fail-fast:false  Test multiple Java versions; always see all results
upload-artifact           Persist test reports across the throw-away runner VM
download-artifact         Retrieve files in a later job (e.g., deploy a built jar)
schedule: cron            Nightly full regression without any manual trigger
workflow_call             DRY: one reusable workflow called by many others
step outputs (GITHUB_OUTPUT) Pass computed values (dates, SHAs) to later steps
```

---

## What's Next

| Topic | Where to learn |
|---|---|
| Branch protection rules — require CI to pass before merging | Repository → Settings → Branches → Add rule |
| Code coverage with JaCoCo | Add `jacoco-maven-plugin` to `pom.xml`, upload the report as an artifact |
| Dependency vulnerability scanning | [`dependency-review-action`](https://github.com/actions/dependency-review-action) |
| Publishing a GitHub Release automatically | [GitHub Docs — Creating releases](https://docs.github.com/en/repositories/releasing-projects-on-github) |
| Deploying to AWS / Azure / GCP | GitHub Marketplace — search your cloud provider's official action |

> 📖 Back to basics: [github-actions-guide.md](github-actions-guide.md)  
> 📖 Full GitHub Actions reference: https://docs.github.com/en/actions
