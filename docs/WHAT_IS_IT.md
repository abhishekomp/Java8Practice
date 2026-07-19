# What Is This Project?

## Overview

**Java8Practice** is a structured, example-driven learning project for developers who want to deeply understand **modern Java features (Java 8 → 21)** by reading and running real code — not just reading theory.

The project now targets **Java 21 (LTS)** and uses its features throughout: `var`, switch expressions, `Stream.toList()`, `String.formatted()`, `Optional.isEmpty()`, and more.

Each package in the project represents one concept, with production-style code and tests that demonstrate the concept in action.

---

## 🎯 Who Is This For?

- Java developers transitioning from Java 7 to Java 8+
- Developers who have heard about `Optional`, lambdas, and functional interfaces but want to see them used *properly*
- Anyone who wants to understand **why** these features exist and **when** to use them in real projects

---

## 🧠 What Will You Learn?

### 1. `Optional<T>` — Eliminating NullPointerException
**Location:** `src/main/java/optionaldemoone/` and `src/test/java/optionaldemoWithPerson/`

`Optional` is a container object that may or may not hold a non-null value. It forces the caller to explicitly handle the "no value" case, making your code safer and more self-documenting.

**Real-world scenario covered:**
- A `Customer` may or may not have a `MemberCard`
- A `MemberCard` may or may not qualify for a discount
- The `DiscountService` chains these optionals without a single `null` check

**Key methods practiced:**
| Method | What it does |
|---|---|
| `Optional.of(value)` | Creates an Optional with a non-null value |
| `Optional.ofNullable(value)` | Creates an Optional that handles null |
| `Optional.empty()` | Creates an empty Optional |
| `.map(fn)` | Transforms the value if present |
| `.flatMap(fn)` | Like map, but for functions that return Optional |
| `.orElse(default)` | Returns value or a default |
| `.orElseGet(supplier)` | Like orElse, but lazy (computed only when needed) |
| `.orElseThrow(supplier)` | Returns value or throws an exception |
| `.ifPresent(action)` | Runs an action if value is present |
| `.ifPresentOrElse(action, emptyAction)` | Handles both cases explicitly |
| `.filter(predicate)` | Keeps the value only if it matches a condition |
| `.or(supplier)` | Returns this Optional or another one if empty (Java 9+) |
| `Optional::stream` | Converts Optional to a Stream (Java 9+) |

---

### 2. `Function<T, R>` — Functional Interface Basics
**Location:** `src/main/java/functionalInterface/function/myimpl/`

`Function<T, R>` is one of the core functional interfaces in Java 8. It takes one input and produces one output.

**Key concepts practiced:**
- Implementing `Function<T, R>` as a class (`PerformSquare`)
- Using lambdas as `Function`
- `andThen(Function)` — compose functions left-to-right
- `compose(Function)` — compose functions right-to-left

---

### 3. JUnit 5 Tagging & Test Organisation
**Location:** `src/test/java/junitdemo/`

In real projects, you run *different subsets of tests* in different phases of your CI/CD pipeline (unit tests, integration tests, sanity checks). JUnit 5 `@Tag` enables this.

**Key concepts practiced:**
- `@Tag("integration")` — mark a class or method as an integration test
- `@Tag("ecomm-unit-tests")` — domain-specific tags
- `@DisplayName("...")` — human-readable test names
- Running tagged groups with Maven: `mvn test -Dgroups="integration"`

---

## 📦 Package Breakdown

| Package | Type | Topic |
|---|---|---|
| `functionalInterface.function.myimpl` | Main | `Function<T,R>`, `andThen`, `compose` |
| `optionaldemoone` | Main | `Optional` with Customer/MemberCard domain |
| `jenkins` | Main | CI/CD placeholder (empty) |
| `junitdemo` | Test | JUnit 5 `@Tag`, `@DisplayName` |
| `optionaldemoone` | Test | `DiscountService` Optional pipeline tests |
| `optionaldemoWithPerson` | Test | Optional with streams, `flatMap`, `filter` |

---

## 🔑 Key Design Philosophy

> **"Make the impossible states unrepresentable."**

This project demonstrates that instead of sprinkling `if (x != null)` checks everywhere, you can model optional values explicitly using `Optional<T>`. This approach:

1. **Self-documents** your API — callers know a field might be absent
2. **Forces handling** of the absent case — no more silent `NullPointerException`
3. **Enables chaining** — clean, readable pipelines without nested null checks
4. **Works beautifully with streams** — `Optional::stream` and `flatMap` on `Stream<Optional<T>>`

---

## ▶️ Quick Start

```bash
# Clone and run all tests
mvn test

# Run only the Optional discount tests
mvn test -Dgroups="Rel1,Rel2,Rel3,Rel4"
```

---

## 📖 Further Reading

- [docs/optional-guide.md](optional-guide.md) — Full Optional API guide with examples
- [docs/advanced-optional-scenarios-explained.md](advanced-optional-scenarios-explained.md) — Advanced Optional patterns explained
- [docs/functional-interface-function-guide.md](functional-interface-function-guide.md) — Function interface guide
- [docs/junit5-tagging-guide.md](junit5-tagging-guide.md) — JUnit 5 tagging guide
- [docs/java21-modern-java-guide.md](java21-modern-java-guide.md) — Java 9→21 features used in this project

