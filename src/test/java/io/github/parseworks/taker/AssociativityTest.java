package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Combinators;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Combinators.attempt;
import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Numeric.doubleValue;
import static io.github.parseworks.taker.parsers.Numeric.number;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssociativityTest {


    @Test
    public void testDouble() {
        Parser<Double> addition = number.then(chr('+')).then(number).map((left, op, right) -> Double.sum(left, right));


        var result = addition.parse(Input.of("2+3"));
        assertEquals(5.0, result.value(), "Expected 5.0 but got " + result.value());
    }


    @Test
    public void testAssociativity() {
        Parser<Double> expression = Parser.ref();
        Parser<Double> term = Parser.ref();

        Parser<Double> addition = attempt(term.then(chr('+')).then(expression).map((left, op, right) -> Double.sum(left, right)));
        Parser<Double> multiplication = attempt(doubleValue.then(chr('*')).then(term).map((left, op, right) -> left * right));
        term.set(multiplication.or(doubleValue));
        expression.set(Combinators.oneOf(List.of(
                addition,
                term
        )));

        var result = expression.parse(Input.of("2*3+4"));
        assertEquals(10.0, result.value());
        result = expression.parse(Input.of("2+3*4"));
        assertEquals(14.0, result.value());
    }

    @Test
    public void associativity2() {
        Parser<Double> expression = Parser.ref();
        Parser<Double> term = Parser.ref();

        Parser<Double> addition = attempt(term.then(chr('+')).then(expression).map((left, op, right) -> Double.sum(left, right)));
        Parser<Double> multiplication = attempt(doubleValue.then(chr('*')).then(term).map((left, op, right) -> left * right));
        term.set(multiplication.or(doubleValue));
        expression.set(Combinators.oneOf(List.of(
                addition,
                term
        )));

        var result = expression.parse(Input.of("2*3+4"));
        assertEquals(10.0, result.value());
        result = expression.parse(Input.of("2+3*4"));
        assertEquals(14.0, result.value());
    }

    @Test
    public void testLeftAssociative() {
        BinaryOperator<Long> add = Long::sum;
        Parser<Long> leftAssocParser = number.chainLeftZeroOrMore(chr('+').as(add), 0l);

        String input = "1+2+3";
        Result<Long> result = leftAssocParser.parse(Input.of(input));
        assertEquals(6, result.value());
    }

    @Test
    public void testRightAssociative() {
        BinaryOperator<Long> power = (a, b) -> (long) Math.pow(a, b);
        var rightAssocParser = number.chainRightOneOrMore(chr('^').as(power));

        String input = "2^3^2";
        Result<Long> result = rightAssocParser.parse(Input.of(input));
        assertEquals(512, result.value());
    }

}