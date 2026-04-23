package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Combinators;
import io.github.parseworks.taker.parsers.Numeric;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Lexical.chr;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CalculatorParserTest
 * deliberately constructs a parser with recursion. This is to demonstrate the
 * ability for the parser to detect infinite recursion and fail gracefully.
 */
public class RecursionProtectionTest {

    public static Taker<Integer> term = Taker.ref();
    public static Taker<Integer> expression = term.chainLeftOneOrMore(operator());
    public static Taker<Integer> term2 = Combinators.oneOf(List.of(
            term,
            number(),
            expression.between('(', ')')
    ));

    public static Taker<Integer> number() {
        return Numeric.numeric.map(Character::getNumericValue);
    }

    public static Taker<BinaryOperator<Integer>> operator() {
        return Combinators.oneOf(List.of(
                chr('+').as(Integer::sum),
                chr('-').as((a, b) -> a - b),
                chr('*').as((a, b) -> a * b),
                chr('/').as((a, b) -> a / b)
        ));
    }

    @Test
    public void calculator() {
        term.set(term2);
        Input input = Input.of("3+(2*4)-5");
        Result<Integer> result = expression.parse(input);
        assertEquals(6, result.value());
    }

}
