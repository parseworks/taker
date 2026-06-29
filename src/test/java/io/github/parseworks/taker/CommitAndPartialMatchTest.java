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
import io.github.parseworks.taker.results.PartialMatch;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.parseworks.taker.parsers.Combinators.commit;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static org.junit.jupiter.api.Assertions.*;

public class CommitAndPartialMatchTest {

    @Test
    void parseAllReportsTrailingInputAsPartial() {
        Taker<String> abc = Lexical.string("abc");
        Input input = Input.of("abcdef");

        Result<String> prefix = abc.parse(input, false);
        assertTrue(prefix.matches());
        assertNotSame(ResultType.PARTIAL, prefix.type());
        assertEquals("abc", prefix.value());
        assertEquals(3, prefix.input().position());

        Result<String> full = abc.parse(input, true);
        assertFalse(full.matches());
        assertEquals(ResultType.PARTIAL, full.type());
        assertThrows(RuntimeException.class, full::value);
        assertEquals(3, full.input().position());
    }

    @Test
    void parseAllSucceedsWhenInputIsFullyConsumed() {
        Result<String> result = Lexical.string("abc").parse(Input.of("abc"), true);

        assertTrue(result.matches());
        assertNotSame(ResultType.PARTIAL, result.type());
        assertEquals("abc", result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    void noMatchIsNotPartialWithoutCommit() {
        Result<String> result = Lexical.string("abc").apply(Input.of("abx"));

        assertFalse(result.matches());
        assertEquals(ResultType.NO_MATCH, result.type());
        assertEquals(2, result.input().position(), "string should report how far it matched");
        assertThrows(RuntimeException.class, result::value);
    }

    @Test
    void committedStringFailureBecomesPartialAfterConsumingInput() {
        Taker<String> parser = commit(string("abc"));
        Result<String> result = parser.apply(Input.of("abx"));

        assertFalse(result.matches());
        assertEquals(ResultType.PARTIAL, result.type());
        assertEquals(2, result.input().position());
    }

    @Test
    void committedPrefixAtEofBecomesPartial() {
        Result<String> result = commit(Lexical.string("abcd")).parse(Input.of("abc"));

        assertEquals(ResultType.PARTIAL, result.type());
        assertInstanceOf(PartialMatch.class, result);
    }

    @Test
    void committedSequenceFailureBecomesPartial() {
        Taker<Character> parser = commit(
            Chars.chr('a').skipThen(Chars.chr('b')).skipThen(Chars.chr('c'))
        );

        Result<Character> result = parser.apply(Input.of("abd"));

        assertEquals(ResultType.PARTIAL, result.type());
    }

    @Test
    void committedRepeatFailureBecomesPartial() {
        Taker<List<Character>> parser = commit(Chars.chr('a').repeat(3));

        Result<List<Character>> result = parser.apply(Input.of("aab"));

        assertEquals(ResultType.PARTIAL, result.type());
    }

    @Test
    void uncommittedStringReportsFarthestFailureWithoutHardCommit() {
        Input testInput = Input.of("prefixabcX").skip(6);

        Result<String> result = Lexical.string("abcd").apply(testInput);

        assertFalse(result.matches());
        assertEquals(ResultType.NO_MATCH, result.type());
        assertEquals(9, result.input().position(), "Lexical.string should report where it failed");
    }
}
