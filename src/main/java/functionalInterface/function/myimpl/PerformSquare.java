package functionalInterface.function.myimpl;

import java.util.function.Function;

/**
 * A concrete implementation of {@link Function}{@code <Long, Long>} that squares its input.
 *
 * This class exists to demonstrate that any class implementing a functional interface
 * can be used wherever a lambda or method reference would work.
 * In practice, for a one-liner like this you would just write:
 *
 *   {@code Function<Long, Long> square = n -> n * n;}
 *
 * A class implementation is preferred when the logic is complex, stateful,
 * or needs to be independently unit-tested.
 */
public class PerformSquare implements Function<Long, Long> {

    /**
     * Returns the square of the given number.
     *
     * @param n the input number
     * @return n * n
     */
    @Override
    public Long apply(Long n) {
        return n * n;
    }
}
