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

import io.github.parseworks.taker.parsers.Combinators;
import org.junit.jupiter.api.Test;

import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Chars.chr;
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