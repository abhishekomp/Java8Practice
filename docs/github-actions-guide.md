# GitHub Actions — Practical Guide for This Project

> **One-line summary:**  
> GitHub Actions automatically runs your tests every time you push code — and lets you run specific test groups on demand from a button in the UI.  
> You write a YAML file that says *when* to run and *what* to run. GitHub does the rest.

---

## The Only Mental Model You Need

```
You push code to GitHub
        ↓
GitHub reads .github/workflows/*.yml
        ↓
Spins up a fresh Linux machine in the cloud
        ↓
Runs your steps: checkout → install Java → mvn test
        ↓
Shows ✅ or ❌ in the GitHub UI
```

That's it. A workflow is just a **script that runs in the cloud when something happens**.

---

## The 3 Things You'll Use 90% of the Time

### 1. The trigger — *when* does it run?

```yaml
on:
  push:
    branches: [ "master" ]       # runs automatically on every push
  pull_request:
    branches: [ "master" ]       # runs automatically on every PR
  workflow_dispatch:             # runs when YOU click a button in the GitHub UI
```

You will use one of these three triggers in virtually every workflow you ever write.  
`push` + `pull_request` = automatic CI. `workflow_dispatch` = manual, on-demand.

---

### 2. The two actions you'll use in every Java workflow

Every single Java workflow in existence starts with these same two steps:

```yaml
steps:
  # Step 1 — always first: get your code onto the machine
  - name: Checkout repository
    uses: actions/checkout@v4

  # Step 2 — always second: install Java
  - name: Set up JDK 21
    uses: actions/setup-java@v4
    with:
      java-version: '21'
      distribution: 'temurin'   # free, open-source build of Java — the standard choice
      cache: maven               # saves ~30s per run by caching downloaded dependencies
```

`uses:` means "run a pre-built script from the GitHub Marketplace".  
`with:` passes parameters to it (like arguments to a method).

---

### 3. Running your Maven command

```yaml
  - name: Run tests
    run: mvn test --no-transfer-progress --batch-mode
```

`run:` executes a shell command on the Linux machine.  
`--no-transfer-progress` stops Maven printing noisy download bars in the logs.  
`--batch-mode` stops Maven waiting for user input (it would hang forever in CI without this).

---

## The 4 Workflows in This Project

This is the most important table to understand:

| Workflow file | Name in GitHub UI | When it runs | What it runs |
|---|---|---|---|
| `maven.yml` | CI — Build & Test All | **Automatically** on every push & PR | ALL tests |
| `ecomm_unit_tests.yml` | Ecommerce — Unit Tests | **Manually** (you click a button) | `@Tag("ecomm-unit-tests")` only |
| `ecomm_sanity_tests.yml` | Ecommerce — Sanity Tests | **Manually** (you click a button) | `@Tag("ecomm-sanity")` only |
| `workflow_dispatch_with_input.yml` | Run Tests by Tag | **Manually** with a text input | Any tag you type |

**Rule of thumb:**
- Push code → `maven.yml` runs automatically, you don't need to do anything.
- Want to run a specific group of tests → use one of the manual workflows.

---

## How to Run a Manual Workflow (Step by Step)

This is the UI flow you'll use regularly:

```
1. Go to your repository on GitHub.com

2. Click the "Actions" tab at the top.
        ┌─────────────────────────────────┐
        │ Code  Issues  Pull requests  Actions  │
        └─────────────────────────────────┘

3. In the LEFT sidebar, click the workflow name.
   e.g. "Run Tests by Tag"

4. You'll see a "Run workflow" button (top right of the run list).
   Click it.

5. A small form drops down.
   - For "Run Tests by Tag": type a tag like  Rel1  or  Rel1 | Rel2
   - For others: just click the green "Run workflow" button.

6. Refresh the page — your new run appears at the top with a yellow dot (running)
   then turns ✅ green (passed) or ❌ red (failed).
```

**Tag expression examples** for the "Run Tests by Tag" workflow:

| What you type | What runs |
|---|---|
| `Rel1` | Only `@Tag("Rel1")` tests |
| `Rel1 \| Rel2` | Tests tagged Rel1 **OR** Rel2 |
| `ecomm-unit-tests` | All unit tests |
| `ecomm & integration` | Tests with **BOTH** tags |
| *(leave blank)* | **All** tests |

---

## How to Read Results When Something Fails

```
Actions tab
  → click the red ❌ run
    → click the failed job name
      → click the failed step (it's expanded by default)
        → read the Maven output — it shows exactly which test failed
          and the AssertJ error message:

  expected: "Jack gets Discount% 10"
   but was: "Jack gets Discount% 0"
```

That's all you need to do. The log is just Maven output — the same thing you see locally.

---

## Reading a Workflow File — The Essentials

Here is the skeleton every workflow follows:

```yaml
name: Human-readable name shown in the GitHub UI

on:               ← WHEN does this run?
  push: ...
  workflow_dispatch: ...

permissions:      ← security: only give read access unless you need more
  contents: read

jobs:
  my-job:         ← job ID (shown in the UI and logs — make it descriptive)
    runs-on: ubuntu-latest   ← use a fresh Linux VM (free, fast)

    steps:
      - name: ...            ← description shown in the log
        uses: ...            ← use a pre-built action
      - name: ...
        run: ...             ← run a shell command
```

**YAML indent rule:** every level of nesting = 2 more spaces.  
`jobs:` → `my-job:` → `steps:` → `- name:` — each one is 2 spaces deeper than its parent.

---

## The One Feature Unique to This Project: `${{ inputs.tags }}`

The "Run Tests by Tag" workflow passes the tag you typed into Maven:

```yaml
run: mvn test --batch-mode -Dgroups="${{ github.event.inputs.tags }}"
```

`${{ }}` is how you inject dynamic values into YAML.  
`github.event.inputs.tags` = the value the user typed in the form.  
This is the only expression you need to understand to use this project.

---

## Quick Reference

```
Want to...                                 Do this
─────────────────────────────────────────────────────────────────────
Run all tests automatically                Push code — maven.yml runs itself
Run unit tests on demand                   Actions → "Ecommerce — Unit Tests" → Run workflow
Run sanity tests on demand                 Actions → "Ecommerce — Sanity Tests" → Run workflow
Run a specific release tag (e.g. Rel1)    Actions → "Run Tests by Tag" → type "Rel1" → Run
Run multiple tags (Rel1 OR Rel2)           Actions → "Run Tests by Tag" → type "Rel1 | Rel2"
See why a test failed                      Actions → click ❌ run → click failed step → read log
```

---

## Deeper Reading (When You're Ready)

Once you're comfortable with the above, these are worth learning next:

| Topic | Why it matters |
|---|---|
| `concurrency: cancel-in-progress: true` | Cancels stale runs when you push twice quickly — saves CI minutes |
| `permissions: contents: read` | Security: limits what the workflow token can do |
| `needs: [other-job]` | Makes jobs run in sequence instead of in parallel |
| `env:` block | Passes environment variables to steps |
| `secrets.MY_SECRET` | Safely inject passwords/tokens without hardcoding them |
| Scheduled triggers (`schedule: cron`) | Run tests nightly without any manual trigger |

> 📖 Ready to go deeper? See **[github-actions-advanced-guide.md](github-actions-advanced-guide.md)** — covers `concurrency`, `secrets`, `matrix`, `needs`, artifacts, scheduled runs, and reusable workflows with worked examples.

> 📖 Full GitHub Actions documentation: https://docs.github.com/en/actions
