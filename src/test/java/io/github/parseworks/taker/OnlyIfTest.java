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

import static org.junit.jupiter.api.Assertions.*;

public class OnlyIfTest {

    @Test
    public void testOnlyIfWithCharPredicate() {
        Taker<Character> parser = Chars.chr('a').onlyIf(CharPredicate.is('a'));
        Result<Character> result = parser.parse("a");
        assertTrue(result.matches(), "Should match 'a'");
        assertEquals('a', result.value());

        result = parser.parse("b");
        assertFalse(result.matches(), "Should not match 'b' because of onlyIf");
    }

    @Test
    public void testOnlyIfEOF() {
        Taker<Character> parser = Chars.chr('a').onlyIf(CharPredicate.is('a'));
        Result<Character> result = parser.parse("");
        assertFalse(result.matches(), "Should not match EOF");
    }
}
