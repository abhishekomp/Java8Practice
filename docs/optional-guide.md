# Java `Optional<T>` — The Complete Practical Guide

## Why Does `Optional` Exist?

Before Java 8, the only way to represent "no value" was `null`. This led to the most common Java runtime error:

```java
// Classic null-check hell
String name = customer.getMemberCard().getDiscountCode().toUpperCase();
// ☠️ NullPointerException if getMemberCard() or getDiscountCode() returns null
```

To avoid it, developers had to write deeply nested null checks:

```java
if (customer.getMemberCard() != null) {
    MemberCard card = customer.getMemberCard();
    if (card.getDiscountCode() != null) {
        String code = card.getDiscountCode().toUpperCase();
    }
}
```

This is verbose, error-prone, and hides business logic inside defensive checks.

**`Optional<T>` solves this** by making the possibility of absence part of the type system. The compiler forces you to handle the missing case.

---

## The Golden Rule

> ✅ Use `Optional` as a **return type** for methods that might not return a value.  
> ❌ Do NOT use `Optional` as a field type, constructor parameter, or method parameter.

```java
// ✅ CORRECT — return type
public Optional<MemberCard> getMemberCard() { ... }

// ❌ WRONG — field type
private Optional<MemberCard> memberCard; // Don't do this

// ❌ WRONG — method parameter
public void process(Optional<Customer> customer) { ... } // Don't do this
```

---

## Creating an Optional

### `Optional.of(value)` — Non-null value only
```java
Optional<Integer> discount = Optional.of(10);
// Throws NullPointerException if value is null!
```

### `Optional.ofNullable(value)` — Safe, handles null
```java
Optional<MemberCard> card = Optional.ofNullable(memberCard); // safe even if null
```

### `Optional.empty()` — Explicitly no value
```java
Optional<Integer> noDiscount = Optional.empty();
```

---

## Reading the Value

### `orElse(default)` — Most common pattern
```java
int discount = getDiscountPercentage(card).orElse(0);
```

### `orElseGet(Supplier)` — Lazy evaluation (PREFER this for expensive defaults)
```java
// orElse always evaluates the default expression, even if value is present
// orElseGet only evaluates the supplier when the Optional IS empty

// ⚠️ orElse — fetchFromDB() is ALWAYS called, even if discount is present
int discount = getDiscount().orElse(fetchFromDB());

// ✅ orElseGet — fetchFromDB() is called ONLY when Optional is empty
int discount = getDiscount().orElseGet(() -> fetchFromDB());
```
> 💡 **Real project tip:** Always prefer `orElseGet` when the default is expensive to compute (DB calls, HTTP calls, heavy object creation).

### `orElseThrow(Supplier)` — Fail fast with a meaningful exception
```java
// Bad — generic NPE with no context
Customer customer = findById(id).get(); // ❌ throws NoSuchElementException

// Good — explicit, meaningful exception
Customer customer = findById(id)
    .orElseThrow(() -> new CustomerNotFoundException("Customer not found for id: " + id));
```

---

## Transforming the Value

### `map(Function)` — Transform if present
```java
// If memberCard is present, get its points; otherwise 0
int points = customer.getMemberCard()
                     .map(MemberCard::getPoints)
                     .orElse(0);
```

### `flatMap(Function)` — When your mapping function also returns Optional
```java
// getDiscountPercentageAsInteger returns Optional<Integer>
// Without flatMap, you'd get Optional<Optional<Integer>>
// flatMap "flattens" it to Optional<Integer>

Optional<Integer> discount = customer.getMemberCard()
                                     .flatMap(this::getDiscountPercentageAsInteger);
```

> 💡 **Rule of thumb:** Use `map` when the transformation returns a plain value. Use `flatMap` when the transformation returns another `Optional`.

---

## Filtering the Value

### `filter(Predicate)` — Keep value only if condition is true
```java
// Get member card only if it has enough points for gold status
Optional<MemberCard> goldCard = customer.getMemberCard()
                                        .filter(card -> card.getPoints() >= 200);
```

This is cleaner than:
```java
// Old way
if (card != null && card.getPoints() >= 200) { ... }
```

---

## Acting on the Value

### `ifPresent(Consumer)` — Do something if present
```java
customer.getMemberCard()
        .ifPresent(card -> sendRewardEmail(card));

// or with method reference
customer.getMemberCard().ifPresent(this::sendRewardEmail);
```

### `ifPresentOrElse(Consumer, Runnable)` — Handle both cases (Java 9+)
```java
customer.getMemberCard()
        .ifPresentOrElse(
            card  -> sendRewardEmail(card),       // present: send email
            ()    -> sendSignupPromotionEmail()   // absent: encourage to sign up
        );
```

> 💡 This is much cleaner than `if (optional.isPresent()) { ... } else { ... }`.

---

## Chaining Optionals

### `or(Supplier<Optional>)` — Fallback to another Optional (Java 9+)
```java
// Try to find customer in primary DB, fall back to archive DB
Optional<Customer> customer = findInPrimaryDB(id)
                                .or(() -> findInArchiveDB(id));
```

---

## Optional with Streams

### Flattening `Stream<Optional<T>>` — Two approaches

**Approach 1: `filter` + `map` (Java 8)**
```java
List<Integer> ages = names.stream()
    .map(Person::findByName)      // Stream<Optional<Person>>
    .filter(Optional::isPresent)  // keep only present ones
    .map(Optional::get)           // unwrap
    .map(Person::getAge)
    .collect(toList());
```

**Approach 2: `flatMap(Optional::stream)` (Java 9+ — preferred)**
```java
List<Integer> ages = names.stream()
    .map(Person::findByName)          // Stream<Optional<Person>>
    .flatMap(Optional::stream)        // flatten: skips empty optionals
    .map(Person::getAge)
    .collect(toList());
```

> 💡 `Optional::stream` converts an `Optional<T>` into a `Stream<T>` with 0 or 1 elements. Using `flatMap` with it is the cleanest way to filter out absent values from a stream.

---

## Real-World Pattern: The Optional Pipeline

This is the core pattern from `DiscountService.java` in this project:

```java
public String getDiscountLine(Customer customer) {
    return customer.getMemberCard()                              // Optional<MemberCard>
            .flatMap(this::getDiscountPercentageAsInteger)      // Optional<Integer>
            .map(d -> customer.getName() + " gets Discount% " + d) // Optional<String>
            .orElse(customer.getName() + " gets Discount% 0");  // String (default)
}
```

**What this achieves:**
1. If customer has no card → `getMemberCard()` returns `empty()` → falls through to `orElse`
2. If card has no qualifying points → `getDiscountPercentageAsInteger` returns `empty()` → falls through to `orElse`
3. If card qualifies → builds the discount message

**Zero null checks. Zero if statements. Fully readable business logic.**

---

## Common Mistakes

### ❌ Mistake 1: Using `isPresent()` + `get()`
```java
// ❌ This is just a verbose null check — defeats the purpose of Optional
if (optional.isPresent()) {
    String value = optional.get();
    process(value);
}

// ✅ Use ifPresent instead
optional.ifPresent(this::process);
```

### ❌ Mistake 2: Returning `null` instead of `Optional.empty()`
```java
// ❌ Null defeats the entire purpose
public Optional<MemberCard> getMemberCard() {
    return null; // Never do this
}

// ✅ Always return Optional.empty() for absence
public Optional<MemberCard> getMemberCard() {
    return Optional.empty();
}
```

### ❌ Mistake 3: Using `orElse` for expensive operations
```java
// ❌ This always calls expensiveDefault(), even when value is present
return optional.orElse(expensiveDefault());

// ✅ orElseGet is lazy — only called when Optional is empty
return optional.orElseGet(() -> expensiveDefault());
```

### ❌ Mistake 4: Wrapping Optional in Optional
```java
// ❌ Never nest Optionals
Optional<Optional<MemberCard>> nested = ...; // Ugly and useless

// ✅ Use flatMap to stay flat
Optional<MemberCard> flat = ...;
```

---

## Quick Reference Card

| Scenario | Method to use |
|---|---|
| Value might be null | `Optional.ofNullable(value)` |
| Guaranteed non-null | `Optional.of(value)` |
| No value | `Optional.empty()` |
| Get value or default | `.orElse(defaultValue)` |
| Get value or compute default (lazy) | `.orElseGet(() -> compute())` |
| Get value or throw exception | `.orElseThrow(() -> new Ex(...))` |
| Transform value | `.map(fn)` |
| Transform where fn returns Optional | `.flatMap(fn)` |
| Keep value conditionally | `.filter(predicate)` |
| Side effect if present | `.ifPresent(consumer)` |
| Handle both present/absent | `.ifPresentOrElse(consumer, runnable)` |
| Fallback to another Optional | `.or(() -> otherOptional)` |
| Use in Stream pipeline | `.flatMap(Optional::stream)` |
| Check presence (use sparingly) | `.isPresent()` / `.isEmpty()` |

---

## See Also

- **Code:** `src/main/java/optionaldemoone/` — `Customer`, `MemberCard`, `DiscountService`
- **Tests:** `src/test/java/optionaldemoone/DiscountServiceTest.java`
- **Tests:** `src/test/java/optionaldemoWithPerson/OptionalScenariosDemoTest.java`
- **Advanced Tests:** `src/test/java/optionaldemoWithPerson/AdvancedOptionalScenariosTest.java`

