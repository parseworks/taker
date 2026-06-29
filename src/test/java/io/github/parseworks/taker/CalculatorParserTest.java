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

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.taker.parsers.Chars.chr;
import static io.github.parseworks.taker.parsers.Numeric.numeric;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculatorParserTest {

    public static Taker<Integer> term = Taker.ref();
    public static Taker<Integer> expression = term.chainLeftZeroOrMore(operator(), 0);
    public static Taker<Integer> term2 = Combinators.oneOf(List.of(
            number(),
            expression.between('(', ')')));

    public static Taker<Integer> number() {
        return numeric.map(Character::getNumericValue);
    }

    public static Taker<BinaryOperator<Integer>> operator() {
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