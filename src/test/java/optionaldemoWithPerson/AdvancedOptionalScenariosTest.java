package optionaldemoWithPerson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Advanced Optional scenarios that demonstrate real-world usage patterns.
 *
 * See docs/optional-guide.md for detailed explanations of every method used here.
 *
 * Scenarios covered:
 *  1. orElse vs orElseGet — the lazy evaluation trap
 *  2. filter — keep value only when a condition is met
 *  3. ifPresent and ifPresentOrElse — side-effect actions
 *  4. orElseThrow — fail fast with a meaningful exception
 *  5. or() — fallback to another Optional (Java 9+)
 *  6. Optional::stream with flatMap — cleanest way to flatten Stream<Optional<T>>
 *  7. Chaining map + flatMap in a real domain pipeline
 *  8. isEmpty() — the positive absence check (Java 11+)
 */
class AdvancedOptionalScenariosTest {

    // -------------------------------------------------------------------------
    // Scenario 1: orElse vs orElseGet — The Lazy Evaluation Trap
    // -------------------------------------------------------------------------
    // KEY LESSON:
    //   orElse(T other)           — 'other' is ALWAYS evaluated, even when
    //                               the Optional contains a value.
    //   orElseGet(Supplier<T>)    — The supplier is called ONLY when the
    //                               Optional is empty (lazy evaluation).
    //
    // In production code, if computing the default involves a DB call, HTTP
    // request, or any expensive operation, ALWAYS use orElseGet to avoid
    // unnecessary work.
    // -------------------------------------------------------------------------

    private int expensiveCallCount = 0;

    private String simulateExpensiveDbCall() {
        expensiveCallCount++;   // track how many times this gets called
        return "DB_DEFAULT";
    }

    @Test
    @DisplayName("orElse always evaluates the default, even when value is present")
    void orElse_alwaysEvaluatesDefault() {
        expensiveCallCount = 0;
        Optional<String> presentOptional = Optional.of("ACTUAL_VALUE");

        // orElse: simulateExpensiveDbCall() IS called even though Optional has a value
        String result = presentOptional.orElse(simulateExpensiveDbCall());

        assertThat(result).isEqualTo("ACTUAL_VALUE");
        assertThat(expensiveCallCount)
                .as("orElse evaluated the default even though Optional had a value")
                .isEqualTo(1); // the expensive call was made unnecessarily!
    }

    @Test
    @DisplayName("orElseGet only evaluates the supplier when Optional is empty")
    void orElseGet_isLazy_onlyEvaluatesWhenEmpty() {
        expensiveCallCount = 0;
        Optional<String> presentOptional = Optional.of("ACTUAL_VALUE");

        // orElseGet: supplier is NOT called because Optional has a value
        String result = presentOptional.orElseGet(() -> simulateExpensiveDbCall());

        assertThat(result).isEqualTo("ACTUAL_VALUE");
        assertThat(expensiveCallCount)
                .as("orElseGet did NOT call the supplier because Optional had a value")
                .isEqualTo(0); // no unnecessary call!
    }

    @Test
    @DisplayName("orElseGet DOES call the supplier when Optional is empty")
    void orElseGet_callsSupplier_whenEmpty() {
        expensiveCallCount = 0;
        Optional<String> emptyOptional = Optional.empty();

        String result = emptyOptional.orElseGet(() -> simulateExpensiveDbCall());

        assertThat(result).isEqualTo("DB_DEFAULT");
        assertThat(expensiveCallCount).isEqualTo(1); // called exactly once, as expected
    }

    // -------------------------------------------------------------------------
    // Scenario 2: filter — Guard the value with a condition
    // -------------------------------------------------------------------------
    // KEY LESSON:
    //   filter(Predicate) keeps the value only if the predicate is true.
    //   If the Optional is empty, filter has no effect (stays empty).
    //   If the predicate returns false, the Optional becomes empty.
    //
    // Real-world use: Only process a customer who is an adult; only apply a
    // discount if the card has enough points.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("filter keeps the value when predicate matches")
    void filter_keepsValue_whenPredicateMatches() {
        Optional<Person> adultPerson = Optional.of(new Person("Alice", 30))
                .filter(p -> p.getAge() >= 18);

        assertThat(adultPerson).isPresent();
        assertThat(adultPerson.get().getName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("filter returns empty when predicate does not match")
    void filter_returnsEmpty_whenPredicateDoesNotMatch() {
        Optional<Person> minorPerson = Optional.of(new Person("Bob", 15))
                .filter(p -> p.getAge() >= 18); // Bob is only 15

        assertThat(minorPerson).isEmpty();
    }

    @Test
    @DisplayName("Chaining filter with map — real-world discount eligibility")
    void filter_chainedWithMap_discountEligibility() {
        // Only people 18+ who are named "Nancy" get a VIP label
        Optional<String> vipLabel = Optional.of(new Person("Nancy", 40))
                .filter(p -> p.getAge() >= 18)
                .filter(p -> p.getName().equalsIgnoreCase("Nancy"))
                .map(p -> "VIP: " + p.getName());

        assertThat(vipLabel).contains("VIP: Nancy");
    }

    // -------------------------------------------------------------------------
    // Scenario 3: ifPresent and ifPresentOrElse — Side-effect actions
    // -------------------------------------------------------------------------
    // KEY LESSON:
    //   ifPresent(Consumer) — do something if value exists (e.g., send an email)
    //   ifPresentOrElse(Consumer, Runnable) — handle BOTH the present and absent
    //   case explicitly, without an ugly if/else block.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ifPresent runs the consumer only when value is present")
    void ifPresent_runsConsumer_whenValuePresent() {
        StringBuilder log = new StringBuilder();

        Optional.of(new Person("Jack", 15))
                .ifPresent(p -> log.append("Found: ").append(p.getName()));

        assertThat(log.toString()).isEqualTo("Found: Jack");
    }

    @Test
    @DisplayName("ifPresent does nothing when Optional is empty")
    void ifPresent_doesNothing_whenEmpty() {
        StringBuilder log = new StringBuilder();

        Optional.<Person>empty()
                .ifPresent(p -> log.append("Should not run"));

        assertThat(log.toString()).isEmpty();
    }

    @Test
    @DisplayName("ifPresentOrElse handles both present and absent cases cleanly")
    void ifPresentOrElse_handlesBothCases() {
        StringBuilder log = new StringBuilder();

        // Case 1: value present — consumer runs
        Optional.of(new Person("Sara", 20))
                .ifPresentOrElse(
                        p  -> log.append("Sending reward email to ").append(p.getName()),
                        () -> log.append("Sending signup promo email")
                );
        assertThat(log.toString()).isEqualTo("Sending reward email to Sara");

        log.setLength(0); // clear

        // Case 2: empty — runnable runs
        Optional.<Person>empty()
                .ifPresentOrElse(
                        p  -> log.append("Sending reward email"),
                        () -> log.append("Sending signup promo email")
                );
        assertThat(log.toString()).isEqualTo("Sending signup promo email");
    }

    // -------------------------------------------------------------------------
    // Scenario 4: orElseThrow — Fail fast with a meaningful exception
    // -------------------------------------------------------------------------
    // KEY LESSON:
    //   Never call .get() on an Optional without a prior isPresent() check.
    //   .get() throws a generic NoSuchElementException which gives no context.
    //   .orElseThrow(() -> new YourException(...)) gives you a domain-specific
    //   exception with a useful message.
    //
    //   This is the "fail fast" principle: surface errors early with context.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("orElseThrow returns value when present")
    void orElseThrow_returnsValue_whenPresent() {
        Person person = Optional.of(new Person("Paula", 35))
                .orElseThrow(() -> new IllegalStateException("Person not found"));

        assertThat(person.getName()).isEqualTo("Paula");
    }

    @Test
    @DisplayName("orElseThrow throws a meaningful exception when empty")
    void orElseThrow_throwsMeaningfulException_whenEmpty() {
        assertThatThrownBy(() ->
                Optional.<Person>empty()
                        .orElseThrow(() -> new IllegalArgumentException("Person not found for the given criteria"))
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Person not found for the given criteria");
    }

    // -------------------------------------------------------------------------
    // Scenario 5: or() — Fallback to another Optional (Java 9+)
    // -------------------------------------------------------------------------
    // KEY LESSON:
    //   or(Supplier<Optional<T>>) lets you chain a fallback Optional.
    //   The supplier is only evaluated if the original Optional is empty.
    //
    //   Real-world use: Try primary data source, fall back to secondary.
    //   e.g., "find in cache first, then DB, then archive"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("or() returns the original Optional when it has a value")
    void or_returnsOriginal_whenPresent() {
        Optional<Person> result = Optional.of(new Person("Bill", 25))
                .or(() -> Optional.of(new Person("Fallback", 0)));

        assertThat(result.get().getName()).isEqualTo("Bill");
    }

    @Test
    @DisplayName("or() falls back to the supplier Optional when empty")
    void or_fallsBack_whenEmpty() {
        Optional<Person> result = Optional.<Person>empty()
                .or(() -> Optional.of(new Person("Fallback Person", 99)));

        assertThat(result.get().getName()).isEqualTo("Fallback Person");
    }

    @Test
    @DisplayName("Chaining or() — simulate: cache miss → DB hit → archive")
    void or_chained_simulatingMultipleDataSources() {
        Optional<String> cachedValue    = Optional.empty();      // cache miss
        Optional<String> dbValue        = Optional.empty();      // DB miss
        Optional<String> archiveValue   = Optional.of("from-archive"); // archive hit

        String result = cachedValue
                .or(() -> dbValue)
                .or(() -> archiveValue)
                .orElse("not found anywhere");

        assertThat(result).isEqualTo("from-archive");
    }

    // -------------------------------------------------------------------------
    // Scenario 6: Optional::stream — Best way to flatten Stream<Optional<T>>
    // -------------------------------------------------------------------------
    // KEY LESSON:
    //   When you do names.stream().map(Person::findByName) you get a
    //   Stream<Optional<Person>>. You need to "unwrap" each Optional.
    //
    //   Option A (Java 8): .filter(Optional::isPresent).map(Optional::get)
    //   Option B (Java 9+, preferred): .flatMap(Optional::stream)
    //
    //   Optional::stream converts:
    //     - Optional.of(value) → Stream.of(value)  (1-element stream)
    //     - Optional.empty()   → Stream.empty()    (0-element stream)
    //   flatMap then merges all those streams — empty ones vanish automatically.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("flatMap(Optional::stream) elegantly skips absent values in a stream")
    void flatMapOptionalStream_skipsAbsentValues() {
        List<String> names = List.of("Jack", "NonExistent", "Sara", "AlsoMissing");

        // Using flatMap(Optional::stream) — preferred approach
        var ages = names.stream()
                .map(Person::findByName)       // Stream<Optional<Person>>
                .flatMap(Optional::stream)      // Stream<Person> — absent ones dropped
                .map(Person::getAge)
                .toList();  // Java 16+: unmodifiable List, no Collectors import needed

        // Only Jack (15) and Sara (20) exist in the person map
        assertThat(ages).containsExactlyInAnyOrder(15, 20);
    }

    @Test
    @DisplayName("Equivalent: filter(isPresent) + map(get) gives the same result")
    void filterIsPresent_mapGet_equivalentToFlatMapStream() {
        List<String> names = List.of("Jack", "NonExistent", "Sara");

        // Old Java 8 approach — more verbose but equivalent
        var ages = names.stream()
                .map(Person::findByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Person::getAge)
                .toList();  // Java 16+

        assertThat(ages).containsExactlyInAnyOrder(15, 20);
    }

    // -------------------------------------------------------------------------
    // Scenario 7: Chaining map + flatMap — multi-level domain pipeline
    // -------------------------------------------------------------------------
    // KEY LESSON:
    //   Real domain models often have multiple levels of Optional.
    //   Chain map() and flatMap() to traverse them without null checks.
    //
    //   Rule: use map() when your transformer returns a plain value.
    //         use flatMap() when your transformer returns Optional<Something>.
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Multi-level Optional chaining")
    class MultiLevelChaining {

        @Test
        @DisplayName("Chaining map then map — two plain transformations")
        void chainedMap_twoTransformations() {
            // Person → name → uppercase
            Optional<String> upperName = Optional.of(new Person("jill", 50))
                    .map(Person::getName)           // Optional<String> ("jill")
                    .map(String::toUpperCase);      // Optional<String> ("JILL")

            assertThat(upperName).contains("JILL");
        }

        @Test
        @DisplayName("flatMap is needed when intermediate step also returns Optional")
        void flatMap_neededWhenIntermediateStepReturnsOptional() {
            // Imagine: given a name, find the Person, then find their buddy
            // findByName returns Optional<Person>, so we need flatMap

            Optional<Person> buddy = Optional.of("Jack")
                    .flatMap(Person::findByName)           // Optional<Person> for Jack
                    .map(p -> new Person("Sara", 20));     // Imagine Sara is Jack's buddy

            assertThat(buddy).isPresent();
            assertThat(buddy.get().getName()).isEqualTo("Sara");
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 8: isEmpty() — The positive absence check (Java 11+)
    // -------------------------------------------------------------------------
    // KEY LESSON:
    //   Before Java 11, checking for absence required !optional.isPresent().
    //   Java 11 added isEmpty() as a readable positive check for absence.
    //   This makes intent clearer in validation logic.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("isEmpty() returns true when Optional has no value")
    void isEmpty_returnsTrueWhenEmpty() {
        Optional<Person> noPerson = Person.findByName("UnknownPerson");

        assertThat(noPerson.isEmpty()).isTrue();
        // Without isEmpty() you'd write: assertThat(!noPerson.isPresent()).isTrue();
        // isEmpty() reads much more naturally
    }

    @Test
    @DisplayName("isEmpty() returns false when Optional has a value")
    void isEmpty_returnsFalseWhenPresent() {
        Optional<Person> jack = Person.findByName("Jack");

        assertThat(jack.isEmpty()).isFalse();
        assertThat(jack.isPresent()).isTrue();
    }
}

