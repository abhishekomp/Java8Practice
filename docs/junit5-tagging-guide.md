# JUnit 5 Tagging & Test Organisation Guide

## Why Tag Tests?

In real projects, you have multiple types of tests with very different characteristics:

| Test Type | Speed | When to Run |
|---|---|---|
| Unit tests | Fast (ms) | Every commit, every build |
| Integration tests | Slow (seconds) | Pre-merge, nightly |
| Sanity / Smoke tests | Medium | After deployment to staging |
| End-to-end tests | Very slow | Before release |

Without tagging, you have to run **all tests every time**, which wastes CI/CD time. JUnit 5 `@Tag` lets you **selectively run** only the tests you need at each stage.

---

## `@Tag` Basics

### Tagging a Class (applies to all tests in the class)
```java
@Tag("integration")
@Tag("ecomm")
public class ECommerceIntegrationTest {
    @Test
    public void test_1() { ... }

    @Test
    public void test_2() { ... }
}
```
Both `test_1` and `test_2` inherit the `integration` and `ecomm` tags.

### Tagging a Single Method
```java
@Test
@Tag("ecomm-sanity")
public void test_3() {
    System.out.println("Ran Sanity test");
}
```

### Multiple Tags on One Test
```java
@Tag("ecomm-unit-tests")
@Tag("ecomm")
public class ECommerceUnitTest { ... }
```
This test class can be selected by either `ecomm-unit-tests` OR `ecomm`.

---

## Running Tagged Tests with Maven

Use the `-Dgroups` property with the `maven-surefire-plugin`:

```bash
# Run only integration tests
mvn test -Dgroups="integration"

# Run only unit tests
mvn test -Dgroups="ecomm-unit-tests"

# Run sanity tests only
mvn test -Dgroups="ecomm-sanity"

# Run tests matching any of multiple tags (OR logic)
mvn test -Dgroups="ecomm-unit-tests | integration"

# Run tests matching ALL tags (AND logic)
mvn test -Dgroups="ecomm & integration"

# Exclude a tag
mvn test -Dgroups="!(integration)"
```

> 💡 The `maven-surefire-plugin` (version 2.22.2+) supports JUnit 5 tag filtering natively.

---

## `@DisplayName` — Human-Readable Test Names

```java
@Test
@DisplayName("Integration Test 1 — Verify product listing loads")
public void test_1() { ... }
```

**Why it matters:**
- In CI dashboards and IDE test results, you see `"Integration Test 1 — Verify product listing loads"` instead of `test_1`
- Makes test failure reports readable by non-developers (product managers, QA)

---

## CI/CD Integration Pattern

A typical CI/CD pipeline uses tags like this:

```
[Developer commits]
       │
       ▼
[Unit Tests] ──── mvn test -Dgroups="ecomm-unit-tests"
       │           Fast! Run on every commit.
       ▼
[Integration Tests] ──── mvn test -Dgroups="integration"
       │                  Slower. Run on PR merge.
       ▼
[Deploy to Staging]
       │
       ▼
[Sanity Tests] ──── mvn test -Dgroups="ecomm-sanity"
                    Verify deployment succeeded.
```

---

## Passing Parameters to Tests at Runtime

The project demonstrates passing system properties via Maven:

```java
// In test code — read a system property with a default value
String inParam = System.getProperty("inParameter", "DefaultValue");
```

```bash
# Pass the property from Maven
mvn test -DinParameter=MyCustomValue
```

**Real-world use case:** Pass environment name (`staging`, `production`), feature flags, or test data identifiers.

---

## Code in This Project

| File | Tags |
|---|---|
| `ECommerceIntegrationTest.java` | `integration`, `ecomm` |
| `ECommerceUnitTest.java` | `ecomm-unit-tests`, `ecomm`, `ecomm-sanity` (on one method) |
| `DiscountServiceTest.java` | `Rel1`, `Rel2`, `Rel3`, `Rel4` (release-based tagging) |

### Release-Based Tagging Pattern
`DiscountServiceTest.java` uses release tags (`Rel1`, `Rel2`, etc.) — a pattern used in teams that ship features incrementally and want to run only tests related to a specific release in regression:

```bash
# Run only Rel1 tests
mvn test -Dgroups="Rel1"

# Run Rel1 and Rel2 tests for combined regression
mvn test -Dgroups="Rel1 | Rel2"
```

---

## AssertJ — Fluent Assertions

The project uses **AssertJ** for readable assertions instead of plain JUnit `assertEquals`:

```java
// JUnit plain
assertEquals("Jack gets Discount% 10", discountLine);

// AssertJ — reads like English
assertThat(discountLine).isEqualTo("Jack gets Discount% 10");

// AssertJ chains
assertThat(list).hasSize(2).containsExactlyInAnyOrder(15, 20);
```

AssertJ also produces much better failure messages when tests fail, showing exactly what was expected vs what was received.

---

## See Also

- [JUnit 5 User Guide — Tagging and Filtering](https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- Maven Surefire Plugin docs for `groups` configuration

