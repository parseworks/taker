package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.CharPredicate;
import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Result;
import io.github.parseworks.taker.Taker;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.CharPredicate.noneOf;
import static io.github.parseworks.taker.parsers.Lexical.alphaNumeric;
import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.oneOf;
import static io.github.parseworks.taker.parsers.Lexical.regex;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static io.github.parseworks.taker.parsers.Lexical.takeUntil;
import static io.github.parseworks.taker.parsers.Lexical.takeWhile;
import static io.github.parseworks.taker.parsers.Lexical.lexeme;
import static io.github.parseworks.taker.parsers.Lexical.trim;
import static io.github.parseworks.taker.parsers.Lexical.trimSpaces;
import static io.github.parseworks.taker.parsers.Lexical.trimWhitespace;
import static io.github.parseworks.taker.parsers.Lexical.word;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void oneOfStringMatchesAnyCharacterInSet() {
        Taker<Character> parser = oneOf("0123456789");

        for (char c = '0'; c <= '9'; c++) {
            assertTrue(parser.parse(String.valueOf(c)).matches());
            assertEquals(c, parser.parse(String.valueOf(c)).value());
        }
        assertFalse(parser.parse("a").matches());
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
        Taker<String> parser = takeWhile(noneOf("=/>\t\n\r\f"));
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
        Taker<String> parser = takeUntil((CharPredicate) (c -> c == '>'));
        Result<String> result = parser.parse(Input.of("abc>def"));

        assertTrue(result.matches());
        assertEquals("abc", result.value());
        assertEquals(3, result.input().position());
    }

    @Test
    void takeUntilPredicateConsumesToEofWhenMissing() {
        Taker<String> parser = takeUntil((CharPredicate) (c -> c == '>'));
        Result<String> result = parser.parse(Input.of("abcdef"));

        assertTrue(result.matches());
        assertEquals("abcdef", result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    void takeUntilPredicateCanMatchWhitespace() {
        Taker<String> parser = takeUntil((CharPredicate) Character::isWhitespace);
        Result<String> result = parser.parse(Input.of("hello world"));

        assertTrue(result.matches());
        assertEquals("hello", result.value());
        assertEquals(5, result.input().position());
    }

    @Test
    void takeUntilPredicateCanSucceedAtCurrentPosition() {
        Taker<String> parser = takeUntil((CharPredicate) (c -> c == 'a'));
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
