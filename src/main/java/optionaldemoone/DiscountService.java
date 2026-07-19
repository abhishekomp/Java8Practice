package optionaldemoone;

import java.util.Optional;

import static java.util.Optional.*;

public class DiscountService {

    /**
     * Builds a human-readable discount line for the given customer.
     *
     * Uses an Optional pipeline:
     *   getMemberCard()                  → Optional<MemberCard>  (empty if no card)
     *   .flatMap(getDiscountPercentage)  → Optional<Integer>     (empty if 0% discount)
     *   .map(format message)             → Optional<String>
     *   .orElse(default message)         → String
     *
     * No null checks, no if/else — the absent cases fall through to orElse automatically.
     */
    public String getDiscountLine(Customer customer) {
        return customer.getMemberCard()
                .flatMap(this::getDiscountPercentage)
                .map(d -> customer.getName() + " gets Discount% " + d)
                .orElse(customer.getName() + " gets Discount% 0");
    }

    /**
     * Returns the discount percentage for a given card wrapped in Optional.
     * Returns Optional.empty() when the card does not qualify for any discount.
     *
     * Uses a Java 14+ switch expression — more concise than if/else chains and
     * exhaustive (the compiler ensures all cases are covered).
     *
     * Points tiers:
     *   >= 100  → 10%
     *   >= 50   → 5%
     *   anything else → no discount (empty Optional)
     */
    private Optional<Integer> getDiscountPercentage(MemberCard memberCard) {
        // Switch expression with arrow syntax — no fall-through, no break needed
        return switch (memberCard.getPoints() / 50) {  // bucket: 0=<50, 1=50-99, 2+=100+
            case 0       -> empty();         // < 50 points  → no discount
            case 1       -> of(5);           // 50–99 points → 5%
            default      -> of(10);          // 100+ points  → 10%
        };
    }
}
