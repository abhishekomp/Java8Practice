package optionaldemoone;

import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Represents a customer who may or may not hold a MemberCard.
 *
 * The getMemberCard() method deliberately returns Optional<MemberCard>
 * rather than a nullable MemberCard.  This signals to every caller that
 * the card might be absent and forces them to handle that case — making
 * NullPointerException impossible here.
 */
public class Customer {
    private String name;
    private MemberCard memberCard;

    /** Customer with a loyalty card. */
    public Customer(String name, MemberCard memberCard) {
        this.name = name;
        this.memberCard = memberCard;
    }

    /** Customer without a loyalty card. */
    public Customer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the customer's MemberCard, if they have one.
     *
     * ofNullable wraps memberCard safely:
     *   - If memberCard is non-null → Optional.of(memberCard)
     *   - If memberCard is null     → Optional.empty()
     */
    public Optional<MemberCard> getMemberCard() {
        return ofNullable(memberCard);
    }
}
