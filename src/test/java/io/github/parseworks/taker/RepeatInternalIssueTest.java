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

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.parseworks.taker.parsers.Combinators.commit;
import static io.github.parseworks.taker.parsers.Chars.chr;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepeatInternalIssueTest {

    @Test
    public void testRepeatInternalFailsWhenRepetitionFailsCritically() {
        // Case 1: Complex item fails partially
        Taker<String> ab = commit(string("A").then(string("B")).map(a -> b -> a + b));
        Taker<List<String>> manyAB = ab.oneOrMore();
        
        // Input "ABA" -> "AB" matches, then "A" matches, then "B" fails.
        // The second "ab" fails at position 3 (the end of input).
        // Since it advanced from position 2 to 3, it's a partial match.
        Result<List<String>> res2 = manyAB.parse(Input.of("ABA"));

        assertFalse(res2.matches(), "Should fail because item failed partially");
        assertTrue(res2.error().contains("expected 'B'"), "Error should mention 'B'");

        // Case 2: oneOrMoreUntil without reaching terminator
        Taker<String> simpleAB = string("AB");
        Taker<List<String>> manyABUntilExcl = simpleAB.oneOrMoreUntil(chr('!'));
        
        // Input "ABC" -> "AB" matches, then "C" is not "!" and not "AB".
        // It should fail because "!" was not reached.
        Result<List<String>> res4 = manyABUntilExcl.parse(Input.of("ABC"));
        assertFalse(res4.matches(), "Should fail because terminator was not reached");
    }
}
