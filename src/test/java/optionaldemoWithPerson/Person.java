package optionaldemoWithPerson;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * Simple domain object representing a person with a name and age.
 *
 * Also acts as a mini-repository via the static findByName() method,
 * which returns Optional<Person> to force callers to handle the
 * "person not found" case explicitly — no null returns, no NPE risk.
 */
public class Person {
    private String name;
    private int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    /**
     * Looks up a person by name.
     *
     * Returns Optional.empty() when no match is found — this is the
     * correct contract for a "find" method that may find nothing.
     * Callers are forced to handle the absent case.
     */
    public static Optional<Person> findByName(String name) {
        return ofNullable(getPersonsMap().get(name));
    }

    /**
     * Builds an in-memory name → Person lookup map.
     *
     * toMap(Person::getName, Function.identity()) means:
     *   key   = person.getName()
     *   value = the Person object itself
     */
    private static Map<String, Person> getPersonsMap() {
        // Java 9+: List.of() creates an unmodifiable list — safe to share
        var people = List.of(
                new Person("Jack", 15),
                new Person("Sara", 20),
                new Person("Bob", 20),
                new Person("Paula", 35),
                new Person("Nancy", 40),
                new Person("Bill", 25),
                new Person("Jill", 50),
                new Person("Tom", 70)
        );

        return people.stream()
                .collect(toMap(Person::getName, Function.identity()));
    }

    @Override
    public String toString() {
        return "Person{name='%s', age=%d}".formatted(name, age); // Java 15+: String.formatted()
    }
}
