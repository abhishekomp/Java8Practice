# Java 8+ Practice Project

A hands-on learning project exploring **Java 8+ features** through practical, real-world examples.  
Topics covered: `Optional`, `Function` (Functional Interface), JUnit 5 tagging, and CI/CD basics.

---

## 📚 What Is This Project?

> See **[docs/WHAT_IS_IT.md](docs/WHAT_IS_IT.md)** for a full overview of the project goals and structure.

---

## 📂 Project Structure

```
src/
├── main/java/
│   ├── functionalInterface/function/myimpl/   ← Function<T,R> interface demo
│   ├── optionaldemoone/                        ← Optional with real-world domain objects
│   └── jenkins/                               ← CI/CD placeholder
└── test/java/
    ├── junitdemo/                              ← JUnit 5 Tags & Display Names
    ├── optionaldemoone/                        ← Tests for DiscountService
    └── optionaldemoWithPerson/                 ← Optional stream scenarios
```

---

## 🗂️ Learning Guides

| Guide | Description |
|---|---|
| [docs/WHAT_IS_IT.md](docs/WHAT_IS_IT.md) | Project overview and learning goals |
| [docs/optional-guide.md](docs/optional-guide.md) | Deep dive into Java `Optional` |
| [docs/advanced-optional-scenarios-explained.md](docs/advanced-optional-scenarios-explained.md) | Advanced Optional patterns explained in depth |
| [docs/functional-interface-function-guide.md](docs/functional-interface-function-guide.md) | `Function<T,R>`, `andThen`, `compose` |
| [docs/junit5-tagging-guide.md](docs/junit5-tagging-guide.md) | JUnit 5 `@Tag`, `@DisplayName`, selective test runs |
| [docs/java21-modern-java-guide.md](docs/java21-modern-java-guide.md) | Java 21 features used in this project |
| [docs/github-actions-guide.md](docs/github-actions-guide.md) | GitHub Actions explained for beginners |
| [docs/github-actions-advanced-guide.md](docs/github-actions-advanced-guide.md) | Advanced GitHub Actions — concurrency, secrets, matrix, artifacts, reusable workflows |

---

## 🚀 Running the Project

### Prerequisites
- Java 21+
- Maven 3.6+

### Run All Tests (locally)
```bash
mvn test
```

### Run Tests by Tag (locally)
```bash
# Run integration tests only
mvn test -Dgroups="integration"

# Run unit tests only
mvn test -Dgroups="ecomm-unit-tests"

# Run sanity tests only
mvn test -Dgroups="ecomm-sanity"

# Run Optional demo tests
mvn test -Dgroups="Rel1"
```

### Pass Custom System Properties
```bash
mvn test -DinParameter=MyValue
```

### 🤖 Run via GitHub Actions (CI/CD)

| Workflow | Trigger | What it runs |
|---|---|---|
| **CI — Build & Test All** | Auto on every push & PR | All tests |
| **Ecommerce — Unit Tests** | Manual (Actions tab) | `@Tag("ecomm-unit-tests")` |
| **Ecommerce — Sanity Tests** | Manual (Actions tab) | `@Tag("ecomm-sanity")` |
| **Run Tests by Tag** | Manual with input | Any tag you specify |

> 📖 New to GitHub Actions? See **[docs/github-actions-guide.md](docs/github-actions-guide.md)** for a full walkthrough.

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | **21 (LTS)** | Language — enables records, switch expressions, `var`, `.toList()`, text blocks, and more |
| Maven | 3.6+ | Build tool |
| JUnit Jupiter | 5.10.2 | Unit testing framework |
| AssertJ | 3.19.0 | Fluent assertions |
