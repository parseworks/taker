/*
 * Copyright (c) 2026 jason bailey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Chars;

import io.github.parseworks.taker.parsers.Combinators;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Chars.chr;
import static io.github.parseworks.taker.parsers.Numeric.doubleValue;
import static io.github.parseworks.taker.parsers.Numeric.number;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssociativityTest {


    @Test
    public void testDouble() {
        Taker<Double> addition = number.then(chr('+')).then(number).map((left, op, right) -> Double.sum(left, right));


        var result = addition.parse(Input.of("2+3"));
        assertEquals(5.0, result.value(), "Expected 5.0 but got " + result.value());
    }


    @Test
    public void testAssociativity() {
        Taker<Double> expression = Taker.ref();
        Taker<Double> term = Taker.ref();

        Taker<Double> addition = term.then(chr('+')).then(expression).map((left, op, right) -> Double.sum(left, right));
        Taker<Double> multiplication = doubleValue.then(chr('*')).then(term).map((left, op, right) -> left * right);
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
        Taker<Double> expression = Taker.ref();
        Taker<Double> term = Taker.ref();

        Taker<Double> addition = term.then(chr('+')).then(expression).map((left, op, right) -> Double.sum(left, right));
        Taker<Double> multiplication = doubleValue.then(chr('*')).then(term).map((left, op, right) -> left * right);
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
        Taker<Long> leftAssocParser = number.chainLeftZeroOrMore(chr('+').as(add), 0l);

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