package io.github.parseworks.taker;

import io.github.parseworks.taker.parsers.Lexical;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextParsersTest {

    @Test
    public void testAlphaNum() {
        Taker<Character> parser = Lexical.alphaNumeric;
        assertTrue(parser.parse(Input.of("a")).matches());
        assertTrue(parser.parse(Input.of("1")).matches());
        assertFalse(parser.parse(Input.of("!")).matches());
    }

    @Test
    public void testWord() {
        Taker<String> parser = Lexical.word;
        assertTrue(parser.parse(Input.of("hello")).matches());
        assertFalse(parser.parseAll(Input.of("hello1")).matches());
        assertFalse(parser.parse(Input.of("123")).matches());
    }

    // Note: integer parsing coverage lives in NumericParsersTest
}