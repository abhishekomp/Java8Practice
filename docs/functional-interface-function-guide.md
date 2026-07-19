# Java `Function<T, R>` — Functional Interface Guide

## What Is a Functional Interface?

A **functional interface** is any interface with **exactly one abstract method**. Java 8 introduced the `@FunctionalInterface` annotation to mark them, and lambda expressions to implement them concisely.

```java
@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
}
```

`Function<T, R>` takes an input of type `T` and returns a result of type `R`.

---

## The Three Ways to Use `Function<T, R>`

### 1. As a Class (Traditional — explicit, reusable logic)

```java
public class PerformSquare implements Function<Long, Long> {
    @Override
    public Long apply(Long aLong) {
        return aLong * aLong;
    }
}
```

**When to use:** When the logic is complex, reusable, or needs to be tested independently.

```java
Function<Long, Long> squareFunc = new PerformSquare();
Long result = squareFunc.apply(6L); // 36
```

### 2. As a Lambda (Most common)

```java
Function<Long, String> resultLabel = (num) -> num + " is the result";
```

**When to use:** For short, one-liner transformations inline in the calling code.

### 3. As a Method Reference

```java
Function<String, Integer> parseLength = String::length;
```

**When to use:** When an existing method signature matches the function contract exactly.

---

## Composing Functions

One of the most powerful features of `Function` is the ability to **compose** multiple functions together.

### `andThen(Function after)` — Left to Right

`f.andThen(g)` means: **first apply `f`, then apply `g` to the result.**

```
Input → [f] → intermediate result → [g] → Output
```

```java
Function<Long, Long>   square      = new PerformSquare();         // Long → Long
Function<Long, String> addLabel    = (num) -> num + " is the result"; // Long → String

// squareFunc.andThen(stringResult): Long → Long → String
String result = square.andThen(addLabel).apply(6L);
// square(6) = 36, addLabel(36) = "36 is the result"
System.out.println(result); // "36 is the result"
```

### `compose(Function before)` — Right to Left

`f.compose(g)` means: **first apply `g`, then apply `f` to the result.**

```
Input → [g] → intermediate result → [f] → Output
```

```java
Function<Long, Long>   square   = new PerformSquare();
Function<Long, String> addLabel = (num) -> num + " is the result";

// addLabel.compose(square): Long → Long → String
// (Same result as andThen but called from a different perspective)
String result = addLabel.compose(square).apply(6L);
// square(6) = 36, addLabel(36) = "36 is the result"
System.out.println(result); // "36 is the result"
```

> 💡 **Key difference:**
> - `f.andThen(g)` — you think: "do f, **and then** do g"
> - `f.compose(g)` — you think: "to do f, **first compose** with g" (g runs first)

---

## Visual Comparison: `andThen` vs `compose`

```
andThen:
  squareFunc.andThen(stringResult).apply(6)
  6 ──► [squareFunc: 6→36] ──► [stringResult: 36→"36 is the result"] ──► "36 is the result"

compose:
  stringResult.compose(squareFunc).apply(6)
  6 ──► [squareFunc: 6→36] ──► [stringResult: 36→"36 is the result"] ──► "36 is the result"
```

Both produce the same result here, but the chain is built differently. In complex pipelines, the distinction matters.

---

## Real-World Example: Data Transformation Pipeline

```java
Function<String, String>  trim       = String::trim;
Function<String, String>  uppercase  = String::toUpperCase;
Function<String, Integer> length     = String::length;

// Pipeline: "  hello  " → "hello" → "HELLO" → 5
Function<String, Integer> pipeline = trim.andThen(uppercase).andThen(length);
int result = pipeline.apply("  hello  "); // 5
```

---

## Other Common Functional Interfaces (Related)

| Interface | Signature | Description |
|---|---|---|
| `Function<T, R>` | `T → R` | Transform input to output |
| `Consumer<T>` | `T → void` | Consume a value (side-effect) |
| `Supplier<T>` | `() → T` | Produce a value |
| `Predicate<T>` | `T → boolean` | Test a condition |
| `BiFunction<T, U, R>` | `(T, U) → R` | Two inputs, one output |
| `UnaryOperator<T>` | `T → T` | Input and output same type |

---

## Code in This Project

| File | What it shows |
|---|---|
| `PerformSquare.java` | Implementing `Function<T,R>` as a class |
| `Main.java` | Using `apply`, `andThen`, and `compose` |

### From `Main.java`:
```java
Function<Long, Long>   squareFunc   = new PerformSquare();
Function<Long, String> stringResult = (num) -> num + " is the result";

squareFunc.apply(6L);                          // 36
squareFunc.andThen(stringResult).apply(6L);    // "36 is the result"
stringResult.compose(squareFunc).apply(6L);    // "36 is the result"
```

---

## See Also

- [docs/optional-guide.md](optional-guide.md) — `Optional` uses `Function` internally in `.map()` and `.flatMap()`
- Java Docs: [`java.util.function.Function`](https://docs.oracle.com/en/java/docs/api/java.base/java/util/function/Function.html)

