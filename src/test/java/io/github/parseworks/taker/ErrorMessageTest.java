package io.github.parseworks.taker;

import org.junit.jupiter.api.Test;

import static io.github.parseworks.taker.parsers.Lexical.chr;
import static io.github.parseworks.taker.parsers.Lexical.string;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for formatted parser error messages. */
public class ErrorMessageTest {

    @Test
    public void testUnexpectedEofError() {
        // Test unexpected EOF error when input ends prematurely
        Taker<Character> parser = chr('a').then(chr('b')).map((a, b) -> a);
        Result<Character> result = parser.parse("a");

        assertFalse(result.matches());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Unexpected end of input") || 
                   errorMessage.contains("reached end of input"));
    }

    @Test
    public void testExpectedEofError() {
        // Test expected EOF error when input has trailing content
        Taker<Character> parser = chr('a');
        Result<Character> result = parser.parseAll("ab");

        assertFalse(result.matches());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Expected end of input") || 
                   errorMessage.contains("expected end of input"));
    }

    @Test
    public void testFoundCharactersAreEscapedInFormattedErrors() {
        Result<String> newline = string("a").parse("\n");
        assertFalse(newline.matches());
        assertTrue(newline.error().contains("found '\\n'"));

        Result<String> tab = string("a").parse("\t");
        assertFalse(tab.matches());
        assertTrue(tab.error().contains("found '\\t'"));
    }

}
