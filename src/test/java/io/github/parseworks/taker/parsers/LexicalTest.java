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

package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.*;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.CharPredicate.notAnyOf;
import static io.github.parseworks.taker.parsers.Chars.*;
import static io.github.parseworks.taker.parsers.Lexical.*;
import static io.github.parseworks.taker.parsers.Lexical.takeUntil;
import static org.junit.jupiter.api.Assertions.*;

public class LexicalTest {

    @Test
    void alphaNumericMatchesAsciiLettersAndDigits() {
        assertTrue(alphaNumeric.parse(Input.of("a")).matches());
        assertTrue(alphaNumeric.parse(Input.of("1")).matches());
        assertFalse(alphaNumeric.parse(Input.of("!")).matches());
    }

    @Test
    void wordMatchesLettersOnly() {
        assertTrue(word.parse(Input.of("hello")).matches());
        assertFalse(word.parseAll(Input.of("hello1")).matches());
        assertFalse(word.parse(Input.of("123")).matches());
    }

    @Test
    void chrMatchesExactCharacter() {
        Taker<Character> parser = chr('!');

        assertTrue(parser.parse("!").matches());
        assertEquals('!', parser.parse("!").value());
        assertFalse(parser.parse("?").matches());
    }

    @Test
    void chrIgnoreCaseMatchesEitherCaseAndReturnsSourceCharacter() {
        Taker<Character> parser = chrIgnoreCase('x');

        assertEquals('x', parser.parse("x").value());
        assertEquals('X', parser.parse("X").value());
        assertFalse(parser.parse("y").matches());
    }

    @Test
    void chrMatchesPredicate() {
        CharPredicate isVowel = c -> "aeiouAEIOU".indexOf(c) >= 0;
        Taker<Character> parser = chr(isVowel);

        assertTrue(parser.parse("a").matches());
        assertTrue(parser.parse("E").matches());
        assertFalse(parser.parse("x").matches());
    }

    @Test
    void stringMatchesExactPrefix() {
        Taker<String> parser = string("hello");

        assertTrue(parser.parse("hello world").matches());
        assertEquals("hello", parser.parse("hello").value());
        assertFalse(parser.parse("hell").matches());
        assertFalse(parser.parse("world").matches());
        assertTrue(string("").parse("").matches());
    }

    @Test
    void stringIgnoreCaseMatchesPrefixAndReturnsExpectedString() {
        Taker<String> parser = stringIgnoreCase("hello");

        Result<String> result = parser.parse("HeLLo world");
        assertTrue(result.matches());
        assertEquals("hello", result.value());
        assertEquals(5, result.input().position());
        assertFalse(parser.parse("help").matches());
        assertTrue(stringIgnoreCase("").parse("").matches());
    }

    @Test
    void stringFailuresReportEscapedExpectedCharacterAtFailurePosition() {
        Failure<String> truncated = (Failure<String>) string("abc").parse("ab");
        assertEquals(2, truncated.input().position());
        assertEquals("'c'", truncated.expected());

        Failure<String> tabMismatch = (Failure<String>) string("a\t").parse("ax");
        assertEquals(1, tabMismatch.input().position());
        assertEquals("'\\t'", tabMismatch.expected());

        Failure<String> newlineMismatch = (Failure<String>) stringIgnoreCase("\n").parse("x");
        assertEquals(0, newlineMismatch.input().position());
        assertEquals("'\\n'", newlineMismatch.expected());

        Failure<Character> charMismatch = (Failure<Character>) chr('\n').parse("x");
        assertEquals(0, charMismatch.input().position());
        assertEquals("'\\n'", charMismatch.expected());

        Failure<String> quoteMismatch = (Failure<String>) escapedString('"', '\\', java.util.Map.of()).parse("x");
        assertEquals(0, quoteMismatch.input().position());
        assertEquals("'\\\"'", quoteMismatch.expected());
    }

    @Test
    void oneOfStringMatchesAnyCharacterInSet() {
        Taker<Character> parser = oneOf("0123456789");

        for (char c = '0'; c <= '9'; c++) {
            assertTrue(parser.parse(String.valueOf(c)).matches());
            assertEquals(c, parser.parse(String.valueOf(c)).value());
        }
        assertFalse(parser.parse("a").matches());
    }

    @Test
    void oneOfIgnoreCaseMatchesAnyCharacterInSetIgnoringCase() {
        Taker<Character> parser = oneOfIgnoreCase("abc");

        assertEquals('a', parser.parse("a").value());
        assertEquals('B', parser.parse("B").value());
        assertEquals('c', parser.parse("c").value());
        assertFalse(parser.parse("d").matches());
    }

    @Test
    void regexMatchesAtCurrentInputPosition() {
        Taker<String> letters = regex("[A-Za-z]+");

        Result<String> result = letters.parse("hello123");
        assertTrue(result.matches());
        assertEquals("hello", result.value());
        assertTrue(letters.parse("abc").matches());
        assertFalse(letters.parse("123").matches());
    }

    @Test
    void takeWhileConsumesOneOrMoreMatchingCharacters() {
        Taker<String> parser = takeWhile(CharPredicate.digit);

        assertEquals("12345", parser.parse("12345").value());
        assertEquals("123", parser.parse("123abc").value());
        assertFalse(parser.parse("abc123").matches());
        assertFalse(parser.parse("").matches());
        assertEquals("123", parser.parse("123abc456").value());
    }

    @Test
    void takeWhileSupportsCharacterSets() {
        Taker<String> parser = takeWhile(notAnyOf("=/>\t\n\r\f"));
        Result<String> result = parser.parse(Input.of("div>"));

        assertTrue(result.matches());
        assertEquals("div", result.value());
        assertEquals('>', result.input().current());
    }

    @Test
    void failedTakeWhileCanBeRepeatedWithoutInfiniteLoop() {
        Taker<String> emptyParser = takeWhile(c -> false);

        Result<java.util.List<String>> result = emptyParser.zeroOrMore().parse(Input.of("abc"));

        assertTrue(result.matches());
        assertTrue(result.value().isEmpty());
    }

    @Test
    void takeUntilStringStopsBeforeNeedle() {
        Taker<String> parser = takeUntil("-->");
        Result<String> result = parser.parse(Input.of("comment-->"));

        assertTrue(result.matches());
        assertEquals("comment", result.value());
        assertEquals(7, result.input().position());
    }

    @Test
    void takeUntilStringConsumesToEofWhenNeedleIsMissing() {
        Taker<String> parser = takeUntil("-->");
        Result<String> result = parser.parse(Input.of("comment"));

        assertTrue(result.matches());
        assertEquals("comment", result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    void takeUntilEmptyNeedleSucceedsWithoutConsuming() {
        Taker<String> parser = takeUntil("");
        Result<String> result = parser.parse(Input.of("anything"));

        assertTrue(result.matches());
        assertEquals("", result.value());
        assertEquals(0, result.input().position());
    }

    @Test
    void takeUntilPredicateStopsBeforeFirstMatch() {
        Taker<String> parser = Chars.takeUntil(c -> c == '>');
        Result<String> result = parser.parse(Input.of("abc>def"));

        assertTrue(result.matches());
        assertEquals("abc", result.value());
        assertEquals(3, result.input().position());
    }

    @Test
    void takeUntilPredicateConsumesToEofWhenMissing() {
        Taker<String> parser = Chars.takeUntil(c -> c == '>');
        Result<String> result = parser.parse(Input.of("abcdef"));

        assertTrue(result.matches());
        assertEquals("abcdef", result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    void takeUntilPredicateCanMatchWhitespace() {
        Taker<String> parser = Chars.takeUntil(Character::isWhitespace);
        Result<String> result = parser.parse(Input.of("hello world"));

        assertTrue(result.matches());
        assertEquals("hello", result.value());
        assertEquals(5, result.input().position());
    }

    @Test
    void takeUntilPredicateCanSucceedAtCurrentPosition() {
        Taker<String> parser = Chars.takeUntil(c -> c == 'a');
        Result<String> result = parser.parse(Input.of("abc"));

        assertTrue(result.matches());
        assertEquals("", result.value());
        assertEquals(0, result.input().position());
    }

    @Test
    void trimSkipsAsciiSpacesOnly() {
        Taker<Character> parser = trim(chr('a'));

        Result<Character> spaced = parser.parse("  a  ");
        assertTrue(spaced.matches());
        assertEquals('a', spaced.value());
        assertTrue(spaced.input().isEof());

        Result<Character> beforeNewline = parser.parse("\na");
        assertFalse(beforeNewline.matches());
        assertEquals(0, beforeNewline.input().position());

        Result<Character> afterNewline = parser.parse(" a\n");
        assertTrue(afterNewline.matches());
        assertEquals('a', afterNewline.value());
        assertEquals(2, afterNewline.input().position());
    }

    @Test
    void trimSpacesIsExplicitAliasForSpaceOnlyTrim() {
        Taker<Character> parser = trimSpaces(chr('a'));

        Result<Character> result = parser.parse("  a  ");
        assertTrue(result.matches());
        assertEquals('a', result.value());
        assertTrue(result.input().isEof());
        assertFalse(parser.parse("\na").matches());
    }

    @Test
    void trimWhitespaceSkipsTabsAndNewlines() {
        Taker<Character> parser = trimWhitespace(chr('a'));

        Result<Character> result = parser.parse("\t\na \r\n");
        assertTrue(result.matches());
        assertEquals('a', result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    void lexemeUsesCallerDefinedIgnoredInput() {
        Taker<Character> parser = lexeme(chr('a'), chr('\t').oneOrMore());

        Result<Character> tabs = parser.parse("\t\ta\t");
        assertTrue(tabs.matches());
        assertEquals('a', tabs.value());
        assertTrue(tabs.input().isEof());

        Result<Character> spaces = parser.parse(" a");
        assertFalse(spaces.matches());
    }
}
