package optionaldemoone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DiscountService — verifies the Optional pipeline produces
 * the correct discount line for each customer scenario.
 *
 * Tags follow a release-based pattern (Rel1, Rel2, ...) so each release's
 * tests can be run in isolation:
 *   mvn test -Dgroups="Rel1"
 *   mvn test -Dgroups="Rel1 | Rel2 | Rel3 | Rel4"
 */
class DiscountServiceTest {

    // Shared service instance — DiscountService is stateless, safe to reuse
    private final DiscountService service = new DiscountService();

    @Test
    @Tag("Rel1")
    @DisplayName("Customer with 100 points gets 10% discount")
    void shouldGetDiscountLineAs10percentForCustomerWithCardPoint100() {
        // Read optional system property — useful for passing environment context from CI
        var inParam = System.getProperty("inParameter", "DefaultValue");
        System.out.println("inParam = " + inParam);

        var discountLine = service.getDiscountLine(new Customer("Jack", new MemberCard(100)));

        System.out.println(discountLine);
        assertThat(discountLine).isEqualTo("Jack gets Discount% 10");
    }

    @Test
    @Tag("Rel2")
    @DisplayName("Customer with 50 points gets 5% discount")
    void shouldGetDiscountLineAs5percentForCustomerWithCardPoint50() {
        var discountLine = service.getDiscountLine(new Customer("Sara", new MemberCard(50)));

        System.out.println(discountLine);
        assertThat(discountLine).isEqualTo("Sara gets Discount% 5");
    }

    @Test
    @Tag("Rel3")
    @DisplayName("Customer with 10 points gets 0% discount (below threshold)")
    void shouldGetDiscountLineAs0percentForCustomerWithCardPoint10() {
        var discountLine = service.getDiscountLine(new Customer("Matt", new MemberCard(10)));

        System.out.println(discountLine);
        assertThat(discountLine).isEqualTo("Matt gets Discount% 0");
    }

    @Test
    @Tag("Rel4")
    @DisplayName("Customer with no card gets 0% discount (Optional.empty path)")
    void shouldGetDiscountLineAsZeroPercentForCustomerWithNoCard() {
        // No MemberCard → getMemberCard() returns Optional.empty()
        // → the whole Optional pipeline short-circuits to orElse("... 0")
        var discountLine = service.getDiscountLine(new Customer("Bill"));

        System.out.println(discountLine);
        assertThat(discountLine).isEqualTo("Bill gets Discount% 0");
    }
}