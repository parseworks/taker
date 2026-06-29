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

import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplyBuilderSkipThenTest {
    @Test
    public void testSkipThenSequencing() {
        StringBuilder sb = new StringBuilder();
        Taker<String> p1 = new Taker<>(in -> {
            sb.append("1");
            return Chars.chr('1').apply(in).map(c -> "1");
        });
        Taker<String> p2 = new Taker<>(in -> {
            sb.append("2");
            return Chars.chr('2').apply(in).map(c -> "2");
        });
        Taker<String> p3 = new Taker<>(in -> {
            sb.append("3");
            return Chars.chr('3').apply(in).map(c -> "3");
        });

        // For p1.then(p2).skipThen(p3):
        // p1.then(p2) is an ApplyBuilder<Character, String, String>
        // .skipThen(p3) returns a Taker<String> (the result of p3)
        // Everything before p3 is executed but results are skipped.
        Taker<String> combined = p1.then(p2).skipThen(p3);

        Result<String> result = combined.parse("123");
        assertTrue(result.matches(), "Should match 123");
        assertEquals("3", result.value());
        assertEquals("123", sb.toString(), "Execution order should be 1, 2, 3");
    }

    @Test
    public void testSkipThenWindBack() {
        Taker<Character> p1 = Chars.chr('1');
        Taker<Character> p2 = Chars.chr('2');
        Taker<Character> p3 = Chars.chr('3');

        // Should be: runs p1, p2, p3. If fails at p3, winds back to start.
        Taker<Character> combined = p1.then(p2).skipThen(p3);
        
        Taker<String> withOr = combined.map(String::valueOf).or(Lexical.string("12X"));

        // With input "12X", p1 and p2 succeed, p3 fails.
        // If it doesn't wind back, it will be a PartialMatch at 'X' (pos 2).
        // If it winds back, it will be a NoMatch at '1' (pos 0), and orElse will try "12X".
        Result<Character> resCombined = combined.parse("12X");
        assertTrue(!resCombined.matches());

        Result<String> result = withOr.parse("12X");
        assertTrue(result.matches(), "Should match 12X because skipThen should have wound back");
        assertEquals("12X", result.value());
    }
}
