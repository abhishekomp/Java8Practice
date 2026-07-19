package functionalInterface.function.myimpl;

import java.util.function.Function;

/**
 * Demonstrates the Java 8 {@link Function} functional interface.
 *
 * Key concepts shown:
 *
 * 1. Implementing Function<T,R> as a class (PerformSquare)
 * 2. andThen(Function after)  — pipeline left-to-right:  f → g
 * 3. compose(Function before) — pipeline right-to-left:  g → f
 *
 * Both andThen and compose produce the same result here but are
 * constructed from opposite perspectives — see comments below.
 */
public class Main {
    public static void main(String[] args) {

        // ── 1. Calling a Function implementation directly ─────────────────
        Function<Long, Long> squareFunc = new PerformSquare();
        var result = squareFunc.apply(6L);   // 6 * 6 = 36
        System.out.println("apply = " + result);  // apply = 36

        // ── 2. andThen — compose left-to-right ────────────────────────────
        // squareFunc.andThen(stringResult): Long → Long → String
        // "first square it, AND THEN turn it into a label"
        Function<Long, String> stringResult = num -> num + " is the result";
        var andThenResult = squareFunc.andThen(stringResult).apply(6L);
        System.out.println("andThen = " + andThenResult);  // andThen = 36 is the result

        // ── 3. compose — compose right-to-left ───────────────────────────
        // stringResult.compose(squareFunc): Long → Long → String
        // "to produce a label, first COMPOSE with squareFunc (squareFunc runs first)"
        // Equivalent to squareFunc.andThen(stringResult) but reads differently.
        var composeResult = stringResult.compose(squareFunc).apply(6L);
        System.out.println("compose = " + composeResult); // compose = 36 is the result
    }
}
