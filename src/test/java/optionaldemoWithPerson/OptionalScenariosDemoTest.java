package optionaldemoWithPerson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OptionalScenariosDemoTest {

    List<Person> people = List.of(
            new Person("Jack", 15),
            new Person("Sara", 20),
            new Person("Bob", 20),
            new Person("Paula", 35),
            new Person("Nancy", 40),
            new Person("Bill", 25),
            new Person("Jill", 50),
            new Person("Tom", 70)
    );

    @Test
    @DisplayName("Given a list of persons, get the age of a person by name (found)")
    public void getAgeOfSpecificPerson() {
        // Stream.findFirst() returns Optional<Person> — we map to age and default to 0
        var age = people.stream()
                .filter(e -> e.getName().equalsIgnoreCase("jack"))
                .findFirst()
                .map(Person::getAge)
                .orElse(0);

        System.out.println("age = " + age);
        assertThat(age).isEqualTo(15);
    }

    @Test
    @DisplayName("Given a list of persons, get 0 when person does not exist")
    public void getAgeOfNonExistingPerson() {
        // findFirst() returns Optional.empty() when no element matches the filter
        var age = people.stream()
                .filter(e -> e.getName().equalsIgnoreCase("jacke"))
                .findFirst()
                .map(Person::getAge)
                .orElse(0);

        System.out.println("age = " + age);
        assertThat(age).isEqualTo(0);
    }

    @Test
    @DisplayName("Given a list of names, collect their ages — two approaches compared")
    public void shouldGetListOfAgesForListOfNames() {
        var names = List.of("Jack", "Sara");

        // Approach A (Java 8 style): filter(isPresent) + map(get) — verbose but explicit
        var agesViaFilter = names.stream()
                .map(Person::findByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Person::getAge)
                .toList();  // Java 16+: returns an unmodifiable List directly
        System.out.println("agesViaFilter = " + agesViaFilter);

        // Approach B (preferred): flatMap(Optional::stream) — empty Optionals vanish automatically
        var agesViaFlatMap = names.stream()
                .map(Person::findByName)
                .flatMap(Optional::stream)
                .map(Person::getAge)
                .toList();  // Java 16+: concise, returns unmodifiable List
        System.out.println("agesViaFlatMap = " + agesViaFlatMap);

        assertThat(agesViaFilter).containsExactlyInAnyOrder(15, 20);
        assertThat(agesViaFlatMap).containsExactlyInAnyOrder(15, 20);
    }
}
