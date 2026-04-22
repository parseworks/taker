package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Combinators;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Numeric.numeric;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculatorParserTest {

    public static Parser<Integer> term = Parser.ref();
    public static Parser<Integer> expression = term.chainLeftZeroOrMore(operator(), 0);
    public static Parser<Integer> term2 = Combinators.oneOf(List.of(
            number(),
            expression.between('(', ')')));

    public static Parser<Integer> number() {
        return numeric.map(Character::getNumericValue);
    }

    public static Parser<BinaryOperator<Integer>> operator() {
        return Combinators.oneOf(List.of(
                chr('+').map(op -> Integer::sum),
                chr('-').map(op -> (a, b) -> a - b),
                chr('*').map(op -> (a, b) -> a * b),
                chr('/').map(op -> (a, b) -> a / b)
        ));
    }

    @Test
    public void calculator() {
        term.set(term2);
        Input input = Input.of("3+(2*4)-5");
        Result<Integer> result = expression.parse(input);
        assertEquals(3 + (2 * 4) - 5, result.value(), "Parsing failed for expression: " + input);
    }


}