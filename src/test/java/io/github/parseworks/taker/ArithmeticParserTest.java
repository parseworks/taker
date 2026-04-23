package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Combinators;
import org.junit.jupiter.api.Test;

import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.trim;
import static io.github.parseworks.taker.parsers.Numeric.doubleValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for arithmetic expression parsing using the Combinators library.
 */
public class ArithmeticParserTest {

    /**
     * A reference to a parser for terms in arithmetic expressions.
     */
    public static Taker<Double> term = Taker.ref();

    public static Taker<Double> trimmedTerm = trim(term);

    /**
     * A parser for arithmetic expressions, supporting addition and subtraction.
     */
    public static Taker<Double> expression = trimmedTerm
            .then(Combinators.oneOf(
                    chr('+').as(Double::sum),
                    chr('-').as((BinaryOperator<Double>) (left, right) -> left - right)
            )).then(trimmedTerm).map((left, op, right) -> op.apply(left, right)).or(trimmedTerm);

    /**
     * A parser for factors in arithmetic expressions, supporting nested expressions and double values.
     */
    public static Taker<Double> factor = trim(Combinators.oneOf(
            doubleValue,
            expression.between('(', ')')
    ));

    static {
        term.set(factor
                .then(Combinators.oneOf(
                        chr('*').as((left, right) -> left * right),
                        chr('/').as((BinaryOperator<Double>) (left, right) -> left / right)
                )).then(factor).map(left -> op -> right -> op.apply(left, right)).or(trim(factor)));
    }

    /**
     * Tests the parsing of a complex arithmetic expression.
     */
    @Test
    public void mathTest() {
        String input = "3 + 5 * (2 * -8)";
        double result = expression.parse(Input.of(input)).value();
        assertEquals(-77, result, "Parsing failed for expression: " + input);
    }
}