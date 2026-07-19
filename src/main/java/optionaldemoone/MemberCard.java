package optionaldemoone;

/**
 * A customer's loyalty card that tracks reward points.
 *
 * The points balance determines which discount tier the customer qualifies for:
 *   >= 100 points → 10% discount
 *   >= 50  points → 5%  discount
 *   < 50   points → no  discount
 *
 * @see DiscountService
 */
public class MemberCard {

    private int points;

    public MemberCard(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }
}
