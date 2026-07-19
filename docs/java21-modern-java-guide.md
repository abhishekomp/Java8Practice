# Modern Java — Features Used in This Project (Java 9 → 21)

This project targets **Java 21 (LTS)** and actively uses the modern features introduced since Java 8.
This guide explains each feature, *why* it exists, and *where* you'll see it in the code.

---

## Java 10 — `var` (Local Variable Type Inference)

**What it is:** The compiler infers the type of a local variable from the right-hand side.  
You write `var` instead of repeating the full type name.

```java
// Old way — type written twice
DiscountService service = new DiscountService();
String discountLine = service.getDiscountLine(customer);

// Java 10+ — type inferred, less noise
var service = new DiscountService();
var discountLine = service.getDiscountLine(customer);
```

### Rules
- Only works for **local variables** (inside methods). Not fields, not parameters.
- The type is still **statically checked** at compile time — this is NOT dynamic typing.
- Don't overuse it when the type isn't obvious from context:

```java
var x = getValue();       // ❌ What type is x? Unclear without checking getValue()
var service = new DiscountService(); // ✅ Type is obvious from the right-hand side
```

**In this project:** `DiscountServiceTest.java`, `OptionalScenariosDemoTest.java`, `AdvancedOptionalScenariosTest.java`, `Main.java`, `Person.java`

---

## Java 14 — Switch Expressions

**What it is:** `switch` can now be used as an **expression** that produces a value.  
Uses `->` arrow syntax — no fall-through, no `break` needed.

```java
// Old switch statement — verbose, fall-through prone
int discount;
if (points >= 100) {
    discount = 10;
} else if (points >= 50) {
    discount = 5;
} else {
    discount = 0;
}

// Java 14+ switch expression — concise, exhaustive
int discount = switch (points / 50) {
    case 0   -> 0;   // < 50 points
    case 1   -> 5;   // 50–99 points
    default  -> 10;  // 100+ points
};
```

### Why it's better
- **No fall-through bugs** — each arm is independent
- **Exhaustiveness** — the compiler warns if cases are missing
- **Returns a value** — can be used inline in assignments or return statements
- **Cleaner than if/else chains** for multi-branch logic

**In this project:** `DiscountService.java` — `getDiscountPercentage()` uses a switch expression to map card points to a discount tier.

---

## Java 15 — `String.formatted()`

**What it is:** An instance method on `String` equivalent to `String.format()` but called on the template string itself.

```java
// Old way
String msg = String.format("Person{name='%s', age=%d}", name, age);

// Java 15+
String msg = "Person{name='%s', age=%d}".formatted(name, age);
```

Reads more naturally — the template string comes first, then `.formatted()` fills it in.

**In this project:** `Person.toString()` in `Person.java`

---

## Java 16 — `Stream.toList()`

**What it is:** A terminal operation on `Stream<T>` that collects elements into an **unmodifiable** `List<T>`.  
No import needed. Returns a list that cannot be mutated after creation.

```java
// Old way (Java 8) — requires import, verbose
import static java.util.stream.Collectors.toList;
List<Integer> ages = stream.collect(toList());

// Java 16+ — concise, no import, signals immutability
List<Integer> ages = stream.toList();
```

### Key difference from `Collectors.toList()`
| | `collect(Collectors.toList())` | `.toList()` |
|---|---|---|
| Mutability | Mutable list | **Unmodifiable** list |
| Import | Required | None |
| Available since | Java 8 | Java 16 |

> 💡 Prefer `.toList()` when you don't need to mutate the result — it communicates intent (this list won't change) and is more concise.

**In this project:** `OptionalScenariosDemoTest.java`, `AdvancedOptionalScenariosTest.java`

---

## Java 9 — `Optional::stream` and `Optional.or()`

### `Optional::stream`
Converts an `Optional<T>` into a `Stream<T>` with 0 or 1 elements.  
Used with `flatMap` to cleanly drop absent values from a `Stream<Optional<T>>`:

```java
List<Integer> ages = names.stream()
    .map(Person::findByName)   // Stream<Optional<Person>>
    .flatMap(Optional::stream) // absent Optionals become empty streams → vanish
    .map(Person::getAge)
    .toList();
```

### `Optional.or(Supplier<Optional<T>>)`
Fallback to another `Optional` when the first is empty:

```java
Optional<User> user = findInCache(id)
    .or(() -> findInDatabase(id))
    .or(() -> findInArchive(id));
```

**In this project:** `OptionalScenariosDemoTest.java`, `AdvancedOptionalScenariosTest.java`

---

## Java 9 — `List.of()` / `Map.of()` (Immutable Collections)

**What it is:** Factory methods for creating compact, unmodifiable collections.

```java
// Old way — mutable, verbose
List<String> names = new ArrayList<>();
names.add("Jack");
names.add("Sara");
names = Collections.unmodifiableList(names); // extra step to make immutable

// Java 9+ — concise and immutable by default
var names = List.of("Jack", "Sara");
```

> ⚠️ `List.of()` throws `NullPointerException` if you pass `null` as an element.  
> Use `Arrays.asList()` if you specifically need to allow null elements.

**In this project:** `OptionalScenariosDemoTest.java`, `Person.java`, `AdvancedOptionalScenariosTest.java`

---

## Java 9 — `ifPresentOrElse()` on Optional

Handles both the present and absent case in a single, readable call:

```java
// Old way
if (optional.isPresent()) {
    sendRewardEmail(optional.get());
} else {
    sendPromo();
}

// Java 9+
optional.ifPresentOrElse(
    card -> sendRewardEmail(card),
    ()   -> sendPromo()
);
```

**In this project:** `AdvancedOptionalScenariosTest.java`

---

## Java 11 — `Optional.isEmpty()`

The positive way to check for absence — reads better than `!isPresent()`:

```java
// Java 8
if (!customer.getMemberCard().isPresent()) { ... }

// Java 11+
if (customer.getMemberCard().isEmpty()) { ... }
```

**In this project:** `AdvancedOptionalScenariosTest.java`

---

## Java 21 — Why It Matters (What's Available But Not Yet Used)

The project runs on Java 21 LTS. These features are available to explore:

### Records (Java 16)
Immutable data carriers with auto-generated constructors, accessors, `equals`, `hashCode`, and `toString`:

```java
// Instead of a full POJO class with boilerplate
record Person(String name, int age) {}

// Usage
var p = new Person("Jack", 15);
System.out.println(p.name());  // accessor is name(), not getName()
System.out.println(p);         // Person[name=Jack, age=15]
```

> 💡 Great for DTOs, value objects, and data transfer classes in real projects.

### Pattern Matching for `instanceof` (Java 16)
Eliminates the cast after `instanceof`:

```java
// Old way
if (shape instanceof Circle) {
    Circle c = (Circle) shape; // redundant cast
    return c.radius() * 2;
}

// Java 16+
if (shape instanceof Circle c) {
    return c.radius() * 2; // c is already typed as Circle
}
```

### Sealed Classes (Java 17)
Restrict which classes can extend a class — makes hierarchies explicit and exhaustive:

```java
sealed interface Shape permits Circle, Rectangle, Triangle {}
```

Combined with switch expressions and pattern matching, this enables **exhaustive type-safe dispatch** — the compiler knows all possible subtypes.

### Text Blocks (Java 15)
Multi-line strings without escape characters:

```java
// Old way
String json = "{\n  \"name\": \"Jack\",\n  \"age\": 15\n}";

// Java 15+
String json = """
        {
          "name": "Jack",
          "age": 15
        }
        """;
```

---

## Summary — Feature Timeline

| Feature | Since | Used in this project |
|---|---|---|
| `Optional`, lambdas, `Function<T,R>` | Java 8 | ✅ Throughout |
| `List.of()`, `Optional::stream`, `or()`, `ifPresentOrElse()` | Java 9 | ✅ Tests |
| `var` (local type inference) | Java 10 | ✅ All files |
| `Optional.isEmpty()` | Java 11 | ✅ AdvancedOptionalScenariosTest |
| Switch expressions | Java 14 | ✅ DiscountService |
| `String.formatted()` | Java 15 | ✅ Person.toString() |
| `Stream.toList()` | Java 16 | ✅ Test files |
| Records | Java 16 | Available — not yet used |
| Pattern matching `instanceof` | Java 16 | Available — not yet used |
| Sealed classes | Java 17 | Available — not yet used |
| Virtual threads | Java 21 | Available — not yet used |

