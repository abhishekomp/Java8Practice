# Advanced Optional Scenarios — Explained

> **Code file:** `src/test/java/optionaldemoWithPerson/AdvancedOptionalScenariosTest.java`

This document explains each scenario in the advanced test file with the "why it matters in real projects" context.

---

## Scenario 1: `orElse` vs `orElseGet` — The Lazy Evaluation Trap

This is one of the most common **performance pitfalls** with `Optional` that developers stumble into.

### The Problem

```java
// ⚠️ This ALWAYS calls expensiveDbCall(), even when the Optional has a value!
return findCustomer(id).orElse(expensiveDbCall());
```

`orElse(T other)` evaluates `other` **eagerly** — the expression is computed before `orElse` is even called, regardless of whether the Optional is empty or not.

### The Fix

```java
// ✅ This calls expensiveDbCall() ONLY when the Optional is empty
return findCustomer(id).orElseGet(() -> expensiveDbCall());
```

`orElseGet(Supplier<T>)` evaluates the supplier **lazily** — it's only invoked when the Optional is actually empty.

### Real Impact

| Situation | `orElse` | `orElseGet` |
|---|---|---|
| Optional has a value | Computes default **unnecessarily** | Does **not** compute default |
| Optional is empty | Computes default (correct) | Computes default (correct) |

> 💡 **Rule of thumb:** If the default value is a literal (`0`, `""`, `"N/A"`), `orElse` is fine. If the default requires computation, always use `orElseGet`.

---

## Scenario 2: `filter` — Guard the Value With a Condition

`filter(Predicate)` lets you make an Optional "conditionally absent" — the value stays only if it passes a condition.

### Before Optional
```java
if (card != null && card.getPoints() >= 100) {
    return card.getDiscountCode();
}
return null;
```

### With Optional
```java
return Optional.ofNullable(card)
        .filter(c -> c.getPoints() >= 100)
        .map(MemberCard::getDiscountCode)
        .orElse(null);
```

### Real-world uses
- Only process orders above a minimum amount
- Only apply a promo code if it hasn't expired
- Only send an email if the customer has opted in

---

## Scenario 3: `ifPresent` and `ifPresentOrElse` — Side-Effect Actions

### `ifPresent` — do something if a value exists
```java
// Old way — verbose null/isPresent check
if (customer.getMemberCard().isPresent()) {
    sendRewardEmail(customer.getMemberCard().get());
}

// Clean way
customer.getMemberCard().ifPresent(this::sendRewardEmail);
```

### `ifPresentOrElse` — handle both cases explicitly (Java 9+)
```java
customer.getMemberCard().ifPresentOrElse(
    card -> sendRewardEmail(card),
    ()   -> sendSignupPromotionEmail()
);
```

This replaces the classic if/else pattern while making the intent crystal clear: "if the card is present do X, otherwise do Y."

> 💡 **When to use `ifPresent` vs `map`:**
> - Use `ifPresent` for **side effects** (logging, sending emails, saving to DB)
> - Use `map` to **transform** the value into something else

---

## Scenario 4: `orElseThrow` — Fail Fast With Context

### ❌ Never use bare `.get()`
```java
Person person = findById(id).get(); // throws NoSuchElementException — no context!
```

### ✅ Always use `orElseThrow` with a meaningful exception
```java
Person person = findById(id)
    .orElseThrow(() -> new PersonNotFoundException("No person found for id: " + id));
```

**Why this matters:**
- `NoSuchElementException` tells you nothing about what was being looked for
- A domain-specific exception with an ID or criteria in the message tells you exactly what went wrong
- It's the **fail-fast principle**: make failures visible and informative immediately

**Best practice in Spring/service layers:**
```java
// In a service class
public Customer getCustomerOrFail(Long id) {
    return customerRepository.findById(id)
        .orElseThrow(() -> new CustomerNotFoundException(
            "Customer with id [" + id + "] not found"));
}
```

---

## Scenario 5: `or()` — Fallback to Another Optional (Java 9+)

`or(Supplier<Optional<T>>)` is like a "plan B Optional". If the first Optional is empty, try the supplier.

### Real-world use case: Multi-source lookup

```java
public Optional<User> findUser(String id) {
    return findInCache(id)          // try L1 cache first
        .or(() -> findInDatabase(id)) // then try DB
        .or(() -> findInArchive(id)); // then try archive
}
```

This is much cleaner than:
```java
Optional<User> user = findInCache(id);
if (user.isEmpty()) {
    user = findInDatabase(id);
}
if (user.isEmpty()) {
    user = findInArchive(id);
}
return user;
```

> 💡 Note: `or()` is Java 9+. For Java 8 you'd need a different approach.

---

## Scenario 6: `Optional::stream` — Flattening a Stream of Optionals

This is the most **elegant stream + Optional integration** and is very common in real codebases.

### The Problem

You have a list of IDs and want to load the corresponding objects, skipping the ones that don't exist:

```java
List<String> names = List.of("Jack", "Unknown", "Sara");
// findByName returns Optional<Person>
Stream<Optional<Person>> stream = names.stream().map(Person::findByName);
// Now what? You have Optionals in the stream...
```

### Solution A — Java 8 (verbose)
```java
names.stream()
    .map(Person::findByName)
    .filter(Optional::isPresent)
    .map(Optional::get)
    .map(Person::getAge)
    .collect(toList());
```

### Solution B — Java 9+ (preferred, elegant)
```java
names.stream()
    .map(Person::findByName)       // Stream<Optional<Person>>
    .flatMap(Optional::stream)      // Stream<Person> — empty optionals vanish
    .map(Person::getAge)
    .collect(toList());
```

**How `Optional::stream` works:**
- `Optional.of(person).stream()` → `Stream` containing that one person
- `Optional.empty().stream()` → an empty `Stream`
- `flatMap` merges all these mini-streams → absent values simply disappear

---

## Scenario 7: Chaining `map` + `flatMap`

The key to using Optional fluently is knowing **when to use `map` vs `flatMap`**:

| Your transform returns... | Use |
|---|---|
| A plain value (String, int, object) | `map(fn)` |
| An `Optional<Something>` | `flatMap(fn)` |

**Why?** `map` wraps the result in an Optional automatically. If your function already returns an Optional and you use `map`, you get `Optional<Optional<T>>` — a nested mess. `flatMap` flattens that for you.

```java
// findByName returns Optional<Person> — use flatMap
Optional<Person> person = Optional.of("Jack")
    .flatMap(Person::findByName);  // ✅ Optional<Person>

// Using map instead would give Optional<Optional<Person>> ❌
Optional<Optional<Person>> wrong = Optional.of("Jack")
    .map(Person::findByName);     // ❌ Don't do this
```

---

## Scenario 8: `isEmpty()` — Positive Absence Check (Java 11+)

Before Java 11, the only way to check for absence was `!optional.isPresent()` — a double negative that's harder to read.

```java
// Java 8 — double negative, reads awkwardly
if (!customer.getMemberCard().isPresent()) {
    sendSignupPromo();
}

// Java 11 — clear and readable
if (customer.getMemberCard().isEmpty()) {
    sendSignupPromo();
}
```

> 💡 Prefer `isEmpty()` over `!isPresent()` whenever you're checking for **absence**.

---

## Summary Table

| Method | Available since | Key use-case |
|---|---|---|
| `orElse` | Java 8 | Simple default literal values |
| `orElseGet` | Java 8 | Expensive computed defaults (lazy) |
| `orElseThrow` | Java 8 | Fail fast with domain exception |
| `filter` | Java 8 | Conditional presence |
| `map` | Java 8 | Transform value (plain return) |
| `flatMap` | Java 8 | Transform value (Optional return) |
| `ifPresent` | Java 8 | Side effects when present |
| `or` | Java 9 | Fallback to another Optional |
| `ifPresentOrElse` | Java 9 | Handle both present/absent |
| `Optional::stream` | Java 9 | Flatten Stream<Optional<T>> |
| `isEmpty` | Java 11 | Positive absence check |

