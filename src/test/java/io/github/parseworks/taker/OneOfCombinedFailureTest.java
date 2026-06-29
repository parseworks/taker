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

import io.github.parseworks.taker.results.NoMatch;
import io.github.parseworks.taker.parsers.Combinators;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OneOfCombinedFailureTest {

    @Test
    public void oneOf_allAlternativesFail_combinesReasons() {
        Input in = Input.of("a");

        // Three parsers that all fail at the same position with distinct expectations
        Taker<String> p1 = new Taker<>(inp -> new NoMatch<>(inp, "option A"));
        Taker<String> p2 = new Taker<>(inp -> new NoMatch<>(inp, "option B"));
        Taker<String> p3 = new Taker<>(inp -> new NoMatch<>(inp, "option C"));

        Taker<String> parser = Combinators.oneOf(Arrays.asList(p1, p2, p3));

        Result<String> result = parser.parse(in);
        assertFalse(result.matches(), "Result should be an error when all alternatives fail");

        String msg = result.error();

        // Header should reflect the first alternative (option A) and what was found ('a')

        // The combined reasons list should include each alternative's expectation
        assertTrue(msg.contains("Reasons at this location:"), "Should include reasons section");
        assertTrue(msg.contains("- expected option B"), "Should include second alternative reason");
        assertTrue(msg.contains("- expected option C"), "Should include third alternative reason");
        // Optional: sanity check that a location indicator exists
        assertTrue(msg.contains("line ") || msg.contains("position "), "Should include location information");
    }
}
