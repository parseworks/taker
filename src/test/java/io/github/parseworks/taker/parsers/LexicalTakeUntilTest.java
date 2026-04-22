package io.github.parseworks.taker.parsers;

import io.github.parseworks.taker.Input;
import io.github.parseworks.taker.Parser;
import io.github.parseworks.taker.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LexicalTakeUntilTest {

    @Test
    public void testTakeUntil() {
        Parser<String> parser = Lexical.takeUntil("-->");
        Result<String> result = parser.parse(Input.of("comment-->"));
        assertTrue(result.matches());
        assertEquals("comment", result.value());
        assertEquals(7, result.input().position());
    }

    @Test
    public void testTakeUntilNotFound() {
        Parser<String> parser = Lexical.takeUntil("-->");
        Result<String> result = parser.parse(Input.of("comment"));
        assertTrue(result.matches());
        assertEquals("comment", result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    public void testTakeUntilEmptyNeedle() {
        Parser<String> parser = Lexical.takeUntil("");
        Result<String> result = parser.parse(Input.of("anything"));
        assertTrue(result.matches());
        assertEquals("", result.value());
        assertEquals(0, result.input().position());
    }

    @Test
    public void testTakeUntilPredicate() {
        Parser<String> parser = Lexical.takeUntil(c -> c == '>');
        Result<String> result = parser.parse(Input.of("abc>def"));
        assertTrue(result.matches());
        assertEquals("abc", result.value());
        assertEquals(3, result.input().position());
    }

    @Test
    public void testTakeUntilPredicateNotFound() {
        Parser<String> parser = Lexical.takeUntil(c -> c == '>');
        Result<String> result = parser.parse(Input.of("abcdef"));
        assertTrue(result.matches());
        assertEquals("abcdef", result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    public void testTakeUntilPredicateWhitespace() {
        Parser<String> parser = Lexical.takeUntil(Character::isWhitespace);
        Result<String> result = parser.parse(Input.of("hello world"));
        assertTrue(result.matches());
        assertEquals("hello", result.value());
        assertEquals(5, result.input().position());
    }

    @Test
    public void testTakeUntilPredicateStartWithDelimiter() {
        Parser<String> parser = Lexical.takeUntil(c -> c == 'a');
        Result<String> result = parser.parse(Input.of("abc"));
        assertTrue(result.matches());
        assertEquals("", result.value());
        assertEquals(0, result.input().position());
    }
}
